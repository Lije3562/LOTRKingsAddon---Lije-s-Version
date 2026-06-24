package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilGeo;
import cpw.mods.fml.client.registry.RenderingRegistry;
import software.bernie.geckolib3.GeckoLib;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        GeckoLib.initialize();
        // Experiment branch only: swap this back to LOTRRenderMumakil to return to the stable ModelBase renderer.
        RenderingRegistry.registerEntityRenderingHandler(LOTREntityMumakil.class, new LOTRRenderMumakilGeo());
    }
}
