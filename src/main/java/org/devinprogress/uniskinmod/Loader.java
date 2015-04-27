package org.devinprogress.uniskinmod;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.io.*;
import java.util.Map;

/**
 * FML Plugin loader for UniSkinMod
 */

@IFMLLoadingPlugin.MCVersion("1.8")
public class Loader implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{AsmTransformer.class.getName()};
    }

    @Override
    public String getModContainerClass() {
        return ModContainer.class.getName();
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        String cfgPath=((File) data.get("mcLocation")).getAbsolutePath()
                + File.separatorChar + "config" + File.separatorChar
                + "UniSkinMod.cfg";
        UniSkinMod.log.info("Loading configuration ... @"+cfgPath);

        try{
            File cfgFile=new File(cfgPath);
            if(!cfgFile.exists()){
                UniSkinMod.log.warn("Configure file do not exists. Creating...");
                cfgFile.createNewFile();
                FileWriter fw = new FileWriter(cfgPath);
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write("# Line starts with '#' is a commit line");bw.newLine();
                bw.write("# Line starts with 'Root: ' indicates a server");bw.newLine();
                bw.write("# All servers will be queried in that order.");bw.newLine();
                bw.write("# Server in front has higher priority");bw.newLine();
                bw.write("# Official server has the lowest priority");bw.newLine();
                bw.write("# No more legacy style link support!");bw.newLine();
                bw.write("# An Example:");bw.newLine();
                bw.write("# Root: http://127.0.0.1:25566/skins");bw.newLine();
                bw.newLine();
                bw.write("# SkinMe Default");bw.newLine();
                bw.write("Root: http://www.skinme.cc/uniskin");bw.newLine();
                bw.flush();bw.close();fw.close();
            }

            FileReader fr = new FileReader(cfgPath);
            BufferedReader br = new BufferedReader(fr);
            String theLine;
            while (br.ready()) {
                theLine = br.readLine().trim();
                if(theLine.startsWith("Root: ")){
                    UniSkinMod.roots.add(theLine.substring(6).trim());
                    UniSkinMod.log.info("Root Url Added: " + theLine.substring(6).trim());
                }else if((!theLine.startsWith("#"))&&(!theLine.equals(""))){
                    UniSkinMod.log.warn("Unknown line when phrasing config: "+theLine);
                }
            }
            br.close();
            fr.close();
        }catch(IOException e){
            UniSkinMod.log.warn("Error happened when loading configuration");
            e.printStackTrace();
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
