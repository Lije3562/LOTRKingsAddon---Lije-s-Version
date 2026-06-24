//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.enovak.lotrmoremobs.entity.animal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lotr.common.LOTRMod;
import lotr.common.LOTRReflection;
import lotr.common.entity.ai.LOTREntityAIAttackOnCollide;
import lotr.common.entity.animal.LOTREntityHorse;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import lotr.common.fac.LOTRFaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class LOTREntityMumakil extends LOTREntityHorse implements IAnimatable {
    private static final double MAX_HEALTH = 120.0D;
    private static final double MOVEMENT_SPEED = 0.30D;
    private static final double KNOCKBACK_RESISTANCE = 0.75D;
    private static final double ATTACK_DAMAGE = 16.0D;
    private static final double WILD_ATTACK_SPEED = 1.30D;
    private static final float CHARGE_MIN_SPEED = 0.24F;
    private static final float MAX_CHARGE_DAMAGE = 36.0F;
    private static final double TUSK_ATTACK_RANGE = 6.5D;
    private static final int TUSK_ATTACK_COOLDOWN_TICKS = 40;
    private static final double TUSK_ATTACK_FRONT_CONE_DOT = 0.3D;
    private static final double TUSK_ATTACK_CLOSE_RANGE = 2.5D;
    private static final int MOB_TARGET_CHECK_INTERVAL = 20;
    private static final double MOB_TARGET_RANGE = 18.0D;
    private static final double MOB_TARGET_VERTICAL_RANGE = 8.0D;
    private static final int ANGER_WAVE_MIN_DURATION = 60;
    private static final int ANGER_WAVE_RANDOM_DURATION = 61;
    private static final int ANGER_WAVE_MIN_COOLDOWN = 180;
    private static final int ANGER_WAVE_RANDOM_COOLDOWN = 81;
    private static final int AGGRO_OBSTACLE_CLEAR_INTERVAL = 2;
    private static final int MAX_OBSTACLES_PER_PASS = 96;
    private static final int TRAMPLE_SCAN_INTERVAL = 2;
    private static final int TRAMPLE_COOLDOWN_TICKS = 20;
    private static final float TRAMPLE_MIN_SPEED = 0.10F;
    private static final float TRAMPLE_DAMAGE = 8.0F;
    private static final float IDLE_YAW_SNAP_THRESHOLD = 45.0F;
    private static final float IDLE_YAW_MAX_STEP = 8.0F;
    private static final float IDLE_HEAD_YAW_LIMIT = 45.0F;
    private static final double IDLE_YAW_MOTION_THRESHOLD_SQ = 4.0E-4D;
    private static final float CHARGE_STOMP_SOUND_MIN_SPEED = 0.13F;
    private static final int CHARGE_STOMP_SOUND_MIN_COOLDOWN = 10;
    private static final int CHARGE_STOMP_SOUND_RANDOM_COOLDOWN = 7;

    private final Map<Integer, Integer> trampleCooldowns = new HashMap<Integer, Integer>();
    private final AnimationFactory animationFactory = new AnimationFactory(this);
    private float lastStableIdleYaw;
    private float lastStableIdleHeadYaw;
    private boolean hasStableIdleYaw;
    private int chargeStompSoundCooldown;
    private int angerWaveCooldownTicks;
    private int angerWaveActiveTicks;
    private int tuskAttackCooldownTicks;

    public LOTREntityMumakil(World world) {
        super(world);
        // Slightly narrower than the rendered body so melee attackers can path close enough to hit this giant target.
        this.setSize(3.75F, 6.25F);
        this.resetAngerWaveCooldown();
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

    @Override
    public void registerControllers(AnimationData data) {
    }

    @Override
    public AnimationFactory getFactory() {
        return this.animationFactory;
    }

    private boolean isWildMumakil() {
        return !this.isTame() && this.riddenByEntity == null;
    }

    public double getMountedYOffset() {
        return 6.75D;
    }

    protected boolean isMountHostile() {
        return true;
    }

    protected EntityAIBase createMountAttackAI() {
        return new LOTREntityAIAttackOnCollide(this, WILD_ATTACK_SPEED, true);
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

    public int getHorseType() {
        return 0;
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

    public void onLivingUpdate() {
        super.onLivingUpdate();
        this.stabilizeIdleYaw();
        if (!this.worldObj.isRemote) {
            if (this.tuskAttackCooldownTicks > 0) {
                --this.tuskAttackCooldownTicks;
            }

            this.updateAngerWave();
            this.tryAcquireWildMobTarget();
            this.tryTuskReachAttack();
            this.clearAggroObstaclesForMovement();
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
        }
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        if (this.tuskAttackCooldownTicks > 0) {
            return false;
        }

        boolean attacked = super.attackEntityAsMob(target);
        if (attacked && !this.worldObj.isRemote) {
            this.tuskAttackCooldownTicks = TUSK_ATTACK_COOLDOWN_TICKS;
            this.applyMumakilHeavyKnockback(target, 1.5F, 0.45F);
            this.playMumakilHitSound();
        }

        return attacked;
    }

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
    }

    private void resetAngerWaveCooldown() {
        this.angerWaveCooldownTicks = ANGER_WAVE_MIN_COOLDOWN + this.rand.nextInt(ANGER_WAVE_RANDOM_COOLDOWN);
    }

    private boolean isWildAngerWaveActive() {
        return this.isWildMumakil() && this.angerWaveActiveTicks > 0;
    }

    private void tryTuskReachAttack() {
        EntityLivingBase target = this.getAttackTarget();
        if (target == null
                || this.tuskAttackCooldownTicks > 0
                || !target.isEntityAlive()
                || this.getDistanceSqToEntity(target) > TUSK_ATTACK_RANGE * TUSK_ATTACK_RANGE
                || !this.canTuskAttackTarget(target)
                || !this.isTuskTargetInFront(target)) {
            return;
        }

        if (target.attackEntityFrom(DamageSource.causeMobDamage(this), (float)ATTACK_DAMAGE)) {
            this.tuskAttackCooldownTicks = TUSK_ATTACK_COOLDOWN_TICKS;
            this.applyMumakilHeavyKnockback(target, 1.75F, 0.5F);
            this.playMumakilHitSound();
        }
    }

    private boolean canTuskAttackTarget(EntityLivingBase target) {
        if (target == this
                || target == this.riddenByEntity
                || target instanceof LOTREntityMumakil
                || !target.isEntityAlive()
                || target.riddenByEntity != null
                || target.ridingEntity != null) {
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

    /**
     * LOTREntityHorse can inherit idle look updates that swing the whole body while the mount is
     * standing still. Keep those idle-only yaw changes from becoming elephant-sized snap-turns,
     * but leave movement, combat, riding, enraged movement, and active pathing untouched.
     */
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

    private void tryAcquireWildMobTarget() {
        if (!this.isWildMumakil()
                || !this.isWildAngerWaveActive()
                || this.getAttackTarget() != null
                || this.ticksExisted % MOB_TARGET_CHECK_INTERVAL != 0) {
            return;
        }

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

        if (bestTarget != null && (bestPriority < 2 || this.rand.nextInt(4) == 0)) {
            this.setAttackTarget(bestTarget);
        }
    }

    private boolean canTargetWildMob(EntityLivingBase target) {
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

        List nearby = this.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, trampleBox);
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
        if (target == this
                || target == this.riddenByEntity
                || target instanceof LOTREntityMumakil
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

    /**
     * Angry Mumakil should be able to shove through trees while pursuing a target. This deliberately
     * uses Forge's leaf/wood hooks instead of Material.wood, so planks, doors, fences, and chests are
     * not treated as casual obstacle clearing targets.
     */
    private void clearAggroObstaclesForMovement() {
        if (this.worldObj.isRemote || this.ticksExisted % AGGRO_OBSTACLE_CLEAR_INTERVAL != 0) {
            return;
        }

        boolean aggroed = this.getAttackTarget() != null || this.isMountEnraged();
        if (!aggroed) {
            return;
        }

        double horizontalSpeedSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
        boolean tryingToMove = horizontalSpeedSq > 0.0004D
                || this.isMountEnraged()
                || (this.getAttackTarget() != null && !this.getNavigator().noPath());

        if (!tryingToMove) {
            return;
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

        AxisAlignedBB bodyBox = AxisAlignedBB.getBoundingBox(
                this.posX - 2.25D,
                this.boundingBox.minY + 0.75D,
                this.posZ - 2.25D,
                this.posX + 2.25D,
                this.boundingBox.maxY + 1.25D,
                this.posZ + 2.25D
        );

        double headX = this.posX + lookX * 3.0D;
        double headZ = this.posZ + lookZ * 3.0D;
        AxisAlignedBB headAndTusksBox = AxisAlignedBB.getBoundingBox(
                headX - 1.75D,
                this.boundingBox.minY + 1.25D,
                headZ - 1.75D,
                headX + 1.75D,
                this.boundingBox.maxY + 1.75D,
                headZ + 1.75D
        );

        double trunkX = this.posX + lookX * 4.5D;
        double trunkZ = this.posZ + lookZ * 4.5D;
        AxisAlignedBB trunkBox = AxisAlignedBB.getBoundingBox(
                trunkX - 1.25D,
                this.boundingBox.minY + 0.25D,
                trunkZ - 1.25D,
                trunkX + 1.25D,
                this.boundingBox.maxY + 0.75D,
                trunkZ + 1.25D
        );

        int remaining = MAX_OBSTACLES_PER_PASS;
        remaining -= this.clearAggroObstaclesInBox(headAndTusksBox, remaining);

        if (remaining > 0) {
            remaining -= this.clearAggroObstaclesInBox(trunkBox, remaining);
        }

        if (remaining > 0) {
            this.clearAggroObstaclesInBox(bodyBox, remaining);
        }
    }

    private int clearAggroObstaclesInBox(AxisAlignedBB obstacleBox, int maximum) {
        int minX = MathHelper.floor_double(obstacleBox.minX);
        int maxX = MathHelper.floor_double(obstacleBox.maxX);
        int minY = MathHelper.floor_double(obstacleBox.minY);
        int maxY = MathHelper.floor_double(obstacleBox.maxY);
        int minZ = MathHelper.floor_double(obstacleBox.minZ);
        int maxZ = MathHelper.floor_double(obstacleBox.maxZ);
        int broken = 0;

        for(int x = minX; x <= maxX; ++x) {
            for(int y = minY; y <= maxY; ++y) {
                for(int z = minZ; z <= maxZ; ++z) {
                    if (broken >= maximum) {
                        return broken;
                    }

                    if (!this.worldObj.blockExists(x, y, z)) {
                        continue;
                    }

                    Block block = this.worldObj.getBlock(x, y, z);
                    if (this.canBreakAggroObstacle(block, x, y, z)) {
                        this.worldObj.setBlockToAir(x, y, z);
                        ++broken;
                    }
                }
            }
        }

        return broken;
    }

    private boolean canBreakAggroObstacle(Block block, int x, int y, int z) {
        return block.isLeaves(this.worldObj, x, y, z) || block.isWood(this.worldObj, x, y, z);
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
                    "mob.irongolem.walk",
                    1.8F,
                    0.45F + this.rand.nextFloat() * 0.1F
            );
            this.chargeStompSoundCooldown = CHARGE_STOMP_SOUND_MIN_COOLDOWN
                    + this.rand.nextInt(CHARGE_STOMP_SOUND_RANDOM_COOLDOWN);
        }
    }

    private void playMumakilHitSound() {
        this.worldObj.playSoundAtEntity(
                this,
                "lotr:troll.ologHai_hammer",
                0.9F,
                0.85F + this.rand.nextFloat() * 0.2F
        );
    }

    protected void dropFewItems(boolean flag, int i) {
        int j = this.rand.nextInt(2) + this.rand.nextInt(1 + i);

        for(int k = 0; k < j; ++k) {
            this.dropItem(LOTRMod.rhinoHorn, 1);
        }

        int meat = this.rand.nextInt(3) + this.rand.nextInt(1 + i);

        for(int l = 0; l < meat; ++l) {
            if (this.isBurning()) {
                this.dropItem(LOTRMod.rhinoCooked, 1);
            } else {
                this.dropItem(LOTRMod.rhinoRaw, 1);
            }
        }
    }

    protected String getLivingSound() {
        super.getLivingSound();
        return "lotr:rhino.say";
    }

    protected String getHurtSound() {
        super.getHurtSound();
        return "lotr:rhino.hurt";
    }

    protected String getDeathSound() {
        super.getDeathSound();
        return "lotr:rhino.death";
    }

    protected String getAngrySoundName() {
        super.getAngrySoundName();
        return "lotr:rhino.say";
    }
}
