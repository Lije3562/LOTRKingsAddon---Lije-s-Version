package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.handler.MumakilEquipmentEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilFearEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHireEventHandler;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {
    public void registerRenderers() {
    }

    public void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new MumakilFearEventHandler());
        MinecraftForge.EVENT_BUS.register(new MumakilEquipmentEventHandler());
        MinecraftForge.EVENT_BUS.register(new MumakilHireEventHandler());
    }
}
