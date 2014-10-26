package org.devinprogress.uniskinmod;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import net.minecraft.util.StringUtils;

@IFMLLoadingPlugin.MCVersion("1.7.2")
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

    public static String alterURL(String link){
        if (link.startsWith("http://skins.minecraft.net/MinecraftSkins/")){
            String p=link.substring(link.lastIndexOf("/")+1,link.length()-4);
            return getInstance().testURLs(p,getInstance().SkinURLs,"http://skins.minecraft.net/MinecraftSkins/%s.png");
        }
        if (link.startsWith("http://skins.minecraft.net/MinecraftCloaks/")){
            String p=link.substring(link.lastIndexOf("/")+1,link.length()-4);
            return getInstance().testURLs(p,getInstance().CloakURLs,"http://skins.minecraft.net/MinecraftCloaks/%s.png");
        }
        return link;
    }

    private String testURLs(String playerName,List<String> templates,String defaultTemplate){
        //log("Test for player: "+playerName);
        for (String s:templates){
            String url=String.format(s,playerName);
            if(checkURL(url)){//Success
                return url;
            }
        }
        return String.format(defaultTemplate,playerName);
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
}
