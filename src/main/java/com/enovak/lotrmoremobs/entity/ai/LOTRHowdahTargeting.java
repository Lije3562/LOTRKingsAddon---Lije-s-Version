package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

/**
 * Shared target-acquisition utilities for NPCs fighting from a Mumakil howdah.
 *
 * The important design choice is that target search can be anchored to the
 * Mumakil's body/feet instead of the rider's high render/seat position. This is
 * useful both for the driver telling the Mumakil what to attack and for future
 * Southron archers firing down from the howdah.
 */
public final class LOTRHowdahTargeting {
    private LOTRHowdahTargeting() {
    }

    public static EntityLivingBase findNearestTargetAroundMumakil(
            LOTREntityMumakil mumakil,
            LOTREntityNPC howdahNPC,
            double horizontalPadding,
            double belowMumakil,
            double verticalHeightFromFeet,
            int maxCandidates
    ) {
        if (mumakil == null || howdahNPC == null || mumakil.worldObj == null) {
            return null;
        }

        if (!mumakil.hasMumakilHowdahEquipped()) {
            return null;
        }

        if (maxCandidates <= 0) {
            return null;
        }

        AxisAlignedBB box = mumakil.boundingBox;
        AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(
                box.minX - horizontalPadding,
                box.minY - belowMumakil,
                box.minZ - horizontalPadding,
                box.maxX + horizontalPadding,
                box.minY + verticalHeightFromFeet,
                box.maxZ + horizontalPadding
        );

        List list = mumakil.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);
        EntityLivingBase closest = null;
        double closestDistance = Double.MAX_VALUE;
        int checked = 0;

        for (int i = 0; i < list.size() && checked < maxCandidates; ++i) {
            Object value = list.get(i);

            if (!(value instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase candidate = (EntityLivingBase)value;

            if (candidate == mumakil || candidate == howdahNPC || candidate == howdahNPC.ridingEntity
                    || !candidate.isEntityAlive()) {
                continue;
            }

            ++checked;

            if (!canNPCAttackTarget(howdahNPC, candidate)) {
                continue;
            }

            double distance = mumakil.getDistanceSqToEntity(candidate);

            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        return closest;
    }

    public static boolean canNPCAttackTarget(LOTREntityNPC npc, EntityLivingBase target) {
        if (npc == null || target == null || target == npc || !target.isEntityAlive()) {
            return false;
        }

        if (!(npc instanceof EntityCreature)) {
            return false;
        }

        return LOTRMod.canNPCAttackEntity((EntityCreature)npc, target, false);
    }

    public static void assignTargetToNPCAndMumakil(
            LOTREntityMumakil mumakil,
            LOTREntityNPC npc,
            EntityLivingBase target
    ) {
        if (!canNPCAttackTarget(npc, target)) {
            return;
        }

        if (npc.getAttackTarget() != target) {
            npc.setAttackTarget(target);
        }

        if (mumakil != null && mumakil.getAttackTarget() != target) {
            mumakil.setAttackTarget(target);
        }
    }

    public static LOTREntityMumakil getRiddenHowdahMumakil(LOTREntityNPC npc) {
        if (npc == null || !(npc.ridingEntity instanceof LOTREntityMumakil)) {
            return null;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)npc.ridingEntity;
        return mumakil.hasMumakilHowdahEquipped() ? mumakil : null;
    }
}
