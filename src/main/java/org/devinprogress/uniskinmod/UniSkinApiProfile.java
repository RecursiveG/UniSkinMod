package org.devinprogress.uniskinmod;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Created by recursiveg on 14-11-20.
 */
public class UniSkinApiProfile {

    private class ProfileJSON{
        public String player_name;
        public long last_update;
        public String uuid;
        public List<String> model_preference;
        public Map<String,String> skins;
        public String cape;
    }

    private String skin=null,cape=null,model=null;
    public boolean hasProfile=false;
    private int errorNo;
    private String httpRequest(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection(Minecraft.getMinecraft().getProxy());
            conn.setReadTimeout(1000 * 5);
            conn.setConnectTimeout(1000 * 5);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            errorNo = conn.getResponseCode();
            if (errorNo != 200)
                return null;
            InputStream iStream = conn.getInputStream();
            return IOUtils.toString(iStream, Charsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public static UniSkinApiProfile getProfile(final String name, final String uuid, final String Root){
        UniSkinApiProfile prof=new UniSkinApiProfile(name,uuid,Root);
        return prof.hasProfile?prof:null;
    }
    private UniSkinApiProfile(final String name, final String uuid, final String Root){
        String profileURL=String.format("%s%s.json",Root,name);
        String profileJSON=httpRequest(profileURL);
        if(errorNo==404)return;
        if(errorNo==400){
            String profileUuidURL=String.format("%s%s.%s.json",Root,name,uuid);
            profileJSON=httpRequest(profileUuidURL);
            if(errorNo!=200)return;
        }
        ProfileJSON json=(new Gson()).fromJson(profileJSON,ProfileJSON.class);
        if(!json.player_name.equals(name))return;
        for(String m:json.model_preference){
            if(json.skins.containsKey(m)){
                model=m;
                skin=String.format("%stextures/%s",Root,json.skins.get(m));
                hasProfile=true;
                break;
            }
        }
        if(json.cape!=null&&json.cape.length()>3){
            cape=String.format("%stextures/%s",Root,json.cape);
            hasProfile=true;
        }
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
}
