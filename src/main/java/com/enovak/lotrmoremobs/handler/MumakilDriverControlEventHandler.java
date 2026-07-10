package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;

import java.util.List;

/**
 * LOTRMoreMobs Mumakil driver-control handler.
 *
 * Purpose:
 * - Controls hired-war Mumakil while they have a valid Near Harad/Southron driver.
 * - Keeps hired-war Mumakil fighting as Near Harad combatants if that driver dies.
 * - Keeps wild Mumakil behavior untouched.
 * - Keeps renderer/howdah/archer/equipment systems untouched.
 * - Prevents NPC-driven Mumakil from fixating forever on unreachable or elevated tower targets.
 *
 * Patch focus:
 * - Per-Mumakil NBT target state.
 * - Temporary target rejection.
 * - Elevated fortress/tower target skip for melee driver targeting.
 * - Throttled target acquisition scans.
 */
public class MumakilDriverControlEventHandler {

    private static final String NBT_DRIVER_TARGET_ID = "lotrmoremobs_driverTargetId";
    private static final String NBT_PROGRESS_TARGET_ID = "lotrmoremobs_driverProgressTargetId";
    private static final String NBT_PROGRESS_LAST_DIST_SQ = "lotrmoremobs_driverProgressLastDistSq";
    private static final String NBT_PROGRESS_STUCK_TICKS = "lotrmoremobs_driverProgressStuckTicks";
    private static final String NBT_NEXT_PROGRESS_CHECK_TICK = "lotrmoremobs_driverNextProgressCheckTick";
    private static final String NBT_REJECTED_TARGET_ID = "lotrmoremobs_driverRejectedTargetId";
    private static final String NBT_REJECTED_UNTIL_TICK = "lotrmoremobs_driverRejectedUntilTick";
    private static final String NBT_NEXT_TARGET_SCAN_TICK = "lotrmoremobs_driverNextTargetScanTick";
    private static final String NBT_NEXT_REPATH_TICK = "lotrmoremobs_driverNextRepathTick";

    private static final double TARGET_SCAN_RANGE = 32.0D;
    private static final double TARGET_SCAN_VERTICAL_RANGE = 16.0D;

    /*
     * Stop slightly outside the entity's tusk hit range so the entity's own attack logic can fire
     * without forcing the mount to try to stand inside the target.
     */
    private static final double APPROACH_STOP_RANGE = 7.0D;
    private static final double APPROACH_STOP_RANGE_SQ = APPROACH_STOP_RANGE * APPROACH_STOP_RANGE;

    private static final double DRIVER_APPROACH_SPEED = 0.42D;
    private static final int DRIVER_REPATH_INTERVAL = 10;

    private static final int DRIVER_TARGET_PROGRESS_CHECK_INTERVAL = 20;
    private static final int DRIVER_TARGET_STUCK_TIMEOUT = 100; // about 5 seconds
    private static final int DRIVER_TARGET_REJECT_TICKS = 200; // about 10 seconds
    private static final double DRIVER_TARGET_PROGRESS_EPSILON_SQ = 4.0D;
    private static final double DRIVER_TARGET_REACHABLE_EXTRA_RANGE = 4.0D;

    /*
     * Tower/fortress protection:
     * A driven Mumakil is a melee siege animal, not a wall-climber.
     * Elevated archers should be handled by howdah archers, not by Mumakil pathing.
     */
    private static final double DRIVER_TARGET_MAX_Y_ABOVE_MUMAKIL = 6.0D;
    private static final int DRIVER_TARGET_ELEVATED_REJECT_TICKS = 200;

    /*
     * Do not scan crowded battlefields every tick.
     * This only throttles new target acquisition, not ordinary mount movement/control.
     */
    private static final int DRIVER_TARGET_SCAN_COOLDOWN = 20;

    private static final boolean DEBUG_DRIVER_TARGETS = false;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil) event.entityLiving;

        if (mumakil.worldObj == null || mumakil.worldObj.isRemote) {
            return;
        }

        EntityLivingBase driver = getValidNearHaradDriver(mumakil);

        if (driver != null) {
            markHiredWarIfApplicable(mumakil);
        } else if (isImplicitHiredWarMumakil(mumakil)) {
            mumakil.setHiredWarMumakil(true);
        }

        if (driver == null && !mumakil.isHiredWarMumakil()) {
            clearDriverTargetState(mumakil);
            return;
        }

        updateDrivenMumakil(mumakil, driver);
    }

    private static void markHiredWarIfApplicable(LOTREntityMumakil mumakil) {
        if (isImplicitHiredWarMumakil(mumakil)) {
            mumakil.setHiredWarMumakil(true);
        }
    }

    private static boolean isImplicitHiredWarMumakil(LOTREntityMumakil mumakil) {
        return mumakil != null
                && mumakil.getBelongsToNPC()
                && mumakil.hasMumakilHowdahEquipped();
    }

    private static void updateDrivenMumakil(LOTREntityMumakil mumakil, EntityLivingBase driver) {
        World world = mumakil.worldObj;
        long worldTick = world.getTotalWorldTime();

        EntityLivingBase currentTarget = getStoredDriverTarget(mumakil);

        if (currentTarget == null) {
            EntityLivingBase attackTarget = mumakil.getAttackTarget();
            if (isValidDriverTarget(mumakil, driver, attackTarget)) {
                setStoredDriverTarget(mumakil, attackTarget);
                resetTargetProgress(mumakil, attackTarget);
                currentTarget = attackTarget;
            }
        }

        if (!isValidDriverTarget(mumakil, driver, currentTarget)) {
            clearStoredDriverTarget(mumakil);

            if (mumakil.getAttackTarget() == currentTarget) {
                mumakil.setAttackTarget(null);
            }

            currentTarget = null;
        }

        if (currentTarget != null && isRejectedDriverTarget(mumakil, currentTarget, worldTick)) {
            clearStoredDriverTarget(mumakil);

            if (mumakil.getAttackTarget() == currentTarget) {
                mumakil.setAttackTarget(null);
            }

            clearNavigatorPath(mumakil);
            currentTarget = null;
        }

        if (currentTarget != null && isTooHighForDrivenMumakilMelee(mumakil, currentTarget)) {
            temporarilyRejectDriverTarget(
                    mumakil,
                    currentTarget,
                    worldTick,
                    DRIVER_TARGET_ELEVATED_REJECT_TICKS,
                    "elevated"
            );

            if (mumakil.getAttackTarget() == currentTarget) {
                mumakil.setAttackTarget(null);
            }

            clearStoredDriverTarget(mumakil);
            clearNavigatorPath(mumakil);
            return;
        }

        if (currentTarget != null) {
            updateCurrentTargetProgress(mumakil, currentTarget, worldTick);

            /*
             * updateCurrentTargetProgress may clear/reject the current target.
             */
            currentTarget = getStoredDriverTarget(mumakil);
        }

        if (currentTarget == null) {
            currentTarget = findNewDriverTarget(mumakil, driver, worldTick);

            if (currentTarget != null) {
                setStoredDriverTarget(mumakil, currentTarget);
                resetTargetProgress(mumakil, currentTarget);
            }
        }

        if (currentTarget != null) {
            driveTowardTarget(mumakil, currentTarget, worldTick);
        }
    }

    private static EntityLivingBase getValidNearHaradDriver(LOTREntityMumakil mumakil) {
        Entity rider = mumakil.riddenByEntity;

        if (!(rider instanceof LOTREntityNPC)) {
            return null;
        }

        LOTREntityNPC npc = (LOTREntityNPC) rider;

        if (!npc.isEntityAlive()) {
            return null;
        }

        if (isNearHaradOrSouthronNPC(npc)) {
            return npc;
        }

        return null;
    }

    private static boolean isNearHaradOrSouthronNPC(LOTREntityNPC npc) {
        try {
            LOTRFaction faction = LOTRMod.getNPCFaction(npc);
            if (faction == LOTRFaction.NEAR_HARAD) {
                return true;
            }
        } catch (Exception e) {
            /*
             * Fall through to class-name fallback.
             */
        }

        /*
         * Fallback for addon/UCP/deobf naming variations.
         */
        String name = npc.getClass().getName().toLowerCase();
        return name.contains("southron") || name.contains("nearharad") || name.contains("near_harad") || name.contains("harad");
    }

    private static EntityLivingBase getStoredDriverTarget(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        int targetId = data.getInteger(NBT_DRIVER_TARGET_ID);

        if (targetId <= 0 || mumakil.worldObj == null) {
            return null;
        }

        Entity entity = mumakil.worldObj.getEntityByID(targetId);
        return entity instanceof EntityLivingBase ? (EntityLivingBase) entity : null;
    }

    private static void setStoredDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (target == null) {
            clearStoredDriverTarget(mumakil);
            return;
        }

        mumakil.getEntityData().setInteger(NBT_DRIVER_TARGET_ID, target.getEntityId());
        mumakil.setAttackTarget(target);

        if (DEBUG_DRIVER_TARGETS) {
            System.out.println("[LOTRMoreMobs] Driven Mumakil " + mumakil.getEntityId()
                    + " selected target " + target.getEntityId()
                    + " " + target.getClass().getSimpleName());
        }
    }

    private static void clearStoredDriverTarget(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(NBT_DRIVER_TARGET_ID, -1);
        data.setInteger(NBT_PROGRESS_TARGET_ID, -1);
        data.setDouble(NBT_PROGRESS_LAST_DIST_SQ, -1.0D);
        data.setInteger(NBT_PROGRESS_STUCK_TICKS, 0);
        data.setLong(NBT_NEXT_PROGRESS_CHECK_TICK, 0L);
    }

    private static void clearDriverTargetState(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(NBT_DRIVER_TARGET_ID, -1);
        data.setInteger(NBT_PROGRESS_TARGET_ID, -1);
        data.setDouble(NBT_PROGRESS_LAST_DIST_SQ, -1.0D);
        data.setInteger(NBT_PROGRESS_STUCK_TICKS, 0);
        data.setLong(NBT_NEXT_PROGRESS_CHECK_TICK, 0L);
        data.setInteger(NBT_REJECTED_TARGET_ID, -1);
        data.setLong(NBT_REJECTED_UNTIL_TICK, 0L);
        data.setLong(NBT_NEXT_TARGET_SCAN_TICK, 0L);
        data.setLong(NBT_NEXT_REPATH_TICK, 0L);
    }

    private static void resetTargetProgress(LOTREntityMumakil mumakil, EntityLivingBase target) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(NBT_PROGRESS_TARGET_ID, target != null ? target.getEntityId() : -1);
        data.setDouble(NBT_PROGRESS_LAST_DIST_SQ, target != null ? mumakil.getDistanceSqToEntity(target) : -1.0D);
        data.setInteger(NBT_PROGRESS_STUCK_TICKS, 0);
        data.setLong(NBT_NEXT_PROGRESS_CHECK_TICK, mumakil.worldObj.getTotalWorldTime() + DRIVER_TARGET_PROGRESS_CHECK_INTERVAL);
    }

    private static void updateCurrentTargetProgress(LOTREntityMumakil mumakil, EntityLivingBase target, long worldTick) {
        if (target == null) {
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();

        if (worldTick < data.getLong(NBT_NEXT_PROGRESS_CHECK_TICK)) {
            return;
        }

        data.setLong(NBT_NEXT_PROGRESS_CHECK_TICK, worldTick + DRIVER_TARGET_PROGRESS_CHECK_INTERVAL);

        double distSq = mumakil.getDistanceSqToEntity(target);
        double directReach = APPROACH_STOP_RANGE + DRIVER_TARGET_REACHABLE_EXTRA_RANGE;
        double directReachSq = directReach * directReach;

        if (distSq <= directReachSq) {
            resetTargetProgress(mumakil, target);
            return;
        }

        int targetId = target.getEntityId();
        int progressTargetId = data.getInteger(NBT_PROGRESS_TARGET_ID);

        if (progressTargetId != targetId) {
            resetTargetProgress(mumakil, target);
            return;
        }

        double lastDistSq = data.getDouble(NBT_PROGRESS_LAST_DIST_SQ);
        int stuckTicks = data.getInteger(NBT_PROGRESS_STUCK_TICKS);

        boolean improved = lastDistSq < 0.0D || distSq < lastDistSq - DRIVER_TARGET_PROGRESS_EPSILON_SQ;
        boolean noPath = mumakil.getNavigator().noPath();

        if (improved) {
            stuckTicks = 0;
            data.setDouble(NBT_PROGRESS_LAST_DIST_SQ, distSq);
        } else {
            stuckTicks += DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        }

        /*
         * If the navigator has no path while the target is outside direct reach, count that check
         * as stuck even if the distance value is noisy.
         */
        if (noPath && !improved) {
            stuckTicks += DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        }

        data.setInteger(NBT_PROGRESS_STUCK_TICKS, stuckTicks);

        if (stuckTicks >= DRIVER_TARGET_STUCK_TIMEOUT) {
            temporarilyRejectDriverTarget(
                    mumakil,
                    target,
                    worldTick,
                    DRIVER_TARGET_REJECT_TICKS,
                    "stuck"
            );

            if (mumakil.getAttackTarget() == target) {
                mumakil.setAttackTarget(null);
            }

            clearStoredDriverTarget(mumakil);
            clearNavigatorPath(mumakil);
        }
    }

    private static void temporarilyRejectDriverTarget(
            LOTREntityMumakil mumakil,
            EntityLivingBase target,
            long worldTick,
            int ticks,
            String reason
    ) {
        if (target == null) {
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(NBT_REJECTED_TARGET_ID, target.getEntityId());
        data.setLong(NBT_REJECTED_UNTIL_TICK, worldTick + ticks);

        if (DEBUG_DRIVER_TARGETS) {
            System.out.println("[LOTRMoreMobs] Driven Mumakil " + mumakil.getEntityId()
                    + " rejected target " + target.getEntityId()
                    + " for " + ticks + " ticks reason=" + reason);
        }
    }

    private static boolean isRejectedDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase target, long worldTick) {
        if (target == null) {
            return false;
        }

        NBTTagCompound data = mumakil.getEntityData();
        int rejectedId = data.getInteger(NBT_REJECTED_TARGET_ID);
        long rejectedUntil = data.getLong(NBT_REJECTED_UNTIL_TICK);

        if (rejectedId <= 0 || worldTick >= rejectedUntil) {
            if (rejectedId > 0 && worldTick >= rejectedUntil) {
                data.setInteger(NBT_REJECTED_TARGET_ID, -1);
                data.setLong(NBT_REJECTED_UNTIL_TICK, 0L);
            }
            return false;
        }

        return rejectedId == target.getEntityId();
    }

    private static EntityLivingBase findNewDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase driver, long worldTick) {
        NBTTagCompound data = mumakil.getEntityData();

        if (worldTick < data.getLong(NBT_NEXT_TARGET_SCAN_TICK)) {
            return null;
        }

        data.setLong(NBT_NEXT_TARGET_SCAN_TICK, worldTick + DRIVER_TARGET_SCAN_COOLDOWN);

        AxisAlignedBB scanBox = mumakil.boundingBox.expand(
                TARGET_SCAN_RANGE,
                TARGET_SCAN_VERTICAL_RANGE,
                TARGET_SCAN_RANGE
        );

        List nearby = mumakil.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, scanBox);

        EntityLivingBase bestTarget = null;
        int bestScore = Integer.MIN_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase candidate = (EntityLivingBase) nearby.get(i);

            if (!isValidDriverTarget(mumakil, driver, candidate)) {
                continue;
            }

            if (isRejectedDriverTarget(mumakil, candidate, worldTick)) {
                continue;
            }

            if (isTooHighForDrivenMumakilMelee(mumakil, candidate)) {
                continue;
            }

            double distanceSq = mumakil.getDistanceSqToEntity(candidate);
            int score = getDriverTargetScore(mumakil, driver, candidate, distanceSq);

            if (score > bestScore || score == bestScore && distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestScore = score;
                bestDistanceSq = distanceSq;
            }
        }

        return bestTarget;
    }

    private static int getDriverTargetScore(
            LOTREntityMumakil mumakil,
            EntityLivingBase driver,
            EntityLivingBase candidate,
            double distanceSq
    ) {
        int score = 0;

        if (isAttacking(candidate, mumakil)) {
            score += 1000;
        }

        if (isAttacking(candidate, driver)) {
            score += 850;
        }

        if (candidate instanceof EntityPlayer) {
            score += 150;
        } else if (candidate instanceof LOTREntityNPC) {
            score += 120;
        } else if (candidate instanceof IMob) {
            score += 60;
        }

        double directReach = APPROACH_STOP_RANGE + DRIVER_TARGET_REACHABLE_EXTRA_RANGE;
        if (distanceSq <= directReach * directReach) {
            score += 300;
        } else if (distanceSq <= 18.0D * 18.0D) {
            score += 120;
        }

        if (mumakil.canEntityBeSeen(candidate)) {
            score += 40;
        }

        /*
         * Light distance bias without making distance beat "this enemy is attacking us."
         */
        score -= MathHelper.floor_double(Math.sqrt(distanceSq));

        return score;
    }

    private static boolean isAttacking(EntityLivingBase attacker, EntityLivingBase victim) {
        if (attacker == null || victim == null) {
            return false;
        }

        if (attacker instanceof EntityLiving) {
            EntityLiving living = (EntityLiving) attacker;
            return living.getAttackTarget() == victim;
        }

        return false;
    }

    private static boolean isValidDriverTarget(
            LOTREntityMumakil mumakil,
            EntityLivingBase driver,
            EntityLivingBase target
    ) {
        if (mumakil == null || target == null) {
            return false;
        }

        if (target == mumakil || target == driver) {
            return false;
        }

        if (!target.isEntityAlive()) {
            return false;
        }

        if (target instanceof LOTREntityMumakil) {
            return false;
        }

        if (target.riddenByEntity != null || target.ridingEntity != null) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) target;
            if (player.capabilities.isCreativeMode) {
                return false;
            }

            if (driver == null) {
                return target == mumakil.getAttackTarget();
            }
        }

        if (target instanceof EntityTameable && ((EntityTameable) target).isTamed()) {
            return false;
        }

        if (target instanceof EntityHorse && ((EntityHorse) target).isTame()) {
            return false;
        }

        if (target instanceof LOTREntityNPC) {
            LOTREntityNPC targetNPC = (LOTREntityNPC) target;

            if (targetNPC.hiredNPCInfo.isActive) {
                return false;
            }

            if (driver instanceof LOTREntityNPC) {
                try {
                    LOTRFaction driverFaction = LOTRMod.getNPCFaction((LOTREntityNPC) driver);
                    LOTRFaction targetFaction = LOTRMod.getNPCFaction(targetNPC);

                    if (driverFaction != null && targetFaction != null && !driverFaction.isBadRelation(targetFaction)) {
                        return false;
                    }
                } catch (Exception e) {
                    /*
                     * If faction reflection fails, fall through to LOTRMod.canNPCAttackEntity below.
                     */
                }
            } else {
                try {
                    LOTRFaction targetFaction = LOTRMod.getNPCFaction(targetNPC);
                    return targetFaction != null && LOTRFaction.NEAR_HARAD.isBadRelation(targetFaction);
                } catch (Exception e) {
                    return false;
                }
            }
        }

        if (driver instanceof EntityCreature) {
            return LOTRMod.canNPCAttackEntity((EntityCreature) driver, target, false);
        }

        return target == mumakil.getAttackTarget();
    }

    private static boolean isTooHighForDrivenMumakilMelee(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (mumakil == null || target == null) {
            return false;
        }

        double yDiff = target.posY - mumakil.posY;

        if (yDiff <= DRIVER_TARGET_MAX_Y_ABOVE_MUMAKIL) {
            return false;
        }

        double directReach = APPROACH_STOP_RANGE + DRIVER_TARGET_REACHABLE_EXTRA_RANGE;
        double directReachSq = directReach * directReach;

        return mumakil.getDistanceSqToEntity(target) > directReachSq;
    }

    private static void driveTowardTarget(LOTREntityMumakil mumakil, EntityLivingBase target, long worldTick) {
        if (target == null) {
            return;
        }

        mumakil.setAttackTarget(target);
        faceTarget(mumakil, target);

        double distSq = mumakil.getDistanceSqToEntity(target);

        if (distSq <= APPROACH_STOP_RANGE_SQ) {
            clearNavigatorPath(mumakil);
            mumakil.setAIMoveSpeed(0.0F);
            mumakil.moveForward = 0.0F;
            mumakil.moveStrafing = 0.0F;
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();

        if (worldTick >= data.getLong(NBT_NEXT_REPATH_TICK)) {
            mumakil.getNavigator().tryMoveToEntityLiving(target, DRIVER_APPROACH_SPEED);
            data.setLong(NBT_NEXT_REPATH_TICK, worldTick + DRIVER_REPATH_INTERVAL);
        }

        mumakil.setAIMoveSpeed((float) DRIVER_APPROACH_SPEED);

        /*
         * Do not use a direct motion fallback here.
         * For driven siege behavior, if pathing cannot reach a fortress/tower target, the
         * target-rejection system should switch targets instead of brute-forcing movement.
         */
    }

    private static void faceTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (mumakil == null || target == null) {
            return;
        }

        mumakil.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);

        double dx = target.posX - mumakil.posX;
        double dz = target.posZ - mumakil.posZ;

        if (dx * dx + dz * dz < 1.0E-4D) {
            return;
        }

        float desiredYaw = (float)(Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        mumakil.rotationYaw = updateRotation(mumakil.rotationYaw, desiredYaw, 8.0F);
        mumakil.renderYawOffset = updateRotation(mumakil.renderYawOffset, mumakil.rotationYaw, 8.0F);
        mumakil.rotationYawHead = mumakil.renderYawOffset;
    }

    private static float updateRotation(float current, float target, float maxStep) {
        float delta = MathHelper.wrapAngleTo180_float(target - current);

        if (delta > maxStep) {
            delta = maxStep;
        }

        if (delta < -maxStep) {
            delta = -maxStep;
        }

        return current + delta;
    }

    private static void clearNavigatorPath(LOTREntityMumakil mumakil) {
        if (mumakil != null && mumakil.getNavigator() != null) {
            mumakil.getNavigator().clearPathEntity();
        }
    }

}
