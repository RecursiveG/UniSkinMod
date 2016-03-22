package org.devinprogress.uniskinmod.coremod;

import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.Map;

public interface ILoadSkinFromCache_old {
    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> loadSkinFromCache_old(GameProfile profile);

    LoadingCache<GameProfile, Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>> getLoadingCache();
}
