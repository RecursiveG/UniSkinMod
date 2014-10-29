package org.devinprogress.uniskinmod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.8")
public class SkinCore implements IFMLLoadingPlugin{
    public static boolean ObfuscatedEnv=true;
    private static SkinCore instance=null;
    private List<String> SkinURLs=new ArrayList<String>();
    private List<String> CloakURLs=new ArrayList<String>();

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

    public static void log(String info){
        System.out.println("[UniSkinMod]"+info);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"org.devinprogress.uniskinmod.CoreTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return "org.devinprogress.uniskinmod.CoreContainer";
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        log("Starting up...");
        ObfuscatedEnv=(Boolean)data.get("runtimeDeobfuscationEnabled");

        try {
            String cfgPath = ((File) data.get("mcLocation")).getAbsolutePath()
                    + File.separatorChar + "config" + File.separatorChar
                    + "UniSkinMod.cfg";
            File f = new File(cfgPath);

            if (!f.exists()) {
                log("Configure file do not exists. Creating...");
                f.createNewFile();
                FileWriter fw = new FileWriter(cfgPath);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("#Skin: http://your.domain/**** ");bw.newLine();
                bw.write("#Cloak: http://your.domain/**** ");bw.newLine();
                bw.write("#Use '%s' to represent the player's name.");bw.newLine();
                bw.write("#The file is Case-Sensitive.");bw.newLine();
                bw.flush();bw.close();fw.close();
            }

            FileReader fr = new FileReader(cfgPath);
            BufferedReader br = new BufferedReader(fr);
            String theLine;
            while (br.ready()) {
                theLine = br.readLine();
                if (theLine.startsWith("#"))
                    continue;
                if (!tryProcessLine(theLine)){//failed
                    log("Failed to process the config line: '"+theLine+"'");
                }
            }
            br.close();
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean tryProcessLine(String line){
        log("Processing:"+line);
        if(line.startsWith("#"))return true;
        if(!line.contains("%s"))return false;
        if(line.startsWith("Skin: ")){
            SkinURLs.add(line.split(" ",2)[1]);
        }else if(line.startsWith("Cloak: ")){
            CloakURLs.add(line.split(" ",2)[1]);
        }else{
            return false;
        }
        return true;
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
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
    }
    
    public static void injectTexture(HashMap map,GameProfile profile){
        //log("Invoked for Player: "+profile.getName());
        //log(map.containsKey(MinecraftProfileTexture.Type.CAPE)?"Contain Cape":"No Cape");
        //log(map.containsKey(MinecraftProfileTexture.Type.SKIN)?"Contain Skin":"No Skin");
        
        if (map.containsKey(MinecraftProfileTexture.Type.CAPE)&&map.containsKey(MinecraftProfileTexture.Type.SKIN))
            return;
        final playerSkinData data=getInstance().getPlayerData(profile.getName(),profile.getId().toString());
        if((!map.containsKey(MinecraftProfileTexture.Type.CAPE))&&(data.cape!=null)){
            map.put(MinecraftProfileTexture.Type.CAPE,new MinecraftProfileTexture(data.cape,null));
        }
        if((!map.containsKey(MinecraftProfileTexture.Type.SKIN))&&(data.skin!=null)){
            map.put(MinecraftProfileTexture.Type.SKIN,new MinecraftProfileTexture(data.skin,
                    new HashMap<String,String>(){{put("model",data.model);}}));
        }
    }
    
    public playerSkinData getPlayerData(String name,String uuid){
        String skinURL=testURLs(name,SkinURLs,"http://skins.minecraft.net/MinecraftSkins/%s.png");
        String capeURL=testURLs(name,CloakURLs,"http://skins.minecraft.net/MinecraftCloaks/%s.png");
        String model="default";
        return new playerSkinData(skinURL,capeURL,model);
    }

    private String testURLs(String playerName,List<String> templates,String defaultTemplate){
        //log("Test for player: "+playerName);
        for (String s:templates){
            String url=String.format(s,playerName);
            if(checkURL(url)){//Success
                return url;
            }
        }
        //return String.format(defaultTemplate,playerName);
        return null;
    }

    private boolean checkURL(String link){
        try {
            URL url = new URL(link);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setDoOutput(false);
            conn.setConnectTimeout(1000 * 5);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int rspCode=conn.getResponseCode();
            conn.disconnect();
            //log(String.format("Code=%d@%s",rspCode,link));
            if(rspCode==200)
                return true;
            else
                return false;
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }
    }

    public static String SHA1SUM(String str){
        try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.update(str.getBytes());
            return toHex(messageDigest.digest());
        }catch(Exception e){
            e.printStackTrace();
            return "MissingTexture";
        }        
    }

    private static final char[] HEX_DIGITS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    private static String toHex(byte[] bytes) {
        int len = bytes.length;
        StringBuilder buf = new StringBuilder(len*2);
        for (int i = 0; i < len; i++) {
            buf.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
            buf.append(HEX_DIGITS[bytes[i] & 0x0f]);
        }
        return buf.toString();
    }
}
