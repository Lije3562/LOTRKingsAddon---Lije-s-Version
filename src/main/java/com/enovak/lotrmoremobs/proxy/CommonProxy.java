package com.enovak.lotrmoremobs.proxy;

import com.enovak.lotrmoremobs.handler.GandalfSurvivalDamageEventHandler; // GANDALF_SURVIVAL_DAMAGE_V1
import com.enovak.lotrmoremobs.handler.MumakilShankHeldEffectHandler; // MUMAKIL_SHANK_SYSTEM_V1_1
import com.enovak.lotrmoremobs.handler.MumakilEquipmentEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilFearEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilDriverControlEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHiredMountEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilMeleeHitboxEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilEnemySightEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilFormationCreditEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilAchievementEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilInvasionProgressEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilHowdahArcherEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilPlayerArrowOriginEventHandler;
import com.enovak.lotrmoremobs.handler.MumakilMakeWayEventHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import net.minecraftforge.common.MinecraftForge;

public class CommonProxy {
    public void registerRenderers() {
    }

    public void prepareMumakilHiredDriverGui(int driverEntityId, int mumakilEntityId) {
    }

    public void registerEventHandlers() {
                MinecraftForge.EVENT_BUS.register(
                new GandalfSurvivalDamageEventHandler()
        );
MinecraftForge.EVENT_BUS.register(new MumakilFearEventHandler());
        MinecraftForge.EVENT_BUS.register(new MumakilEquipmentEventHandler());
        MinecraftForge.EVENT_BUS.register(new MumakilHiredMountEventHandler());
        MinecraftForge.EVENT_BUS.register(
                new MumakilPlayerArrowOriginEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(new MumakilMeleeHitboxEventHandler()); // MUMAKIL_EDGE_MELEE_AND_REACH_V1
        MinecraftForge.EVENT_BUS.register(new MumakilEnemySightEventHandler()); // HIRED_MUMAKIL_NEAR_HARAD_ENEMY_SIGHT_V1
        MinecraftForge.EVENT_BUS.register(new MumakilDriverControlEventHandler());
        MinecraftForge.EVENT_BUS.register(new MumakilMakeWayEventHandler());
        MinecraftForge.EVENT_BUS.register(
                new MumakilFormationCreditEventHandler()
        );
        MinecraftForge.EVENT_BUS.register(
                new MumakilInvasionProgressEventHandler()
        );
        MumakilAchievementEventHandler mumakilAchievementHandler =
                new MumakilAchievementEventHandler();
        MinecraftForge.EVENT_BUS.register(mumakilAchievementHandler);
        FMLCommonHandler.instance().bus().register(
                mumakilAchievementHandler
        );

        MumakilHowdahArcherEventHandler howdahArcherHandler = new MumakilHowdahArcherEventHandler();
        MinecraftForge.EVENT_BUS.register(howdahArcherHandler);
        FMLCommonHandler.instance().bus().register(howdahArcherHandler);

        MumakilShankHeldEffectHandler shankHeldEffectHandler =
                new MumakilShankHeldEffectHandler();
        FMLCommonHandler.instance().bus().register(shankHeldEffectHandler);
    }
}
