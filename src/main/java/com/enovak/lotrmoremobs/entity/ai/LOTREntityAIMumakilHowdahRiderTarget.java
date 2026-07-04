package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

/**
 * Lightweight Mumakil-specific target assist for a howdah driver.
 *
 * Vanilla/LOTR nearest-target AI is centered on the rider's own bounding box.
 * On a Mumakil, the driver is high in the howdah, so enemies near the feet can
 * fall outside the rider's normal vertical search area even when followRange is
 * boosted. This AI keeps the normal LOTR faction/allegiance check, but searches
 * from the Mumakil body box instead of from the rider's high position.
 */
public class LOTREntityAIMumakilHowdahRiderTarget extends EntityAIBase {
    private static final int TARGET_CHECK_INTERVAL = 10;
    private static final double DEFAULT_TARGET_RANGE = 40.0D;
    private static final double NEAR_FOOT_VISIBILITY_BYPASS_RANGE = 16.0D;
    private static final double SEARCH_VERTICAL_EXPANSION = 4.0D;

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

        if (this.rider.ticksExisted % TARGET_CHECK_INTERVAL != 0) {
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
        double range = this.getTargetRange();
        AxisAlignedBB searchBox = mumakil.boundingBox.expand(range, SEARCH_VERTICAL_EXPANSION, range);
        List list = this.rider.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, searchBox);

        EntityLivingBase bestTarget = null;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < list.size(); ++i) {
            EntityLivingBase candidate = (EntityLivingBase)list.get(i);
            if (!this.isValidTarget(mumakil, candidate)) {
                continue;
            }

            double distanceSq = mumakil.getDistanceSqToEntity(candidate);
            if (distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestDistanceSq = distanceSq;
            }
        }

        return bestTarget;
    }

    private double getTargetRange() {
        IAttributeInstance followRange = this.rider.getEntityAttribute(SharedMonsterAttributes.followRange);
        if (followRange != null) {
            return Math.max(DEFAULT_TARGET_RANGE, followRange.getAttributeValue());
        }

        return DEFAULT_TARGET_RANGE;
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

        return this.hasUsefulVisibility(mumakil, target);
    }

    private boolean hasUsefulVisibility(LOTREntityMumakil mumakil, EntityLivingBase target) {
        double closeRangeSq = NEAR_FOOT_VISIBILITY_BYPASS_RANGE * NEAR_FOOT_VISIBILITY_BYPASS_RANGE;
        if (mumakil.getDistanceSqToEntity(target) <= closeRangeSq) {
            return true;
        }

        return this.rider.canEntityBeSeen(target) || mumakil.canEntityBeSeen(target);
    }
}
