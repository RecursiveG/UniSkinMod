package org.devinprogress.uniskinmod;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import com.mojang.util.UUIDTypeAdapter;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * Pack & Unpack Mojang's official RequestResponse to add custom URLs
 */
public class ProfileResponseBuilder {
    private final MinecraftProfilePropertiesResponse orig;
    private static final Gson gson=new GsonBuilder()
            .registerTypeAdapter(UUID.class, new UUIDTypeAdapter())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .registerTypeAdapter(ProfileSearchResultsResponse.class, new ProfileSearchResultsResponse.Serializer())
            .create();

    private UUID id;
    private String name;
    private PropertyMap otherProperties;
    private MojangTexturePayload payload;

    private class MojangTexturePayload {
        public long timestamp;
        public UUID profileId;
        public String profileName;
        public boolean isPublic;
        public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;
    }

    private class UniSkinApiJson{
        public String player_name;
        public long last_update;
        public List<String> model_preference;
        public Map<String,String> skins;
        public String cape;
    }

    private class ResponseStructure{
        public UUID id;
        public String name;
        public PropertyMap properties=new PropertyMap();
    }

    public ProfileResponseBuilder(final MinecraftProfilePropertiesResponse orig, String name){
        this.orig=orig;

        if (orig==null||orig.getName()==null||orig.getName().trim().equals(""))
            this.name=name;
        else
            this.name=orig.getName();

        if (orig==null||orig.getId()==null)
            this.id=UniSkinMod.getOfflineUUID(name);
        else
            this.id=orig.getId();

        this.otherProperties=new PropertyMap();
        if (orig!=null&&orig.getProperties()!=null)
            this.otherProperties.putAll(orig.getProperties());

        Property textureProperty = Iterables.getFirst(this.otherProperties.get("textures"), null);
        if (textureProperty==null) {
            this.payload = new MojangTexturePayload();
            this.payload.profileName=this.name;
            this.payload.profileId=this.id;
            this.payload.isPublic=true;
            this.payload.textures=new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            this.payload.timestamp=System.currentTimeMillis();
        }else{
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            this.payload=gson.fromJson(json,MojangTexturePayload.class);
        }

        this.otherProperties.removeAll("textures");
    }

    private static final String URL_PROFILE_FMT="{root}/{player_name}.json";
    private static final String URL_TEXTURE_FMT="{root}/textures/{texture_hash}";

    public MinecraftProfilePropertiesResponse getFilledResponse(final List<String> roots){
        if (this.payload.textures.containsKey(MinecraftProfileTexture.Type.CAPE) &&
                this.payload.textures.containsKey(MinecraftProfileTexture.Type.SKIN))
            return this.orig;

        boolean modified=false;
        for (String root:roots){
            try {
                String jsonURI = URL_PROFILE_FMT.replace("{player_name}", this.name).replace("{root}",root);
                String json = httpRequest(jsonURI);
                if (json == null) continue;
                UniSkinMod.log.debug(String.format("Querying player %s@%s got reply %s",this.name,root,json.replace("\n","")));
                UniSkinApiJson obj=gson.fromJson(json,UniSkinApiJson.class);

                if (!this.payload.textures.containsKey(MinecraftProfileTexture.Type.CAPE)) {
                    if (obj.cape!=null){
                        String cape_url=URL_TEXTURE_FMT.replace("{root}",root).
                                replace("{texture_hash}",obj.cape);
                        this.payload.textures.put(MinecraftProfileTexture.Type.CAPE,
                                new MinecraftProfileTexture(cape_url,null));
                        modified=true;
                        UniSkinMod.log.info(String.format("New cape url from %s for player %s added: %s",root,this.name,cape_url));
                    }
                }

                if (!this.payload.textures.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                    if (obj.skins!=null&&obj.model_preference!=null&&obj.model_preference.size()>0){
                        for(String model:obj.model_preference) {
                            if (obj.skins.containsKey(model) && obj.skins.get(model)!=null && obj.skins.get(model).length()>0){
                                boolean slimModel=model.equalsIgnoreCase("slim");
                                String skin_url=URL_TEXTURE_FMT.replace("{root}",root).replace("{texture_hash}", obj.skins.get(model));
                                this.payload.textures.put(MinecraftProfileTexture.Type.SKIN,
                                        new MinecraftProfileTexture(skin_url,slimModel?
                                                new HashMap<String, String>(){{put("model","slim");}}:null));
                                modified=true;
                                UniSkinMod.log.info(String.format("New %s skin url from %s for player %s added: %s",
                                        model,root,this.name,skin_url));
                            }
                        }
                    }
                }

                if (this.payload.textures.containsKey(MinecraftProfileTexture.Type.CAPE) &&
                        this.payload.textures.containsKey(MinecraftProfileTexture.Type.SKIN))
                    break;
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

        if (modified) {
            try{
                ResponseStructure tmp=new ResponseStructure();
                tmp.id=this.id;
                tmp.name=this.name;
                tmp.properties=this.otherProperties;

                String payloadJson=gson.toJson(this.payload);
                UniSkinMod.log.debug(String.format("Final texture property for player %s: %s",this.name,payloadJson));
                String payloadBase64=Base64.encodeBase64String(payloadJson.getBytes(Charsets.UTF_8));
                Property property=new Property("textures",payloadBase64);

                tmp.properties.put("textures",property);

                String payloadStr=gson.toJson(tmp);
                UniSkinMod.log.debug(String.format("Full payload for player %s: %s",this.name,payloadStr));

                return gson.fromJson(payloadStr,MinecraftProfilePropertiesResponse.class);
            }catch(Exception ex){
                ex.printStackTrace();
                return this.orig;
            }
        }else{
            return this.orig;
        }
    }

    private String httpRequest(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection(Minecraft.getMinecraft().getProxy());
            conn.setReadTimeout(1000 * 5);
            conn.setConnectTimeout(1000 * 5);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int errorNo = conn.getResponseCode();
            if (errorNo != 200)
                return null;
            InputStream iStream = conn.getInputStream();
            return IOUtils.toString(iStream, Charsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
