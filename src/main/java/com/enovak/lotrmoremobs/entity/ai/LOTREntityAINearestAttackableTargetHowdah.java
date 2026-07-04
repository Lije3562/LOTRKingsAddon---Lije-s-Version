package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.List;
import lotr.common.entity.ai.LOTREntityAINearestAttackableTargetBasic;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;

/**
 * Small howdah-rider target helper.
 *
 * This deliberately avoids World.selectEntitiesWithinAABB(..., selector), because
 * that method runs the expensive suitability selector on every entity in the box
 * before our candidate cap can help. Instead, it gets a raw list first and caps
 * how many candidates are allowed to reach the expensive LOTR target checks.
 *
 * The vertical box is intentionally asymmetric: it searches mostly downward from
 * the high howdah rider and only slightly upward, because ground enemies are the
 * problem and there usually should not be valid targets above the Mumakil.
 */
public class LOTREntityAINearestAttackableTargetHowdah extends LOTREntityAINearestAttackableTargetBasic {
    private static final double HOWDAH_HORIZONTAL_TARGET_RANGE = 10.0D;
    private static final double HOWDAH_TARGET_RANGE_BELOW = 20.0D;
    private static final double HOWDAH_TARGET_RANGE_ABOVE = 2.0D;
    private static final int MAX_RAW_ENTITIES_PER_CHECK = 16;
    private static final int MAX_EXPENSIVE_TARGET_CHECKS = 4;

    private final Class targetClassHowdah;
    private final int targetChanceHowdah;
    private final IEntitySelector targetSelectorHowdah;
    private EntityLivingBase targetEntityHowdah;

    public LOTREntityAINearestAttackableTargetHowdah(EntityCreature owner, Class targetClass, int targetChance,
                                                     boolean checkSight) {
        this(owner, targetClass, targetChance, checkSight, false, null);
    }

    public LOTREntityAINearestAttackableTargetHowdah(EntityCreature owner, Class targetClass, int targetChance,
                                                     boolean checkSight, IEntitySelector selector) {
        this(owner, targetClass, targetChance, checkSight, false, selector);
    }

    public LOTREntityAINearestAttackableTargetHowdah(EntityCreature owner, Class targetClass, int targetChance,
                                                     boolean checkSight, boolean nearbyOnly,
                                                     final IEntitySelector selector) {
        super(owner, targetClass, targetChance, checkSight, nearbyOnly, selector);
        this.targetClassHowdah = targetClass;
        this.targetChanceHowdah = targetChance;
        this.targetSelectorHowdah = new IEntitySelector() {
            public boolean isEntityApplicable(Entity entity) {
                if (!(entity instanceof EntityLivingBase)) {
                    return false;
                }

                if (selector != null && !selector.isEntityApplicable(entity)) {
                    return false;
                }

                return LOTREntityAINearestAttackableTargetHowdah.this.isSuitableTarget(
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

        if (this.targetChanceHowdah > 0 && this.taskOwner.getRNG().nextInt(this.targetChanceHowdah) != 0) {
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

        AxisAlignedBB riderBox = this.taskOwner.boundingBox;
        AxisAlignedBB searchBox = AxisAlignedBB.getBoundingBox(
                riderBox.minX - HOWDAH_HORIZONTAL_TARGET_RANGE,
                riderBox.minY - HOWDAH_TARGET_RANGE_BELOW,
                riderBox.minZ - HOWDAH_HORIZONTAL_TARGET_RANGE,
                riderBox.maxX + HOWDAH_HORIZONTAL_TARGET_RANGE,
                riderBox.maxY + HOWDAH_TARGET_RANGE_ABOVE,
                riderBox.maxZ + HOWDAH_HORIZONTAL_TARGET_RANGE
        );

        List list = this.taskOwner.worldObj.getEntitiesWithinAABB(this.targetClassHowdah, searchBox);

        EntityLivingBase closest = null;
        double closestDistance = Double.MAX_VALUE;
        int rawChecked = 0;
        int expensiveChecked = 0;

        for (int i = 0; i < list.size() && rawChecked < MAX_RAW_ENTITIES_PER_CHECK
                && expensiveChecked < MAX_EXPENSIVE_TARGET_CHECKS; ++i) {
            Object value = list.get(i);
            ++rawChecked;

            if (!(value instanceof EntityLivingBase)) {
                continue;
            }

            EntityLivingBase candidate = (EntityLivingBase)value;

            if (candidate == this.taskOwner || candidate == this.taskOwner.ridingEntity || !candidate.isEntityAlive()) {
                continue;
            }

            ++expensiveChecked;

            if (!this.targetSelectorHowdah.isEntityApplicable(candidate)) {
                continue;
            }

            double distance = this.taskOwner.getDistanceSqToEntity(candidate);

            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }

        if (closest == null) {
            return false;
        }

        this.targetEntityHowdah = closest;
        return true;
    }

    @Override
    public void startExecuting() {
        this.taskOwner.setAttackTarget(this.targetEntityHowdah);
    }

    private boolean isRidingHowdahMumakil() {
        if (!(this.taskOwner.ridingEntity instanceof LOTREntityMumakil)) {
            return false;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)this.taskOwner.ridingEntity;
        return mumakil.hasMumakilHowdahEquipped();
    }
}
