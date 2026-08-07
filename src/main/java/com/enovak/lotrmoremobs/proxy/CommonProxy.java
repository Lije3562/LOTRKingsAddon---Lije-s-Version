package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.handler.MumakilAchievementEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilConquestUnitRollEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilDriverControlEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilEnemySightEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilEquipmentEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilFearEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilFormationCreditEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHiredMountEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHomeUnitRollEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHowdahArcherEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilInvasionProgressEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilInvasionUnitRollEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilMakeWayEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilMeleeHitboxEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilPlayerArrowOriginEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilShankHeldEffectHandler;
import com.enovak.lotrmoremobs.spawning.MumakilFormationReplacementService;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;
import com.enovak.lotrmoremobs.handler.PickupFilterEventHandler;

public class CommonProxy {
    public void registerRenderers() {
    }

    public void prepareMumakilHiredDriverGui(
            int driverEntityId,
            int mumakilEntityId
    ) {
    }

    public void registerEventHandlers() {
        MinecraftForge.EVENT_BUS.register(
                new MumakilFearEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilEquipmentEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilHiredMountEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilPlayerArrowOriginEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilMeleeHitboxEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilEnemySightEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilDriverControlEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilMakeWayEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilFormationCreditEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilInvasionProgressEventHandler()
        );
        MumakilFormationReplacementService replacementService =
                new MumakilFormationReplacementService();
        MinecraftForge.EVENT_BUS.register(replacementService);
        FMLCommonHandler.instance().bus().register(
                replacementService
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilInvasionUnitRollEventHandler(
                        replacementService
                )
        );
        MumakilConquestUnitRollEventHandler conquestUnitRollHandler =
                new MumakilConquestUnitRollEventHandler(
                        replacementService
                );
        MinecraftForge.EVENT_BUS.register(conquestUnitRollHandler);

        MumakilHomeUnitRollEventHandler homeUnitRollHandler =
                new MumakilHomeUnitRollEventHandler(
                        replacementService
                );
        MinecraftForge.EVENT_BUS.register(homeUnitRollHandler);

        MumakilAchievementEventHandler mumakilAchievementHandler =
                new MumakilAchievementEventHandler();

        MinecraftForge.EVENT_BUS.register(
                mumakilAchievementHandler
        );
        FMLCommonHandler.instance().bus().register(
                mumakilAchievementHandler
        );

        MumakilHowdahArcherEventHandler howdahArcherHandler =
                new MumakilHowdahArcherEventHandler();

        MinecraftForge.EVENT_BUS.register(
                howdahArcherHandler
        );
        FMLCommonHandler.instance().bus().register(
                howdahArcherHandler
        );

        MumakilShankHeldEffectHandler shankHeldEffectHandler =
                new MumakilShankHeldEffectHandler();

        FMLCommonHandler.instance().bus().register(
                shankHeldEffectHandler
        );
        MinecraftForge.EVENT_BUS.register(
                new PickupFilterEventHandler()
        );
    }
}
