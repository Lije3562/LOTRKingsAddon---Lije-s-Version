package com.enovak.lotrmoremobs.entity.animal;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.inventory.ContainerMumakilInventory;
import com.enovak.lotrmoremobs.util.MumakilPerformanceTracker;
import lotr.common.LOTRCommonProxy;
import lotr.common.LOTRMod;
import lotr.common.LOTRReflection;
import lotr.common.entity.LOTREntityUtils;
import lotr.common.entity.ai.LOTREntityAIAttackOnCollide;
import lotr.common.entity.ai.LOTREntityAIHorseFollowHiringPlayer;
import lotr.common.entity.ai.LOTREntityAIHorseMoveToRiderTarget;
import lotr.common.entity.animal.LOTREntityHorse;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class LOTREntityMumakil extends LOTREntityHorse implements IAnimatable {

    // ---------------------------------------------------------------------
    // Build / debug flags
    // ---------------------------------------------------------------------

    private static final boolean DEBUG_COMBAT_LOGS = false;

    // LOTRMoreMobs Mumakil entity patch: STRIKE_TIMER_SOUND_MAPPING_V12_4_NORMAL_HIT_SOUND_2026_06_28

    // ---------------------------------------------------------------------
    // Base stats
    // ---------------------------------------------------------------------

    private static final double MAX_HEALTH = 1000.0D;
    private static final double MOVEMENT_SPEED = 0.30D;
    private static final double KNOCKBACK_RESISTANCE = 20.0D;
    private static final double ATTACK_DAMAGE = 16.0D;

    // ---------------------------------------------------------------------
    // Rider positioning
    // Forward = positive value moves rider toward Mumakil head.
    // Side = positive value moves rider to Mumakil right side.
    // ---------------------------------------------------------------------

    private static final double RIDER_WILD_FORWARD = 4.0D;
    private static final double RIDER_WILD_SIDE = 0.0D;
    private static final double RIDER_WILD_Y = 16.0D;

    private static final double RIDER_SADDLE_FORWARD = 4.0D;
    private static final double RIDER_SADDLE_SIDE = 0.0D;
    private static final double RIDER_SADDLE_Y = 16.0D;

    private static final double RIDER_HOWDAH_FORWARD = 9.8D;
    private static final double RIDER_HOWDAH_SIDE = 0.0D;
    private static final double RIDER_HOWDAH_Y = 16.5D;

    // ---------------------------------------------------------------------
    // Wild movement
    // ---------------------------------------------------------------------

    private static final double WILD_WANDER_SPEED = 0.22D;
    private static final double WILD_CHASE_SPEED = 0.40D;
    private static final int WILD_CHASE_REPATH_INTERVAL = 25;
    private static final int WILD_WANDER_MIN_INTERVAL = 80;
    private static final int WILD_WANDER_RANDOM_INTERVAL = 81;
    private static final int WILD_WANDER_RADIUS = 16;
    private static final int WILD_WANDER_VERTICAL_RANGE = 4;
    private static final int WILD_FALLBACK_MOVE_TICKS = 30;
    private static final float WILD_FALLBACK_TURN_STEP = 8.0F;
    private static final double WILD_CHASE_FALLBACK_ACCELERATION = 0.08D;
    private static final double WILD_WANDER_FALLBACK_ACCELERATION = 0.04D;
    private static final double WILD_CHASE_FALLBACK_MAX_SPEED = 0.28D;
    private static final double WILD_WANDER_FALLBACK_MAX_SPEED = 0.14D;
    private static final double WILD_WANDER_FALLBACK_STOP_RANGE = 3.0D;

    private static final int MOB_TARGET_CHECK_INTERVAL = 20;
    private static final double MOB_TARGET_RANGE = 18.0D;
    private static final double MOB_TARGET_VERTICAL_RANGE = 8.0D;

    private static final int ANGER_WAVE_MIN_DURATION = 60;
    private static final int ANGER_WAVE_RANDOM_DURATION = 61;
    private static final int ANGER_WAVE_MIN_COOLDOWN = 180;
    private static final int ANGER_WAVE_RANDOM_COOLDOWN = 81;

    private static final String NBT_HIRED_WAR_MUMAKIL = "lotrmoremobs_hiredWarMumakil";

    // ---------------------------------------------------------------------
    // Combat / tusk attack / trample
    // ---------------------------------------------------------------------

    private static final double WILD_ATTACK_SPEED = 1.30D;
    private static final int COMBAT_PATH_REPATH_COOLDOWN = 20;
    private static final int COMBAT_PATH_NO_PATH_RETRY_COOLDOWN = 40;
    private static final int COMBAT_PATH_FAILURE_BACKOFF_MIN = 40;
    private static final int COMBAT_PATH_FAILURE_BACKOFF_MAX = 100;
    private static final int COMBAT_PATH_STAGGER_TICKS = 5;
    private static final int COMBAT_PATH_PROGRESS_CHECK_TICKS = 20;
    private static final int COMBAT_PATH_NO_PROGRESS_TICKS = 60;
    private static final double COMBAT_PATH_TARGET_MOVE_THRESHOLD_SQ = 36.0D;
    private static final double COMBAT_PATH_PROGRESS_THRESHOLD_SQ = 1.0D;
    private static final float CHARGE_MIN_SPEED = 0.24F;
    private static final float MAX_CHARGE_DAMAGE = 36.0F;

    private static final double TUSK_ATTACK_RANGE = 6.5D;
    private static final int TUSK_ATTACK_COOLDOWN_TICKS = 400;
    private static final double TUSK_ATTACK_FRONT_CONE_DOT = 0.3D;
    private static final double TUSK_ATTACK_CLOSE_RANGE = 2.5D;

    private static final float TUSK_AOE_DAMAGE = 16.0F;
    private static final double TUSK_AOE_RADIUS = 4.25D;
    private static final double TUSK_AOE_VERTICAL_RANGE = 2.25D;
    private static final float TUSK_AOE_KNOCKBACK_HORIZONTAL = 3.0F;
    private static final float TUSK_AOE_KNOCKBACK_VERTICAL = 1.0F;

    private static final int TRAMPLE_SCAN_INTERVAL = 2;
    private static final int TRAMPLE_COOLDOWN_TICKS = 60;
    private static final float TRAMPLE_MIN_SPEED = 0.10F;
    private static final float TRAMPLE_DAMAGE = 8.0F;

    private static final int PROJECTILE_HURT_RESISTANT_TICKS = 6;

    // ---------------------------------------------------------------------
    // Tree/obstacle clearing
    // ---------------------------------------------------------------------

    private static final int AGGRO_OBSTACLE_CLEAR_INTERVAL = 2;
    private static final int MAX_OBSTACLES_PER_PASS = 96;

    // ---------------------------------------------------------------------
    // Sounds / animations
    // ---------------------------------------------------------------------

    private static final float CHARGE_STOMP_SOUND_MIN_SPEED = 0.13F;
    private static final int CHARGE_STOMP_SOUND_MIN_COOLDOWN = 10;
    private static final int CHARGE_STOMP_SOUND_RANDOM_COOLDOWN = 7;

    private static final int MUMAKIL_STRIKE_ANIMATION_TICKS = 36;
    private static final byte MUMAKIL_STRIKE_LEFT_STATUS = 80;
    private static final byte MUMAKIL_STRIKE_RIGHT_STATUS = 81;

    // ---------------------------------------------------------------------
    // Idle yaw stabilization
    // ---------------------------------------------------------------------

    private static final float IDLE_YAW_SNAP_THRESHOLD = 45.0F;
    private static final float IDLE_YAW_MAX_STEP = 8.0F;
    private static final float IDLE_HEAD_YAW_LIMIT = 45.0F;
    private static final double IDLE_YAW_MOTION_THRESHOLD_SQ = 4.0E-4D;

    // ---------------------------------------------------------------------
    // Mount inventory / data watcher
    // ---------------------------------------------------------------------

    private static final int HORSE_ARMOR_WATCHER_ID = 22;
    private static final int MUMAKIL_HOWDAH_SYNC_ARMOR_INDEX = 1;

    // ---------------------------------------------------------------------
    // Runtime state
    // ---------------------------------------------------------------------

    private final Map<Integer, Integer> trampleCooldowns = new HashMap<Integer, Integer>();
    private final AnimationFactory animationFactory = new AnimationFactory(this);
    private final DriverTargetProgressState driverTargetProgressState = new DriverTargetProgressState();

    private float lastStableIdleYaw;
    private float lastStableIdleHeadYaw;
    private boolean hasStableIdleYaw;

    private int chargeStompSoundCooldown;
    private int angerWaveCooldownTicks;
    private int angerWaveActiveTicks;
    private int tuskAttackCooldownTicks;

    private int mumakilStrikeAnimationTicks;
    private int prevMumakilStrikeAnimationTicks;
    private boolean mumakilStrikeAnimationLeft;
    private int mumakilAngrySoundTriggerCounter;

    public DriverTargetProgressState getDriverTargetProgressState() {
        return this.driverTargetProgressState;
    }

    public static final class DriverTargetProgressState {
        public int progressTargetEntityId = -1;
        public long nextProgressCheckTick;
        public double lastProgressX;
        public double lastProgressY;
        public double lastProgressZ;
        public int stuckTicks;

        public void reset() {
            this.progressTargetEntityId = -1;
            this.nextProgressCheckTick = 0L;
            this.lastProgressX = 0.0D;
            this.lastProgressY = 0.0D;
            this.lastProgressZ = 0.0D;
            this.stuckTicks = 0;
        }
    }

    // ---------------------------------------------------------------------
    // Construction / GeckoLib / basic entity hooks
    // ---------------------------------------------------------------------

    public LOTREntityMumakil(World world) {
        super(world);
        // Main physical/hurt box. Wide and tall enough to roughly fit the rendered Mumakil body.
        this.setSize(7.0F, 15.0F);
        this.resetAngerWaveCooldown();
        this.replaceInheritedRiderTargetAI();
        this.replaceInheritedFollowHiringPlayerAI();
        this.replaceInheritedHurtByTargetAI();

        this.tasks.addTask(4, new EntityAIWildMumakilMove());
        this.tasks.addTask(5, new EntityAIBlockHiredWarWander());
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget(this, EntityPlayer.class, 10, true) {
            @Override
            public boolean shouldExecute() {
                return LOTREntityMumakil.this.isWildMumakil() && super.shouldExecute();
            }


            @Override
            public boolean continueExecuting() {
                return LOTREntityMumakil.this.isWildMumakil() && super.continueExecuting();
            }
        });
        this.targetTasks.addTask(3, this.createWildMobTargetAI(IMob.class));
        this.targetTasks.addTask(4, this.createWildMobTargetAI(LOTREntityNPC.class));
        this.targetTasks.addTask(5, this.createWildMobTargetAI(EntityLivingBase.class));
    }

    private void replaceInheritedRiderTargetAI() {
        EntityAITasks.EntityAITaskEntry inherited = LOTREntityUtils.removeAITask(
                this,
                LOTREntityAIHorseMoveToRiderTarget.class
        );

        if (inherited != null) {
            this.tasks.addTask(inherited.priority, new EntityAIMumakilMoveToRiderTarget());
        }
    }

    private void replaceInheritedFollowHiringPlayerAI() {
        EntityAITasks.EntityAITaskEntry inherited = LOTREntityUtils.removeAITask(
                this,
                LOTREntityAIHorseFollowHiringPlayer.class
        );

        if (inherited != null) {
            this.tasks.addTask(inherited.priority, new EntityAIMumakilFollowHiringPlayer());
        }
    }

    private void replaceInheritedHurtByTargetAI() {
        EntityAITasks.EntityAITaskEntry inherited = LOTREntityUtils.removeAITask(
                this,
                EntityAIHurtByTarget.class
        );

        if (inherited != null) {
            this.targetTasks.addTask(inherited.priority, new EntityAIMumakilHurtByTarget());
        }
    }

    public float getCollisionBorderSize() {
        // Small melee/raycast padding so normal-reach weapons can hit the large body reliably.
        return 1.25F;
    }

    public void registerControllers(AnimationData data) {
    }

    public AnimationFactory getFactory() {
        return this.animationFactory;
    }

    public int getHorseType() {
        return 0;
    }


    // ---------------------------------------------------------------------
    // Wild and hired-war state gates
    // ---------------------------------------------------------------------

    public boolean isHiredWarMumakil() {
        return this.getEntityData().getBoolean(NBT_HIRED_WAR_MUMAKIL);
    }

    public void setHiredWarMumakil(boolean hiredWar) {
        this.getEntityData().setBoolean(NBT_HIRED_WAR_MUMAKIL, hiredWar);
    }

    private boolean isWildMumakil() {
        return !this.isTame() && !this.getBelongsToNPC() && this.riddenByEntity == null;
    }

    private boolean shouldWildWander() {
        return this.isWildMumakil()
                && !this.getBelongsToNPC()
                && !this.hasMumakilHowdahEquipped()
                && !this.isMountEnraged()
                && this.getAttackTarget() == null;
    }

    private boolean shouldWildChaseTarget() {
        EntityLivingBase target = this.getAttackTarget();
        return this.isWildMumakil()
                && !this.hasMumakilHowdahEquipped()
                && target != null
                && this.canTuskAttackTarget(target);
    }


    // ---------------------------------------------------------------------
    // Wild target selection and movement AI
    // ---------------------------------------------------------------------

    private void tryAcquireWildMobTarget() {
        if (!this.isWildMumakil()
                || !this.isWildAngerWaveActive()
                || this.getAttackTarget() != null
                || this.ticksExisted % MOB_TARGET_CHECK_INTERVAL != 0) {
            return;
        }

        long perfStart = MumakilPerformanceTracker.startTimer();
        List nearby = this.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                this.boundingBox.expand(MOB_TARGET_RANGE, MOB_TARGET_VERTICAL_RANGE, MOB_TARGET_RANGE)
        );

        EntityLivingBase bestTarget = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;

        for(int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase candidate = (EntityLivingBase)nearby.get(i);
            if (!this.canTargetWildMob(candidate)) {
                continue;
            }

            int priority = this.getWildMobTargetPriority(candidate);
            double distanceSq = this.getDistanceSqToEntity(candidate);

            if (priority < bestPriority || priority == bestPriority && distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestPriority = priority;
                bestDistanceSq = distanceSq;
            }
        }

        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordMountTargetScan(this, nearby.size(), System.nanoTime() - perfStart);
        }

        if (bestTarget != null && (bestPriority < 2 || this.rand.nextInt(4) == 0)) {
            this.playMumakilAngrySound();
            this.setAttackTarget(bestTarget);
        }
    }

    private boolean canTargetWildMob(EntityLivingBase target) {
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordMountCandidateCheck(this);
        }

        if (target == this
                || target instanceof EntityPlayer
                || target instanceof LOTREntityMumakil
                || !target.isEntityAlive()
                || target.riddenByEntity != null
                || target.ridingEntity != null) {
            return false;
        }

        if (target instanceof EntityTameable && ((EntityTameable)target).isTamed()) {
            return false;
        }

        if (target instanceof EntityHorse && ((EntityHorse)target).isTame()) {
            return false;
        }

        if (target instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)target;
            return !npc.hiredNPCInfo.isActive;
        }

        return true;
    }

    private int getWildMobTargetPriority(EntityLivingBase target) {
        if (target instanceof IMob) {
            return 0;
        }

        if (target instanceof LOTREntityNPC || !(target instanceof EntityAnimal)) {
            return 1;
        }

        return 2;
    }

    private EntityAIBase createWildMobTargetAI(Class targetClass) {
        return new EntityAINearestAttackableTarget(this, targetClass, 5, true, false, new IEntitySelector() {
            @Override
            public boolean isEntityApplicable(Entity entity) {
                return entity instanceof EntityLivingBase
                        && LOTREntityMumakil.this.isWildMumakil()
                        && LOTREntityMumakil.this.canTargetWildMob((EntityLivingBase)entity);
            }
        }) {
            @Override
            public boolean shouldExecute() {
                return LOTREntityMumakil.this.isWildMumakil()
                        && (LOTREntityMumakil.this.getAttackTarget() != null || LOTREntityMumakil.this.isWildAngerWaveActive())
                        && super.shouldExecute();
            }

            @Override
            public boolean continueExecuting() {
                return LOTREntityMumakil.this.isWildMumakil() && super.continueExecuting();
            }
        };
    }

    private class EntityAIWildMumakilMove extends EntityAIBase {
        private int nextChasePathTick;
        private int nextWanderPathTick;
        private int fallbackMoveTicks;
        private double fallbackX;
        private double fallbackZ;

        public EntityAIWildMumakilMove() {
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return LOTREntityMumakil.this.isWildMumakil()
                    && (LOTREntityMumakil.this.shouldWildChaseTarget()
                    || LOTREntityMumakil.this.shouldWildWander());
        }

        @Override
        public boolean continueExecuting() {
            if (!LOTREntityMumakil.this.isWildMumakil()) {
                return false;
            }

            if (LOTREntityMumakil.this.shouldWildChaseTarget()) {
                return true;
            }

            return LOTREntityMumakil.this.shouldWildWander()
                    && (!LOTREntityMumakil.this.getNavigator().noPath() || this.fallbackMoveTicks > 0);
        }

        @Override
        public void resetTask() {
            this.fallbackMoveTicks = 0;
        }

        @Override
        public void updateTask() {
            if (LOTREntityMumakil.this.shouldWildChaseTarget()) {
                this.updateChaseMovement();
            } else if (LOTREntityMumakil.this.shouldWildWander()) {
                this.updateWanderMovement();
            }
        }

        private void updateChaseMovement() {
            EntityLivingBase target = LOTREntityMumakil.this.getAttackTarget();
            if (target == null) {
                return;
            }

            if (LOTREntityMumakil.this.ticksExisted >= this.nextChasePathTick) {
                long perfStart = MumakilPerformanceTracker.startTimer();
                boolean pathAccepted = LOTREntityMumakil.this.getNavigator().tryMoveToEntityLiving(target, WILD_CHASE_SPEED);
                if (MumakilPerformanceTracker.isEnabled()) {
                    MumakilPerformanceTracker.recordMountPathRequest(
                            LOTREntityMumakil.this,
                            System.nanoTime() - perfStart,
                            pathAccepted && !LOTREntityMumakil.this.getNavigator().noPath(),
                            true
                    );
                }
                this.nextChasePathTick = LOTREntityMumakil.this.ticksExisted + WILD_CHASE_REPATH_INTERVAL;
            }

            LOTREntityMumakil.this.setAIMoveSpeed((float)WILD_CHASE_SPEED);

            if (LOTREntityMumakil.this.getNavigator().noPath()) {
                this.applyFallbackMove(
                        target.posX,
                        target.posZ,
                        WILD_CHASE_SPEED,
                        WILD_CHASE_FALLBACK_ACCELERATION,
                        WILD_CHASE_FALLBACK_MAX_SPEED,
                        TUSK_ATTACK_RANGE
                );
            }
        }

        private void updateWanderMovement() {
            if (!LOTREntityMumakil.this.getNavigator().noPath()) {
                LOTREntityMumakil.this.setAIMoveSpeed((float)WILD_WANDER_SPEED);
                return;
            }

            if (this.fallbackMoveTicks > 0) {
                if (this.applyFallbackMove(
                        this.fallbackX,
                        this.fallbackZ,
                        WILD_WANDER_SPEED,
                        WILD_WANDER_FALLBACK_ACCELERATION,
                        WILD_WANDER_FALLBACK_MAX_SPEED,
                        WILD_WANDER_FALLBACK_STOP_RANGE
                )) {
                    --this.fallbackMoveTicks;
                } else {
                    this.fallbackMoveTicks = 0;
                }
                return;
            }

            if (LOTREntityMumakil.this.ticksExisted < this.nextWanderPathTick) {
                return;
            }

            this.nextWanderPathTick = LOTREntityMumakil.this.ticksExisted
                    + WILD_WANDER_MIN_INTERVAL
                    + LOTREntityMumakil.this.rand.nextInt(WILD_WANDER_RANDOM_INTERVAL);

            Vec3 wanderTarget = RandomPositionGenerator.findRandomTarget(
                    LOTREntityMumakil.this,
                    WILD_WANDER_RADIUS,
                    WILD_WANDER_VERTICAL_RANGE
            );

            if (wanderTarget == null) {
                return;
            }

            long perfStart = MumakilPerformanceTracker.startTimer();
            boolean pathAccepted = LOTREntityMumakil.this.getNavigator().tryMoveToXYZ(
                    wanderTarget.xCoord,
                    wanderTarget.yCoord,
                    wanderTarget.zCoord,
                    WILD_WANDER_SPEED
            );
            boolean pathUsable = pathAccepted && !LOTREntityMumakil.this.getNavigator().noPath();

            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordMountPathRequest(
                        LOTREntityMumakil.this,
                        System.nanoTime() - perfStart,
                        pathUsable,
                        false
                );
            }

            if (!pathUsable) {
                this.fallbackX = wanderTarget.xCoord;
                this.fallbackZ = wanderTarget.zCoord;
                this.fallbackMoveTicks = WILD_FALLBACK_MOVE_TICKS;
            }
        }

        private boolean applyFallbackMove(
                double x,
                double z,
                double aiSpeed,
                double acceleration,
                double maxSpeed,
                double stopRange
        ) {
            if (!LOTREntityMumakil.this.isWildMumakil()) {
                return false;
            }

            LOTREntityMumakil.this.faceWildMovePoint(x, z);

            double dx = x - LOTREntityMumakil.this.posX;
            double dz = z - LOTREntityMumakil.this.posZ;
            double distSq = dx * dx + dz * dz;
            if (distSq <= stopRange * stopRange || distSq < 1.0E-4D) {
                LOTREntityMumakil.this.moveForward = 0.0F;
                LOTREntityMumakil.this.moveStrafing = 0.0F;
                return false;
            }

            double dist = Math.sqrt(distSq);
            LOTREntityMumakil.this.motionX += dx / dist * acceleration;
            LOTREntityMumakil.this.motionZ += dz / dist * acceleration;

            double horizontalMotionSq = LOTREntityMumakil.this.motionX * LOTREntityMumakil.this.motionX
                    + LOTREntityMumakil.this.motionZ * LOTREntityMumakil.this.motionZ;
            if (horizontalMotionSq > maxSpeed * maxSpeed) {
                double horizontalMotion = Math.sqrt(horizontalMotionSq);
                LOTREntityMumakil.this.motionX = LOTREntityMumakil.this.motionX / horizontalMotion * maxSpeed;
                LOTREntityMumakil.this.motionZ = LOTREntityMumakil.this.motionZ / horizontalMotion * maxSpeed;
            }

            LOTREntityMumakil.this.setAIMoveSpeed((float)aiSpeed);
            LOTREntityMumakil.this.moveForward = 1.0F;
            LOTREntityMumakil.this.moveStrafing = 0.0F;
            LOTREntityMumakil.this.velocityChanged = true;
            return true;
        }
    }


    // ---------------------------------------------------------------------
    // Howdah / saddle / inventory helpers
    // ---------------------------------------------------------------------

    public boolean hasMumakilHowdahEquipped() {
        return this.hasMumakilHowdahInventoryStack()
                || this.getMumakilSyncedArmorIndex() > 0;
    }

    private boolean hasMumakilHowdahInventoryStack() {
        ItemStack stack = this.getMumakilInventoryStack(1);
        return stack != null && stack.getItem() == Main.mumakilHowdah;
    }

    public boolean isMumakilHowdahEquipped() {
        return this.hasMumakilHowdahEquipped();
    }

    public boolean hasMumakilSaddleEquipped() {
        ItemStack stack = this.getMumakilInventoryStack(0);
        return this.isMountSaddled()
                || stack != null && stack.getItem() == Items.saddle;
    }

    private int getMumakilSyncedArmorIndex() {
        try {
            return this.dataWatcher.getWatchableObjectInt(HORSE_ARMOR_WATCHER_ID);
        } catch (Exception e) {
            return 0;
        }
    }

    private void setMumakilSyncedArmorIndex(int armorIndex) {
        try {
            this.dataWatcher.updateObject(HORSE_ARMOR_WATCHER_ID, Integer.valueOf(armorIndex));
        } catch (Exception e) {
        }
    }

    private void updateMumakilHowdahSyncState() {
        if (this.worldObj.isRemote) {
            return;
        }

        int desiredArmorIndex = this.hasMumakilHowdahInventoryStack() ? MUMAKIL_HOWDAH_SYNC_ARMOR_INDEX : 0;

        if (this.getMumakilSyncedArmorIndex() != desiredArmorIndex) {
            this.setMumakilSyncedArmorIndex(desiredArmorIndex);
        }
    }

    public void setMumakilHowdahEquipped(boolean equipped) {
        if (!this.worldObj.isRemote) {
            this.setMumakilSyncedArmorIndex(equipped ? MUMAKIL_HOWDAH_SYNC_ARMOR_INDEX : 0);
        }
    }

    private ItemStack getMumakilInventoryStack(int slot) {
        IInventory inventory = this.findMumakilMountInventory();
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) {
            return null;
        }

        return inventory.getStackInSlot(slot);
    }

    private boolean setMumakilInventoryStack(int slot, ItemStack stack) {
        IInventory inventory = this.findMumakilMountInventory();
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) {
            return false;
        }

        inventory.setInventorySlotContents(slot, stack);
        inventory.markDirty();

        if (slot == 1 && !this.worldObj.isRemote) {
            this.updateMumakilHowdahSyncState();
        }

        return true;
    }

    private boolean tryEquipMumakilHowdah(EntityPlayer player) {
        if (this.getBelongsToNPC()) {
            return false;
        }

        ItemStack held = player.getCurrentEquippedItem();

        if (!this.hasMumakilSaddleEquipped()) {
            return false;
        }

        if (this.hasMumakilHowdahEquipped()) {
            return false;
        }

        if (this.worldObj.isRemote) {
            return true;
        }

        ItemStack howdahStack = new ItemStack(Main.mumakilHowdah);
        if (!this.setMumakilInventoryStack(1, howdahStack)) {
            return false;
        }

        if (!player.capabilities.isCreativeMode) {
            --held.stackSize;
            if (held.stackSize <= 0) {
                player.inventory.setInventorySlotContents(player.inventory.currentItem, null);
            }
        }

        player.swingItem();
        return true;
    }

    private IInventory findMumakilMountInventory() {
        String[] inventoryFieldNames = new String[] {
                "horseChest",
                "mountInventory",
                "horseInventory",
                "inventory"
        };

        for (int i = 0; i < inventoryFieldNames.length; ++i) {
            Field field = this.findMumakilField(this.getClass(), inventoryFieldNames[i]);
            if (field != null) {
                try {
                    Object value = field.get(this);
                    if (value instanceof IInventory) {
                        return (IInventory)value;
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private Field findMumakilField(Class type, String name) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }


    // ---------------------------------------------------------------------
    // Rider placement and player interaction
    // ---------------------------------------------------------------------

    public double getMountedYOffset() {
        if (this.hasMumakilHowdahEquipped()) {
            return RIDER_HOWDAH_Y;
        }

        if (this.hasMumakilSaddleEquipped()) {
            return RIDER_SADDLE_Y;
        }

        return RIDER_WILD_Y;
    }

    public void updateRiderPosition() {
        if (this.riddenByEntity != null) {
            boolean hasHowdah = this.hasMumakilHowdahEquipped();

            double forwardOffset;
            double sideOffset;

            if (hasHowdah) {
                forwardOffset = RIDER_HOWDAH_FORWARD;
                sideOffset = RIDER_HOWDAH_SIDE;
            } else if (this.hasMumakilSaddleEquipped()) {
                forwardOffset = RIDER_SADDLE_FORWARD;
                sideOffset = RIDER_SADDLE_SIDE;
            } else {
                forwardOffset = RIDER_WILD_FORWARD;
                sideOffset = RIDER_WILD_SIDE;
            }

            double verticalOffset = this.getMountedYOffset() + this.riddenByEntity.getYOffset();

            /*
             * Important:
             * For howdah riders, use renderYawOffset because that is the yaw used by the
             * visible Mumakil body/howdah. Using rotationYaw can put the rider beside the
             * howdah when the body and path-facing yaw disagree.
             *
             * Do NOT force NPC rider yaw here. The hired Southron driver should keep his
             * own look/target logic so his allegiances and aggro can control the mount
             * normally through LOTR's hired-horse AI.
             */
            float placementYaw = hasHowdah ? this.renderYawOffset : this.rotationYaw;
            float yawRadians = placementYaw * 3.1415927F / 180.0F;

            double forwardX = -MathHelper.sin(yawRadians) * forwardOffset;
            double forwardZ = MathHelper.cos(yawRadians) * forwardOffset;

            double sideX = MathHelper.cos(yawRadians) * sideOffset;
            double sideZ = MathHelper.sin(yawRadians) * sideOffset;

            this.riddenByEntity.setPosition(
                    this.posX + forwardX + sideX,
                    this.posY + verticalOffset,
                    this.posZ + forwardZ + sideZ
            );

            /*
             * Keep player riding comfortable, but do not force NPC driver rotation.
             * Forcing the NPC driver's rotation/head yaw was what caused the earlier glitch.
             */
            if (hasHowdah && this.riddenByEntity instanceof EntityPlayer) {
                this.riddenByEntity.rotationYaw = placementYaw;
                this.riddenByEntity.prevRotationYaw = placementYaw;
            }
        }
    }

    public boolean shouldRiderSit() {
        return !this.hasMumakilHowdahEquipped();
    }

    public boolean interact(EntityPlayer player) {
        if (this.shouldRouteHiredWarBodyClick()) {
            return this.interactHiredWarDriver(player);
        }

        if (this.getBelongsToNPC()) {
            return super.interact(player);
        }

        if (this.tryEquipMumakilHowdah(player)) {
            return true;
        }

        if (player.isSneaking()) {
            this.openGUI(player);
            return true;
        }

        return super.interact(player);
    }

    private boolean shouldRouteHiredWarBodyClick() {
        if (this.isHiredWarMumakil()) {
            return true;
        }

        if (this.riddenByEntity instanceof LOTREntityNPC) {
            LOTREntityNPC driver = (LOTREntityNPC)this.riddenByEntity;
            return driver.hiredNPCInfo != null && driver.hiredNPCInfo.isActive;
        }

        return false;
    }

    private boolean interactHiredWarDriver(EntityPlayer player) {
        LOTREntityNPC driver = this.getLivingHiredWarDriver();

        if (driver == null) {
            return true;
        }

        if (driver.hiredNPCInfo.getHiringPlayer() == player) {
            if (this.worldObj.isRemote) {
                Main.proxy.prepareMumakilHiredDriverGui(driver.getEntityId(), this.getEntityId());
                player.openGui(
                        LOTRMod.instance,
                        LOTRCommonProxy.GUI_ID_HIRED_INTERACT,
                        player.worldObj,
                        driver.getEntityId(),
                        0,
                        0
                );
            } else {
                // Keep client hired data fresh; the initial interact GUI itself is client-only in LOTR.
                driver.hiredNPCInfo.sendClientPacket(false);
            }
        }

        return true;
    }

    private LOTREntityNPC getLivingHiredWarDriver() {
        if (!(this.riddenByEntity instanceof LOTREntityNPC)) {
            return null;
        }

        LOTREntityNPC driver = (LOTREntityNPC)this.riddenByEntity;
        if (driver.isDead || !driver.isEntityAlive() || driver.hiredNPCInfo == null || !driver.hiredNPCInfo.isActive) {
            return null;
        }

        return driver;
    }

    public void openGUI(EntityPlayer player) {
        if (this.getBelongsToNPC()) {
            return;
        }

        if (!this.worldObj.isRemote) {
            this.updateMumakilHowdahSyncState();
            IInventory inventory = this.findMumakilMountInventory();

            if (inventory != null && player instanceof EntityPlayerMP) {
                EntityPlayerMP playerMP = (EntityPlayerMP)player;

                /*
                 * First let vanilla open the normal horse GUI window on the client.
                 */
                playerMP.displayGUIHorse(this, inventory);

                /*
                 * Then replace the server-side container with a Mumakil-safe version.
                 * This keeps the same GUI but prevents the distance check from instantly closing it.
                 */
                int windowId = playerMP.openContainer.windowId;
                playerMP.openContainer = new ContainerMumakilInventory(playerMP.inventory, inventory, this);
                playerMP.openContainer.windowId = windowId;
                playerMP.openContainer.addCraftingToCrafters(playerMP);

                return;
            }

            super.openGUI(player);
        }
    }


    // ---------------------------------------------------------------------
    // Mount flags, attributes, breeding, and NBT
    // ---------------------------------------------------------------------

    protected boolean isMountHostile() {
        return true;
    }

    protected EntityAIBase createMountAttackAI() {
        return new EntityAIMumakilAttackOnCollide();
    }

    private boolean hasLivingNPCCombatDriver() {
        if (!(this.riddenByEntity instanceof LOTREntityNPC)) {
            return false;
        }

        LOTREntityNPC driver = (LOTREntityNPC)this.riddenByEntity;
        return !driver.isDead && driver.isEntityAlive();
    }

    private boolean shouldUsePersonalAttackAI() {
        if (this.isChild() || this.riddenByEntity instanceof EntityPlayer) {
            return false;
        }

        if (this.isHiredWarMumakil()) {
            if (!this.hasLivingNPCCombatDriver()) {
                return true;
            }

            EntityLivingBase driverTarget = ((LOTREntityNPC)this.riddenByEntity).getAttackTarget();
            return driverTarget != null
                    && driverTarget.isEntityAlive()
                    && this.getAttackTarget() == driverTarget;
        }

        return this.isWildMumakil();
    }

    private boolean hasExpectedRiderTargetPathRequest() {
        if (this.isHiredWarMumakil()
                || !this.getBelongsToNPC()
                || !this.hasLivingNPCCombatDriver()) {
            return false;
        }

        EntityLivingBase target = ((LOTREntityNPC)this.riddenByEntity).getAttackTarget();
        return target != null && target.isEntityAlive();
    }

    private boolean hasActiveHiredWarCombatTarget() {
        if (!this.isHiredWarMumakil()) {
            return false;
        }

        EntityLivingBase target;
        if (this.hasLivingNPCCombatDriver()) {
            target = ((LOTREntityNPC)this.riddenByEntity).getAttackTarget();
        } else {
            target = this.getAttackTarget();
        }

        return target != null && target.isEntityAlive();
    }

    private boolean hasActiveLivingSouthronDriver() {
        if (!this.isHiredWarMumakil() || !(this.riddenByEntity instanceof LOTREntityNPC)) {
            return false;
        }

        LOTREntityNPC driver = (LOTREntityNPC)this.riddenByEntity;
        if (driver.isDead
                || !driver.isEntityAlive()
                || driver.hiredNPCInfo == null
                || !driver.hiredNPCInfo.isActive) {
            return false;
        }

        try {
            return LOTRMod.getNPCFaction(driver) == LOTRFaction.NEAR_HARAD;
        } catch (Exception e) {
            return false;
        }
    }

    /* Recurring repaths remain private inside LOTR; only the directly visible initial path is timed here. */
    private class EntityAIMumakilMoveToRiderTarget extends LOTREntityAIHorseMoveToRiderTarget {
        private EntityAIMumakilMoveToRiderTarget() {
            super(LOTREntityMumakil.this);
        }

        @Override
        public boolean shouldExecute() {
            if (LOTREntityMumakil.this.isHiredWarMumakil()) {
                return false;
            }

            boolean measuresPath = LOTREntityMumakil.this.hasExpectedRiderTargetPathRequest();
            long start = measuresPath ? MumakilPerformanceTracker.startTimer() : 0L;
            boolean execute = super.shouldExecute();

            if (measuresPath && MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordRiderTargetPathRequest(
                        LOTREntityMumakil.this,
                        System.nanoTime() - start,
                        execute
                );
            }

            return execute;
        }

        @Override
        public boolean continueExecuting() {
            return !LOTREntityMumakil.this.isHiredWarMumakil() && super.continueExecuting();
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            MumakilPerformanceTracker.recordRiderTargetAIStart(LOTREntityMumakil.this);
        }

        @Override
        public void updateTask() {
            super.updateTask();
            MumakilPerformanceTracker.recordRiderTargetAIUpdate(LOTREntityMumakil.this);
        }

        @Override
        public void resetTask() {
            super.resetTask();
            MumakilPerformanceTracker.recordRiderTargetAIReset(LOTREntityMumakil.this);
        }
    }

    private class EntityAIMumakilAttackOnCollide extends LOTREntityAIAttackOnCollide {
        private int controlledTargetId = -1;
        private boolean newTargetPathPending;
        private boolean noProgressPathPending;
        private int nextControlledPathTick;
        private int failureBackoffTicks = COMBAT_PATH_FAILURE_BACKOFF_MIN;
        private double lastPathTargetX;
        private double lastPathTargetY;
        private double lastPathTargetZ;
        private double lastProgressX;
        private double lastProgressZ;
        private int nextProgressCheckTick;
        private int noProgressTicks;

        private EntityAIMumakilAttackOnCollide() {
            super(LOTREntityMumakil.this, WILD_ATTACK_SPEED, true);
        }

        @Override
        public boolean shouldExecute() {
            if (this.isHiredWarMountAttackAIDisabled()
                    || !LOTREntityMumakil.this.shouldUsePersonalAttackAI()) {
                return false;
            }

            if (LOTREntityMumakil.this.isHiredWarMumakil()) {
                long start = MumakilPerformanceTracker.startTimer();
                EntityLivingBase target = LOTREntityMumakil.this.getAttackTarget();
                boolean execute = target != null && target.isEntityAlive();

                if (execute) {
                    this.attackTarget = target;
                    LOTREntityMumakil.this.getNavigator().setAvoidsWater(false);
                    this.resetControlledPathForNewTarget(target);
                    this.updateControlledPath(target, true);
                }

                MumakilPerformanceTracker.recordMountAttackShould(
                        LOTREntityMumakil.this,
                        System.nanoTime() - start
                );
                return execute;
            }

            boolean measuresPath = LOTREntityMumakil.this.getAttackTarget() != null;
            long start = MumakilPerformanceTracker.startTimer();
            boolean execute = super.shouldExecute();

            if (MumakilPerformanceTracker.isEnabled()) {
                long elapsed = System.nanoTime() - start;
                MumakilPerformanceTracker.recordMountAttackShould(LOTREntityMumakil.this, elapsed);
                if (measuresPath) {
                    MumakilPerformanceTracker.recordMountAttackPathRequest(
                            LOTREntityMumakil.this,
                            elapsed,
                            execute
                    );
                }
            }

            return execute;
        }

        @Override
        public boolean continueExecuting() {
            if (this.isHiredWarMountAttackAIDisabled()
                    || !LOTREntityMumakil.this.shouldUsePersonalAttackAI()) {
                return false;
            }

            long start = MumakilPerformanceTracker.startTimer();
            boolean execute = super.continueExecuting();
            if (execute && LOTREntityMumakil.this.isHiredWarMumakil()) {
                this.resetControlledPathForNewTarget(this.attackTarget);
            }
            MumakilPerformanceTracker.recordMountAttackContinue(
                    LOTREntityMumakil.this,
                    System.nanoTime() - start
            );
            return execute;
        }

        @Override
        public void startExecuting() {
            long start = MumakilPerformanceTracker.startTimer();
            if (LOTREntityMumakil.this.isHiredWarMumakil()) {
                if (this.entityPathEntity != null) {
                    LOTREntityMumakil.this.getNavigator().setPath(this.entityPathEntity, this.moveSpeed);
                    this.entityPathEntity = null;
                }
                this.pathCheckTimer = 0;
            } else {
                super.startExecuting();
            }
            MumakilPerformanceTracker.recordMountAttackStart(
                    LOTREntityMumakil.this,
                    System.nanoTime() - start
            );
            MumakilPerformanceTracker.recordMountAttackAIStart(LOTREntityMumakil.this);
        }

        @Override
        public void updateTask() {
            long start = MumakilPerformanceTracker.startTimer();
            super.updateTask();
            MumakilPerformanceTracker.recordMountAttackUpdate(
                    LOTREntityMumakil.this,
                    System.nanoTime() - start
            );
        }

        @Override
        public void resetTask() {
            long start = MumakilPerformanceTracker.startTimer();
            super.resetTask();
            MumakilPerformanceTracker.recordMountAttackReset(
                    LOTREntityMumakil.this,
                    System.nanoTime() - start
            );
            MumakilPerformanceTracker.recordMountAttackAIReset(LOTREntityMumakil.this);
        }

        @Override
        protected void updateLookAndPathing() {
            if (!LOTREntityMumakil.this.isHiredWarMumakil()) {
                super.updateLookAndPathing();
                return;
            }

            LOTREntityMumakil.this.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 30.0F);
            this.resetControlledPathForNewTarget(this.attackTarget);
            this.updateControlledPath(this.attackTarget, false);
        }

        private void resetControlledPathForNewTarget(EntityLivingBase target) {
            int targetId = target.getEntityId();
            if (targetId == this.controlledTargetId) {
                return;
            }

            this.controlledTargetId = targetId;
            this.newTargetPathPending = true;
            this.noProgressPathPending = false;
            this.nextControlledPathTick = LOTREntityMumakil.this.ticksExisted;
            this.failureBackoffTicks = COMBAT_PATH_FAILURE_BACKOFF_MIN;
            this.lastPathTargetX = target.posX;
            this.lastPathTargetY = target.posY;
            this.lastPathTargetZ = target.posZ;
            this.lastProgressX = LOTREntityMumakil.this.posX;
            this.lastProgressZ = LOTREntityMumakil.this.posZ;
            this.nextProgressCheckTick = LOTREntityMumakil.this.ticksExisted + COMBAT_PATH_PROGRESS_CHECK_TICKS;
            this.noProgressTicks = 0;
            this.entityPathEntity = null;
            LOTREntityMumakil.this.getNavigator().clearPathEntity();
        }

        private void updateControlledPath(EntityLivingBase target, boolean preparingStart) {
            if (LOTREntityMumakil.this.getDistanceSqToEntity(target)
                    <= TUSK_ATTACK_RANGE * TUSK_ATTACK_RANGE) {
                this.resetProgressTracking();
                return;
            }

            this.updateProgressTracking();

            int reason = this.getControlledPathReason(target);
            if (reason == 0) {
                MumakilPerformanceTracker.recordCombatPathSkippedExistingPath(LOTREntityMumakil.this);
                return;
            }

            int currentTick = LOTREntityMumakil.this.ticksExisted;
            if (currentTick < this.nextControlledPathTick || !this.isStaggerTick(currentTick)) {
                MumakilPerformanceTracker.recordCombatPathSkippedCooldown(LOTREntityMumakil.this);
                return;
            }

            long start = MumakilPerformanceTracker.startTimer();
            PathEntity path = LOTREntityMumakil.this.getNavigator().getPathToEntityLiving(target);
            boolean accepted = path != null;

            if (accepted) {
                if (preparingStart) {
                    this.entityPathEntity = path;
                } else {
                    accepted = LOTREntityMumakil.this.getNavigator().setPath(path, this.moveSpeed);
                }
            }

            long elapsed = System.nanoTime() - start;
            MumakilPerformanceTracker.recordCombatPathRequest(
                    LOTREntityMumakil.this,
                    elapsed,
                    accepted,
                    reason
            );

            this.lastPathTargetX = target.posX;
            this.lastPathTargetY = target.posY;
            this.lastPathTargetZ = target.posZ;
            this.newTargetPathPending = false;
            this.noProgressPathPending = false;
            this.noProgressTicks = 0;
            this.lastProgressX = LOTREntityMumakil.this.posX;
            this.lastProgressZ = LOTREntityMumakil.this.posZ;
            this.nextProgressCheckTick = currentTick + COMBAT_PATH_PROGRESS_CHECK_TICKS;

            if (accepted) {
                this.failureBackoffTicks = COMBAT_PATH_FAILURE_BACKOFF_MIN;
                this.nextControlledPathTick = currentTick
                        + (reason == MumakilPerformanceTracker.COMBAT_PATH_REASON_NO_PATH
                        ? COMBAT_PATH_NO_PATH_RETRY_COOLDOWN
                        : COMBAT_PATH_REPATH_COOLDOWN);
            } else {
                MumakilPerformanceTracker.recordCombatPathBackoff(LOTREntityMumakil.this);
                this.nextControlledPathTick = currentTick + this.failureBackoffTicks;
                this.failureBackoffTicks = Math.min(
                        COMBAT_PATH_FAILURE_BACKOFF_MAX,
                        this.failureBackoffTicks * 2
                );
            }
        }

        private int getControlledPathReason(EntityLivingBase target) {
            if (this.newTargetPathPending) {
                return MumakilPerformanceTracker.COMBAT_PATH_REASON_NEW_TARGET;
            }
            if (this.noProgressPathPending) {
                return MumakilPerformanceTracker.COMBAT_PATH_REASON_NO_PROGRESS;
            }

            double movedX = target.posX - this.lastPathTargetX;
            double movedY = target.posY - this.lastPathTargetY;
            double movedZ = target.posZ - this.lastPathTargetZ;
            if (movedX * movedX + movedY * movedY + movedZ * movedZ
                    >= COMBAT_PATH_TARGET_MOVE_THRESHOLD_SQ) {
                return MumakilPerformanceTracker.COMBAT_PATH_REASON_TARGET_MOVED;
            }
            if (LOTREntityMumakil.this.getNavigator().noPath()) {
                return MumakilPerformanceTracker.COMBAT_PATH_REASON_NO_PATH;
            }
            return 0;
        }

        private void updateProgressTracking() {
            int currentTick = LOTREntityMumakil.this.ticksExisted;
            if (currentTick < this.nextProgressCheckTick) {
                return;
            }

            double movedX = LOTREntityMumakil.this.posX - this.lastProgressX;
            double movedZ = LOTREntityMumakil.this.posZ - this.lastProgressZ;
            if (movedX * movedX + movedZ * movedZ >= COMBAT_PATH_PROGRESS_THRESHOLD_SQ) {
                this.noProgressTicks = 0;
            } else {
                this.noProgressTicks += COMBAT_PATH_PROGRESS_CHECK_TICKS;
                if (this.noProgressTicks >= COMBAT_PATH_NO_PROGRESS_TICKS) {
                    this.noProgressPathPending = true;
                }
            }

            this.lastProgressX = LOTREntityMumakil.this.posX;
            this.lastProgressZ = LOTREntityMumakil.this.posZ;
            this.nextProgressCheckTick = currentTick + COMBAT_PATH_PROGRESS_CHECK_TICKS;
        }

        private void resetProgressTracking() {
            this.noProgressTicks = 0;
            this.noProgressPathPending = false;
            this.lastProgressX = LOTREntityMumakil.this.posX;
            this.lastProgressZ = LOTREntityMumakil.this.posZ;
            this.nextProgressCheckTick = LOTREntityMumakil.this.ticksExisted + COMBAT_PATH_PROGRESS_CHECK_TICKS;
        }

        private boolean isStaggerTick(int currentTick) {
            int entitySlot = (LOTREntityMumakil.this.getEntityId() & Integer.MAX_VALUE)
                    % COMBAT_PATH_STAGGER_TICKS;
            return currentTick % COMBAT_PATH_STAGGER_TICKS == entitySlot;
        }

        private boolean isHiredWarMountAttackAIDisabled() {
            return MumakilPerformanceTracker.DEBUG_DISABLE_HIRED_WAR_MOUNT_ATTACK_AI
                    && LOTREntityMumakil.this.isHiredWarMumakil();
        }
    }

    private class EntityAIMumakilFollowHiringPlayer extends LOTREntityAIHorseFollowHiringPlayer {
        private int followUpdatesSinceStart;

        private EntityAIMumakilFollowHiringPlayer() {
            super(LOTREntityMumakil.this);
        }

        @Override
        public boolean shouldExecute() {
            boolean blockedByDriver = LOTREntityMumakil.this.hasActiveLivingSouthronDriver();
            boolean execute = !blockedByDriver
                    && !LOTREntityMumakil.this.hasActiveHiredWarCombatTarget()
                    && super.shouldExecute();
            MumakilPerformanceTracker.recordMountFollowShould(
                    LOTREntityMumakil.this,
                    blockedByDriver,
                    execute
            );
            return execute;
        }

        @Override
        public boolean continueExecuting() {
            return !LOTREntityMumakil.this.hasActiveLivingSouthronDriver()
                    && !LOTREntityMumakil.this.hasActiveHiredWarCombatTarget()
                    && super.continueExecuting();
        }

        @Override
        public void startExecuting() {
            this.followUpdatesSinceStart = 0;
            super.startExecuting();
            MumakilPerformanceTracker.recordMountFollowStart(LOTREntityMumakil.this);
        }

        @Override
        public void updateTask() {
            boolean pathCall = this.followUpdatesSinceStart % 10 == 0;
            super.updateTask();
            ++this.followUpdatesSinceStart;
            MumakilPerformanceTracker.recordMountFollowUpdate(
                    LOTREntityMumakil.this,
                    pathCall
            );
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.followUpdatesSinceStart = 0;
        }
    }

    private class EntityAIMumakilHurtByTarget extends EntityAIHurtByTarget {
        private EntityAIMumakilHurtByTarget() {
            super(LOTREntityMumakil.this, false);
        }

        @Override
        public boolean shouldExecute() {
            return !(LOTREntityMumakil.this.isHiredWarMumakil()
                    && LOTREntityMumakil.this.hasLivingNPCCombatDriver())
                    && super.shouldExecute();
        }
    }

    private class EntityAIBlockHiredWarWander extends EntityAIBase {
        private EntityAIBlockHiredWarWander() {
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            return LOTREntityMumakil.this.hasActiveHiredWarCombatTarget();
        }

        @Override
        public boolean continueExecuting() {
            return LOTREntityMumakil.this.hasActiveHiredWarCombatTarget();
        }
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.applyConfiguredAttributes();
    }

    private void applyConfiguredAttributes() {
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(MOVEMENT_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(KNOCKBACK_RESISTANCE);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    protected void onLOTRHorseSpawn() {
        this.applyConfiguredAttributes();

        double jumpStrength = this.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).getAttributeValue();
        jumpStrength *= 0.5D;
        this.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).setBaseValue(jumpStrength);

        this.setHealth(this.getMaxHealth());
    }

    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.applyConfiguredAttributes();

        if (this.angerWaveCooldownTicks <= 0 && this.angerWaveActiveTicks <= 0) {
            this.resetAngerWaveCooldown();
        }
    }

    protected double clampChildHealth(double health) {
        return MathHelper.clamp_double(health, 100.0D, MAX_HEALTH);
    }

    protected double clampChildJump(double jump) {
        return MathHelper.clamp_double(jump, 0.2D, 0.8D);
    }

    protected double clampChildSpeed(double speed) {
        return MathHelper.clamp_double(speed, 0.18D, MOVEMENT_SPEED);
    }

    public boolean isBreedingItem(ItemStack itemstack) {
        return itemstack != null && itemstack.getItem() == Items.wheat;
    }


    // ---------------------------------------------------------------------
    // Main server tick update
    // ---------------------------------------------------------------------

    public void onLivingUpdate() {
        long superPerfStart = !this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()
                ? MumakilPerformanceTracker.startTimer()
                : 0L;

        try {
            super.onLivingUpdate();
        } finally {
            if (!this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.recordMountSuperLiving(
                        this,
                        System.nanoTime() - superPerfStart
                );
            }
        }
        this.stabilizeIdleYaw();

        this.prevMumakilStrikeAnimationTicks = this.mumakilStrikeAnimationTicks;

        if (this.mumakilStrikeAnimationTicks > 0) {
            --this.mumakilStrikeAnimationTicks;
        }

        if (!this.worldObj.isRemote) {
            this.updateMumakilHowdahSyncState();

            if (this.tuskAttackCooldownTicks > 0) {
                --this.tuskAttackCooldownTicks;
            }

            this.updateAngerWave();
            this.tryAcquireWildMobTarget();
            this.tryTuskReachAttack();
            if (!MumakilPerformanceTracker.DEBUG_DISABLE_MUMAKIL_TREE_CLEARING) {
                long treePerfStart = MumakilPerformanceTracker.startTimer();
                try {
                    this.clearAggroObstaclesForMovement();
                } finally {
                    if (MumakilPerformanceTracker.isEnabled()) {
                        MumakilPerformanceTracker.recordTreeScan(
                                this,
                                System.nanoTime() - treePerfStart
                        );
                    }
                }
            }
            this.updateChargeStompSound();
            this.applyTrampleDamage();

            if (this.riddenByEntity instanceof EntityLivingBase) {
                EntityLivingBase rider = (EntityLivingBase)this.riddenByEntity;
                float momentum = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
                this.setSprinting(momentum > 0.18F);

                if (momentum >= CHARGE_MIN_SPEED) {
                    float strength = Math.min((float)ATTACK_DAMAGE + momentum * 50.0F, MAX_CHARGE_DAMAGE);
                    Vec3 look = this.getLookVec();
                    List list = this.worldObj.getEntitiesWithinAABBExcludingEntity(
                            this,
                            this.boundingBox
                                    .addCoord(look.xCoord * 1.5D, 0.0D, look.zCoord * 1.5D)
                                    .expand(0.75D, 0.5D, 0.75D)
                    );
                    boolean hitAnyEntities = false;

                    for(int i = 0; i < list.size(); ++i) {
                        Entity obj = (Entity)list.get(i);
                        if (obj instanceof EntityLivingBase) {
                            EntityLivingBase entity = (EntityLivingBase)obj;
                            if (entity != rider
                                    && entity.riddenByEntity == null
                                    && (!(rider instanceof EntityPlayer) || LOTRMod.canPlayerAttackEntity((EntityPlayer)rider, entity, false))
                                    && (!(rider instanceof EntityCreature) || LOTRMod.canNPCAttackEntity((EntityCreature)rider, entity, false))) {
                                boolean flag = entity.attackEntityFrom(DamageSource.causeMobDamage(this), strength);
                                if (flag) {
                                    this.applyMumakilHeavyKnockback(entity, 2.0F, 0.55F);
                                    hitAnyEntities = true;
                                    if (entity instanceof EntityLiving) {
                                        EntityLiving entityliving = (EntityLiving)entity;
                                        if (entityliving.getAttackTarget() == this) {
                                            entityliving.getNavigator().clearPathEntity();
                                            entityliving.setAttackTarget(rider);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (hitAnyEntities) {
                        this.playMumakilHitSound();
                    }
                }
            } else if (this.getAttackTarget() != null) {
                float momentum = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
                this.setSprinting(momentum > 0.18F);
            } else {
                this.setSprinting(false);
            }

            if (MumakilPerformanceTracker.isEnabled()) {
                MumakilPerformanceTracker.reportIfDue(this);
            }
        }
    }


    // ---------------------------------------------------------------------
    // Direct melee, tusk attack, and projectile damage
    // ---------------------------------------------------------------------

    @Override
    public void setAttackTarget(EntityLivingBase target) {
        EntityLivingBase previousTarget = this.getAttackTarget();

        if (MumakilPerformanceTracker.isEnabled()
                && this.worldObj != null
                && !this.worldObj.isRemote
                && previousTarget != target) {
            MumakilPerformanceTracker.recordTargetChange(this);
        }

        super.setAttackTarget(target);
    }

    public boolean attackEntityAsMob(Entity target) {
        if (this.tuskAttackCooldownTicks > 0) {
            return false;
        }

        boolean attacked = super.attackEntityAsMob(target);
        if (attacked && !this.worldObj.isRemote) {
            this.tuskAttackCooldownTicks = TUSK_ATTACK_COOLDOWN_TICKS;
            this.startMumakilStrikeAnimation();
            this.applyMumakilHeavyKnockback(target, 1.5F, 0.45F);

            if (this.applyMumakilStrikeAOEDamage(target, TUSK_AOE_DAMAGE)) {
                this.playMumakilHitSound();
            }
        }

        return attacked;
    }

    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.shouldBlockOwnHowdahArcherArrow(source)) {
            return true;
        }

        boolean arrowDamage = this.isMumakilArrowDamage(source);
        boolean damaged = super.attackEntityFrom(source, amount);

        if (damaged && !this.worldObj.isRemote && arrowDamage) {
            this.hurtResistantTime = Math.min(this.hurtResistantTime, PROJECTILE_HURT_RESISTANT_TICKS);
        }

        if (damaged && !this.worldObj.isRemote && amount > 0.0F && !this.isDead) {
            this.playMumakilNormalHitSound();
        }

        if (!damaged && this.shouldConsumeBlockedMumakilArrow(source, amount)) {
            return true;
        }

        return damaged;
    }

    private boolean isMumakilArrowDamage(DamageSource source) {
        return source != null
                && (source.isProjectile() || source.getSourceOfDamage() instanceof EntityArrow);
    }

    private boolean shouldBlockOwnHowdahArcherArrow(DamageSource source) {
        return source != null
                && this.isHiredWarMumakil()
                && this.getMumakilDamageArrow(source) != null
                && this.isOwnAttachedHowdahArcher(this.getMumakilArrowShooter(source));
    }

    private EntityArrow getMumakilDamageArrow(DamageSource source) {
        Entity damageEntity = source.getSourceOfDamage();
        return damageEntity instanceof EntityArrow ? (EntityArrow)damageEntity : null;
    }

    private Entity getMumakilArrowShooter(DamageSource source) {
        Entity shooter = source.getEntity();
        if (shooter instanceof LOTREntityMumakilHowdahArcher) {
            return shooter;
        }

        EntityArrow arrow = this.getMumakilDamageArrow(source);
        return arrow == null ? null : arrow.shootingEntity;
    }

    private boolean isOwnAttachedHowdahArcher(Entity shooter) {
        if (!(shooter instanceof LOTREntityMumakilHowdahArcher)) {
            return false;
        }

        LOTREntityMumakilHowdahArcher archer = (LOTREntityMumakilHowdahArcher)shooter;
        if (!archer.isRuntimeHowdahPassenger()) {
            return false;
        }

        int mountEntityId = archer.getHowdahMountEntityId();
        if (mountEntityId != 0 && mountEntityId == this.getEntityId()) {
            return true;
        }

        String mountUuid = archer.getHowdahMountUuid();
        return mountUuid != null
                && mountUuid.length() > 0
                && mountUuid.equals(this.getPersistentID().toString());
    }

    private boolean shouldConsumeBlockedMumakilArrow(DamageSource source, float amount) {
        return this.isMumakilArrowDamage(source)
                && amount > 0.0F
                && !this.worldObj.isRemote
                && !this.isDead
                && this.getHealth() > 0.0F
                && !this.isEntityInvulnerable();
    }

    public void knockBack(Entity attacker, float strength, double xRatio, double zRatio) {
        // A Mumakil's mass lets damage land normally without letting ordinary hits shove the war beast around.
        super.knockBack(attacker, strength * 0.1F, xRatio, zRatio);
    }

    private void tryTuskReachAttack() {
        if (this.tuskAttackCooldownTicks > 0) {
            return;
        }

        EntityLivingBase target = this.getAttackTarget();
        if (target == null || !this.canTuskAttackTarget(target)) {
            return;
        }

        if (this.getDistanceSqToEntity(target) > TUSK_ATTACK_RANGE * TUSK_ATTACK_RANGE) {
            return;
        }

        if (!this.isTuskTargetInFront(target)) {
            return;
        }

        if (!this.canEntityBeSeen(target)) {
            return;
        }

        if (target.attackEntityFrom(DamageSource.causeMobDamage(this), (float)ATTACK_DAMAGE)) {
            this.tuskAttackCooldownTicks = TUSK_ATTACK_COOLDOWN_TICKS;
            this.startMumakilStrikeAnimation();
            this.applyMumakilHeavyKnockback(target, 1.75F, 0.5F);

            if (this.applyMumakilStrikeAOEDamage(target, TUSK_AOE_DAMAGE)) {
                this.playMumakilHitSound();
            }
        }
    }

    private boolean applyMumakilStrikeAOEDamage(Entity primaryTarget, float damage) {
        if (this.worldObj.isRemote || !(primaryTarget instanceof EntityLivingBase)) {
            return false;
        }

        EntityLivingBase impactTarget = (EntityLivingBase)primaryTarget;
        AxisAlignedBB aoeBox = impactTarget.boundingBox.expand(
                TUSK_AOE_RADIUS,
                TUSK_AOE_VERTICAL_RANGE,
                TUSK_AOE_RADIUS
        );

        List nearby = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, aoeBox);
        boolean hitAny = false;

        for (int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase target = (EntityLivingBase)nearby.get(i);

            if (target == primaryTarget || !this.canTuskAttackTarget(target)) {
                continue;
            }

            /*
             * Keep the splash roughly circular around the impact point.
             * The expanded AABB finds candidates cheaply, then this radius check
             * prevents weird square-corner hits.
             */
            double dx = target.posX - impactTarget.posX;
            double dz = target.posZ - impactTarget.posZ;
            if (dx * dx + dz * dz > TUSK_AOE_RADIUS * TUSK_AOE_RADIUS) {
                continue;
            }

            if (target.attackEntityFrom(DamageSource.causeMobDamage(this), damage)) {
                this.applyMumakilHeavyKnockback(
                        target,
                        TUSK_AOE_KNOCKBACK_HORIZONTAL,
                        TUSK_AOE_KNOCKBACK_VERTICAL
                );
                hitAny = true;
            }
        }

        return hitAny;
    }

    private boolean canTuskAttackTarget(EntityLivingBase target) {
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordTuskCandidateCheck(this);
        }

        if (target == this
                || target == this.riddenByEntity
                || target instanceof LOTREntityMumakil
                || !target.isEntityAlive()
                || target.riddenByEntity != null) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)target;
            if (player.capabilities.isCreativeMode || this.isOwner(player)) {
                return false;
            }
        }

        if (target instanceof EntityTameable && ((EntityTameable)target).isTamed()) {
            return false;
        }

        if (target instanceof EntityHorse && ((EntityHorse)target).isTame()) {
            return false;
        }

        if (target instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)target;
            if (npc.hiredNPCInfo.isActive) {
                return false;
            }
        }

        if (this.riddenByEntity instanceof EntityPlayer
                && !LOTRMod.canPlayerAttackEntity((EntityPlayer)this.riddenByEntity, target, false)) {
            return false;
        }

        if (this.riddenByEntity instanceof EntityCreature
                && !LOTRMod.canNPCAttackEntity((EntityCreature)this.riddenByEntity, target, false)) {
            return false;
        }

        return true;
    }

    private boolean isTuskTargetInFront(EntityLivingBase target) {
        double deltaX = target.posX - this.posX;
        double deltaZ = target.posZ - this.posZ;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalDistance <= TUSK_ATTACK_CLOSE_RANGE) {
            return true;
        }

        Vec3 look = this.getLookVec();
        double lookX = look.xCoord;
        double lookZ = look.zCoord;
        double lookLength = Math.sqrt(lookX * lookX + lookZ * lookZ);

        if (lookLength > 1.0E-4D) {
            lookX /= lookLength;
            lookZ /= lookLength;
        } else {
            lookX = (double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F));
            lookZ = (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F));
        }

        return (lookX * deltaX + lookZ * deltaZ) / horizontalDistance >= TUSK_ATTACK_FRONT_CONE_DOT;
    }

    private void applyMumakilHeavyKnockback(Entity target, float horizontalStrength, float verticalStrength) {
        if (!(target instanceof EntityLivingBase)) {
            return;
        }

        double deltaX = target.posX - this.posX;
        double deltaZ = target.posZ - this.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance > 1.0E-4D) {
            deltaX /= distance;
            deltaZ /= distance;
        } else {
            deltaX = (double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F));
            deltaZ = (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F));
        }

        target.addVelocity(deltaX * (double)horizontalStrength, (double)verticalStrength, deltaZ * (double)horizontalStrength);
        target.velocityChanged = true;
    }


    // ---------------------------------------------------------------------
    // Trample damage
    // ---------------------------------------------------------------------

    private void applyTrampleDamage() {
        if (this.worldObj.isRemote || this.ticksExisted % TRAMPLE_SCAN_INTERVAL != 0) {
            return;
        }

        if (this.ticksExisted % 20 == 0) {
            this.cleanupTrampleCooldowns();
        }

        boolean trampleActive = this.isWildMumakil()
                || this.isMountEnraged()
                || this.riddenByEntity != null;

        if (!trampleActive) {
            return;
        }

        float momentum = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        if (momentum < TRAMPLE_MIN_SPEED) {
            return;
        }

        boolean mountedChargeActive = this.riddenByEntity instanceof EntityLivingBase
                && momentum >= CHARGE_MIN_SPEED;
        if (mountedChargeActive) {
            return;
        }

        double directionX = this.motionX / (double)momentum;
        double directionZ = this.motionZ / (double)momentum;
        AxisAlignedBB trampleBox = this.boundingBox
                .expand(0.85D, 0.5D, 0.85D)
                .addCoord(directionX * 1.5D, -0.35D, directionZ * 1.5D);

        long perfStart = MumakilPerformanceTracker.startTimer();
        List nearby = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, trampleBox);
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordTrampleScan(this, nearby.size(), System.nanoTime() - perfStart);
        }

        boolean hitAnyEntities = false;

        for(int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase target = (EntityLivingBase)nearby.get(i);
            if (!this.canTrample(target)) {
                continue;
            }

            Integer cooldownEnd = this.trampleCooldowns.get(target.getEntityId());
            if (cooldownEnd != null && cooldownEnd > this.ticksExisted) {
                continue;
            }

            this.trampleCooldowns.put(target.getEntityId(), this.ticksExisted + TRAMPLE_COOLDOWN_TICKS);
            if (target.attackEntityFrom(DamageSource.causeMobDamage(this), TRAMPLE_DAMAGE)) {
                target.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 0));
                this.applyTrampleKnockback(target, directionX, directionZ);
                hitAnyEntities = true;
            }
        }

        if (hitAnyEntities) {
            this.playMumakilHitSound();
        }
    }

    private boolean canTrample(EntityLivingBase target) {
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordTrampleCandidateCheck(this);
        }

        if (target == this
                || target == this.riddenByEntity
                || target instanceof LOTREntityMumakil
                || target.riddenByEntity != null
                || !target.isEntityAlive()) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)target;
            if (player.capabilities.isCreativeMode || this.isOwner(player)) {
                return false;
            }
        }

        if (target instanceof EntityTameable && ((EntityTameable)target).isTamed()) {
            return false;
        }

        if (target instanceof EntityHorse && ((EntityHorse)target).isTame()) {
            return false;
        }

        if (target instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)target;
            if (npc.hiredNPCInfo.isActive) {
                return false;
            }

            LOTRFaction targetFaction = LOTRMod.getNPCFaction(npc);
            if (!LOTRFaction.NEAR_HARAD.isBadRelation(targetFaction)) {
                return false;
            }
        }

        if (this.riddenByEntity instanceof EntityPlayer
                && !LOTRMod.canPlayerAttackEntity((EntityPlayer)this.riddenByEntity, target, false)) {
            return false;
        }

        if (this.riddenByEntity instanceof EntityCreature
                && !LOTRMod.canNPCAttackEntity((EntityCreature)this.riddenByEntity, target, false)) {
            return false;
        }

        return true;
    }

    private boolean isOwner(EntityPlayer player) {
        if (!this.isTame()) {
            return false;
        }

        String ownerId = this.func_152119_ch();
        return ownerId != null
                && ownerId.length() > 0
                && ownerId.equals(player.getUniqueID().toString());
    }

    private void applyTrampleKnockback(EntityLivingBase target, double fallbackX, double fallbackZ) {
        double deltaX = target.posX - this.posX;
        double deltaZ = target.posZ - this.posZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (distance > 1.0E-4D) {
            deltaX /= distance;
            deltaZ /= distance;
        } else {
            deltaX = fallbackX;
            deltaZ = fallbackZ;
        }

        target.addVelocity(deltaX * 0.75D, 0.42D, deltaZ * 0.75D);
        target.velocityChanged = true;
    }

    private void cleanupTrampleCooldowns() {
        Iterator<Map.Entry<Integer, Integer>> iterator = this.trampleCooldowns.entrySet().iterator();

        while(iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            if (entry.getValue() <= this.ticksExisted) {
                iterator.remove();
            }
        }
    }


    // ---------------------------------------------------------------------
    // Tree / obstacle clearing
    // ---------------------------------------------------------------------

    private void clearAggroObstaclesForMovement() {
        if (this.worldObj.isRemote || this.ticksExisted % AGGRO_OBSTACLE_CLEAR_INTERVAL != 0) {
            return;
        }

        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordTreePass(this);
        }

        Vec3 look = this.getLookVec();
        double horizontalLookLength = Math.sqrt(look.xCoord * look.xCoord + look.zCoord * look.zCoord);
        double lookX;
        double lookZ;

        if (horizontalLookLength > 1.0E-4D) {
            lookX = look.xCoord / horizontalLookLength;
            lookZ = look.zCoord / horizontalLookLength;
        } else {
            lookX = -MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F);
            lookZ = MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F);
        }

        int remaining = MAX_OBSTACLES_PER_PASS;

        /*
         * Clear logs/leaves directly under and around the Mumakil.
         * This fixes the case where the Mumakil is standing on tree blocks.
         */
        AxisAlignedBB standingBox = AxisAlignedBB.getBoundingBox(
                this.posX - 3.5D,
                this.boundingBox.minY - 1.5D,
                this.posZ - 3.5D,
                this.posX + 3.5D,
                this.boundingBox.minY + 2.0D,
                this.posZ + 3.5D
        );

        remaining -= this.clearAggroObstaclesInBox(standingBox, remaining);

        if (remaining <= 0 || !this.shouldBreakForwardTrees()) {
            return;
        }

        /*
         * Clear around the body while moving.
         */
        AxisAlignedBB bodyBox = AxisAlignedBB.getBoundingBox(
                this.posX - 3.5D,
                this.boundingBox.minY + 0.25D,
                this.posZ - 3.5D,
                this.posX + 3.5D,
                this.boundingBox.maxY + 1.25D,
                this.posZ + 3.5D
        );

        /*
         * Clear in front of the head/tusks.
         */
        double headX = this.posX + lookX * 4.0D;
        double headZ = this.posZ + lookZ * 4.0D;
        AxisAlignedBB headAndTusksBox = AxisAlignedBB.getBoundingBox(
                headX - 2.25D,
                this.boundingBox.minY + 1.0D,
                headZ - 2.25D,
                headX + 2.25D,
                this.boundingBox.maxY + 1.75D,
                headZ + 2.25D
        );

        /*
         * Clear lower blocks farther forward for trunk/tusk area.
         */
        double trunkX = this.posX + lookX * 5.5D;
        double trunkZ = this.posZ + lookZ * 5.5D;
        AxisAlignedBB trunkBox = AxisAlignedBB.getBoundingBox(
                trunkX - 1.5D,
                this.boundingBox.minY + 0.25D,
                trunkZ - 1.5D,
                trunkX + 1.5D,
                this.boundingBox.maxY + 0.75D,
                trunkZ + 1.5D
        );

        remaining -= this.clearAggroObstaclesInBox(headAndTusksBox, remaining);

        if (remaining > 0) {
            remaining -= this.clearAggroObstaclesInBox(trunkBox, remaining);
        }

        if (remaining > 0) {
            this.clearAggroObstaclesInBox(bodyBox, remaining);
        }
    }

    private boolean shouldBreakForwardTrees() {
        if (this.riddenByEntity != null) {
            return false;
        }

        if (!this.isWildMumakil() && !this.isMountEnraged()) {
            return false;
        }

        if (this.getAttackTarget() == null || this.getNavigator().noPath()) {
            return false;
        }

        float momentum = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        return momentum >= CHARGE_MIN_SPEED;
    }

    private int clearAggroObstaclesInBox(AxisAlignedBB obstacleBox, int maximum) {
        int minX = MathHelper.floor_double(obstacleBox.minX);
        int maxX = MathHelper.floor_double(obstacleBox.maxX);
        int minY = MathHelper.floor_double(obstacleBox.minY);
        int maxY = MathHelper.floor_double(obstacleBox.maxY);
        int minZ = MathHelper.floor_double(obstacleBox.minZ);
        int maxZ = MathHelper.floor_double(obstacleBox.maxZ);
        int broken = 0;
        int checked = 0;

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (broken >= maximum) {
                        if (MumakilPerformanceTracker.isEnabled()) {
                            MumakilPerformanceTracker.recordTreeBlocksChecked(this, checked);
                        }
                        return broken;
                    }

                    ++checked;
                    if (!this.worldObj.blockExists(x, y, z)) {
                        continue;
                    }
                    Block block = this.worldObj.getBlock(x, y, z);
                    if (this.canBreakAggroObstacle(block, x, y, z)) {
                        this.breakAggroObstacleBlock(block, x, y, z);
                        ++broken;
                        if (MumakilPerformanceTracker.isEnabled()) {
                            MumakilPerformanceTracker.recordTreeBlocksDestroyed(this, 1);
                        }
                    }
                }
            }
        }

        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordTreeBlocksChecked(this, checked);
        }

        return broken;
    }

    private void breakAggroObstacleBlock(Block block, int x, int y, int z) {
        int metadata = this.worldObj.getBlockMetadata(x, y, z);

        /*
         * Drop normal block drops first.
         * Logs drop logs.
         * Leaves can drop saplings/apples according to their normal drop logic.
         */
        block.dropBlockAsItem(this.worldObj, x, y, z, metadata, 0);

        /*
         * Block break particles/sound.
         */
        this.worldObj.playAuxSFX(
                2001,
                x,
                y,
                z,
                Block.getIdFromBlock(block) + (metadata << 12)
        );

        this.worldObj.setBlockToAir(x, y, z);
    }

    private boolean canBreakAggroObstacle(Block block, int x, int y, int z) {
        return block.isLeaves(this.worldObj, x, y, z) || block.isWood(this.worldObj, x, y, z);
    }


    // ---------------------------------------------------------------------
    // Idle yaw stabilization and fallback-facing helpers
    // ---------------------------------------------------------------------

    private void stabilizeIdleYaw() {
        if (!this.isStationaryIdleForYawLock()) {
            this.rememberStableIdleYaw();
            return;
        }

        if (!this.hasStableIdleYaw) {
            this.rememberStableIdleYaw();
            return;
        }

        boolean corrected = false;
        float bodyDelta = MathHelper.wrapAngleTo180_float(this.renderYawOffset - this.lastStableIdleYaw);
        if (Math.abs(bodyDelta) > IDLE_YAW_SNAP_THRESHOLD) {
            this.renderYawOffset = this.clampYawStep(this.lastStableIdleYaw, this.renderYawOffset, IDLE_YAW_MAX_STEP);
            corrected = true;
        }

        float entityDelta = MathHelper.wrapAngleTo180_float(this.rotationYaw - this.lastStableIdleYaw);
        if (Math.abs(entityDelta) > IDLE_YAW_SNAP_THRESHOLD) {
            this.rotationYaw = this.clampYawStep(this.lastStableIdleYaw, this.rotationYaw, IDLE_YAW_MAX_STEP);
            corrected = true;
        }

        float headDelta = MathHelper.wrapAngleTo180_float(this.rotationYawHead - this.lastStableIdleHeadYaw);
        float headFromBody = MathHelper.wrapAngleTo180_float(this.rotationYawHead - this.renderYawOffset);
        if (Math.abs(headDelta) > IDLE_YAW_SNAP_THRESHOLD && Math.abs(headFromBody) > IDLE_HEAD_YAW_LIMIT) {
            this.rotationYawHead = this.renderYawOffset + MathHelper.clamp_float(headFromBody, -IDLE_HEAD_YAW_LIMIT, IDLE_HEAD_YAW_LIMIT);
            corrected = true;
        }

        if (corrected) {
            this.prevRotationYaw = this.rotationYaw;
            this.prevRenderYawOffset = this.renderYawOffset;
            this.prevRotationYawHead = this.rotationYawHead;
        }

        this.rememberStableIdleYaw();
    }

    private boolean isStationaryIdleForYawLock() {
        if (this.riddenByEntity != null
                || this.getAttackTarget() != null
                || this.isMountEnraged()
                || this.isSprinting()
                || !this.onGround
                || Math.abs(this.moveForward) > 0.01F
                || Math.abs(this.moveStrafing) > 0.01F) {
            return false;
        }

        double horizontalMotionSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
        return horizontalMotionSq <= IDLE_YAW_MOTION_THRESHOLD_SQ && this.getNavigator().noPath();
    }

    private void rememberStableIdleYaw() {
        this.lastStableIdleYaw = this.renderYawOffset;
        this.lastStableIdleHeadYaw = this.rotationYawHead;
        this.hasStableIdleYaw = true;
    }

    private float clampYawStep(float stableYaw, float candidateYaw, float maximumStep) {
        float delta = MathHelper.wrapAngleTo180_float(candidateYaw - stableYaw);
        return stableYaw + MathHelper.clamp_float(delta, -maximumStep, maximumStep);
    }

    private void faceWildMovePoint(double x, double z) {
        double deltaX = x - this.posX;
        double deltaZ = z - this.posZ;
        if (deltaX * deltaX + deltaZ * deltaZ < 1.0E-4D) {
            return;
        }

        float desiredYaw = (float)(Math.atan2(deltaZ, deltaX) * 180.0D / Math.PI) - 90.0F;
        this.rotationYaw = this.clampYawStep(this.rotationYaw, desiredYaw, WILD_FALLBACK_TURN_STEP);
        this.renderYawOffset = this.clampYawStep(this.renderYawOffset, this.rotationYaw, WILD_FALLBACK_TURN_STEP);
        this.rotationYawHead = this.renderYawOffset;
    }


    // ---------------------------------------------------------------------
    // Anger wave, sounds, strike animation, and drops
    // ---------------------------------------------------------------------

    private void updateAngerWave() {
        if (!this.isWildMumakil()) {
            this.angerWaveActiveTicks = 0;
            if (this.angerWaveCooldownTicks <= 0) {
                this.resetAngerWaveCooldown();
            }
            return;
        }

        if (this.angerWaveActiveTicks > 0) {
            --this.angerWaveActiveTicks;
            if (this.angerWaveActiveTicks <= 0) {
                this.resetAngerWaveCooldown();
            }
            return;
        }

        if (this.angerWaveCooldownTicks > 0) {
            --this.angerWaveCooldownTicks;
            return;
        }

        this.angerWaveActiveTicks = ANGER_WAVE_MIN_DURATION + this.rand.nextInt(ANGER_WAVE_RANDOM_DURATION);
        this.playMumakilAngrySound();
    }

    private void resetAngerWaveCooldown() {
        this.angerWaveCooldownTicks = ANGER_WAVE_MIN_COOLDOWN + this.rand.nextInt(ANGER_WAVE_RANDOM_COOLDOWN);
    }

    private boolean isWildAngerWaveActive() {
        return this.isWildMumakil() && this.angerWaveActiveTicks > 0;
    }

    private void updateChargeStompSound() {
        if (this.chargeStompSoundCooldown > 0) {
            --this.chargeStompSoundCooldown;
        }

        EntityLivingBase target = this.getAttackTarget();
        if (target == null || (!this.isWildMumakil() && !this.isMountEnraged())) {
            return;
        }

        if (this.chargeStompSoundCooldown > 0) {
            return;
        }

        float momentum = MathHelper.sqrt_double(this.motionX * this.motionX + this.motionZ * this.motionZ);
        if (momentum >= CHARGE_STOMP_SOUND_MIN_SPEED && !this.getNavigator().noPath()) {
            this.worldObj.playSoundAtEntity(
                    this,
                    "lotrmoremobs:mumakil.step",
                    1.8F,
                    0.45F + this.rand.nextFloat() * 0.1F
            );
            this.chargeStompSoundCooldown = CHARGE_STOMP_SOUND_MIN_COOLDOWN
                    + this.rand.nextInt(CHARGE_STOMP_SOUND_RANDOM_COOLDOWN);
        }
    }

    private boolean shouldPlayMumakilAngrySoundThisTrigger() {
        ++this.mumakilAngrySoundTriggerCounter;
        return this.mumakilAngrySoundTriggerCounter % 10 == 0;
    }

    private void playMumakilNormalHitSound() {
        this.worldObj.playSoundAtEntity(
                this,
                "game.neutral.hurt",
                0.9F,
                0.62F + this.rand.nextFloat() * 0.10F
        );
    }

    private void playMumakilAngrySound() {
        if (!this.shouldPlayMumakilAngrySoundThisTrigger()) {
            return;
        }

        this.worldObj.playSoundAtEntity(
                this,
                "lotrmoremobs:mumakil.angry",
                1.25F,
                0.62F + this.rand.nextFloat() * 0.10F
        );
    }

    private void playMumakilHitSound() {
        this.worldObj.playSoundAtEntity(
                this,
                "lotrmoremobs:mumakil.step",
                1.2F,
                0.75F + this.rand.nextFloat() * 0.15F
        );
    }

    protected void dropFewItems(boolean flag, int i) {
        this.dropItem(Main.mumakilTusk, 1);
        if (i > 0 && this.rand.nextInt(4) < i) {
            this.dropItem(Main.mumakilTusk, 1);
        }

        int shanks = 2 + this.rand.nextInt(4) + this.rand.nextInt(1 + i);

        for(int j = 0; j < shanks; ++j) {
            this.dropItem(Main.mumakilShank, 1);
        }
    }

    protected float getSoundPitch() {
        return 0.62F + this.rand.nextFloat() * 0.10F;
    }

    protected String getLivingSound() {
        return null;
    }

    protected String getHurtSound() {
        return this.shouldPlayMumakilAngrySoundThisTrigger() ? "lotrmoremobs:mumakil.angry" : null;
    }

    protected String getDeathSound() {
        return "lotrmoremobs:mumakil.death";
    }

    protected String getAngrySoundName() {
        return this.shouldPlayMumakilAngrySoundThisTrigger() ? "lotrmoremobs:mumakil.angry" : null;
    }

    private void startMumakilStrikeAnimation() {
        this.mumakilStrikeAnimationLeft = this.rand.nextBoolean();
        this.mumakilStrikeAnimationTicks = MUMAKIL_STRIKE_ANIMATION_TICKS;
        this.prevMumakilStrikeAnimationTicks = this.mumakilStrikeAnimationTicks;

        if (DEBUG_COMBAT_LOGS) {
            System.out.println("[LOTRMoreMobs] Starting Mumakil strike animation side="
                    + (this.mumakilStrikeAnimationLeft ? "left" : "right")
                    + " worldRemote=" + this.worldObj.isRemote
                    + " entityId=" + this.getEntityId());
        }

        this.swingItem();

        this.worldObj.playSoundAtEntity(
                this,
                "lotrmoremobs:mumakil.strike",
                1.2F,
                0.85F + this.rand.nextFloat() * 0.2F
        );

        if (!this.worldObj.isRemote) {
            this.worldObj.setEntityState(
                    this,
                    this.mumakilStrikeAnimationLeft ? MUMAKIL_STRIKE_LEFT_STATUS : MUMAKIL_STRIKE_RIGHT_STATUS
            );
        }
    }

    public void handleHealthUpdate(byte status) {
        if (status == MUMAKIL_STRIKE_LEFT_STATUS || status == MUMAKIL_STRIKE_RIGHT_STATUS) {
            this.mumakilStrikeAnimationLeft = status == MUMAKIL_STRIKE_LEFT_STATUS;
            this.mumakilStrikeAnimationTicks = MUMAKIL_STRIKE_ANIMATION_TICKS;
            this.prevMumakilStrikeAnimationTicks = this.mumakilStrikeAnimationTicks;

            if (DEBUG_COMBAT_LOGS) {
                System.out.println("[LOTRMoreMobs] Client received Mumakil strike animation side="
                        + (this.mumakilStrikeAnimationLeft ? "left" : "right")
                        + " entityId=" + this.getEntityId());
            }

            return;
        }

        super.handleHealthUpdate(status);
    }

    public float getMumakilStrikeAnimationProgress(float partialTicks) {
        if (this.mumakilStrikeAnimationTicks <= 0 && this.prevMumakilStrikeAnimationTicks <= 0) {
            return 0.0F;
        }

        float remaining = this.prevMumakilStrikeAnimationTicks
                + (this.mumakilStrikeAnimationTicks - this.prevMumakilStrikeAnimationTicks) * partialTicks;

        float progress = 1.0F - remaining / (float)MUMAKIL_STRIKE_ANIMATION_TICKS;

        if (progress < 0.0F) {
            return 0.0F;
        }

        if (progress > 1.0F) {
            return 1.0F;
        }

        return progress;
    }

    public boolean isMumakilStrikeAnimationLeft() {
        return this.mumakilStrikeAnimationLeft;
    }

}
