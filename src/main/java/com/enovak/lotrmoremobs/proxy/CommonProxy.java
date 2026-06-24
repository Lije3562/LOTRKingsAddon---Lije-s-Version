package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.handler.MumakilFearEventHandler;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {
    public void registerRenderers() {
    }

    public void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(new MumakilFearEventHandler());
    }
}
