package org.devinprogress.uniskinmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UniSkinConfig {
    public List<String> rootURIs = new ArrayList<String>();
    public List<String> legacySkinURIs = new ArrayList<String>();
    public List<String> legacyCapeURIs = new ArrayList<String>();

    public static UniSkinConfig loadFromFile(File configFile) throws IOException {
        if (configFile == null) return null;
        configFile = configFile.getAbsoluteFile();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        if (!configFile.exists()) {
            UniSkinMod.log.info("Creating new configuration file {}", configFile.getPath());
            UniSkinConfig tmpCfg = new UniSkinConfig();
            tmpCfg.rootURIs.add("http://www.skinme.cc/uniskin");
            tmpCfg.rootURIs.add("https://skin.prinzeugen.net/usm");
            String json = gson.toJson(tmpCfg);
            configFile.getParentFile().mkdirs();
            FileWriter wr = new FileWriter(configFile);
            wr.write(json);
            wr.close();
        }
        UniSkinMod.log.info("Loading configuration file {}", configFile.getPath());
        return gson.fromJson(new FileReader(configFile), UniSkinConfig.class);
    }
}