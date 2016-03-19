package org.devinprogress.uniskinmod;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DynamicSkinManager {
    private final List<String> rootURIs;
    private final File localSkinDir;

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

    public DynamicSkinManager(List<String> r, File localSkin) {
        rootURIs = r;
        localSkinDir = localSkin;
    }

    public final LoadingCache<String, CachedDynamicSkin> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS).build(new CacheLoader<String, CachedDynamicSkin>() {

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
                skinManager.loadSkin(new MinecraftProfileTexture(cache.skinURL[i], slimModel ?
                        new HashMap<String, String>() {{
                            put("model", "slim");
                        }} : null), MinecraftProfileTexture.Type.SKIN);
            }
        }

        if (cache.capeURL != null) {
            for (int i = 0; i < cache.capeURL.length; i++) {
                skinManager.loadSkin(new MinecraftProfileTexture(cache.capeURL[i], null), MinecraftProfileTexture.Type.CAPE);
            }
        }

        if (cache.elytraURL != null) {
            for (int i = 0; i < cache.elytraURL.length; i++) {
                skinManager.loadSkin(new MinecraftProfileTexture(cache.elytraURL[i], null), MinecraftProfileTexture.Type.ELYTRA);
            }
        }
    }
}