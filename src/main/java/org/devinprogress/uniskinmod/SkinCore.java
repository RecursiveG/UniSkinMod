package org.devinprogress.uniskinmod;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import scala.collection.parallel.ParIterableLike;

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

@IFMLLoadingPlugin.MCVersion("1.7.10")
public class SkinCore implements IFMLLoadingPlugin{
    public static boolean ObfuscatedEnv=true;
    private static SkinCore instance=null;
    private List<String> RootURLs =new ArrayList<String>();
    private static final String CFG_VER_STR="Version: 2";
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
                bw.write("# Line starts with '#' is a commit line");bw.newLine();
                bw.write("# Line starts with 'Root: ' indicates a server");bw.newLine();
                bw.write("# All servers will be queried in that order.");bw.newLine();
                bw.write("# Server in front has higher priority");bw.newLine();
                bw.write("# Official server has the highest priority");bw.newLine();
                bw.write("# No more legacy style link support!");bw.newLine();
                bw.write("# An Example:");bw.newLine();
                bw.write("# Root: http://127.0.0.1:25566/skins");bw.newLine();
                bw.newLine();
                bw.write("# SkinMe Default");bw.newLine();
                bw.write("Root: http://www.skinme.cc/uniskin");bw.newLine();
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
                if(theLine.startsWith("Root: ")) {
                    LogManager.getLogger("UniSkinMod").info("Root address added: " + theLine.substring(6).trim());
                    RootURLs.add(theLine.substring(6).trim());
                } else if (!(theLine.startsWith("#") || theLine.equals("") || theLine.startsWith("Version:"))) {
                    LogManager.getLogger("UniSkinMod").info("Unknown Config Line: " + theLine);
                }
            }
            br.close(); fr.close();
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
                this.model=model.equalsIgnoreCase("alex")?"alex":"default";
        }

        @Override
        public String toString(){
            return String.format("{skin: %s, cape: %s, model: %s}", skin, cape, model);
        }
    }
    
    public static void injectTexture(HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture> map,GameProfile profile){
        LogManager.getLogger("UniSkinMod").info("Injecting Skin Data for Player: " + profile.getName());

        if (map.containsKey(MinecraftProfileTexture.Type.CAPE)&&map.containsKey(MinecraftProfileTexture.Type.SKIN))
            return;
        final playerSkinData data=getInstance().getPlayerData(profile.getName());
        if (data==null) return;

        if(!(map.containsKey(MinecraftProfileTexture.Type.CAPE))&&data.cape != null){
            map.put(MinecraftProfileTexture.Type.CAPE,constructTexture(data.cape));
            LogManager.getLogger("UniSkinMod").debug("Cape Injected");
        }
        if(!(map.containsKey(MinecraftProfileTexture.Type.SKIN))&&data.skin != null){
            map.put(MinecraftProfileTexture.Type.SKIN,constructTexture(data.skin));
            LogManager.getLogger("UniSkinMod").debug("Skin Injected");
        }
    }

    /** Deal with different constructor of MinecraftProfileTexture in different versions of authlib */
    private static MinecraftProfileTexture constructTexture(String url){
        Constructor con = null;
        boolean legacy = false;
        try {
            con = MinecraftProfileTexture.class.getConstructor(String.class, Map.class);
        }catch(NoSuchMethodException ex) {
            legacy = true;
        }

        if(legacy){
            try {
                con = MinecraftProfileTexture.class.getConstructor(String.class);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

        LogManager.getLogger("UniSkinMod").debug("Using " +(legacy ? "old" : "new")+ " constructor of authlib.");

        if (con==null)return null;
        try {
            if (legacy) {
                return (MinecraftProfileTexture) con.newInstance(url);
            }else{
                return (MinecraftProfileTexture) con.newInstance(url, null);
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private static Cache<String, playerSkinData> cache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();
    public playerSkinData getPlayerData(final String name){
        try{
            return cache.get(name, new Callable<playerSkinData>() {
                @Override
                public playerSkinData call() throws Exception {
                    String skin=null, cape=null;
                    for (String root: RootURLs){
                        if (skin !=null && cape!=null) break;
                        UniSkinApiProfile api = UniSkinApiProfile.getProfile(name, root);
                        if (api==null) continue;
                        if (skin==null) skin = api.getSkinURL();
                        if (cape==null) cape = api.getCapeURL();
                    }
                    return new playerSkinData(skin,cape,"default");
                }
            });
        }catch(Exception ex){
            ex.printStackTrace();
            return null;
        }
    }
}
