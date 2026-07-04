package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

/**
 * Lightweight Mumakil-specific target assist for a howdah driver.
 *
 * This is deliberately conservative. The normal LOTR target AI still gets first
 * chance. Only when the rider has no target, this AI occasionally checks a small
 * area around the Mumakil's lower/body box so enemies at the feet are not missed
 * purely because the rider is high in the howdah.
 */
public class LOTREntityAIMumakilHowdahRiderTarget extends EntityAIBase {
    private static final int TARGET_CHECK_INTERVAL = 40;
    private static final double NEAR_FOOT_TARGET_RANGE = 18.0D;
    private static final double SEARCH_VERTICAL_EXPANSION = 2.0D;
    private static final int MAX_CANDIDATES_PER_SCAN = 24;

    private final LOTREntityNPC rider;
    private EntityLivingBase targetEntity;

    public LOTREntityAIMumakilHowdahRiderTarget(LOTREntityNPC rider) {
        this.rider = rider;
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (this.rider == null || this.rider.worldObj == null || this.rider.worldObj.isRemote) {
            return false;
        }

        EntityLivingBase currentTarget = this.rider.getAttackTarget();
        if (currentTarget != null && currentTarget.isEntityAlive()) {
            return false;
        }

        if ((this.rider.ticksExisted + this.rider.getEntityId()) % TARGET_CHECK_INTERVAL != 0) {
            return false;
        }

        LOTREntityMumakil mumakil = this.getMountedHowdahMumakil();
        if (mumakil == null) {
            return false;
        }

        this.targetEntity = this.findBestTarget(mumakil);
        return this.targetEntity != null;
    }

    @Override
    public void startExecuting() {
        this.rider.setAttackTarget(this.targetEntity);

        LOTREntityMumakil mumakil = this.getMountedHowdahMumakil();
        if (mumakil != null) {
            mumakil.setAttackTarget(this.targetEntity);
        }

        super.startExecuting();
    }

    @Override
    public boolean continueExecuting() {
        return false;
    }

    private LOTREntityMumakil getMountedHowdahMumakil() {
        if (this.rider.ridingEntity instanceof LOTREntityMumakil) {
            LOTREntityMumakil mumakil = (LOTREntityMumakil)this.rider.ridingEntity;
            if (mumakil.hasMumakilHowdahEquipped()) {
                return mumakil;
            }
        }

        return null;
    }

    private EntityLivingBase findBestTarget(LOTREntityMumakil mumakil) {
        AxisAlignedBB searchBox = mumakil.boundingBox.expand(
                NEAR_FOOT_TARGET_RANGE,
                SEARCH_VERTICAL_EXPANSION,
                NEAR_FOOT_TARGET_RANGE
        );
        List list = this.rider.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);

        EntityLivingBase bestTarget = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;
        int checked = 0;

        for (int i = 0; i < list.size(); ++i) {
            if (checked >= MAX_CANDIDATES_PER_SCAN) {
                break;
            }

            EntityLivingBase candidate = (EntityLivingBase)list.get(i);
            if (!this.isValidTarget(mumakil, candidate)) {
                continue;
            }

            ++checked;
            int priority = this.getTargetPriority(candidate);
            double distanceSq = mumakil.getDistanceSqToEntity(candidate);

            if (priority < bestPriority || priority == bestPriority && distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestPriority = priority;
                bestDistanceSq = distanceSq;
            }
        }

        return bestTarget;
    }

    private int getTargetPriority(EntityLivingBase target) {
        if (target instanceof IMob) {
            return 0;
        }

        if (target instanceof LOTREntityNPC) {
            return 1;
        }

        return 2;
    }

    private boolean isValidTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (target == null
                || target == this.rider
                || target == mumakil
                || target == mumakil.riddenByEntity
                || !target.isEntityAlive()) {
            return false;
        }

        if (target instanceof EntityPlayer && ((EntityPlayer)target).capabilities.isCreativeMode) {
            return false;
        }

        if (!LOTRMod.canNPCAttackEntity((EntityCreature)this.rider, target, false)) {
            return false;
        }

        return mumakil.canEntityBeSeen(target) || this.rider.canEntityBeSeen(target);
    }
}
