package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakil;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        RenderingRegistry.registerEntityRenderingHandler(LOTREntityMumakil.class, new LOTRRenderMumakil());
    }
}
