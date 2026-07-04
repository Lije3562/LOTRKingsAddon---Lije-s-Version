package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.client.MumakilInventoryKeyHandler;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilGeoInventoryScaled;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import lotr.client.render.entity.LOTRRenderNearHaradrim;
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

        this.registerHowdahArcherRenderer();

        MumakilInventoryKeyHandler mumakilInventoryKeyHandler = new MumakilInventoryKeyHandler();
        FMLCommonHandler.instance().bus().register(mumakilInventoryKeyHandler);
        MinecraftForge.EVENT_BUS.register(mumakilInventoryKeyHandler);

    }

    private void registerHowdahArcherRenderer() {
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntityMumakilHowdahArcher.class,
                new LOTRRenderNearHaradrim()
        );
        System.out.println("[LOTRMoreMobs] Registered Mumakil howdah archer renderer as LOTR Near Haradrim renderer.");
    }
}
