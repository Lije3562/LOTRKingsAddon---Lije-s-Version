package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.ai.LOTRHowdahTargeting;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

/**
 * Dedicated combat support for NPCs in a Mumakil howdah.
 *
 * This handler intentionally runs from the Mumakil itself instead of depending on
 * the rider's normal target AI. The rider sits high in the howdah, so normal NPC
 * target boxes can miss enemies at the Mumakil's feet. Future howdah archers can
 * reuse the same LOTRHowdahTargeting helper without forcing the Mumakil to charge.
 */
public class MumakilHowdahCombatEventHandler {
    private static final int TARGET_RELAY_INTERVAL = 10;
    private static final int DEBUG_INTERVAL = 80;

    private static final double DRIVER_TARGET_HORIZONTAL_RANGE = 6.0D;
    private static final double DRIVER_TARGET_BELOW = 1.0D;
    private static final double DRIVER_TARGET_HEIGHT = 7.0D;
    private static final int DRIVER_TARGET_MAX_CANDIDATES = 12;

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null) {
            return;
        }

        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        this.updateMumakilHowdahCombat((LOTREntityMumakil)event.entityLiving);
    }

    private void updateMumakilHowdahCombat(LOTREntityMumakil mumakil) {
        if (!mumakil.hasMumakilHowdahEquipped()) {
            return;
        }

        LOTREntityNPC driver = this.getMumakilDriver(mumakil);
        long worldTime = mumakil.worldObj.getTotalWorldTime();

        if (driver == null) {
            this.debugOccasionally(mumakil, worldTime,
                    "[LOTRMoreMobs] Howdah combat relay skipped: no NPC driver. mumakilId="
                            + mumakil.getEntityId()
                            + " riddenBy="
                            + this.getEntityDebugName(mumakil.riddenByEntity)
                            + ".");
            return;
        }

        EntityLivingBase currentMumakilTarget = mumakil.getAttackTarget();
        if (currentMumakilTarget != null && currentMumakilTarget.isEntityAlive()) {
            if (driver.getAttackTarget() == null && LOTRHowdahTargeting.canNPCAttackTarget(driver, currentMumakilTarget)) {
                driver.setAttackTarget(currentMumakilTarget);
            }
            return;
        }

        EntityLivingBase currentDriverTarget = driver.getAttackTarget();
        if (currentDriverTarget != null && currentDriverTarget.isEntityAlive()
                && LOTRHowdahTargeting.canNPCAttackTarget(driver, currentDriverTarget)) {
            mumakil.setAttackTarget(currentDriverTarget);
            return;
        }

        if ((worldTime + mumakil.getEntityId()) % TARGET_RELAY_INTERVAL != 0L) {
            return;
        }

        EntityLivingBase target = LOTRHowdahTargeting.findNearestTargetAroundMumakil(
                mumakil,
                driver,
                DRIVER_TARGET_HORIZONTAL_RANGE,
                DRIVER_TARGET_BELOW,
                DRIVER_TARGET_HEIGHT,
                DRIVER_TARGET_MAX_CANDIDATES
        );

        if (target == null) {
            this.debugOccasionally(mumakil, worldTime,
                    "[LOTRMoreMobs] Howdah combat relay scan found no legal target. mumakilId="
                            + mumakil.getEntityId()
                            + " driver="
                            + driver.getClass().getName()
                            + ".");
            return;
        }

        LOTRHowdahTargeting.assignTargetToNPCAndMumakil(mumakil, driver, target);
        System.out.println("[LOTRMoreMobs] Howdah combat relay assigned target: mumakilId="
                + mumakil.getEntityId()
                + " driver="
                + driver.getClass().getName()
                + " target="
                + target.getClass().getName()
                + " targetId="
                + target.getEntityId()
                + ".");
    }

    private LOTREntityNPC getMumakilDriver(LOTREntityMumakil mumakil) {
        if (mumakil != null && mumakil.riddenByEntity instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)mumakil.riddenByEntity;

            if (npc.ridingEntity == mumakil) {
                return npc;
            }
        }

        return null;
    }

    private void debugOccasionally(LOTREntityMumakil mumakil, long worldTime, String message) {
        if ((worldTime + mumakil.getEntityId()) % DEBUG_INTERVAL == 0L) {
            System.out.println(message);
        }
    }

    private String getEntityDebugName(Entity entity) {
        if (entity == null) {
            return "null";
        }

        return entity.getClass().getName() + "#" + entity.getEntityId();
    }
}
