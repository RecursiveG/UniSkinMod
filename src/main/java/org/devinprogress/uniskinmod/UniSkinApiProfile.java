package org.devinprogress.uniskinmod;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/** Universal Skin API */
public class UniSkinApiProfile {
    private static Logger log = LogManager.getLogger("UniSkinAPI");

    private class ProfileJSON{
        public String player_name;
        public long last_update;
        public List<String> model_preference;
        public Map<String,String> skins;
        public String cape;
    }

    public boolean hasProfile=false;
    private int errorNo;
    private String httpRequest(String url) {
        try {
            log.info("Fetching URL: "+url);
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection(Minecraft.getMinecraft().getProxy());
            conn.setReadTimeout(1000 * 5);
            conn.setConnectTimeout(1000 * 5);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            errorNo = conn.getResponseCode();
            if (errorNo > 299 || errorNo < 200) {
                log.error("rspCode not 2xx: " + url);
                return null;
            }
            InputStream iStream = conn.getInputStream();
            return IOUtils.toString(iStream, Charsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static final String URL_PROFILE_FMT="{root}/{player_name}.json";
    private static final String URL_TEXTURE_FMT="{root}/textures/{texture_hash}";
    private String skin=null,cape=null,model=null;
    private long update=0;

    public static UniSkinApiProfile getProfile(final String name, final String Root){
        UniSkinApiProfile prof=new UniSkinApiProfile(name,Root);
        return prof.hasProfile?prof:null;
    }
    private UniSkinApiProfile(String name, String root){
        String profileURL=URL_PROFILE_FMT.replace("{root}",root).replace("{player_name}",name);
        String profileJSON=httpRequest(profileURL);
        if(errorNo==404 || profileJSON==null)return;
        ProfileJSON json=(new Gson()).fromJson(profileJSON,ProfileJSON.class);
        if(!json.player_name.equalsIgnoreCase(name))return;
        json.model_preference.remove("slim"); //1710 hack, no slim model
        for(String m:json.model_preference){
            if(json.skins.containsKey(m)){
                model=m;
                skin=URL_TEXTURE_FMT.replace("{root}",root).replace("{texture_hash}",json.skins.get(m));
                hasProfile=true;
                log.info(String.format("Player Skin Selected: %s %s %s",name,model,json.skins.get(m)));
                break;
            }
        }
        if(json.cape!=null&&json.cape.length()>3){
            cape=URL_TEXTURE_FMT.replace("{root}",root).replace("{texture_hash}",json.cape);
            hasProfile=true;
            log.info(String.format("Player Cape Selected: %s %s",name,json.cape));
        }
        update=json.last_update;
    }
    public String getSkinURL(){
        return skin;
    }
    public String getCapeURL(){
        return cape;
    }
    public String getModel(){
        return model;
    }
    public long lastUpdate(){
        return update;
    }
}