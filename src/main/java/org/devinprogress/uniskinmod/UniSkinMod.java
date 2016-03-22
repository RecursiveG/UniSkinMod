package org.devinprogress.uniskinmod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.devinprogress.uniskinmod.coremod.IGetTexture_old;
import org.devinprogress.uniskinmod.coremod.ILoadSkinFromCache_old;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Mod(modid = "uniskinmod")
@SideOnly(Side.CLIENT)
public class UniSkinMod {
    public static Logger log = null;
    private UniSkinCore core = null;
    @Mod.Instance
    private static UniSkinMod instance;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        try {
            File configDir = event.getModConfigurationDirectory();
            configDir = new File(configDir, "UniSkinMod");
            File localSkin = new File(configDir, "local_skins");
            UniSkinConfig config = UniSkinConfig.loadFromFile(new File(configDir, "UniSkinMod.json"));
            if (config == null || localSkin == null) {
                core = null;
                log.fatal("Cannot get configure. UniSkinMod disabled");
                log.debug("config = {}, localSkin = {}", config, localSkin);
                return;
            }
            core = new UniSkinCore(config, localSkin);
            log.info("UniSkinMod Core Loaded.");
        } catch (Exception ex) {
            log.catching(Level.FATAL, ex);
            log.fatal("UniSkinMod disabled due to an error.");
            core = null;
        }
    }

    /* Callback interfaces */
    private static final Cache<String, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>>
            profileToTextureMapCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures_wrapper(GameProfile profile,
                                                                                                 boolean requireSecure, IGetTexture_old sessionService) {
        if (instance.core == null) return sessionService.getTextures_old(profile, requireSecure);
        String name = profile.getName();
        if (name == null || name.length() == 0) return sessionService.getTextures_old(profile, requireSecure);
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> cached = profileToTextureMapCache.getIfPresent(name);
        if (cached != null) return cached;
        instance.core.injectProfile(profile);
        cached = sessionService.getTextures_old(profile, false);
        profileToTextureMapCache.put(name, cached);
        return cached;
    }

    public static ResourceLocation getDynamicSkinResource(NetworkPlayerInfo player) {
        return instance.core == null ? null : instance.core.getDynamicSkinResource(player);
    }

    public static ResourceLocation getDynamicSkinResourceForSkull(GameProfile gp, ResourceLocation def) {
        return instance.core == null ? null : instance.core.getDynamicSkinResourceForSkull(gp, def);
    }

    public static String getDynamicSkinModel(NetworkPlayerInfo player) {
        return instance.core == null ? null : instance.core.getDynamicSkinModel(player);
    }

    public static ResourceLocation getDynamicCapeResource(NetworkPlayerInfo player) {
        return instance.core == null ? null : instance.core.getDynamicCapeResource(player);
    }

    public static ResourceLocation getDynamicElytraResource(NetworkPlayerInfo player) {
        return instance.core == null ? null : instance.core.getDynamicElytraResource(player);
    }


    private static final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> defaultReturn = Collections.emptyMap();
    private static final Set<GameProfile> onLoading = Collections.newSetFromMap(new ConcurrentHashMap<GameProfile, Boolean>());

    public static Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache_wrapper(final GameProfile gp, final ILoadSkinFromCache_old skinManager) {
        if (onLoading.contains(gp)) return defaultReturn;
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> ret = skinManager.getLoadingCache().getIfPresent(gp);
        if (ret == null) {
            onLoading.add(gp);
            String name = gp.getName() == null ? gp.toString() : gp.getName();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    skinManager.getLoadingCache().getUnchecked(gp);
                    onLoading.remove(gp);
                }
            }, "Skin-Fetch-" + name).start();
            return defaultReturn;
        }
        return ret;
    }
}
