package org.devinprogress.uniskinmod;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;

import cpw.mods.fml.common.DummyModContainer;
import cpw.mods.fml.common.LoadController;
import cpw.mods.fml.common.ModMetadata;

public class CoreContainer extends DummyModContainer{
    public CoreContainer()
    {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "uniskinmod";
        meta.name = "UniSkinMod";
        meta.version = "1.0.0";
        meta.authorList = Arrays.asList("RecursiveG");
        meta.description = "A Coremod provided the ability to access to many skin servers.\nView config/UniSkinMod.cfg for more information.";
        meta.url = "https://github.com/RecursiveG/UniSkinMod";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        bus.register(this);
        return true;
    }
}
