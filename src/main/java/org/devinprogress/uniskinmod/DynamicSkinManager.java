package org.devinprogress.uniskinmod;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DynamicSkinManager {
    private final List<String> rootURIs;
    private final File localSkinDir;
    private final File mcSkinCacheDir;

    public class CachedDynamicSkin {
        ResourceLocation[] skin = null;
        ResourceLocation[] cape = null;
        ResourceLocation[] elytra = null;
        String[] skinURL = null;
        String[] capeURL = null;
        String[] elytraURL = null;
        String model = null;
        int skinInterval, capeInterval, elytraInterval;

        public boolean complete() {
            return skin != null && cape != null && elytra != null;
        }
    }

    public DynamicSkinManager(List<String> r, File localSkin, File mcCache) {
        rootURIs = r;
        localSkinDir = localSkin;
        mcSkinCacheDir = mcCache;
    }

    public final LoadingCache<String, CachedDynamicSkin> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES).build(new CacheLoader<String, CachedDynamicSkin>() {

                @Override
                public CachedDynamicSkin load(String playerName) throws Exception {
                    CachedDynamicSkin cache = new CachedDynamicSkin();
                    UniSkinProfile.DynamicTexture tmp;

                    List<String> roots = new ArrayList<String>();
                    roots.add("#local#");
                    roots.addAll(rootURIs);
                    // load piracy dynamic skins.
                    for (String root : roots) {
                        if (cache.complete()) break;
                        UniSkinProfile api = null;
                        if (root.equals("#local#")) {
                            File[] fileList = localSkinDir.listFiles();
                            if (fileList == null) continue;
                            for (File f : fileList) {
                                if (f.getName().equalsIgnoreCase(playerName + ".json")) {
                                    api = UniSkinProfile.getLocalProfile(f, new File(localSkinDir, "textures"));
                                    break;
                                }
                            }
                        } else {
                            api = UniSkinProfile.getProfile(playerName, root);
                        }
                        if (api == null) continue;

                        tmp = api.getDynamicSkin();
                        if (cache.skin == null && tmp != null) {
                            cache.model = tmp.skinModel;
                            cache.skinInterval = tmp.playTime;
                            ResourceLocation[] arr = new ResourceLocation[tmp.textures.length];
                            for (int i = 0; i < arr.length; i++) {
                                arr[i] = new ResourceLocation("skins/" + tmp.textures[i]);
                            }
                            cache.skin = arr;
                            cache.skinURL = tmp.url;
                        }

                        tmp = api.getDynamicCape();
                        if (cache.cape == null && tmp != null) {
                            cache.capeInterval = tmp.playTime;
                            ResourceLocation[] arr = new ResourceLocation[tmp.textures.length];
                            for (int i = 0; i < arr.length; i++) {
                                arr[i] = new ResourceLocation("skins/" + tmp.textures[i]);
                            }
                            cache.cape = arr;
                            cache.capeURL = tmp.url;
                        }

                        tmp = api.getDynamicElytra();
                        if (cache.elytra == null && tmp != null) {
                            cache.elytraInterval = tmp.playTime;
                            ResourceLocation[] arr = new ResourceLocation[tmp.textures.length];
                            for (int i = 0; i < arr.length; i++) {
                                arr[i] = new ResourceLocation("skins/" + tmp.textures[i]);
                            }
                            cache.elytra = arr;
                            cache.elytraURL = tmp.url;
                        }
                    }
                    forceLoadTextures(cache);
                    return cache;
                }
            });

    private void forceLoadTextures(CachedDynamicSkin cache) {
        SkinManager skinManager = Minecraft.getMinecraft().getSkinManager();
        if (cache.skinURL != null) {
            boolean slimModel = cache.model.equalsIgnoreCase("slim");
            for (int i = 0; i < cache.skinURL.length; i++) {
                String url = cache.skinURL[i];
                if (url.startsWith("local:")) {
                    File src = new File(url.substring(6));
                    forceLoadTexture(src, MinecraftProfileTexture.Type.SKIN, slimModel);
                } else {
                    skinManager.loadSkin(new MinecraftProfileTexture(url, slimModel ?
                            new HashMap<String, String>() {{
                                put("model", "slim");
                            }} : null), MinecraftProfileTexture.Type.SKIN);
                }
            }
        }

        if (cache.capeURL != null) {
            for (int i = 0; i < cache.capeURL.length; i++) {
                String url = cache.capeURL[i];
                if (url.startsWith("local:")) {
                    forceLoadTexture(new File(url.substring(6)), MinecraftProfileTexture.Type.CAPE, false);
                } else {
                    skinManager.loadSkin(new MinecraftProfileTexture(url, null), MinecraftProfileTexture.Type.CAPE);
                }
            }
        }

        if (cache.elytraURL != null) {
            for (int i = 0; i < cache.elytraURL.length; i++) {
                String url = cache.elytraURL[i];
                if (url.startsWith("local:")) {
                    forceLoadTexture(new File(url.substring(6)), MinecraftProfileTexture.Type.ELYTRA, false);
                } else {
                    skinManager.loadSkin(new MinecraftProfileTexture(url, null), MinecraftProfileTexture.Type.ELYTRA);
                }
            }
        }
    }

    public void forceLoadTexture(File sourceFile, MinecraftProfileTexture.Type textureType, boolean isAlex) {
        try {
            File dstFolder = this.mcSkinCacheDir;
            String sha256 = DigestUtils.sha256Hex(new FileInputStream(sourceFile)).toLowerCase();
            String dir = sha256.substring(0, 2);
            File subDir = new File(dstFolder, dir);
            subDir.mkdirs();
            File dstFile = new File(subDir, sha256);
            FileUtils.copyFile(sourceFile, dstFile);
            Map<String, String> metadata = (textureType == MinecraftProfileTexture.Type.SKIN && isAlex)
                    ? new HashMap<String, String>() {{
                put("model", "slim");
            }} : null;
            String url = "http://127.0.0.1/" + sha256;
            Minecraft.getMinecraft().getSkinManager().loadSkin(
                    new MinecraftProfileTexture(url, metadata), textureType);
        } catch (Exception ex) {
            UniSkinMod.log.catching(Level.WARN, ex);
        }
    }

    public String forceLoadTexture(String local_file, MinecraftProfileTexture.Type textureType, boolean isAlex) {
        try {
            if (local_file == null || textureType == null) return null;
            if (!local_file.startsWith("local:")) return null;
            File src = new File(local_file.substring(6));
            String sha256 = DigestUtils.sha256Hex(new FileInputStream(src)).toLowerCase();
            forceLoadTexture(src, textureType, isAlex);
            return "http://127.0.0.1/" + sha256;
        } catch (Exception ex) {
            UniSkinMod.log.catching(Level.WARN, ex);
            return null;
        }
    }
}