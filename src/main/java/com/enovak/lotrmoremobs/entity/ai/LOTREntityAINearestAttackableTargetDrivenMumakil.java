package com.enovak.lotrmoremobs.entity.ai;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Mumakil-owned target AI for hired howdah mounts.
 *
 * This deliberately moves target acquisition onto the Mumakil instead of the
 * high howdah rider. The rider still supplies faction/permission rules, but the
 * Mumakil is the entity whose targetTasks run. Future howdah archers should read
 * the Mumakil's current attack target rather than running their own downward
 * search from the platform.
 */
public class LOTREntityAINearestAttackableTargetDrivenMumakil extends EntityAINearestAttackableTarget {
    private final LOTREntityMumakil mumakil;

    public LOTREntityAINearestAttackableTargetDrivenMumakil(LOTREntityMumakil mumakil, int targetChance) {
        super(mumakil, EntityLivingBase.class, targetChance, false, false, new DrivenMumakilTargetSelector(mumakil));
        this.mumakil = mumakil;
    }

    @Override
    public boolean shouldExecute() {
        return isDrivenHowdahMumakil(this.mumakil) && super.shouldExecute();
    }

    @Override
    public boolean continueExecuting() {
        return isDrivenHowdahMumakil(this.mumakil) && super.continueExecuting();
    }

    private static boolean isDrivenHowdahMumakil(LOTREntityMumakil mumakil) {
        return getDriver(mumakil) != null;
    }

    private static LOTREntityNPC getDriver(LOTREntityMumakil mumakil) {
        if (mumakil == null || !mumakil.hasMumakilHowdahEquipped()) {
            return null;
        }

        if (mumakil.riddenByEntity instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)mumakil.riddenByEntity;

            if (npc.ridingEntity == mumakil) {
                return npc;
            }
        }

        return null;
    }

    private static boolean canDrivenMumakilAttack(LOTREntityMumakil mumakil, EntityLivingBase target) {
        LOTREntityNPC driver = getDriver(mumakil);

        if (driver == null || target == null || target == mumakil || target == driver || !target.isEntityAlive()) {
            return false;
        }

        if (target instanceof LOTREntityMumakil || target.riddenByEntity != null || target.ridingEntity != null) {
            return false;
        }

        if (!(target instanceof LOTREntityNPC) && !(target instanceof EntityPlayer) && !(target instanceof IMob)) {
            return false;
        }

        if (!(driver instanceof EntityCreature)) {
            return false;
        }

        return LOTRMod.canNPCAttackEntity((EntityCreature)driver, target, false);
    }

    private static class DrivenMumakilTargetSelector implements IEntitySelector {
        private final LOTREntityMumakil mumakil;

        private DrivenMumakilTargetSelector(LOTREntityMumakil mumakil) {
            this.mumakil = mumakil;
        }

        @Override
        public boolean isEntityApplicable(Entity entity) {
            return entity instanceof EntityLivingBase
                    && canDrivenMumakilAttack(this.mumakil, (EntityLivingBase)entity);
        }
    }
}
