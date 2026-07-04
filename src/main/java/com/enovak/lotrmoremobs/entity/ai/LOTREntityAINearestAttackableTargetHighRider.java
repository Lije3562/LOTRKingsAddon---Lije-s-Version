package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lotr.common.entity.ai.LOTREntityAINearestAttackableTargetBasic;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.entity.npc.LOTREntityNPCRideable;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;

/**
 * LOTR nearest-target AI with a taller vertical search box for howdah riders.
 *
 * The stock LOTR target AI uses horizontal followRange, but clamps vertical
 * expansion to min(range, 8). That works for ground NPCs, but a Mumakil howdah
 * driver sits high enough that enemies at the Mumakil's feet can be outside the
 * rider's target AABB. This class keeps the same target suitability checks and
 * only raises the vertical AABB while the NPC is riding a howdah Mumakil.
 */
public class LOTREntityAINearestAttackableTargetHighRider extends LOTREntityAINearestAttackableTargetBasic {
    private static final double HOWDAH_VERTICAL_TARGET_RANGE = 24.0D;

    private final Class targetClassHighRider;
    private final int targetChanceHighRider;
    private final IEntitySelector targetSelectorHighRider;
    private final Comparator targetSorterHighRider;
    private EntityLivingBase highRiderTargetEntity;

    public LOTREntityAINearestAttackableTargetHighRider(EntityCreature owner, Class targetClass, int targetChance,
                                                        boolean checkSight) {
        this(owner, targetClass, targetChance, checkSight, false, null);
    }

    public LOTREntityAINearestAttackableTargetHighRider(EntityCreature owner, Class targetClass, int targetChance,
                                                        boolean checkSight, IEntitySelector selector) {
        this(owner, targetClass, targetChance, checkSight, false, selector);
    }

    public LOTREntityAINearestAttackableTargetHighRider(EntityCreature owner, Class targetClass, int targetChance,
                                                        boolean checkSight, boolean nearbyOnly,
                                                        final IEntitySelector selector) {
        super(owner, targetClass, targetChance, checkSight, nearbyOnly, selector);
        this.targetClassHighRider = targetClass;
        this.targetChanceHighRider = targetChance;
        this.targetSorterHighRider = new Comparator() {
            public int compare(Object first, Object second) {
                double firstDistance = LOTREntityAINearestAttackableTargetHighRider.this.taskOwner
                        .getDistanceSqToEntity((Entity)first);
                double secondDistance = LOTREntityAINearestAttackableTargetHighRider.this.taskOwner
                        .getDistanceSqToEntity((Entity)second);
                return firstDistance < secondDistance ? -1 : firstDistance > secondDistance ? 1 : 0;
            }
        };
        this.targetSelectorHighRider = new IEntitySelector() {
            public boolean isEntityApplicable(Entity entity) {
                if (!(entity instanceof EntityLivingBase)) {
                    return false;
                }

                if (selector != null && !selector.isEntityApplicable(entity)) {
                    return false;
                }

                return LOTREntityAINearestAttackableTargetHighRider.this.isSuitableTarget(
                        (EntityLivingBase)entity,
                        false
                );
            }
        };
        this.setMutexBits(1);
    }

    @Override
    public boolean shouldExecute() {
        if (!this.isRidingHowdahMumakil()) {
            return false;
        }

        EntityLivingBase currentTarget = this.taskOwner.getAttackTarget();
        if (currentTarget != null && currentTarget.isEntityAlive()) {
            return false;
        }

        if (this.targetChanceHighRider > 0
                && this.taskOwner.getRNG().nextInt(this.targetChanceHighRider) != 0) {
            return false;
        }

        if (this.taskOwner instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)this.taskOwner;
            if (npc.hiredNPCInfo.isActive && npc.hiredNPCInfo.isHalted()) {
                return false;
            }
            if (npc.isChild()) {
                return false;
            }
        }

        if (this.taskOwner instanceof LOTREntityNPCRideable) {
            LOTREntityNPCRideable rideableNPC = (LOTREntityNPCRideable)this.taskOwner;
            if (!rideableNPC.isNPCTamed() || rideableNPC.riddenByEntity instanceof net.minecraft.entity.player.EntityPlayer) {
                return false;
            }
        }

        double horizontalRange = this.getTargetDistance();
        double verticalRange = Math.min(horizontalRange, HOWDAH_VERTICAL_TARGET_RANGE);
        List list = this.taskOwner.worldObj.selectEntitiesWithinAABB(
                this.targetClassHighRider,
                this.taskOwner.boundingBox.expand(horizontalRange, verticalRange, horizontalRange),
                this.targetSelectorHighRider
        );
        Collections.sort(list, this.targetSorterHighRider);

        if (list.isEmpty()) {
            return false;
        }

        this.highRiderTargetEntity = (EntityLivingBase)list.get(0);
        return true;
    }

    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.highRiderTargetEntity);
        super.startExecuting();
    }

    private boolean isRidingHowdahMumakil() {
        if (!(this.taskOwner.ridingEntity instanceof LOTREntityMumakil)) {
            return false;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)this.taskOwner.ridingEntity;
        return mumakil.hasMumakilHowdahEquipped();
    }
}
