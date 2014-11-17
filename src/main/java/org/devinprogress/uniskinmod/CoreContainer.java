package org.devinprogress.uniskinmod;

import java.util.Arrays;

import com.google.common.eventbus.EventBus;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.ModMetadata;

public class CoreContainer extends DummyModContainer {
    public CoreContainer()
    {
        super(new ModMetadata());
        ModMetadata meta = getMetadata();
        meta.modId = "uniskinmod";
        meta.name = "Universal Skin Mod";
        meta.version = "1.1";
        meta.authorList = Arrays.asList("RecursiveG");
        meta.description = "A Coremod provided the ability to access to many skin servers.";
        meta.url = "https://github.com/RecursiveG/UniSkinMod";
    }

    @Override
    public boolean registerBus(EventBus bus, LoadController controller)
    {
        bus.register(this);
        return true;
    }
}
