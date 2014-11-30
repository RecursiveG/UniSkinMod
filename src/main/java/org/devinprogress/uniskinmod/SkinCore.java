package org.devinprogress.uniskinmod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;

/**
 This file is part of Universal Skin Mod,
 Copyright (C) 2014  RecursiveG

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

@IFMLLoadingPlugin.MCVersion("1.8")
public class SkinCore implements IFMLLoadingPlugin {
    public static boolean ObfuscatedEnv=true;
    private static SkinCore instance=null;
    private List<String> SkinURLs=new ArrayList<String>();
    private List<String> CapeURLs =new ArrayList<String>();
    private static final String CFG_VER_STR="Version: 1";
    private static String SkinCachePath="";

    public SkinCore(){
        if(instance!=null)
            throw new RuntimeException("Duplicated Initialization for SkinCore");
        instance=this;
    }
    public static SkinCore getInstance(){
        if(instance==null)
            throw new RuntimeException("SkinCore.getInstance() before constructor");
        return instance;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{CoreTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return CoreContainer.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        LogManager.getLogger("UniSkinMod").info("Injecting Data ...");
        ObfuscatedEnv=(Boolean)data.get("runtimeDeobfuscationEnabled");
        String cfgPath=((File) data.get("mcLocation")).getAbsolutePath()
                + File.separatorChar + "config" + File.separatorChar
                + "UniSkinMod.cfg";
        updateCfg(cfgPath);
        loadCfg(cfgPath);

        SkinCachePath=((File) data.get("mcLocation")).getAbsolutePath()
                + File.separatorChar + "assets" + File.separatorChar
                + "skins";
        if(System.getProperty("uniskinmod.forceCleanUp","false").equals("true")){
            File skin_dir=new File(SkinCachePath);
            try {
                FileUtils.deleteDirectory(skin_dir);
                LogManager.getLogger("UniSkinMod").info("Skin cache cleaned.");
            } catch (IOException e) {
                LogManager.getLogger("UniSkinMod").warn("Failed to clean cache.");
                e.printStackTrace();
            }
        }
    }

    private void updateCfg(String cfgPath){
        try{
            File cfgFile=new File(cfgPath);
            if(cfgFile.exists()){
                FileReader fr = new FileReader(cfgPath);
                BufferedReader br = new BufferedReader(fr);
                String headLine="";
                while(headLine.equals("")&&br.ready())
                    headLine=br.readLine();
                br.close();
                fr.close();
                if(!headLine.equals(CFG_VER_STR)){
                    LogManager.getLogger("UniSkinMod").warn("Configure file version not match, deleting ...");
                    cfgFile.delete();
                }
            }
            if(!cfgFile.exists()){
                LogManager.getLogger("UniSkinMod").warn("Configure file do not exists. Creating...");
                cfgFile.createNewFile();
                FileWriter fw = new FileWriter(cfgPath);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(CFG_VER_STR);bw.newLine();
                bw.write("#Do not edit the line above");bw.newLine();
                bw.write("#请勿随意修改以上两行");bw.newLine();
                bw.write("Skin: http://www.skinme.cc/MinecraftSkins/%s.png");bw.newLine();
                bw.write("Cape: http://www.skinme.cc/MinecraftCloaks/%s.png");bw.newLine();
                bw.write("Skin: http://skins.minecraft.net/MinecraftSkins/%s.png");bw.newLine();
                bw.write("Cape: http://skins.minecraft.net/MinecraftCloaks/%s.png");bw.newLine();
                bw.flush();bw.close();fw.close();
            }
        }catch(IOException e){
            LogManager.getLogger("UniSkinMod").warn("Error happened when updating configuration");
            e.printStackTrace();
        }
    }

    private void loadCfg(String cfgPath){
        try{
            FileReader fr = new FileReader(cfgPath);
            BufferedReader br = new BufferedReader(fr);
            String theLine;
            while (br.ready()) {
                theLine = br.readLine().trim();
                if (theLine.startsWith("#") || theLine.equals("")) {
                    continue;
                }else if(theLine.startsWith("Cape: ")){
                    CapeURLs.add(theLine.substring(6).trim());
                }else if(theLine.startsWith("Skin: ")) {
                    SkinURLs.add(theLine.substring(6).trim());
                }
            }
            br.close();
            fr.close();
        }catch(IOException e){
            LogManager.getLogger("UniSkinMod").warn("Error happened when loading configuration");
            e.printStackTrace();
        }
    }

    private class playerSkinData{
        public String skin,cape,model;
        public playerSkinData(String skinURL,String capeURL,String model){
            skin=skinURL;
            cape=capeURL;
            if(model==null)
                this.model="default";
            else
                this.model=model.equalsIgnoreCase("slim")?"slim":"default";
        }
    }
    
    public static void injectTexture(HashMap map,GameProfile profile){
        LogManager.getLogger("UniSkinMod").info("Preparing skins for Player: " + profile.getName());
        //log(map.containsKey(MinecraftProfileTexture.Type.CAPE)?"Contain Cape":"No Cape");
        //log(map.containsKey(MinecraftProfileTexture.Type.SKIN)?"Contain Skin":"No Skin");

        if(System.getProperty("uniskinmod.forceCustomServers","false").equals("true"))
            map.clear();
        if (map.containsKey(MinecraftProfileTexture.Type.CAPE)&&map.containsKey(MinecraftProfileTexture.Type.SKIN))
            return;
        final playerSkinData data=getInstance().getPlayerData(profile.getName(), profile.getId() == null ? null :profile.getId().toString());
        if((!map.containsKey(MinecraftProfileTexture.Type.CAPE))&&(data.cape!=null)){
            map.put(MinecraftProfileTexture.Type.CAPE,new MinecraftProfileTexture(data.cape,null));
        }
        if((!map.containsKey(MinecraftProfileTexture.Type.SKIN))&&(data.skin!=null)){
            map.put(MinecraftProfileTexture.Type.SKIN,new MinecraftProfileTexture(data.skin,
                    new HashMap<String,String>(){{put("model",data.model);}}));
        }
    }

    private static boolean queued=false;
    private static Map<String,MinecraftProfileTexture> skullMap=new HashMap<String, MinecraftProfileTexture>();
    public static void injectSkullTexture(final Map map,final GameProfile profile){
        if(queued)return;
        if(map.containsKey(MinecraftProfileTexture.Type.SKIN))return;
        final String name=profile.getName();
        //LogManager.getLogger("UniSkinMod").info("Skull Renderer Invoked for "+name);
        if (skullMap.containsKey(name)) {
            if(skullMap.get(name)!=null)
                map.put(MinecraftProfileTexture.Type.SKIN, skullMap.get(name));
            return;
        }
        queued=true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogManager.getLogger("UniSkinMod").info("SkullSkinFetchThreadStarted "+name);
                injectTexture((HashMap) map, profile);
                if(map.containsKey(MinecraftProfileTexture.Type.SKIN))
                    skullMap.put(name,(MinecraftProfileTexture)map.get(MinecraftProfileTexture.Type.SKIN));
                else
                    skullMap.put(name,null);
                queued=false;
                LogManager.getLogger("UniSkinMod").info("SkullSkinFetchThreadEnded "+name);
            }
        },"FetchSkullSkinThread").start();
    }

    public playerSkinData getPlayerData(String name,String uuid){
        String skinURL=testURLs(name,SkinURLs);
        String capeURL=testURLs(name,CapeURLs);
        String model="default";
        return new playerSkinData(skinURL,capeURL,model);
    }

    private String testURLs(String playerName,List<String> templates){
        //log("Test for player: "+playerName);
        String CachedLink=null;
        for (String s:templates){
            String link=String.format(s,playerName);
            int rspCode=0;
            try {
                URL url = new URL(link);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setDoOutput(false);
                conn.setConnectTimeout(1000 * 5);
                conn.setInstanceFollowRedirects(true);
                conn.connect();
                rspCode=conn.getResponseCode();
                conn.disconnect();
            } catch (IOException e) {
                rspCode=-1;
                //e.printStackTrace();
            }
            //LogManager.getLogger("UniSkinMod").info(String.format("HTTP Return Code: %d  @%s",rspCode,link));
            if(rspCode==404)
                cleanCache(link);
            if(rspCode==200){
                cleanCache(link);
                return link;
            }else if(CachedLink==null&&cacheExists(link))
                CachedLink=link;
        }
        return CachedLink;
    }

    private static boolean cacheExists(String link){
        String hash=getHashForTexture(link);
        String domain=hash.substring(0, 2);
        File t=new File(SkinCachePath+File.separator+domain+File.separator+hash);
        return t.exists();
    }

    private static void cleanCache(String link){
        String hash=getHashForTexture(link);
        String domain=hash.substring(0, 2);
        File t=new File(SkinCachePath+File.separator+domain+File.separator+hash);
        if(t.exists()) {
            if (t.delete()) {
                //LogManager.getLogger("UniSkinMod").info("Delete success: " + link);
            }else{
                LogManager.getLogger("UniSkinMod").warn("Failed to delete cache: " + link);
            }
        }
    }

    public static String getHashForTexture(String url){
        String fileName= FilenameUtils.getBaseName(url);
        if(fileName.length()<=30)return SHA1SUM(url);
        for(int i=0;i<fileName.length();i++)
            if("0123456789abcdefABCDEF".indexOf(fileName.charAt(i))<0)
                return SHA1SUM(url);
        return fileName;
    }

    private static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static String SHA1SUM(String str){
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(str.getBytes());
            byte[] bytes=messageDigest.digest();
            StringBuilder buf = new StringBuilder(bytes.length*2);
            for(byte b:bytes){
                buf.append(HEX_DIGITS[(b>>4)&0x0f]);
                buf.append(HEX_DIGITS[ b    &0x0f]);
            }
            return buf.toString();
        }catch(Exception e){
            e.printStackTrace();
            return "MissingTexture";
        }        
    }
}
