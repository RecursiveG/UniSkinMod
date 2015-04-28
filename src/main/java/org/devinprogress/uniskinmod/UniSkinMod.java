package org.devinprogress.uniskinmod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * This file is part of Universal Skin Mod,
 * Copyright (C) 2014  RecursiveG
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
public class UniSkinMod {
    public static final Logger log= LogManager.getLogger("UniSkinMod");
    public static final List<String> roots=new ArrayList<String>();
    private static Cache<String, Property> cache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    /** Hijack the GameProfile in NetworkPlayerInfo & Skull renderer*/
    public static GameProfile fillGameProfile(final GameProfile gameProfileIn) {
        if (gameProfileIn==null) return null;
        final String player_name=gameProfileIn.getName();

        Property finalP=null;
        try {
            finalP = cache.get(player_name, new Callable<Property>() {
                @Override
                public Property call() throws Exception {
                    log.info("Filling profile for player: "+player_name);
                    Property textureProperty = (Property) Iterables.getFirst(gameProfileIn.getProperties().get("textures"), (Object) null);
                    MojangTexturePayload payload = MojangTexturePayload.fromProperty(textureProperty);
                    if (payload==null) payload=new MojangTexturePayload(player_name);

                    for (String root : roots) {
                        if (payload.isComplete()) break;
                        UniSkinApiProfile api=UniSkinApiProfile.getProfile(player_name,root);
                        if (api==null) continue;
                        payload.addCape(api.getCapeURL());
                        payload.addSkin(api.getSkinURL(),api.getModel());
                    }

                    return payload.toProperty();
                }
            });
        }catch(Exception ex){
            ex.printStackTrace();
            return gameProfileIn;
        }

        gameProfileIn.getProperties().removeAll("textures");
        gameProfileIn.getProperties().put("textures", finalP);
        return gameProfileIn;
    }
}
