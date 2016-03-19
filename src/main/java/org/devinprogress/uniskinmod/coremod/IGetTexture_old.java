package org.devinprogress.uniskinmod.coremod;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;

import java.util.Map;

public interface IGetTexture_old {
    Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures_old(GameProfile profile, boolean requireSecure);
}
