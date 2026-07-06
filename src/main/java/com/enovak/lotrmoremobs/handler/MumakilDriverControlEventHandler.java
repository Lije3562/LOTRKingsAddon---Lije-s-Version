package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.ai.LOTRNPCTargetSelector;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.living.LivingEvent;


/**
 * Lightweight NPC driver steering for Near Harad NPC-driven Mumakil.
 *
 * This does not add target AI to howdah archers or replace the Mumakil's own
 * combat. It only gives a Near Harad NPC rider a shared target and
 * nudges the mount toward that target so existing tusk/trample attacks can fire.
 */
public class MumakilDriverControlEventHandler {
    private static final String DRIVER_TARGET_ID_KEY = "LOTRMoreMobsMumakilDriverTargetId";
    private static final String DRIVER_NEXT_SCAN_KEY = "LOTRMoreMobsMumakilDriverNextScan";
    private static final String DRIVER_NEXT_PATH_KEY = "LOTRMoreMobsMumakilDriverNextPath";
    private static final String DRIVER_NEXT_FALLBACK_KEY = "LOTRMoreMobsMumakilDriverNextFallback";
    private static final String DRIVER_NEXT_STUCK_CHECK_KEY = "LOTRMoreMobsMumakilDriverNextStuckCheck";
    private static final String DRIVER_LAST_TARGET_X_KEY = "LOTRMoreMobsMumakilDriverLastTargetX";
    private static final String DRIVER_LAST_TARGET_Y_KEY = "LOTRMoreMobsMumakilDriverLastTargetY";
    private static final String DRIVER_LAST_TARGET_Z_KEY = "LOTRMoreMobsMumakilDriverLastTargetZ";
    private static final String DRIVER_LAST_MUMAKIL_X_KEY = "LOTRMoreMobsMumakilDriverLastMumakilX";
    private static final String DRIVER_LAST_MUMAKIL_Z_KEY = "LOTRMoreMobsMumakilDriverLastMumakilZ";
    private static final String DRIVER_PROGRESS_TARGET_ID_KEY = "LOTRMoreMobsMumakilDriverProgressTargetId";
    private static final String DRIVER_LAST_TARGET_DISTANCE_SQ_KEY = "LOTRMoreMobsMumakilDriverLastTargetDistanceSq";
    private static final String DRIVER_TARGET_STUCK_TICKS_KEY = "LOTRMoreMobsMumakilDriverTargetStuckTicks";
    private static final String DRIVER_NEXT_TARGET_PROGRESS_CHECK_KEY = "LOTRMoreMobsMumakilDriverNextTargetProgressCheck";
    private static final String DRIVER_REJECTED_TARGET_ID_KEY = "LOTRMoreMobsMumakilDriverRejectedTargetId";
    private static final String DRIVER_REJECTED_TARGET_UNTIL_KEY = "LOTRMoreMobsMumakilDriverRejectedTargetUntil";

    private static final int TARGET_SCAN_MIN_INTERVAL = 20;
    private static final int TARGET_SCAN_RANDOM_INTERVAL = 21;
    private static final int PATH_REFRESH_INTERVAL = 50;
    private static final int FALLBACK_MOTION_INTERVAL = 10;
    private static final int STUCK_CHECK_INTERVAL = 40;
    private static final int DRIVER_TARGET_PROGRESS_CHECK_INTERVAL = 20;
    private static final int DRIVER_TARGET_STUCK_TIMEOUT = 100;
    private static final int DRIVER_TARGET_REJECT_TICKS = 200;
    private static final double TARGET_SCAN_RANGE = 42.0D;
    private static final double TARGET_SCAN_VERTICAL_RANGE = 34.0D;
    private static final double TARGET_RETAIN_RANGE = 48.0D;
    private static final double APPROACH_STOP_RANGE = 5.75D;
    private static final double TARGET_REPATH_DISTANCE_SQ = 9.0D;
    private static final double STUCK_MOVEMENT_DISTANCE_SQ = 0.36D;
    private static final double DRIVER_TARGET_PROGRESS_EPSILON_SQ = 4.0D;
    private static final double DRIVER_TARGET_REACHABLE_EXTRA_RANGE = 4.0D;
    private static final double NAVIGATOR_SPEED = 1.25D;
    private static final double FALLBACK_ACCELERATION = 0.045D;
    private static final double FALLBACK_MAX_SPEED = 0.23D;
    private static final double FALLBACK_CLOSE_MAX_SPEED = 0.15D;
    private static final float FALLBACK_TURN_STEP = 9.0F;

    private static final String DRIVER_NEXT_HOSTILE_REDIRECT_SCAN_KEY = "LOTRMoreMobsMumakilDriverNextHostileRedirectScan";

    private static final int HOSTILE_REDIRECT_SCAN_MIN_INTERVAL = 60;
    private static final int HOSTILE_REDIRECT_SCAN_RANDOM_INTERVAL = 41;

    private static final double HOSTILE_REDIRECT_SCAN_RANGE = 36.0D;
    private static final double HOSTILE_REDIRECT_VERTICAL_RANGE = 24.0D;

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null
                || event.entityLiving == null
                || event.entityLiving.worldObj == null
                || event.entityLiving.worldObj.isRemote
                || !(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        this.updateDriverControl((LOTREntityMumakil)event.entityLiving);
    }

    private void updateDriverControl(LOTREntityMumakil mumakil) {
        LOTREntityNPC driver = this.getValidNearHaradDriver(mumakil);
        if (driver == null) {
            this.clearStoredDriverTarget(mumakil);
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        long worldTime = mumakil.worldObj.getTotalWorldTime();
        this.clearDriverNavigator(driver);
        this.updateHostileNPCRedirectsToMumakil(mumakil, driver, worldTime);

        LOTRNPCTargetSelector targetSelector = new LOTRNPCTargetSelector(driver);
        EntityLivingBase target = this.getStoredDriverTarget(mumakil);

        if (!this.canDriverTarget(mumakil, driver, target, targetSelector)
                || this.isTemporarilyRejectedDriverTarget(mumakil, target, worldTime)
                || mumakil.getDistanceSqToEntity(target) > TARGET_RETAIN_RANGE * TARGET_RETAIN_RANGE) {
            target = null;
        }

        if (target != null && this.shouldRejectUnreachableDriverTarget(mumakil, target, data, worldTime)) {
            this.rejectDriverTarget(mumakil, target, worldTime);
            data.setLong(DRIVER_NEXT_SCAN_KEY, worldTime);
            target = null;
        }

        long nextScan = data.getLong(DRIVER_NEXT_SCAN_KEY);
        if (target == null && nextScan <= worldTime) {
            target = this.findBestDriverTarget(mumakil, driver, targetSelector, worldTime);
            data.setLong(
                    DRIVER_NEXT_SCAN_KEY,
                    worldTime + TARGET_SCAN_MIN_INTERVAL + mumakil.worldObj.rand.nextInt(TARGET_SCAN_RANDOM_INTERVAL)
            );
        }

        if (target == null) {
            this.clearStoredDriverTarget(mumakil);
            return;
        }

        data.setInteger(DRIVER_TARGET_ID_KEY, target.getEntityId());
        this.driveMumakilTowardTarget(mumakil, driver, target, worldTime);
    }

    private LOTREntityNPC getValidNearHaradDriver(LOTREntityMumakil mumakil) {
        Entity rider = mumakil.riddenByEntity;
        if (rider instanceof EntityPlayer || !(rider instanceof LOTREntityNPC)) {
            return null;
        }

        LOTREntityNPC driver = (LOTREntityNPC)rider;
        if (!driver.isEntityAlive()
                || driver instanceof LOTREntityMumakilHowdahArcher
                || driver.ridingEntity != mumakil) {
            return null;
        }

        return LOTRMod.getNPCFaction(driver) == LOTRFaction.NEAR_HARAD ? driver : null;
    }

    private EntityLivingBase getStoredDriverTarget(LOTREntityMumakil mumakil) {
        int targetId = mumakil.getEntityData().getInteger(DRIVER_TARGET_ID_KEY);
        if (targetId == 0) {
            return null;
        }

        Entity target = mumakil.worldObj.getEntityByID(targetId);
        return target instanceof EntityLivingBase ? (EntityLivingBase)target : null;
    }

    private EntityLivingBase findBestDriverTarget(LOTREntityMumakil mumakil, LOTREntityNPC driver, LOTRNPCTargetSelector targetSelector, long worldTime) {
        List nearby = mumakil.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                mumakil.boundingBox.expand(TARGET_SCAN_RANGE, TARGET_SCAN_VERTICAL_RANGE, TARGET_SCAN_RANGE)
        );

        this.clearExpiredRejectedDriverTarget(mumakil.getEntityData(), worldTime);

        EntityLivingBase bestTarget = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase candidate = (EntityLivingBase)nearby.get(i);
            if (!this.canDriverTarget(mumakil, driver, candidate, targetSelector)
                    || this.isTemporarilyRejectedDriverTarget(mumakil, candidate, worldTime)) {
                continue;
            }

            int priority = this.getTargetPriority(mumakil, driver, candidate);
            double distanceSq = mumakil.getDistanceSqToEntity(candidate);
            if (priority < bestPriority || priority == bestPriority && distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestPriority = priority;
                bestDistanceSq = distanceSq;
            }
        }

        return bestTarget;
    }

    private boolean canDriverTarget(LOTREntityMumakil mumakil, LOTREntityNPC driver, EntityLivingBase target, LOTRNPCTargetSelector targetSelector) {
        if (target == null
                || target == mumakil
                || target == driver
                || target instanceof LOTREntityMumakil
                || target instanceof LOTREntityMumakilHowdahArcher
                || !target.isEntityAlive()
                || target.riddenByEntity != null) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)target;
            return !player.capabilities.isCreativeMode
                    && targetSelector.isEntityApplicable(target)
                    && LOTRMod.canNPCAttackEntity(driver, target, false);
        }

        if (!(target instanceof LOTREntityNPC)) {
            return false;
        }

        LOTREntityNPC npc = (LOTREntityNPC)target;
        return !npc.hiredNPCInfo.isActive
                && targetSelector.isEntityApplicable(target)
                && LOTRMod.canNPCAttackEntity(driver, target, false);
    }

    private boolean shouldRejectUnreachableDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase target, NBTTagCompound data, long worldTime) {
        double distanceSq = mumakil.getDistanceSqToEntity(target);
        if (this.isNearEnoughForDriverReach(mumakil, target)) {
            this.rememberDriverTargetProgress(data, target, distanceSq, worldTime, 0);
            return false;
        }

        int targetId = target.getEntityId();
        if (data.getInteger(DRIVER_PROGRESS_TARGET_ID_KEY) != targetId
                || !data.hasKey(DRIVER_LAST_TARGET_DISTANCE_SQ_KEY)) {
            this.rememberDriverTargetProgress(data, target, distanceSq, worldTime, 0);
            return false;
        }

        if (data.getLong(DRIVER_NEXT_TARGET_PROGRESS_CHECK_KEY) > worldTime) {
            return false;
        }

        double lastDistanceSq = data.getDouble(DRIVER_LAST_TARGET_DISTANCE_SQ_KEY);
        boolean improved = lastDistanceSq - distanceSq >= DRIVER_TARGET_PROGRESS_EPSILON_SQ;
        PathNavigate navigator = mumakil.getNavigator();
        boolean noPath = navigator == null || navigator.noPath();

        if (improved) {
            this.rememberDriverTargetProgress(data, target, distanceSq, worldTime, 0);
            return false;
        }

        int stuckTicks = data.getInteger(DRIVER_TARGET_STUCK_TICKS_KEY);
        if (noPath || distanceSq > lastDistanceSq - DRIVER_TARGET_PROGRESS_EPSILON_SQ) {
            stuckTicks += DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        }

        this.rememberDriverTargetProgress(data, target, distanceSq, worldTime, stuckTicks);
        return stuckTicks >= DRIVER_TARGET_STUCK_TIMEOUT;
    }

    private void rememberDriverTargetProgress(NBTTagCompound data, EntityLivingBase target, double distanceSq, long worldTime, int stuckTicks) {
        data.setInteger(DRIVER_PROGRESS_TARGET_ID_KEY, target.getEntityId());
        data.setDouble(DRIVER_LAST_TARGET_DISTANCE_SQ_KEY, distanceSq);
        data.setInteger(DRIVER_TARGET_STUCK_TICKS_KEY, stuckTicks);
        data.setLong(DRIVER_NEXT_TARGET_PROGRESS_CHECK_KEY, worldTime + DRIVER_TARGET_PROGRESS_CHECK_INTERVAL);
    }

    private void clearDriverTargetProgress(NBTTagCompound data) {
        data.setInteger(DRIVER_PROGRESS_TARGET_ID_KEY, 0);
        data.setDouble(DRIVER_LAST_TARGET_DISTANCE_SQ_KEY, 0.0D);
        data.setInteger(DRIVER_TARGET_STUCK_TICKS_KEY, 0);
        data.setLong(DRIVER_NEXT_TARGET_PROGRESS_CHECK_KEY, 0L);
    }

    private void rejectDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase target, long worldTime) {
        NBTTagCompound data = mumakil.getEntityData();
        int targetId = target.getEntityId();
        data.setInteger(DRIVER_REJECTED_TARGET_ID_KEY, targetId);
        data.setLong(DRIVER_REJECTED_TARGET_UNTIL_KEY, worldTime + DRIVER_TARGET_REJECT_TICKS);
        this.clearStoredDriverTarget(mumakil);

        if (mumakil.getAttackTarget() == target) {
            mumakil.setAttackTarget(null);
        }

        PathNavigate navigator = mumakil.getNavigator();
        if (navigator != null && !navigator.noPath()) {
            navigator.clearPathEntity();
        }
    }

    private boolean isTemporarilyRejectedDriverTarget(LOTREntityMumakil mumakil, EntityLivingBase target, long worldTime) {
        NBTTagCompound data = mumakil.getEntityData();
        this.clearExpiredRejectedDriverTarget(data, worldTime);
        return data.getInteger(DRIVER_REJECTED_TARGET_ID_KEY) == target.getEntityId()
                && data.getLong(DRIVER_REJECTED_TARGET_UNTIL_KEY) > worldTime;
    }

    private void clearExpiredRejectedDriverTarget(NBTTagCompound data, long worldTime) {
        if (data.getInteger(DRIVER_REJECTED_TARGET_ID_KEY) != 0
                && data.getLong(DRIVER_REJECTED_TARGET_UNTIL_KEY) <= worldTime) {
            data.setInteger(DRIVER_REJECTED_TARGET_ID_KEY, 0);
            data.setLong(DRIVER_REJECTED_TARGET_UNTIL_KEY, 0L);
        }
    }

    private boolean isActivelyAttackingDriverOrMumakil(EntityLivingBase target, LOTREntityMumakil mumakil, LOTREntityNPC driver) {
        if (!(target instanceof EntityLiving)) {
            return false;
        }

        EntityLivingBase attackTarget = ((EntityLiving)target).getAttackTarget();
        return attackTarget == mumakil || attackTarget == driver;
    }

    private boolean isNearEnoughForDriverReach(LOTREntityMumakil mumakil, EntityLivingBase target) {
        double reachableRange = APPROACH_STOP_RANGE + DRIVER_TARGET_REACHABLE_EXTRA_RANGE;
        return mumakil.getDistanceSqToEntity(target) <= reachableRange * reachableRange;
    }

    private int getTargetPriority(LOTREntityMumakil mumakil, LOTREntityNPC driver, EntityLivingBase target) {
        int priority;
        if (target instanceof LOTREntityNPC) {
            priority = 20;
        } else if (target instanceof EntityPlayer) {
            priority = 30;
        } else {
            priority = 40;
        }

        if (this.isActivelyAttackingDriverOrMumakil(target, mumakil, driver)) {
            priority -= 30;
        }

        if (this.isNearEnoughForDriverReach(mumakil, target)) {
            priority -= 5;
        }

        return priority;
    }

    private void updateHostileNPCRedirectsToMumakil(LOTREntityMumakil mumakil, LOTREntityNPC driver, long worldTime) {
        if (!mumakil.getBelongsToNPC()
                || mumakil.riddenByEntity instanceof EntityPlayer
                || driver == null
                || driver.ridingEntity != mumakil) {
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        if (data.getLong(DRIVER_NEXT_HOSTILE_REDIRECT_SCAN_KEY) > worldTime) {
            return;
        }

        data.setLong(
                DRIVER_NEXT_HOSTILE_REDIRECT_SCAN_KEY,
                worldTime + HOSTILE_REDIRECT_SCAN_MIN_INTERVAL + mumakil.worldObj.rand.nextInt(HOSTILE_REDIRECT_SCAN_RANDOM_INTERVAL)
        );

        List nearbyNPCs = mumakil.worldObj.getEntitiesWithinAABB(
                LOTREntityNPC.class,
                mumakil.boundingBox.expand(HOSTILE_REDIRECT_SCAN_RANGE, HOSTILE_REDIRECT_VERTICAL_RANGE, HOSTILE_REDIRECT_SCAN_RANGE)
        );

        for (int i = 0; i < nearbyNPCs.size(); ++i) {
            LOTREntityNPC enemy = (LOTREntityNPC)nearbyNPCs.get(i);
            if (!this.canEnemyNPCRedirectToMumakil(enemy, mumakil, driver)) {
                continue;
            }

            enemy.setAttackTarget(mumakil);
        }
    }

    private boolean canEnemyNPCRedirectToMumakil(LOTREntityNPC enemy, LOTREntityMumakil mumakil, LOTREntityNPC driver) {
        if (enemy == null
                || enemy == driver
                || enemy == mumakil.riddenByEntity
                || enemy instanceof LOTREntityMumakilHowdahArcher
                || !enemy.isEntityAlive()
                || enemy.ridingEntity == mumakil) {
            return false;
        }

        EntityLivingBase currentTarget = enemy.getAttackTarget();
        if (currentTarget != null
                && currentTarget.isEntityAlive()
                && currentTarget != driver
                && !this.isHowdahHelperAssignedToMumakil(currentTarget, mumakil)) {
            return false;
        }

        LOTRNPCTargetSelector enemyTargetSelector = new LOTRNPCTargetSelector(enemy);
        return enemyTargetSelector.isEntityApplicable(driver)
                && LOTRMod.canNPCAttackEntity(enemy, driver, false);
    }

    private boolean isHowdahHelperAssignedToMumakil(EntityLivingBase target, LOTREntityMumakil mumakil) {
        if (!(target instanceof LOTREntityMumakilHowdahArcher)) {
            return false;
        }

        LOTREntityMumakilHowdahArcher archer = (LOTREntityMumakilHowdahArcher)target;
        if (archer.getHowdahMountEntityId() == mumakil.getEntityId()) {
            return true;
        }

        String archerMountUuid = archer.getHowdahMountUuid();
        String mumakilUuid = mumakil.getPersistentID().toString();
        return archerMountUuid != null
                && archerMountUuid.length() > 0
                && archerMountUuid.equals(mumakilUuid);
    }

    private void driveMumakilTowardTarget(LOTREntityMumakil mumakil, LOTREntityNPC driver, EntityLivingBase target, long worldTime) {
        this.clearDriverNavigator(driver);
        mumakil.setAttackTarget(target);
        mumakil.getLookHelper().setLookPositionWithEntity(target, 30.0F, 30.0F);
        this.faceTarget(mumakil, target);

        double distanceSq = mumakil.getDistanceSqToEntity(target);
        PathNavigate navigator = mumakil.getNavigator();

        if (distanceSq <= APPROACH_STOP_RANGE * APPROACH_STOP_RANGE) {
            if (navigator != null) {
                navigator.clearPathEntity();
            }

            mumakil.moveForward = 0.0F;
            mumakil.moveStrafing = 0.0F;
            mumakil.motionX *= 0.65D;
            mumakil.motionZ *= 0.65D;
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        boolean navigatorHasNoPath = navigator == null || navigator.noPath();
        boolean pathFailed = false;

        if (navigator != null
                && data.getLong(DRIVER_NEXT_PATH_KEY) <= worldTime
                && (navigatorHasNoPath
                || this.hasTargetMovedForRepath(data, target)
                || this.isMumakilStuckForRepath(data, mumakil, worldTime, distanceSq))) {
            boolean pathAccepted = navigator.tryMoveToEntityLiving(target, NAVIGATOR_SPEED);
            pathFailed = !pathAccepted;
            navigatorHasNoPath = !pathAccepted || navigator.noPath();
            data.setLong(DRIVER_NEXT_PATH_KEY, worldTime + PATH_REFRESH_INTERVAL);
            this.rememberTargetPosition(data, target);
        }

        if ((navigatorHasNoPath || pathFailed) && data.getLong(DRIVER_NEXT_FALLBACK_KEY) <= worldTime) {
            this.applyFallbackDrivenMotion(mumakil, target, distanceSq);
            data.setLong(DRIVER_NEXT_FALLBACK_KEY, worldTime + FALLBACK_MOTION_INTERVAL);
        }
    }

    private boolean hasTargetMovedForRepath(NBTTagCompound data, EntityLivingBase target) {
        if (!data.hasKey(DRIVER_LAST_TARGET_X_KEY)
                || !data.hasKey(DRIVER_LAST_TARGET_Y_KEY)
                || !data.hasKey(DRIVER_LAST_TARGET_Z_KEY)) {
            return true;
        }

        double dx = target.posX - data.getDouble(DRIVER_LAST_TARGET_X_KEY);
        double dy = target.posY - data.getDouble(DRIVER_LAST_TARGET_Y_KEY);
        double dz = target.posZ - data.getDouble(DRIVER_LAST_TARGET_Z_KEY);
        return dx * dx + dy * dy + dz * dz >= TARGET_REPATH_DISTANCE_SQ;
    }

    private void rememberTargetPosition(NBTTagCompound data, EntityLivingBase target) {
        data.setDouble(DRIVER_LAST_TARGET_X_KEY, target.posX);
        data.setDouble(DRIVER_LAST_TARGET_Y_KEY, target.posY);
        data.setDouble(DRIVER_LAST_TARGET_Z_KEY, target.posZ);
    }

    private boolean isMumakilStuckForRepath(NBTTagCompound data, LOTREntityMumakil mumakil, long worldTime, double distanceSq) {
        if (distanceSq <= APPROACH_STOP_RANGE * APPROACH_STOP_RANGE
                || data.getLong(DRIVER_NEXT_STUCK_CHECK_KEY) > worldTime) {
            return false;
        }

        boolean hasPreviousPosition = data.hasKey(DRIVER_LAST_MUMAKIL_X_KEY)
                && data.hasKey(DRIVER_LAST_MUMAKIL_Z_KEY);
        double previousX = data.getDouble(DRIVER_LAST_MUMAKIL_X_KEY);
        double previousZ = data.getDouble(DRIVER_LAST_MUMAKIL_Z_KEY);

        data.setDouble(DRIVER_LAST_MUMAKIL_X_KEY, mumakil.posX);
        data.setDouble(DRIVER_LAST_MUMAKIL_Z_KEY, mumakil.posZ);
        data.setLong(DRIVER_NEXT_STUCK_CHECK_KEY, worldTime + STUCK_CHECK_INTERVAL);

        if (!hasPreviousPosition) {
            return false;
        }

        double dx = mumakil.posX - previousX;
        double dz = mumakil.posZ - previousZ;
        return dx * dx + dz * dz <= STUCK_MOVEMENT_DISTANCE_SQ;
    }

    private void faceTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        double deltaX = target.posX - mumakil.posX;
        double deltaZ = target.posZ - mumakil.posZ;
        float desiredYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;

        mumakil.rotationYaw = this.updateRotation(mumakil.rotationYaw, desiredYaw, FALLBACK_TURN_STEP);
        mumakil.renderYawOffset = this.updateRotation(mumakil.renderYawOffset, mumakil.rotationYaw, FALLBACK_TURN_STEP);
        mumakil.rotationYawHead = mumakil.renderYawOffset;
    }

    private float updateRotation(float current, float desired, float maxStep) {
        float delta = MathHelper.wrapAngleTo180_float(desired - current);
        return current + MathHelper.clamp_float(delta, -maxStep, maxStep);
    }

    private void applyFallbackDrivenMotion(LOTREntityMumakil mumakil, EntityLivingBase target, double distanceSq) {
        if (mumakil.riddenByEntity instanceof EntityPlayer
                || distanceSq <= APPROACH_STOP_RANGE * APPROACH_STOP_RANGE) {
            return;
        }

        float yawRadians = mumakil.rotationYaw * (float)Math.PI / 180.0F;
        double forwardX = -MathHelper.sin(yawRadians);
        double forwardZ = MathHelper.cos(yawRadians);

        mumakil.motionX += forwardX * FALLBACK_ACCELERATION;
        mumakil.motionZ += forwardZ * FALLBACK_ACCELERATION;
        mumakil.moveForward = 1.0F;
        mumakil.moveStrafing = 0.0F;

        double maxSpeed = distanceSq < 144.0D ? FALLBACK_CLOSE_MAX_SPEED : FALLBACK_MAX_SPEED;
        double horizontalSpeedSq = mumakil.motionX * mumakil.motionX + mumakil.motionZ * mumakil.motionZ;
        if (horizontalSpeedSq > maxSpeed * maxSpeed) {
            double horizontalSpeed = MathHelper.sqrt_double(horizontalSpeedSq);
            mumakil.motionX = mumakil.motionX / horizontalSpeed * maxSpeed;
            mumakil.motionZ = mumakil.motionZ / horizontalSpeed * maxSpeed;
        }
    }

    private void clearDriverNavigator(LOTREntityNPC driver) {
        if (driver == null || driver.getNavigator() == null || driver.getNavigator().noPath()) {
            return;
        }

        driver.getNavigator().clearPathEntity();
    }

    private void clearStoredDriverTarget(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(DRIVER_TARGET_ID_KEY, 0);
        this.clearDriverTargetProgress(data);
    }

    private void clearDriverControl(LOTREntityMumakil mumakil, LOTREntityNPC driver) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(DRIVER_TARGET_ID_KEY, 0);
        this.clearDriverTargetProgress(data);

        if (mumakil.getAttackTarget() != null) {
            mumakil.setAttackTarget(null);
        }

        PathNavigate navigator = mumakil.getNavigator();
        if (navigator != null && !navigator.noPath()) {
            navigator.clearPathEntity();
        }

        if (driver != null && driver.ridingEntity == mumakil) {
            this.clearDriverNavigator(driver);
        }
    }
}
