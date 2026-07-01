package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.client.MumakilInventoryKeyHandler;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilGeoInventoryScaled;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import software.bernie.geckolib3.GeckoLib;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        GeckoLib.initialize();

        // Uses the stable Geo renderer path, with a wrapper that only shrinks the inventory preview.
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntityMumakil.class,
                new LOTRRenderMumakilGeoInventoryScaled()
        );


        MumakilInventoryKeyHandler mumakilInventoryKeyHandler = new MumakilInventoryKeyHandler();
        FMLCommonHandler.instance().bus().register(mumakilInventoryKeyHandler);
        MinecraftForge.EVENT_BUS.register(mumakilInventoryKeyHandler);

    }
}