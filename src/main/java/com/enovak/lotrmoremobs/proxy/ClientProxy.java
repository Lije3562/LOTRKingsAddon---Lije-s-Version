package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.client.MumakilShankFovHandler; // MUMAKIL_SHANK_SYSTEM_V1_1
import com.enovak.lotrmoremobs.client.MumakilHiredDriverGuiHandler;
import com.enovak.lotrmoremobs.client.MumakilInventoryKeyHandler;
import com.enovak.lotrmoremobs.client.UnitTradePledgeNavigationHandler;
import com.enovak.lotrmoremobs.client.config.MumakilConfigChangeHandler;
import com.enovak.lotrmoremobs.client.gui.MumakilHiredDriverGuiContext;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHirePreviewDriver;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilHowdahArcher;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilHirePreviewDriver;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilDriver;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilGeoInventoryScaled;
import com.enovak.lotrmoremobs.render.entity.LOTRRenderMumakilHowdahPlayer;
import com.enovak.lotrmoremobs.render.tileentity.LOTRRenderMumakilSpawnCageContext;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import lotr.common.tileentity.LOTRTileEntityMobSpawner;
import lotr.common.entity.npc.LOTREntitySouthronChampion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import software.bernie.geckolib3.GeckoLib;
import com.enovak.lotrmoremobs.client.command.CommandPickupFilterGui;
import net.minecraftforge.client.ClientCommandHandler;
import com.enovak.lotrmoremobs.client.pickupfilter.PickupFilterGuiOpenHandler;
import com.enovak.lotrmoremobs.client.pickupfilter.PickupFilterInventoryButtonHandler;

public class ClientProxy extends CommonProxy {
    @Override
    public void registerRenderers() {
        GeckoLib.initialize();

        // Uses the stable Geo renderer path, with a wrapper that only shrinks the inventory preview.
        LOTRRenderMumakilGeoInventoryScaled mumakilInventoryRenderer =
                new LOTRRenderMumakilGeoInventoryScaled();
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntityMumakil.class,
                mumakilInventoryRenderer
        );
        FMLCommonHandler.instance().bus().register(mumakilInventoryRenderer);
        MinecraftForge.EVENT_BUS.register(mumakilInventoryRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(
                LOTRTileEntityMobSpawner.class,
                new LOTRRenderMumakilSpawnCageContext()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntityMumakilHirePreviewDriver.class,
                new LOTRRenderMumakilHirePreviewDriver()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntitySouthronChampion.class,
                new LOTRRenderMumakilDriver()
        );
        RenderingRegistry.registerEntityRenderingHandler(
                EntityPlayer.class,
                new LOTRRenderMumakilHowdahPlayer()
        );

        this.registerHowdahArcherRenderer();

        MumakilInventoryKeyHandler mumakilInventoryKeyHandler = new MumakilInventoryKeyHandler();
        FMLCommonHandler.instance().bus().register(mumakilInventoryKeyHandler);
        MinecraftForge.EVENT_BUS.register(mumakilInventoryKeyHandler);

    }

    @Override
    public void prepareMumakilHiredDriverGui(int driverEntityId, int mumakilEntityId) {
        MumakilHiredDriverGuiContext.begin(driverEntityId, mumakilEntityId);
    }

    @Override
    public void registerEventHandlers() {
        super.registerEventHandlers();

        FMLCommonHandler.instance().bus().register(
                new MumakilConfigChangeHandler()
        );
        MinecraftForge.EVENT_BUS.register(new MumakilShankFovHandler());
        MinecraftForge.EVENT_BUS.register(
                new UnitTradePledgeNavigationHandler()
        );
        ClientCommandHandler.instance.registerCommand(
                new CommandPickupFilterGui()
        );

        MumakilHiredDriverGuiHandler hiredDriverGuiHandler = new MumakilHiredDriverGuiHandler();
        MinecraftForge.EVENT_BUS.register(hiredDriverGuiHandler);
        FMLCommonHandler.instance().bus().register(hiredDriverGuiHandler);

        FMLCommonHandler.instance().bus().register(
                new PickupFilterGuiOpenHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new PickupFilterInventoryButtonHandler()
        );
    }

    private void registerHowdahArcherRenderer() {
        RenderingRegistry.registerEntityRenderingHandler(
                LOTREntityMumakilHowdahArcher.class,
                new LOTRRenderMumakilHowdahArcher()
        );
        System.out.println("[LOTRMoreMobs] Registered Mumak howdah archer renderer.");
    }
}
