package org.devinprogress.uniskinmod;

import com.google.common.collect.Iterables;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

/**
 * Provided the interface to interact with Mojang codes
 */
public class UniSkinCore {
    private final UniSkinConfig cfg;
    private final File localSkinDir;
    private final GameProfileRepository mojangProfileRepo;
    private final DynamicSkinManager dynamicSkinManager; // This is a dynamic skin cache, actually.

    public UniSkinCore(UniSkinConfig configuration, File localSkin) {
        cfg = configuration;
        if (localSkin.isDirectory()) {
            localSkinDir = localSkin;
        } else {
            localSkinDir = null;
            UniSkinMod.log.warn("Local skin dir not exists. Local skin load disabled");
        }
        for (String str : cfg.rootURIs) UniSkinMod.log.info("Added Root URI: {}", str);
        for (String str : cfg.legacySkinURIs) UniSkinMod.log.info("Added Skin URI: {}", str);
        for (String str : cfg.legacyCapeURIs) UniSkinMod.log.info("Added Cape URI: {}", str);
        UniSkinMod.log.info("Load genuine skins: {}", cfg.loadGenuineSkins? "Enabled": "Disabled");
        mojangProfileRepo = new YggdrasilAuthenticationService(Minecraft.getMinecraft().getProxy(), UUID.randomUUID().toString())
                .createProfileRepository();

        File assertDir;
        try {
            Field f;
            try {
                f = Minecraft.class.getDeclaredField("field_110446_Y");
            } catch (NoSuchFieldException ex) {
                f = null;
            }
            if (f == null) f = Minecraft.class.getDeclaredField("fileAssets");
            f.setAccessible(true);
            Object obj = f.get(Minecraft.getMinecraft());
            assertDir = (File) obj;
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Unable to determine skin cache dir.", ex);
        }
        if (assertDir == null) throw new RuntimeException("Unable to determine skin cache dir.");
        dynamicSkinManager = new DynamicSkinManager(cfg.rootURIs, localSkin, new File(assertDir, "skins"));
    }

    public void injectProfile(GameProfile profile) {
        if (profile.getName() == null) return;
        if (isComplete(profile)) return;
        if (profile.getId() == null || MojangTexturePayload.getOfflineUUID(profile.getName()).equals(profile.getId())) {
            injectGenuineProfile(profile);
            if (isComplete(profile)) return;
        }
        injectLocalProfile(profile);
        if (isComplete(profile)) return;
        injectServerProfile(profile);
        if (isComplete(profile)) return;
        injectPiracyProfile(profile);
        if (isComplete(profile)) return;
        injectLegacyProfile(profile);
    }

    private static boolean isComplete(GameProfile profile) {
        Property textureProperty = (Property) Iterables.getFirst(profile.getProperties().get("textures"), (Object) null);
        MojangTexturePayload payload = MojangTexturePayload.fromProperty(textureProperty);
        return payload != null && payload.isComplete();
    }

    private static class iHateThisDesignCallback implements ProfileLookupCallback {
        public GameProfile profile = null;

        @Override
        public void onProfileLookupSucceeded(GameProfile var1) {
            profile = var1;
        }

        @Override
        public void onProfileLookupFailed(GameProfile var1, Exception var2) {
            profile = null;
        }
    }

    private void injectGenuineProfile(GameProfile profile) {
        if (!cfg.loadGenuineSkins) return;
        MinecraftSessionService sessionService = Minecraft.getMinecraft().getSessionService();
        if (mojangProfileRepo == null || sessionService == null) {
            UniSkinMod.log.warn("Cannot perform mojang id lookup since YggdrasilGameProfileRepository or sessionService is null");
            return;
        }
        UniSkinMod.log.info("Looking for genuine Mojang Profile: {}", profile.getName());
        iHateThisDesignCallback callback = new iHateThisDesignCallback();
        mojangProfileRepo.findProfilesByNames(new String[]{profile.getName()}, Agent.MINECRAFT, callback);
        if (callback.profile == null) return;
        GameProfile gameProfile = sessionService.fillProfileProperties(callback.profile, false);
        Property textureProperty = (Property) Iterables.getFirst(gameProfile.getProperties().get("textures"), (Object) null);
        Property origProperty = (Property) Iterables.getFirst(profile.getProperties().get("textures"), (Object) null);

        MojangTexturePayload mojangPayload = MojangTexturePayload.fromProperty(textureProperty);
        MojangTexturePayload origPayload = MojangTexturePayload.fromProperty(origProperty);

        if (mojangPayload == null) return;
        if (origPayload == null) {
            profile.getProperties().removeAll("textures");
            profile.getProperties().put("textures", textureProperty);
            return;
        }
        if (origPayload.isComplete()) return;

        if (mojangPayload.textures.containsKey(MinecraftProfileTexture.Type.SKIN) &&
                !origPayload.textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
            origPayload.textures.put(MinecraftProfileTexture.Type.SKIN, mojangPayload.textures.get(MinecraftProfileTexture.Type.SKIN));
            UniSkinMod.log.info("[Inject Genuine] Skin: {}", profile.getName());
        }

        if (mojangPayload.textures.containsKey(MinecraftProfileTexture.Type.CAPE) &&
                !origPayload.textures.containsKey(MinecraftProfileTexture.Type.CAPE)) {
            origPayload.textures.put(MinecraftProfileTexture.Type.CAPE, mojangPayload.textures.get(MinecraftProfileTexture.Type.CAPE));
            UniSkinMod.log.info("[Inject Genuine] Cape: {}", profile.getName());
        }

        if (mojangPayload.textures.containsKey(MinecraftProfileTexture.Type.ELYTRA) &&
                !origPayload.textures.containsKey(MinecraftProfileTexture.Type.ELYTRA)) {
            origPayload.textures.put(MinecraftProfileTexture.Type.ELYTRA, mojangPayload.textures.get(MinecraftProfileTexture.Type.ELYTRA));
            UniSkinMod.log.info("[Inject Genuine] Elytra: {}", profile.getName());
        }

        Property finalP = origPayload.toProperty();
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", finalP);
    }

    private void injectLocalProfile(GameProfile profile) {
        if (localSkinDir == null) return;
        String player_name = profile.getName();
        if (player_name == null || player_name.length() == 0) return;
        UniSkinMod.log.info("Looking up Local Profile: {}", profile.getName());

        File[] fileList = localSkinDir.listFiles();
        if (fileList == null) return;
        for (File f : fileList) {
            if (f.getName().equalsIgnoreCase(player_name + ".json")) {
                UniSkinProfile api = UniSkinProfile.getLocalProfile(f, new File(localSkinDir, "textures"));
                if (api == null) return;
                MojangTexturePayload payload = MojangTexturePayload.fromGameProfile(profile);
                payload.addCape(dynamicSkinManager.forceLoadTexture(api.getCapeURL(), MinecraftProfileTexture.Type.CAPE, false));
                boolean isAlex = ("slim".equalsIgnoreCase(api.getModel()) || "alex".equalsIgnoreCase(api.getModel()));
                payload.addSkin(dynamicSkinManager.forceLoadTexture(api.getSkinURL(), MinecraftProfileTexture.Type.SKIN, isAlex), api.getModel());
                payload.addElytra(dynamicSkinManager.forceLoadTexture(api.getElytraURL(), MinecraftProfileTexture.Type.ELYTRA, false));
                payload.dumpIntoGameProfile(profile);
                return;
            }
        }
    }

    private void injectServerProfile(GameProfile profile) {
        // if (!cfg.loadServerSkins || skinCacheDir == null) return;
        // log.info("Injecting Server Profile(WIP): {}", profile.getName());
        // TODO;
    }

    private void injectPiracyProfile(final GameProfile gameProfile) {
        if (cfg.rootURIs == null || cfg.rootURIs.size() == 0) return;
        if (gameProfile.getName() == null || gameProfile.getName().length() == 0) return;
        UniSkinMod.log.info("Looking up Piracy Profile: {}", gameProfile.getName());
        String player_name = gameProfile.getName();

        MojangTexturePayload payload = MojangTexturePayload.fromGameProfile(gameProfile);

        for (String root : cfg.rootURIs) {
            if (payload.isComplete()) break;
            UniSkinProfile api = UniSkinProfile.getProfile(player_name, root);
            if (api == null) continue;
            payload.addCape(api.getCapeURL());
            payload.addSkin(api.getSkinURL(), api.getModel());
            payload.addElytra(api.getElytraURL());
        }

        payload.dumpIntoGameProfile(gameProfile);
    }

    private static byte[] fetchURL(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) (new URL(url).openConnection(Minecraft.getMinecraft().getProxy()));
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            InputStream input = connection.getInputStream();
            return IOUtils.toByteArray(input);
        } catch (Exception ex) {
            UniSkinMod.log.catching(Level.DEBUG, ex);
            UniSkinMod.log.warn("Failed to fetch url: {}", url);
            return null;
        }
    }

    private void injectLegacyProfile(GameProfile profile) {
        String playerName = profile.getName();
        if (playerName == null || playerName.length() == 0) return;
        UniSkinMod.log.info("Looking up Legacy Profile: {}", profile.getName());

        MojangTexturePayload payload = MojangTexturePayload.fromGameProfile(profile);

        if (cfg.legacySkinURIs != null) {
            byte[] skinData = null;
            for (String url : cfg.legacySkinURIs) {
                if (skinData != null) break;
                url = String.format(url, playerName);
                skinData = fetchURL(url);
            }
            if (skinData != null) {
                String hash = DigestUtils.sha256Hex(skinData).toLowerCase();
                File localTexture = new File(localSkinDir, "textures");
                try {
                    File localFile = new File(localTexture, hash);
                    FileUtils.writeByteArrayToFile(localFile, skinData);
                    dynamicSkinManager.forceLoadTexture(localFile, MinecraftProfileTexture.Type.SKIN, false);
                    payload.addSkin("http://127.0.0.1/" + hash, "default");
                    UniSkinMod.log.info("Injecting legacy skin: {} {}", playerName, hash);
                } catch (IOException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                }
            }
        }

        if (cfg.legacyCapeURIs != null) {
            byte[] capeData = null;
            for (String url : cfg.legacyCapeURIs) {
                if (capeData != null) break;
                url = String.format(url, playerName);
                capeData = fetchURL(url);
            }
            if (capeData != null) {
                String hash = DigestUtils.sha256Hex(capeData).toLowerCase();
                File localTexture = new File(localSkinDir, "textures");
                try {
                    File localFile = new File(localTexture, hash);
                    FileUtils.writeByteArrayToFile(localFile, capeData);
                    dynamicSkinManager.forceLoadTexture(localFile, MinecraftProfileTexture.Type.CAPE, false);
                    payload.addCape("http://127.0.0.1/" + hash);
                    UniSkinMod.log.info("Injecting legacy cape: {} {}", playerName, hash);
                } catch (IOException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                }
            }
        }

        payload.dumpIntoGameProfile(profile);
    }


    /* Codes below require forge and provided support for dynamic skins
     * Comment them out if compiled as a standalone library. */

    /**
     * called from AbstractClientPlayer.getLocationSkin()
     */
    public ResourceLocation getDynamicSkinResource(NetworkPlayerInfo player) {
        return player == null ? null : getDynamicSkinResourceForSkull(player.getGameProfile(), null);
    }

    /**
     * called from TileEntitySkllRenderer.renderSkull()
     */
    private final Set<String> loading = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    public ResourceLocation getDynamicSkinResourceForSkull(GameProfile gp, ResourceLocation def) {
        if (gp == null) return def;
        final String name = gp.getName();
        if (name == null || name.length() <= 0) return def;

        if (loading.contains(name)) return def;
        DynamicSkinManager.CachedDynamicSkin s = dynamicSkinManager.cache.getIfPresent(name);
        if (s == null) {
            loading.add(name);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        dynamicSkinManager.cache.get(name);
                    } catch (ExecutionException ex) {
                        UniSkinMod.log.catching(Level.WARN, ex);
                    }
                    loading.remove(name);
                }
            }, "Skull-Texture-Fetch-" + name).start();
            return def;
        }
        if (s.skin != null && s.skin.length != 0) {
            double spf = (double) s.skinInterval / (double) s.skin.length;
            int id = ((int) Math.floor((double) (System.currentTimeMillis() % s.skinInterval) / spf)) % (s.skin.length);
            return s.skin[id];
        }

        return def;
    }

    /**
     * called from AbstractClientPlayer.getSkinType()
     */
    public String getDynamicSkinModel(NetworkPlayerInfo player) {
        if (player == null) return null;
        GameProfile gp = player.getGameProfile();
        if (gp != null) {
            String name = gp.getName();
            if (name != null && name.length() > 0) {
                try {
                    DynamicSkinManager.CachedDynamicSkin s = dynamicSkinManager.cache.get(name);
                    if (s.skin != null && s.skin.length != 0) {
                        return s.model;
                    }
                } catch (ExecutionException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * called from AbstractClientPlayer.getLocationCape()
     */
    public ResourceLocation getDynamicCapeResource(NetworkPlayerInfo player) {
        if (player == null) return null;
        GameProfile gp = player.getGameProfile();
        if (gp != null) {
            String name = gp.getName();
            if (name != null && name.length() > 0) {
                try {
                    DynamicSkinManager.CachedDynamicSkin s = dynamicSkinManager.cache.get(name);
                    if (s.cape != null && s.cape.length != 0) {
                        double spf = (double) s.capeInterval / (double) s.cape.length;
                        int id = ((int) Math.floor((double) (System.currentTimeMillis() % s.capeInterval) / spf)) % (s.cape.length);
                        return s.cape[id];
                    }
                } catch (ExecutionException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * called from AbstractClientPlayer.getLocationElytra()
     */
    public ResourceLocation getDynamicElytraResource(NetworkPlayerInfo player) {
        if (player == null) return null;
        GameProfile gp = player.getGameProfile();
        if (gp != null) {
            String name = gp.getName();
            if (name != null && name.length() > 0) {
                try {
                    DynamicSkinManager.CachedDynamicSkin s = dynamicSkinManager.cache.get(name);
                    if (s.elytra != null && s.elytra.length != 0) {
                        double spf = (double) s.elytraInterval / (double) s.elytra.length;
                        int id = ((int) Math.floor((double) (System.currentTimeMillis() % s.elytraInterval) / spf)) % (s.elytra.length);
                        return s.elytra[id];
                    }
                } catch (ExecutionException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                    return null;
                }
            }
        }
        return null;
    }
}
