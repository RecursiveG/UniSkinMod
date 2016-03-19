package org.devinprogress.uniskinmod;

import com.google.common.base.Charsets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.Level;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Universal Skin API
 */
public class UniSkinProfile {
    private static Cache<String, ProfileJSON> cache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    private class ProfileJSON {
        public String player_name = "";
        public long last_update;
        public List<String> model_preference;
        public Map<String, String> skins;
        public String cape;
    }

    public static class DynamicTexture {
        public final int playTime;
        public final String[] textures; //Hashes
        public final String skinModel;
        public final String[] url;      //url must match the hashes in textures, or use "local#" prefix to indicate a local file

        public DynamicTexture(int p, String[] t, String tp, String[] u) {
            playTime = p;
            textures = t;
            skinModel = tp;
            url = u;
        }
    }

    public static UniSkinProfile getProfile(final String name, String root) {
        if (root.endsWith("/")) root = root.substring(0, root.length() - 1);
        UniSkinProfile prof = new UniSkinProfile(name, root);
        return prof.hasProfile ? prof : null;
    }

    public static UniSkinProfile getLocalProfile(File profileFile, File textureFolder) {
        if (profileFile == null || textureFolder == null) return null;
        UniSkinProfile prof = new UniSkinProfile(profileFile, textureFolder);
        return prof.hasProfile ? prof : null;
    }

    // === Profile === //
    public boolean hasProfile = false;

    private static String httpRequest(String url) {
        try {
            UniSkinMod.log.info("Fetching URL: " + url);
            Proxy proxy = Minecraft.getMinecraft().getProxy();
            HttpURLConnection conn = (HttpURLConnection) (new URL(url)).openConnection(proxy);
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setReadTimeout(1000 * 5);
            conn.setConnectTimeout(1000 * 5);
            conn.setUseCaches(false);
            conn.setInstanceFollowRedirects(true);
            conn.connect();
            int errorNo = conn.getResponseCode();
            if (errorNo > 299 || errorNo < 200) {
                UniSkinMod.log.error("Server returned a error {} @ {}", errorNo, url);
                return null;
            }

            InputStream iStream;
            if ("gzip".equals(conn.getContentEncoding())) {
                iStream = new GZIPInputStream(conn.getInputStream());
            } else {
                iStream = conn.getInputStream();
            }
            return IOUtils.toString(iStream, Charsets.UTF_8);
        } catch (IOException ex) {
            UniSkinMod.log.debug(Level.WARN, ex);
            UniSkinMod.log.warn("Error happened when fetching {}", url);
            return null;
        }
    }

    private static final String URL_PROFILE_FMT = "{root}/{player_name}.json";
    private static final String URL_TEXTURE_FMT = "{root}/textures/{texture_hash}";
    private String skin = null, cape = null, elytra = null,
            model = null, name = null;
    private DynamicTexture dynSkin = null, dynCape = null, dynElytra = null;
    private long update = 0;

    private UniSkinProfile(String name, String root) {
        String profileURL;
        try {
            profileURL = URL_PROFILE_FMT.replace("{root}", root).replace("{player_name}", URLEncoder.encode(name, "UTF-8"));
        } catch (UnsupportedEncodingException ex) {
            UniSkinMod.log.catching(Level.WARN, ex);
            profileURL = URL_PROFILE_FMT.replace("{root}", root).replace("{player_name}", name);
        }
        final String profileURL_final = profileURL;

        ProfileJSON json;
        try {
            UniSkinMod.log.info("Accessing URL: " + profileURL);
            json = cache.get(profileURL, new Callable<ProfileJSON>() {
                @Override
                public ProfileJSON call() throws Exception {
                    ProfileJSON ret = null;
                    String profileJSON = httpRequest(profileURL_final);
                    if (profileJSON != null) ret = (new Gson()).fromJson(profileJSON, ProfileJSON.class);
                    if (ret == null) ret = new ProfileJSON();
                    return ret;
                }
            });
        } catch (ExecutionException ex) {
            UniSkinMod.log.catching(Level.FATAL, ex);
            return;
        }

        if (!json.player_name.equalsIgnoreCase(name)) return;
        for (String m : json.model_preference) {
            if (json.skins.containsKey(m) && (m.equals("default") || m.equals("slim"))) {
                model = m;
                skin = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", json.skins.get(m));
                hasProfile = true;
                UniSkinMod.log.info("Player Skin Selected: {} {} {}", name, model, json.skins.get(m));
                break;
            }
        }

        if (json.model_preference.contains("cape") && json.skins.containsKey("cape")) {
            String tmp = json.skins.get("cape");
            cape = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", tmp);
            hasProfile = true;
            UniSkinMod.log.info("Player Cape Selected: {} {}", name, tmp);
        }
        if (json.cape != null && json.cape.length() > 3 && cape != null) {
            cape = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", json.cape);
            hasProfile = true;
            UniSkinMod.log.info("Player Cape Selected: {} {}", name, json.cape);
        }

        if (json.model_preference.contains("elytra") && json.skins.containsKey("elytra")) {
            String tmp = json.skins.get("elytra");
            elytra = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", tmp);
            hasProfile = true;
            UniSkinMod.log.info("Player Elytra Selected: {} {}", name, tmp);
        }

        for (String m : json.model_preference) {
            if (json.skins.containsKey(m) && (m.equals("default_dynamic") || m.equals("slim_dynamic"))) {
                if (json.model_preference.contains("slim") && json.model_preference.indexOf("slim") < json.model_preference.indexOf(m))
                    break;
                if (json.model_preference.contains("default") && json.model_preference.indexOf("default") < json.model_preference.indexOf(m))
                    break;

                String model = m.equals("default_dynamic") ? "default" : "slim";
                String tmp = json.skins.get(m);
                int time;
                try {
                    time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
                } catch (NumberFormatException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                    UniSkinMod.log.warn("Bad dynamic model texture string {} {}/{}", m, root, name);
                    continue;
                }
                if (time <= 0) continue;
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", hashes[i]);
                dynSkin = new DynamicTexture(time, hashes, model, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Skin Selected: {}", name);
                break;
            }
        }

        if (json.model_preference.contains("cape_dynamic") && json.skins.containsKey("cape_dynamic") &&
                (!json.model_preference.contains("cape") ||
                        json.model_preference.indexOf("cape_dynamic") < json.model_preference.indexOf("cape"))) {
            String tmp = json.skins.get("cape_dynamic");
            int time;
            try {
                time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
            } catch (NumberFormatException ex) {
                UniSkinMod.log.catching(Level.WARN, ex);
                UniSkinMod.log.warn("Bad dynamic model texture string cape_dynamic {}/{}", root, name);
                time = -1;
            }
            if (time > 0) {
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", hashes[i]);
                dynCape = new DynamicTexture(time, hashes, null, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Cape Selected: {}", name);
            }
        }

        if (json.model_preference.contains("elytra_dynamic") && json.skins.containsKey("elytra_dynamic") &&
                (!json.model_preference.contains("elytra") ||
                        json.model_preference.indexOf("elytra_dynamic") < json.model_preference.indexOf("elytra"))) {
            String tmp = json.skins.get("elytra_dynamic");
            int time;
            try {
                time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
            } catch (NumberFormatException ex) {
                UniSkinMod.log.catching(Level.WARN, ex);
                UniSkinMod.log.warn("Bad dynamic model texture string elytra_dynamic {}/{}", root, name);
                time = -1;
            }
            if (time > 0) {
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = URL_TEXTURE_FMT.replace("{root}", root).replace("{texture_hash}", hashes[i]);
                dynElytra = new DynamicTexture(time, hashes, null, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Elytra Selected: {}", name);
            }
        }

        update = json.last_update;
        this.name = json.player_name;
    }

    private UniSkinProfile(File profileFile, File textureFolder) {
        ProfileJSON json;
        try {
            json = (new Gson()).fromJson(new FileReader(profileFile), ProfileJSON.class);
        } catch (FileNotFoundException ex) {
            return;
        }

        if (!json.player_name.equalsIgnoreCase(name)) return;
        for (String m : json.model_preference) {
            if (json.skins.containsKey(m) && (m.equals("default") || m.equals("slim"))) {
                model = m;
                skin = new File(textureFolder, json.skins.get(m)).toURI().toString();
                hasProfile = true;
                UniSkinMod.log.info("Player Skin Selected: {} {} {}", name, model, json.skins.get(m));
                break;
            }
        }

        if (json.model_preference.contains("cape") && json.skins.containsKey("cape")) {
            String tmp = json.skins.get("cape");
            cape = new File(textureFolder, tmp).toURI().toString();
            hasProfile = true;
            UniSkinMod.log.info("Player Cape Selected: {} {}", name, tmp);
        }
        if (json.cape != null && json.cape.length() > 3 && cape != null) {
            cape = new File(textureFolder, json.cape).toURI().toString();
            hasProfile = true;
            UniSkinMod.log.info("Player Cape Selected: {} {}", name, json.cape);
        }

        if (json.model_preference.contains("elytra") && json.skins.containsKey("elytra")) {
            String tmp = json.skins.get("elytra");
            elytra = new File(textureFolder, tmp).toURI().toString();
            hasProfile = true;
            UniSkinMod.log.info("Player Elytra Selected: {} {}", name, tmp);
        }

        for (String m : json.model_preference) {
            if (json.skins.containsKey(m) && (m.equals("default_dynamic") || m.equals("slim_dynamic"))) {
                if (json.model_preference.contains("slim") && json.model_preference.indexOf("slim") < json.model_preference.indexOf(m))
                    break;
                if (json.model_preference.contains("default") && json.model_preference.indexOf("default") < json.model_preference.indexOf(m))
                    break;

                String model = m.equals("default_dynamic") ? "default" : "slim";
                String tmp = json.skins.get(m);
                int time;
                try {
                    time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
                } catch (NumberFormatException ex) {
                    UniSkinMod.log.catching(Level.WARN, ex);
                    UniSkinMod.log.warn("Bad dynamic model texture string {} local/{}", m, name);
                    continue;
                }
                if (time <= 0) continue;
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++) urls[i] = new File(textureFolder, hashes[i]).toURI().toString();
                dynSkin = new DynamicTexture(time, hashes, model, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Skin Selected: {}", name);
                break;
            }
        }

        if (json.model_preference.contains("cape_dynamic") && json.skins.containsKey("cape_dynamic") &&
                (!json.model_preference.contains("cape") ||
                        json.model_preference.indexOf("cape_dynamic") < json.model_preference.indexOf("cape"))) {
            String tmp = json.skins.get("cape_dynamic");
            int time;
            try {
                time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
            } catch (NumberFormatException ex) {
                UniSkinMod.log.catching(Level.WARN, ex);
                UniSkinMod.log.warn("Bad dynamic model texture string cape_dynamic local/{}", name);
                time = -1;
            }
            if (time > 0) {
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = new File(textureFolder, hashes[i]).toURI().toString();
                dynCape = new DynamicTexture(time, hashes, null, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Cape Selected: {}", name);
            }
        }

        if (json.model_preference.contains("elytra_dynamic") && json.skins.containsKey("elytra_dynamic") &&
                (!json.model_preference.contains("elytra") ||
                        json.model_preference.indexOf("elytra_dynamic") < json.model_preference.indexOf("elytra"))) {
            String tmp = json.skins.get("elytra_dynamic");
            int time;
            try {
                time = Integer.parseInt(tmp.substring(0, tmp.indexOf(',')));
            } catch (NumberFormatException ex) {
                UniSkinMod.log.catching(Level.WARN, ex);
                UniSkinMod.log.warn("Bad dynamic model texture string elytra_dynamic local/{}", name);
                time = -1;
            }
            if (time > 0) {
                String[] hashes = tmp.substring(tmp.indexOf(",") + 1).split(",");
                String[] urls = new String[hashes.length];
                for (int i = 0; i < urls.length; i++)
                    urls[i] = new File(textureFolder, hashes[i]).toURI().toString();
                dynElytra = new DynamicTexture(time, hashes, null, urls);
                hasProfile = true;
                UniSkinMod.log.info("Dynamic Elytra Selected: {}", name);
            }
        }

        update = json.last_update;
        this.name = json.player_name;
    }

    public String getSkinURL() {
        return skin;
    }

    public String getCapeURL() {
        return cape;
    }

    public String getModel() {
        return model;
    }

    public String getElytraURL() {
        return elytra;
    }

    public long getLastUpdate() {
        return update;
    }

    public String getName() {
        return name;
    }

    public DynamicTexture getDynamicSkin() {
        return dynSkin;
    }

    public DynamicTexture getDynamicCape() {
        return dynCape;
    }

    public DynamicTexture getDynamicElytra() {
        return dynElytra;
    }
}