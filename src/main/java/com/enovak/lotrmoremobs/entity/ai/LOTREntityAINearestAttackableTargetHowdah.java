package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.util.List;
import lotr.common.entity.ai.LOTREntityAINearestAttackableTargetBasic;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;

/**
 * Small howdah-rider target helper.
 *
 * This is not a wide invasion-style scan. It deliberately caps horizontal range
 * to a normal NPC-sized bubble while allowing a taller Y box so a Southron rider
 * sitting high on a Mumakil can notice enemies at ground level.
 */
public class LOTREntityAINearestAttackableTargetHowdah extends LOTREntityAINearestAttackableTargetBasic {
    private static final double HOWDAH_HORIZONTAL_TARGET_RANGE = 18.0D;
    private static final double HOWDAH_VERTICAL_TARGET_RANGE = 24.0D;
    private static final int MAX_CANDIDATES_PER_CHECK = 12;

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

        List list = this.taskOwner.worldObj.selectEntitiesWithinAABB(
                this.targetClassHowdah,
                this.taskOwner.boundingBox.expand(
                        HOWDAH_HORIZONTAL_TARGET_RANGE,
                        HOWDAH_VERTICAL_TARGET_RANGE,
                        HOWDAH_HORIZONTAL_TARGET_RANGE
                ),
                this.targetSelectorHowdah
        );

        EntityLivingBase closest = null;
        double closestDistance = Double.MAX_VALUE;
        int checked = 0;

        for (int i = 0; i < list.size() && checked < MAX_CANDIDATES_PER_CHECK; ++i) {
            Object value = list.get(i);
            if (!(value instanceof EntityLivingBase)) {
                continue;
            }

            ++checked;
            EntityLivingBase candidate = (EntityLivingBase)value;
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
