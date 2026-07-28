package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.util.MumakilPerformanceTracker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

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
    private static final String NBT_REJECTED_TARGET_ID = "lotrmoremobs_driverRejectedTargetId";
    private static final String NBT_REJECTED_UNTIL_TICK = "lotrmoremobs_driverRejectedUntilTick";
    private static final String NBT_NEXT_TARGET_SCAN_TICK = "lotrmoremobs_driverNextTargetScanTick";

    private static final double TARGET_SCAN_RANGE = 16.0D;
    private static final double TARGET_SCAN_VERTICAL_RANGE = 8.0D;

    private static final double APPROACH_STOP_RANGE = 7.0D;

    private static final int DRIVER_TARGET_PROGRESS_CHECK_INTERVAL = 20;
    private static final int DRIVER_TARGET_STUCK_TIMEOUT = 100; // about 5 seconds
    private static final int DRIVER_TARGET_REJECT_TICKS = 200; // about 10 seconds
    private static final double DRIVER_PROGRESS_MOVE_THRESHOLD_SQ = 1.0D;
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
    private static final int DRIVER_TARGET_SCAN_COOLDOWN = 10;
    private static final int MOUNTED_DRIVER_HORN_CHANCE_DENOMINATOR = 10;
    private static final int MOUNTED_DRIVER_HORN_COOLDOWN_TICKS = 6000;
    private static final int MOUNTED_DRIVER_HORN_WORLD_COOLDOWN_TICKS = 600;
    private static final int MOUNTED_DRIVER_HORN_DISPLAY_TICKS = 40;
    private static final int MOUNTED_DRIVER_TARGET_LOSS_CONFIRM_TICKS = 20;
    private static final String MOUNTED_DRIVER_HORN_SOUND =
            "lotr:item.horn";
    private static final String NBT_MOUNTED_DRIVER_NEXT_HORN_TICK =
            "lotrmoremobs_mumakDriverNextHornTick";
    private static final String NBT_MOUNTED_DRIVER_OBSERVED_TARGET_ID =
            "lotrmoremobs_mumakDriverObservedTargetId";
    private static final String NBT_MOUNTED_DRIVER_TARGET_LOST_SINCE_TICK =
            "lotrmoremobs_mumakDriverTargetLostSinceTick";

    private static final boolean DEBUG_DRIVER_TARGETS = false;
    private static final boolean DEBUG_DRIVER_HORN = false;
    private static final Map<LOTREntityNPC, Boolean> MOUNTED_DRIVER_COMBAT_GUARDS =
            new WeakHashMap<LOTREntityNPC, Boolean>();
    private static final Map<LOTREntityNPC, MountedDriverHornState> MOUNTED_DRIVER_HORN_STATES =
            new WeakHashMap<LOTREntityNPC, MountedDriverHornState>();
    private static final Map<World, MountedDriverHornWorldState> MOUNTED_DRIVER_HORN_WORLD_STATES =
            new WeakHashMap<World, MountedDriverHornWorldState>();

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event.entityLiving instanceof LOTREntityNPC) {
            restoreFormerMountedDriverWeapon((LOTREntityNPC)event.entityLiving);
        }

        if (!(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil) event.entityLiving;

        if (mumakil.worldObj == null) {
            return;
        }

        LOTREntityNPC driver = getValidNearHaradDriver(mumakil);
        if (mumakil.worldObj.isRemote) {
            updateClientMountedDriverHeldItem(mumakil, driver);
            return;
        }

        boolean trackPerformance =
                MumakilPerformanceTracker.isEnabled();
        long perfStart = trackPerformance
                ? MumakilPerformanceTracker.startTimer()
                : 0L;

        try {
            if (driver != null) {
                markHiredWarIfApplicable(mumakil);
                mumakil.capturePlayerHiredFormationOwner(driver);
            } else if (isImplicitHiredWarMumakil(mumakil)) {
                mumakil.setHiredWarMumakil(true);
            }

            if (!mumakil.isHiredWarMumakil()) {
                if (hasDriverTargetState(mumakil)) {
                    clearDriverTargetState(mumakil);
                }
                if (mumakil.getMountedDriverHornTicks() > 0) {
                    mumakil.setMountedDriverHornTicks(0);
                }
                return;
            }

            if (driver != null) {
                ensureMountedDriverCombatGuard(driver);
            }

            updateDrivenMumakil(mumakil, driver);
            observeMountedDriverTargetAcquisition(
                    mumakil,
                    driver
            );
            updateMountedDriverHorn(mumakil, driver);
        } finally {
            if (trackPerformance) {
                MumakilPerformanceTracker.recordDriverHandler(
                        mumakil,
                        System.nanoTime() - perfStart
                );
            }
        }
    }

    private static void updateClientMountedDriverHeldItem(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver
    ) {
        if (driver == null) {
            return;
        }

        setMountedDriverHeldItem(
                driver,
                mumakil.isHiredWarMumakil()
                        && mumakil.getMountedDriverHornTicks() > 0
        );
    }

    private static void updateMountedDriverHorn(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver
    ) {
        if (driver == null) {
            if (mumakil.getMountedDriverHornTicks() > 0) {
                mumakil.setMountedDriverHornTicks(0);
            }
            return;
        }

        long worldTick = mumakil.worldObj.getTotalWorldTime();
        MountedDriverHornState state = MOUNTED_DRIVER_HORN_STATES.get(driver);
        int hornTicks = mumakil.getMountedDriverHornTicks();
        if (state == null) {
            if (hornTicks > 0) {
                mumakil.setMountedDriverHornTicks(0);
            }
            setMountedDriverHeldItem(driver, false);
            return;
        }

        if (hornTicks > 0) {
            int remainingTicks = hornTicks - 1;
            mumakil.setMountedDriverHornTicks(remainingTicks);
            setMountedDriverHeldItem(driver, remainingTicks > 0);
            if (remainingTicks == 0) {
                restoreMountedDriverHeldItem(driver, state);
                MOUNTED_DRIVER_HORN_STATES.remove(driver);
                logDriverHorn(
                        mumakil,
                        driver,
                        "horn end"
                );
            }
            return;
        }

        restoreMountedDriverHeldItem(driver, state);
        MOUNTED_DRIVER_HORN_STATES.remove(driver);
        logDriverHorn(mumakil, driver, "horn end");
    }

    private static void setMountedDriverHeldItem(
            LOTREntityNPC driver,
            boolean displayHorn
    ) {
        ItemStack heldItem = driver.getHeldItem();
        if (displayHorn) {
            if (heldItem == null || heldItem.getItem() != LOTRMod.horn) {
                driver.setCurrentItemOrArmor(0, new ItemStack(LOTRMod.horn));
            }
        } else if (heldItem != null
                && heldItem.getItem() == LOTRMod.horn) {
            driver.setCurrentItemOrArmor(0, null);
        }
    }

    private static void restoreMountedDriverHeldItem(
            LOTREntityNPC driver,
            MountedDriverHornState state
    ) {
        if (driver == null || state == null) {
            return;
        }
        driver.setCurrentItemOrArmor(
                0,
                state.originalHeldItem == null
                        ? null
                        : state.originalHeldItem.copy()
        );
    }

    private static void restoreFormerMountedDriverWeapon(LOTREntityNPC driver) {
        if (!MOUNTED_DRIVER_HORN_STATES.containsKey(driver)) {
            return;
        }

        if (driver.ridingEntity instanceof LOTREntityMumakil
                && ((LOTREntityMumakil)driver.ridingEntity).riddenByEntity == driver
                && ((LOTREntityMumakil)driver.ridingEntity).isHiredWarMumakil()
                && driver.isEntityAlive()) {
            return;
        }

        MountedDriverHornState state =
                MOUNTED_DRIVER_HORN_STATES.remove(driver);
        if (driver.worldObj != null
                && !driver.worldObj.isRemote
                && driver.isEntityAlive()) {
            restoreMountedDriverHeldItem(driver, state);
            driver.refreshCurrentAttackMode();
        }
    }

    private static final class MountedDriverHornState {
        private final ItemStack originalHeldItem;

        private MountedDriverHornState(ItemStack originalHeldItem) {
            this.originalHeldItem = originalHeldItem;
        }
    }

    private static final class MountedDriverHornWorldState {
        private long quietUntilTick;
    }

    @SubscribeEvent
    public void onFormationMemberHurt(LivingHurtEvent event) {
        if (event == null
                || event.entityLiving == null
                || event.entityLiving.worldObj == null
                || event.entityLiving.worldObj.isRemote
                || event.source == null) {
            return;
        }

        Entity sourceEntity = event.source.getEntity();
        if (!(sourceEntity instanceof EntityLivingBase)) {
            return;
        }

        LOTREntityMumakil mumakil = getHiredFormationMumakil(event.entityLiving);
        if (mumakil != null) {
            mumakil.recordRecentFormationThreat((EntityLivingBase)sourceEntity);
        }
    }

    private static LOTREntityMumakil getHiredFormationMumakil(EntityLivingBase victim) {
        LOTREntityMumakil mumakil = null;
        if (victim instanceof LOTREntityMumakil) {
            mumakil = (LOTREntityMumakil)victim;
        } else if (victim instanceof LOTREntityMumakilHowdahArcher) {
            LOTREntityMumakilHowdahArcher archer =
                    (LOTREntityMumakilHowdahArcher)victim;
            Entity mount = victim.worldObj.getEntityByID(archer.getHowdahMountEntityId());
            if (archer.isRuntimeHowdahPassenger()
                    && mount instanceof LOTREntityMumakil) {
                mumakil = (LOTREntityMumakil)mount;
            }
        } else if (victim instanceof LOTREntityNPC
                && victim.ridingEntity instanceof LOTREntityMumakil) {
            LOTREntityMumakil riddenMumakil =
                    (LOTREntityMumakil)victim.ridingEntity;
            if (riddenMumakil.riddenByEntity == victim) {
                mumakil = riddenMumakil;
            }
        }

        return mumakil != null && mumakil.isHiredWarMumakil()
                ? mumakil
                : null;
    }

    private static void ensureMountedDriverCombatGuard(LOTREntityNPC driver) {
        if (MOUNTED_DRIVER_COMBAT_GUARDS.containsKey(driver)) {
            return;
        }

        driver.tasks.addTask(1, new EntityAIBlockMountedMumakilDriverCombat(driver));
        MOUNTED_DRIVER_COMBAT_GUARDS.put(driver, Boolean.TRUE);
    }

    private static final class EntityAIBlockMountedMumakilDriverCombat extends EntityAIBase {
        private final LOTREntityNPC driver;

        private EntityAIBlockMountedMumakilDriverCombat(LOTREntityNPC driver) {
            this.driver = driver;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            EntityLivingBase target = this.driver.getAttackTarget();
            return target != null && target.isEntityAlive() && this.isDrivingHiredWarMumakil();
        }

        @Override
        public boolean continueExecuting() {
            return this.shouldExecute();
        }

        private boolean isDrivingHiredWarMumakil() {
            if (!(this.driver.ridingEntity instanceof LOTREntityMumakil)) {
                return false;
            }

            LOTREntityMumakil mumakil = (LOTREntityMumakil)this.driver.ridingEntity;
            return mumakil.isHiredWarMumakil()
                    && mumakil.riddenByEntity == this.driver
                    && this.driver.isEntityAlive();
        }
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

    private static void updateDrivenMumakil(LOTREntityMumakil mumakil, LOTREntityNPC driver) {
        World world = mumakil.worldObj;
        long worldTick = world.getTotalWorldTime();
        EntityLivingBase currentTarget = getStoredDriverTarget(mumakil);
        EntityLivingBase authoritativeTarget = getAuthoritativeAttackTarget(mumakil, driver);

        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordDriverTargetRead(mumakil);
        }

        if (currentTarget == null
                && mumakil.getEntityData().getInteger(NBT_DRIVER_TARGET_ID) > 0) {
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetReset(mumakil);
            }
            clearStoredDriverTarget(mumakil);
        }

        /*
         * A living driver owns target selection. Do not resurrect the last
         * stored target after the driver's normal LOTR targeting has cleared it.
         */
        if (driver != null && authoritativeTarget == null && currentTarget != null) {
            clearAuthoritativeAttackTarget(mumakil, driver, currentTarget);
            clearStoredDriverTarget(mumakil);
            currentTarget = null;
        }

        if (authoritativeTarget != null && authoritativeTarget != currentTarget) {
            if (isValidDriverTarget(mumakil, driver, authoritativeTarget)
                    && !isRejectedDriverTarget(mumakil, authoritativeTarget, worldTick)
                    && !isTooHighForDrivenMumakilMelee(mumakil, authoritativeTarget)) {
                setStoredDriverTarget(
                        mumakil,
                        authoritativeTarget
                );
                currentTarget = authoritativeTarget;
            } else {
                if (isTooHighForDrivenMumakilMelee(mumakil, authoritativeTarget)) {
                    temporarilyRejectDriverTarget(
                            mumakil,
                            authoritativeTarget,
                            worldTick,
                            DRIVER_TARGET_ELEVATED_REJECT_TICKS,
                            "elevated"
                    );
                }
                clearAuthoritativeAttackTarget(mumakil, driver, authoritativeTarget);
                if (driver != null) {
                    clearStoredDriverTarget(mumakil);
                    currentTarget = null;
                }
            }
        }

        if (currentTarget != null && !isValidDriverTarget(mumakil, driver, currentTarget)) {
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetReset(mumakil);
            }
            clearStoredDriverTarget(mumakil);
            clearAuthoritativeAttackTarget(mumakil, driver, currentTarget);
            currentTarget = null;
        }

        if (currentTarget != null && isRejectedDriverTarget(mumakil, currentTarget, worldTick)) {
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetReset(mumakil);
            }
            clearStoredDriverTarget(mumakil);
            clearAuthoritativeAttackTarget(mumakil, driver, currentTarget);
            currentTarget = null;
        }

        if (currentTarget != null && isTooHighForDrivenMumakilMelee(mumakil, currentTarget)) {
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetReset(mumakil);
            }
            temporarilyRejectDriverTarget(
                    mumakil,
                    currentTarget,
                    worldTick,
                    DRIVER_TARGET_ELEVATED_REJECT_TICKS,
                    "elevated"
            );

            clearAuthoritativeAttackTarget(mumakil, driver, currentTarget);
            clearStoredDriverTarget(mumakil);
            return;
        }

        if (currentTarget != null) {
            updateCurrentTargetProgress(mumakil, driver, currentTarget, worldTick);

            /*
             * updateCurrentTargetProgress may clear/reject the current target.
             */
            currentTarget = getStoredDriverTarget(mumakil);
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetRead(mumakil);
            }
        }

        if (currentTarget == null) {
            if (driver != null) {
                EntityLivingBase formationThreat = mumakil.getRecentFormationThreat();
                if (isValidDriverTarget(mumakil, driver, formationThreat)
                        && !isRejectedDriverTarget(mumakil, formationThreat, worldTick)
                        && !isTooHighForDrivenMumakilMelee(mumakil, formationThreat)) {
                    currentTarget = formationThreat;
                }
            } else {
                currentTarget = findNewDriverTarget(mumakil, null, worldTick);
            }

            if (currentTarget != null) {
                setStoredDriverTarget(
                        mumakil,
                        currentTarget
                );
            }
        }

        if (currentTarget != null) {
            setAuthoritativeAttackTarget(mumakil, driver, currentTarget);
        }
    }

    private static LOTREntityNPC getValidNearHaradDriver(LOTREntityMumakil mumakil) {
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

    private static boolean setStoredDriverTarget(
            LOTREntityMumakil mumakil,
            EntityLivingBase target
    ) {
        if (target == null) {
            clearStoredDriverTarget(mumakil);
            return false;
        }

        NBTTagCompound data = mumakil.getEntityData();
        int targetId = target.getEntityId();
        boolean storedTargetChanged = data.getInteger(NBT_DRIVER_TARGET_ID) != targetId;

        if (storedTargetChanged) {
            data.setInteger(NBT_DRIVER_TARGET_ID, targetId);
            resetTargetProgress(mumakil, target);
        }

        if (DEBUG_DRIVER_TARGETS && storedTargetChanged) {
            System.out.println("[LOTRMoreMobs] Driven Mumakil " + mumakil.getEntityId()
                    + " selected target " + targetId
                    + " " + target.getClass().getSimpleName());
        }
        return storedTargetChanged;
    }

    private static void observeMountedDriverTargetAcquisition(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver
    ) {
        NBTTagCompound data = mumakil.getEntityData();
        EntityLivingBase target = driver == null
                ? null
                : driver.getAttackTarget();
        int currentTargetId = target != null
                && target.isEntityAlive()
                ? target.getEntityId()
                : 0;
        int observedTargetId = data.getInteger(
                NBT_MOUNTED_DRIVER_OBSERVED_TARGET_ID
        );

        long worldTick = mumakil.worldObj.getTotalWorldTime();
        if (currentTargetId <= 0) {
            long lostSinceTick = data.getLong(
                    NBT_MOUNTED_DRIVER_TARGET_LOST_SINCE_TICK
            );
            if (lostSinceTick <= 0L) {
                data.setLong(
                        NBT_MOUNTED_DRIVER_TARGET_LOST_SINCE_TICK,
                        worldTick
                );
            } else if (worldTick - lostSinceTick
                    >= MOUNTED_DRIVER_TARGET_LOSS_CONFIRM_TICKS) {
                data.setInteger(
                        NBT_MOUNTED_DRIVER_OBSERVED_TARGET_ID,
                        0
                );
                data.setLong(
                        NBT_MOUNTED_DRIVER_TARGET_LOST_SINCE_TICK,
                        0L
                );
            }
            return;
        }

        data.setLong(
                NBT_MOUNTED_DRIVER_TARGET_LOST_SINCE_TICK,
                0L
        );
        if (currentTargetId == observedTargetId) {
            return;
        }

        data.setInteger(
                NBT_MOUNTED_DRIVER_OBSERVED_TARGET_ID,
                currentTargetId
        );

        logDriverHorn(
                mumakil,
                driver,
                "genuine target change "
                        + observedTargetId
                        + " -> "
                        + currentTargetId
        );
        tryPlayMountedDriverHornForNewTarget(
                mumakil,
                driver,
                target,
                worldTick
        );
    }

    private static void tryPlayMountedDriverHornForNewTarget(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver,
            EntityLivingBase target,
            long worldTick
    ) {
        if (mumakil == null
                || driver == null
                || target == null
                || !target.isEntityAlive()
                || mumakil.worldObj == null
                || mumakil.worldObj.isRemote
                || mumakil.riddenByEntity != driver
                || !driver.isEntityAlive()) {
            return;
        }

        if (mumakil.getMountedDriverHornTicks() > 0) {
            logDriverHorn(
                    mumakil,
                    driver,
                    "driver horn already active"
            );
            return;
        }

        long nextDriverHornTick =
                driver.getEntityData().getLong(
                        NBT_MOUNTED_DRIVER_NEXT_HORN_TICK
                );
        if (worldTick < nextDriverHornTick) {
            logDriverHorn(
                    mumakil,
                    driver,
                    "driver cooldown rejection remaining="
                            + (nextDriverHornTick - worldTick)
            );
            return;
        }

        MountedDriverHornWorldState worldState =
                MOUNTED_DRIVER_HORN_WORLD_STATES.get(
                        mumakil.worldObj
                );
        if (worldState == null) {
            worldState = new MountedDriverHornWorldState();
            MOUNTED_DRIVER_HORN_WORLD_STATES.put(
                    mumakil.worldObj,
                    worldState
            );
        }
        if (worldTick < worldState.quietUntilTick) {
            logDriverHorn(
                    mumakil,
                    driver,
                    "world quiet rejection remaining="
                            + (worldState.quietUntilTick - worldTick)
            );
            return;
        }

        int chanceRoll = mumakil.getRNG().nextInt(
                MOUNTED_DRIVER_HORN_CHANCE_DENOMINATOR
        );
        logDriverHorn(
                mumakil,
                driver,
                "chance roll="
                        + chanceRoll
                        + "/"
                        + MOUNTED_DRIVER_HORN_CHANCE_DENOMINATOR
        );
        if (chanceRoll != 0) {
            return;
        }

        ItemStack heldItem = driver.getHeldItem();
        MountedDriverHornState driverState =
                new MountedDriverHornState(
                        heldItem == null
                                ? null
                                : heldItem.copy()
                );
        MOUNTED_DRIVER_HORN_STATES.put(driver, driverState);
        driver.getEntityData().setLong(
                NBT_MOUNTED_DRIVER_NEXT_HORN_TICK,
                worldTick + MOUNTED_DRIVER_HORN_COOLDOWN_TICKS
        );
        worldState.quietUntilTick =
                worldTick
                        + MOUNTED_DRIVER_HORN_WORLD_COOLDOWN_TICKS;

        mumakil.setMountedDriverHornTicks(
                MOUNTED_DRIVER_HORN_DISPLAY_TICKS
        );
        setMountedDriverHeldItem(driver, true);
        logDriverHorn(
                mumakil,
                driver,
                "horn start target=" + target.getEntityId()
        );
        /*
         * This is LOTR's native horn item and native horn sound. Selection,
         * pose, and the single sound all begin on the server in one event, so
         * there is no queued sound to replay after a chunk reload.
         */
        mumakil.worldObj.playSoundAtEntity(
                driver,
                MOUNTED_DRIVER_HORN_SOUND,
                4.0F,
                0.65F + mumakil.getRNG().nextFloat() * 0.1F
        );
        logDriverHorn(
                mumakil,
                driver,
                "sound playback " + MOUNTED_DRIVER_HORN_SOUND
        );
    }

    private static void logDriverHorn(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver,
            String message
    ) {
        if (!DEBUG_DRIVER_HORN) {
            return;
        }
        System.out.println(
                "[LOTRMoreMobs][MumakDriverHorn] mount="
                        + (mumakil == null
                        ? -1
                        : mumakil.getEntityId())
                        + " driver="
                        + (driver == null
                        ? -1
                        : driver.getEntityId())
                        + " "
                        + message
        );
    }

    private static void clearStoredDriverTarget(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();

        if (data.getInteger(NBT_DRIVER_TARGET_ID) <= 0
                && !hasActiveDriverTargetProgress(mumakil.getDriverTargetProgressState())) {
            return;
        }

        data.setInteger(NBT_DRIVER_TARGET_ID, -1);
        mumakil.getDriverTargetProgressState().reset();
    }

    private static void clearDriverTargetState(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        data.setInteger(NBT_DRIVER_TARGET_ID, -1);
        data.setInteger(NBT_REJECTED_TARGET_ID, -1);
        data.setLong(NBT_REJECTED_UNTIL_TICK, 0L);
        data.setLong(NBT_NEXT_TARGET_SCAN_TICK, 0L);
        mumakil.getDriverTargetProgressState().reset();
    }

    private static boolean hasDriverTargetState(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();
        return data.getInteger(NBT_DRIVER_TARGET_ID) > 0
                || data.getInteger(NBT_REJECTED_TARGET_ID) > 0
                || data.getLong(NBT_REJECTED_UNTIL_TICK) > 0L
                || data.getLong(NBT_NEXT_TARGET_SCAN_TICK) > 0L
                || hasActiveDriverTargetProgress(mumakil.getDriverTargetProgressState());
    }

    private static boolean hasActiveDriverTargetProgress(LOTREntityMumakil.DriverTargetProgressState state) {
        return state.progressTargetEntityId > 0
                || state.nextProgressCheckTick > 0L
                || state.stuckTicks > 0;
    }

    private static EntityLivingBase getAuthoritativeAttackTarget(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver
    ) {
        return driver != null ? driver.getAttackTarget() : mumakil.getAttackTarget();
    }

    private static void setAuthoritativeAttackTarget(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver,
            EntityLivingBase target
    ) {
        boolean authoritativeTargetChanged = getAuthoritativeAttackTarget(mumakil, driver) != target;
        boolean mountTargetChanged = driver != null && mumakil.getAttackTarget() != target;

        if (!authoritativeTargetChanged && !mountTargetChanged) {
            return;
        }

        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordDriverTargetSyncAttempt(mumakil);
        }

        if (driver != null) {
            if (authoritativeTargetChanged) {
                driver.setAttackTarget(target);
            }
            if (mountTargetChanged) {
                mumakil.setAttackTarget(target);
            }
        } else if (authoritativeTargetChanged) {
            mumakil.setAttackTarget(target);
        }
    }

    private static void clearAuthoritativeAttackTarget(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver,
            EntityLivingBase target
    ) {
        if (driver != null && driver.getAttackTarget() == target) {
            driver.setAttackTarget(null);
        }
        if (mumakil.getAttackTarget() == target) {
            mumakil.setAttackTarget(null);
        }
    }

    private static void resetTargetProgress(LOTREntityMumakil mumakil, EntityLivingBase target) {
        LOTREntityMumakil.DriverTargetProgressState state = mumakil.getDriverTargetProgressState();
        state.progressTargetEntityId = target != null ? target.getEntityId() : -1;
        state.nextProgressCheckTick = mumakil.worldObj.getTotalWorldTime() + DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        state.lastProgressX = mumakil.posX;
        state.lastProgressY = mumakil.posY;
        state.lastProgressZ = mumakil.posZ;
        state.stuckTicks = 0;
    }

    private static void updateCurrentTargetProgress(
            LOTREntityMumakil mumakil,
            LOTREntityNPC driver,
            EntityLivingBase target,
            long worldTick
    ) {
        if (target == null) {
            return;
        }

        LOTREntityMumakil.DriverTargetProgressState state = mumakil.getDriverTargetProgressState();

        if (worldTick < state.nextProgressCheckTick) {
            return;
        }

        state.nextProgressCheckTick = worldTick + DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordUnreachableCheck(mumakil);
        }

        double distSq = mumakil.getDistanceSqToEntity(target);
        double directReach = APPROACH_STOP_RANGE + DRIVER_TARGET_REACHABLE_EXTRA_RANGE;
        double directReachSq = directReach * directReach;

        if (distSq <= directReachSq) {
            resetTargetProgress(mumakil, target);
            return;
        }

        int targetId = target.getEntityId();

        if (state.progressTargetEntityId != targetId) {
            resetTargetProgress(mumakil, target);
            return;
        }

        double progressX = mumakil.posX - state.lastProgressX;
        double progressZ = mumakil.posZ - state.lastProgressZ;
        double horizontalProgressSq = progressX * progressX + progressZ * progressZ;

        if (horizontalProgressSq >= DRIVER_PROGRESS_MOVE_THRESHOLD_SQ) {
            state.stuckTicks = 0;
        } else {
            state.stuckTicks += DRIVER_TARGET_PROGRESS_CHECK_INTERVAL;
        }

        state.lastProgressX = mumakil.posX;
        state.lastProgressY = mumakil.posY;
        state.lastProgressZ = mumakil.posZ;

        if (state.stuckTicks >= DRIVER_TARGET_STUCK_TIMEOUT) {
            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordDriverTargetReset(mumakil);
            }
            temporarilyRejectDriverTarget(
                    mumakil,
                    target,
                    worldTick,
                    DRIVER_TARGET_REJECT_TICKS,
                    "stuck"
            );

            clearAuthoritativeAttackTarget(mumakil, driver, target);
            clearStoredDriverTarget(mumakil);
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

        boolean trackPerformance =
                MumakilPerformanceTracker.isEnabled();
        long perfStart = trackPerformance
                ? MumakilPerformanceTracker.startTimer()
                : 0L;
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

        if (trackPerformance) {
            MumakilPerformanceTracker.recordMountTargetScan(mumakil, nearby.size(), System.nanoTime() - perfStart);
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

        if (isAttackingAttachedArcher(candidate, mumakil)) {
            score += 900;
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

    private static boolean isAttackingAttachedArcher(EntityLivingBase attacker, LOTREntityMumakil mumakil) {
        if (!(attacker instanceof EntityLiving) || mumakil == null) {
            return false;
        }

        EntityLivingBase victim = ((EntityLiving)attacker).getAttackTarget();
        return victim instanceof LOTREntityMumakilHowdahArcher
                && ((LOTREntityMumakilHowdahArcher)victim).getHowdahMountEntityId()
                == mumakil.getEntityId();
    }

    private static boolean isValidDriverTarget(
            LOTREntityMumakil mumakil,
            EntityLivingBase driver,
            EntityLivingBase target
    ) {
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordMountCandidateCheck(mumakil);
        }

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

        if (target instanceof LOTREntityMumakilHowdahArcher) {
            return false;
        }

        if (target instanceof EntityAnimal
                && !isAttacking(target, mumakil)
                && !isAttacking(target, driver)
                && !isAttackingAttachedArcher(target, mumakil)) {
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
                return target == mumakil.getRecentFormationThreat();
            }

            if (driver instanceof LOTREntityNPC) {
                UUID hiringPlayerId =
                        ((LOTREntityNPC)driver).hiredNPCInfo.getHiringPlayerUUID();
                if (hiringPlayerId != null
                        && hiringPlayerId.equals(player.getUniqueID())) {
                    return false;
                }
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
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordUnreachableCheck(mumakil);
        }

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

}
