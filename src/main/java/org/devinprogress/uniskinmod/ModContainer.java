package org.devinprogress.uniskinmod;

import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.ModMetadata;

import java.util.Arrays;

/**
 * Metadata about UniSkinMod
 */
public class ModContainer extends DummyModContainer{
    public ModContainer(){
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "uniskinmod";
        meta.name = "UniSkinMod";
        meta.version = "1.3-dev2";
        meta.authorList = Arrays.asList("RecursiveG");
        meta.description = "This mod makes it possible to load skins & capes from servers besides the official one.";
        meta.url = "https://github.com/RecursiveG/UniSkinMod";
    }
}
