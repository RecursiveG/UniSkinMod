package org.devinprogress.uniskinmod;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A modifiable version of com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload
 */
public class MojangTexturePayload {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .create();

    public long timestamp;
    public UUID profileId;
    public String profileName;
    public boolean isPublic;
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

    public MojangTexturePayload(String playerName) {
        isPublic = true;
        profileName = playerName;
        profileId = getOfflineUUID(playerName);
        textures = new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();

    }

    public static MojangTexturePayload fromProperty(Property property) {
        if (property == null) return null;
        if (!property.getName().equalsIgnoreCase("textures")) return null;
        String e = new String(Base64.decodeBase64(property.getValue()), Charsets.UTF_8);
        return gson.fromJson(e, MojangTexturePayload.class);
    }

    public static MojangTexturePayload fromGameProfile(GameProfile profile) {
        String playerName = profile.getName();
        if (playerName == null || playerName.length() == 0) throw new IllegalArgumentException("Bad player name");
        Property textureProperty = (Property) Iterables.getFirst(profile.getProperties().get("textures"), (Object) null);
        MojangTexturePayload payload = MojangTexturePayload.fromProperty(textureProperty);
        if (payload == null) payload = new MojangTexturePayload(playerName);
        return payload;
    }

    public void dumpIntoGameProfile(final GameProfile profile) {
        Property finalP = toProperty();
        profile.getProperties().removeAll("textures");
        profile.getProperties().put("textures", finalP);
    }

    public static UUID getOfflineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
    }

    public Property toProperty() {
        String j = gson.toJson(this);
        String b64 = Base64.encodeBase64String(j.getBytes(Charsets.UTF_8));
        return new Property("textures", b64);
    }

    public boolean isComplete() {
        return textures.containsKey(MinecraftProfileTexture.Type.SKIN) &&
                textures.containsKey(MinecraftProfileTexture.Type.CAPE) &&
                textures.containsKey(MinecraftProfileTexture.Type.ELYTRA);
    }

    public void addSkin(String url, String model) {
        if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) return;
        if (url == null || url.length() == 0 || model == null) return;
        boolean slimModel = model.equalsIgnoreCase("slim") || model.equalsIgnoreCase("alex");
        textures.put(MinecraftProfileTexture.Type.SKIN,
                new MinecraftProfileTexture(url, slimModel ?
                        new HashMap<String, String>() {{
                            put("model", "slim");
                        }} : null));
        UniSkinMod.log.info("Injecting Skin: {} {} {}", profileName, url, model);
    }

    public void addCape(String url) {
        if (textures.containsKey(MinecraftProfileTexture.Type.CAPE)) return;
        if (url == null || url.length() == 0) return;
        textures.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture(url, null));
        UniSkinMod.log.info("Injecting Cape: {} {} {}", profileName, url);
    }

    public void addElytra(String url) {
        if (textures.containsKey(MinecraftProfileTexture.Type.ELYTRA)) return;
        if (url == null || url.length() == 0) return;
        textures.put(MinecraftProfileTexture.Type.ELYTRA, new MinecraftProfileTexture(url, null));
        UniSkinMod.log.info("Injecting Elytra: {} {}", profileName, url);
    }
}