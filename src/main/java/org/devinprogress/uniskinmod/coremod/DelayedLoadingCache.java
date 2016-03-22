package org.devinprogress.uniskinmod.coremod;

import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class DelayedLoadingCache extends ForwardingLoadingCache.SimpleForwardingLoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> {
    public static LoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> wrap(LoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> inside) {
        return new DelayedLoadingCache(inside);
    }

    private DelayedLoadingCache(LoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> inside) {
        super(inside);
    }

    private final Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> defaultReturn = Collections.emptyMap();
    private Set<GameProfile> onLoading = Collections.newSetFromMap(new ConcurrentHashMap<GameProfile, Boolean>());

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> get(GameProfile key) throws ExecutionException {
        final GameProfile gp = key;
        if (onLoading.contains(key)) return defaultReturn;
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> ret = super.getIfPresent(gp);
        if (ret == null) {
            onLoading.add(gp);
            String name = gp.getName() == null ? gp.toString() : gp.getName();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DelayedLoadingCache.super.getUnchecked(gp);
                    onLoading.remove(gp);
                }
            }, "Skin-Fetch-" + name).start();
            return defaultReturn;
        }
        return ret;
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getUnchecked(GameProfile key) {
        try {
            return get(key);
        } catch (ExecutionException ex) {
            throw new UncheckedExecutionException(ex);
        }
    }
}
