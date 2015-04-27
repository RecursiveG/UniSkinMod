package org.devinprogress.uniskinmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A modifiable version of com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload */
public class MojangTexturePayload {
    private static final Gson gson=new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .create();

    public long timestamp;
    public UUID profileId;
    public String profileName;
    public boolean isPublic;
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;

    public MojangTexturePayload(String playerName){
        isPublic=true;
        profileName=playerName;
        profileId=UniSkinMod.getOfflineUUID(playerName);
        textures=new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();

    }

    public static MojangTexturePayload fromProperty(Property property){
        if (property==null) return null;
        if (!property.getName().equalsIgnoreCase("textures")) return null;
        String e = new String(Base64.decodeBase64(property.getValue()), Charsets.UTF_8);
        return gson.fromJson(e, MojangTexturePayload.class);
    }

    public Property toProperty(){
        String j = gson.toJson(this);
        String b64 = Base64.encodeBase64String(j.getBytes(Charsets.UTF_8));
        return new Property("textures",b64);
    }

    public boolean isComplete(){
        return textures.containsKey(MinecraftProfileTexture.Type.SKIN) && textures.containsKey(MinecraftProfileTexture.Type.SKIN);
    }

    public void addSkin(String url, String model){
        if (textures.containsKey(MinecraftProfileTexture.Type.SKIN)) return;
        if (url==null || url.length()==0 || model==null) return;
        boolean slimModel=model.equalsIgnoreCase("slim");
        textures.put(MinecraftProfileTexture.Type.SKIN,
                new MinecraftProfileTexture(url,slimModel?
                        new HashMap<String, String>(){{put("model","slim");}}:null));
    }

    public void addCape(String url){
        if (textures.containsKey(MinecraftProfileTexture.Type.CAPE)) return;
        if (url==null || url.length()==0) return;
        textures.put(MinecraftProfileTexture.Type.CAPE,new MinecraftProfileTexture(url,null));
    }
}