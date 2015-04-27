package org.devinprogress.uniskinmod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import com.mojang.authlib.yggdrasil.response.MinecraftTexturesPayload;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
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
    private static Field profilesFieldAccessor;
    public static final Logger log= LogManager.getLogger("UniSkinMod");
    public static final List<String> roots=new ArrayList<String>();

    static{
        try{
            profilesFieldAccessor=ProfileSearchResultsResponse.class.getDeclaredField("profiles");
            profilesFieldAccessor.setAccessible(true);
        }catch(Exception ex){
            profilesFieldAccessor=null;
        }
    }

    private static Cache<String, Property> cache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();


    /** Hijack the constructor of NetworkPlayerInfo & Skull renderer*/
    public static GameProfile fillGameProfile(final GameProfile gameProfileIn) {
        //if (cache.getIfPresent(gameProfileIn)!=null) return gameProfileIn;
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
        //cache.put(gameProfileIn,gameProfileIn);
        return gameProfileIn;
    }

    public static ProfileSearchResultsResponse fillMissionProfile(ProfileSearchResultsResponse response, List<String> request){
        if(response.getProfiles().length<request.size()){
            List<GameProfile> profs=new ArrayList<GameProfile>();
            try{
                profs.addAll(Arrays.asList((GameProfile[]) profilesFieldAccessor.get(response)));

                for (String name : request) {
                    boolean ok=false;
                    for (GameProfile prof:profs) {
                        if (name.equalsIgnoreCase(prof.getName())) {
                            ok=true; break;
                        }
                    }
                    if (!ok) {
                        UUID uuid=getOfflineUUID(name);
                        profs.add(new GameProfile(uuid, name));
                        log.info(String.format("Player: %s has no uuid, assigning: %s",name,uuid.toString()));
                    }
                }

                GameProfile[] tmp=new GameProfile[profs.size()];
                for(int i=0;i<tmp.length;i++)
                    tmp[i]=profs.get(i);
                profilesFieldAccessor.set(response,tmp);
            }catch(Exception ex){
                ex.printStackTrace();
                return response;
            }
        }
        return response;
    }

    public static boolean isOnlinePlayer(GameProfile p){
        if (p.getName()==null) return true;
        if (p.getId().equals(getOfflineUUID(p.getName()))||
                p.getId().equals(getOfflineUUID(p.getName().toLowerCase()))) {
            log.info(String.format("Player: %s is in offline-mode.",p.getName()));
            return false;
        }
        log.info(String.format("Player: %s is in online-mode.",p.getName()));
        return true;
    }

    public static MinecraftProfilePropertiesResponse fillResponse(GameProfile p, MinecraftProfilePropertiesResponse orig){
        log.info(String.format("Fetching External Profile: %s.%s",p.getName(),orig==null?" Origin Response is NULL.":""));
        ProfileResponseBuilder builder=new ProfileResponseBuilder(orig,p.getName());
        return builder.getFilledResponse(roots);
    }

    public static UUID getOfflineUUID(String name){
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
    }
}
