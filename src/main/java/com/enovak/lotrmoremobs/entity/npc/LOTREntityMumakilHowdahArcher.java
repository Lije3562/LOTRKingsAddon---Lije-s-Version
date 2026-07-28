package com.enovak.lotrmoremobs.entity.npc;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.handler.MumakilHowdahArcherEventHandler;
import com.enovak.lotrmoremobs.spawning.MumakilWarFormationFactory;
import com.enovak.lotrmoremobs.spawning.MumakilWarFormationSpawnRegistry;
import com.enovak.lotrmoremobs.spawning.MumakilInvasionFormationRegistry;
import com.enovak.lotrmoremobs.util.MumakilPerformanceTracker;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNearHaradrimArcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * Visual/passive howdah passenger for the custom hired Mumakil.
 *
 * This is intentionally a subclass of the normal Near Haradrim archer so it can
 * inherit normal LOTR NPC behavior/data, but it disables normal gravity, pathing,
 * knockback, and combat while attached to its Mumakil.
 */
public class LOTREntityMumakilHowdahArcher extends LOTREntityNearHaradrimArcher implements IEntityAdditionalSpawnData {
    private static final double HOWDAH_ARCHER_MAX_HEALTH = 24.0D;
    private static final String NBT_MOUNT_ID = "LOTRMoreMobsHowdahMountId";
    private static final String NBT_MOUNT_UUID = "LOTRMoreMobsHowdahMountUuid";
    private static final String NBT_SLOT = "LOTRMoreMobsHowdahArcherSlot";
    private static final String LEGACY_NBT_SLOT = "LOTRMoreMobsHowdahSlot";
    private static final int MOUNT_LOOKUP_GRACE_TICKS = 100;
    private static final int UUID_LOOKUP_INTERVAL = 20;
    private static final int DETACHED_DESPAWN_TICKS = 600;
    private static final double PREVIOUS_PLACEMENT_SNAP_DISTANCE_SQ = 256.0D;
    private static final String MUMAKIL_SHARED_TARGET_ID_KEY = "LOTRMoreMobsHowdahArcherTargetId";
    private static final double HOWDAH_ARCHER_TRACK_RANGE = 38.0D;
    private static final double HOWDAH_ARCHER_SHOOT_RANGE = 34.0D;
    private static final double HOWDAH_ARCHER_TRACK_RANGE_SQ =
            HOWDAH_ARCHER_TRACK_RANGE * HOWDAH_ARCHER_TRACK_RANGE;
    private static final double HOWDAH_ARCHER_SHOOT_RANGE_SQ =
            HOWDAH_ARCHER_SHOOT_RANGE * HOWDAH_ARCHER_SHOOT_RANGE;
    private static final int PRIMARY_SHOOT_COOLDOWN_MIN = 70;
    private static final int PRIMARY_SHOOT_COOLDOWN_RANDOM = 70;
    private static final int HIGH_PERCH_SHOOT_COOLDOWN_MIN = 110;
    private static final int HIGH_PERCH_SHOOT_COOLDOWN_RANDOM = 90;

    private int howdahMountEntityId;
    private int howdahSlot;
    private String howdahMountUuid = "";
    private int missingMountTicks;
    private int detachedTicks;
    private int howdahShootCooldown;
    private int assignedHowdahTargetEntityId;
    private int howdahIdleLookTicks;
    private float howdahIdleYawOffset;
    private float howdahLookYaw;
    private float howdahLookPitch;
    private boolean runtimeHowdahPassenger;
    private boolean passengerAICleared;
    private boolean detachedFromDeadMumakil;
    private boolean hasHowdahLookRotation;
    private boolean howdahAttachmentDataLoaded;
    private boolean naturalFormationBootstrapPending = true;
    private boolean naturalFormationConquestSpawn;
    private boolean naturalFormationChanceEvaluated;
    private boolean naturalFormationChancePassed;

    public LOTREntityMumakilHowdahArcher(World world) {
        super(world);
        this.clearPassengerAI();
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HOWDAH_ARCHER_MAX_HEALTH);
    }

    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        data = super.onSpawnWithEgg(data);
        this.ensureNearHaradBowEquipped();
        /*
         * LOTR invokes this after the native bootstrap entity has joined the
         * world. Complete the one-member spawn entry synchronously so the full
         * formation is visible to later spawn-cap checks in the same pass.
         * Manually created attached archers call this before joining the world,
         * so they cannot accidentally bootstrap another formation.
         */
        if (this.isNaturalFormationBootstrapPending()
                && this.worldObj != null
                && !this.worldObj.isRemote
                && this.worldObj.loadedEntityList.contains(this)) {
            this.consumeNaturalFormationBootstrap();
            boolean formed = this.isInvasionSpawned()
                    ? MumakilWarFormationFactory
                    .createInvasionFormation(this)
                    : MumakilWarFormationFactory
                    .createNaturalFormation(
                            this,
                            this.wasNaturalFormationConquestSpawn()
                    );
            if (!formed) {
                this.setDead();
            }
        }
        return data;
    }

    @Override
    public void setConquestSpawning(boolean conquestSpawn) {
        super.setConquestSpawning(conquestSpawn);
        if (conquestSpawn && this.naturalFormationBootstrapPending) {
            this.naturalFormationConquestSpawn = true;
        }
    }

    @Override
    public boolean getCanSpawnHere() {
        if (this.isNaturalFormationBootstrapPending()
                && (this.isInvasionSpawned()
                || this.liftSpawnRestrictions)
                && MumakilInvasionFormationRegistry
                .getEligibleSpawner(this) == null) {
            return false;
        }
        if (this.isNaturalFormationBootstrapPending()
                && !this.isInvasionSpawned()
                && !this.liftSpawnRestrictions
                && this.wasNaturalFormationConquestSpawn()
                && !this.evaluateConquestFormationChanceOnce()) {
            return false;
        }
        if (this.isNaturalFormationBootstrapPending()
                && !this.isInvasionSpawned()
                && !this.liftSpawnRestrictions
                && !this.wasNaturalFormationConquestSpawn()
                && !this.evaluateHomeFormationChanceOnce()) {
            return false;
        }
        if (this.naturalFormationBootstrapPending
                && (this.worldObj == null
                || this.worldObj.isRemote
                || !MumakilWarFormationFactory
                .hasNaturalFormationSpawnCapacity(
                        this.worldObj,
                        this
                )
                || !MumakilWarFormationFactory.canNaturalFormationSpawnAt(
                        this.worldObj,
                        this,
                        this.posX,
                        this.posY,
                        this.posZ
                ))) {
            return false;
        }
        return super.getCanSpawnHere();
    }

    private boolean evaluateConquestFormationChanceOnce() {
        if (!this.naturalFormationChanceEvaluated) {
            this.naturalFormationChancePassed =
                    MumakilWarFormationSpawnRegistry
                            .passesConquestBootstrapChance(this);
            this.naturalFormationChanceEvaluated = true;
        }
        return this.naturalFormationChancePassed;
    }

    private boolean evaluateHomeFormationChanceOnce() {
        if (!this.naturalFormationChanceEvaluated) {
            this.naturalFormationChancePassed =
                    MumakilWarFormationSpawnRegistry
                            .passesHomeBootstrapChance(this);
            this.naturalFormationChanceEvaluated = true;
        }
        return this.naturalFormationChancePassed;
    }

    public boolean isNaturalFormationBootstrapPending() {
        return this.naturalFormationBootstrapPending
                && !this.isRuntimeHowdahPassenger()
                && this.getHowdahMountEntityId() == 0;
    }

    public boolean wasNaturalFormationConquestSpawn() {
        return this.naturalFormationConquestSpawn;
    }

    public void consumeNaturalFormationBootstrap() {
        this.naturalFormationBootstrapPending = false;
    }

    public void setHowdahAttachment(LOTREntityMumakil mumakil, int slot) {
        if (mumakil != null) {
            this.naturalFormationBootstrapPending = false;
        }
        this.howdahMountEntityId = mumakil == null ? 0 : mumakil.getEntityId();
        this.howdahMountUuid = mumakil == null ? "" : getEntityPersistentIdString(mumakil);
        this.howdahSlot = slot;
        this.howdahAttachmentDataLoaded = true;
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setString(NBT_MOUNT_UUID, this.howdahMountUuid);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);

        /*
         * Important: position before world.spawnEntityInWorld(). If the entity is
         * still at the default 0,0,0 position, Minecraft may reject the spawn
         * because that chunk is not loaded.
         */
        if (mumakil != null) {
            this.placeOnHowdah(mumakil, slot);
        }
    }

    public int getHowdahMountEntityId() {
        this.ensureHowdahAttachmentDataLoaded();
        return this.howdahMountEntityId;
    }

    public int getHowdahSlot() {
        this.ensureHowdahAttachmentDataLoaded();
        return this.howdahSlot;
    }

    public String getHowdahMountUuid() {
        this.ensureHowdahAttachmentDataLoaded();
        return this.howdahMountUuid == null ? "" : this.howdahMountUuid;
    }

    private void ensureHowdahAttachmentDataLoaded() {
        if (this.howdahAttachmentDataLoaded) {
            return;
        }

        NBTTagCompound data = this.getEntityData();
        this.howdahMountEntityId = data.getInteger(NBT_MOUNT_ID);
        this.howdahMountUuid = data.hasKey(NBT_MOUNT_UUID)
                ? data.getString(NBT_MOUNT_UUID)
                : "";
        if (data.hasKey(NBT_SLOT)) {
            this.howdahSlot = data.getInteger(NBT_SLOT);
        } else if (data.hasKey(LEGACY_NBT_SLOT)) {
            this.howdahSlot = data.getInteger(LEGACY_NBT_SLOT);
            data.setInteger(NBT_SLOT, this.howdahSlot);
        }
        this.howdahAttachmentDataLoaded = true;
    }

    public void setRuntimeHowdahPassenger(boolean runtimeHowdahPassenger) {
        this.runtimeHowdahPassenger = runtimeHowdahPassenger;
        if (runtimeHowdahPassenger) {
            this.naturalFormationBootstrapPending = false;
            this.detachedFromDeadMumakil = false;
            this.detachedTicks = 0;
            this.primeHowdahShootCooldown();
            this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(HOWDAH_ARCHER_MAX_HEALTH);
            this.setHealth(this.getMaxHealth());
            this.clearPassengerAI();
            this.ensureNearHaradBowEquipped();
            this.noClip = true;
        }
    }

    public boolean isRuntimeHowdahPassenger() {
        return this.runtimeHowdahPassenger;
    }

    public boolean isDetachedFromDeadMumakil() {
        return this.detachedFromDeadMumakil;
    }

    public void detachFromHowdahForMumakilDeath(LOTREntityMumakil mumakil) {
        if (this.detachedFromDeadMumakil) {
            return;
        }

        if (this.riddenByEntity != null) {
            this.riddenByEntity.mountEntity(null);
        }
        if (this.ridingEntity != null) {
            this.mountEntity(null);
        }
        this.setPosition(this.posX, this.posY, this.posZ);
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.lastTickPosX = this.posX;
        this.lastTickPosY = this.posY;
        this.lastTickPosZ = this.posZ;

        /*
         * Preserve the exact howdah-slot X/Z. Gravity supplies the first
         * downward velocity on the replacement's normal living tick.
         */
        double detachMotionX = 0.0D;
        double detachMotionY = 0.0D;
        double detachMotionZ = 0.0D;

        if (this.replaceWithNormalNearHaradrimArcherForDeath(detachMotionX, detachMotionY, detachMotionZ)) {
            return;
        }

        this.clearPassengerAI();
        this.runtimeHowdahPassenger = false;
        this.detachedFromDeadMumakil = true;
        this.detachedTicks = 0;
        this.noClip = false;
        this.onGround = false;
        this.isAirBorne = true;
        this.fallDistance = 0.0F;
        this.motionX = detachMotionX;
        this.motionY = detachMotionY;
        this.motionZ = detachMotionZ;
        this.velocityChanged = true;
        this.clearHowdahAttachment();
    }

    private boolean replaceWithNormalNearHaradrimArcherForDeath(double motionX, double motionY, double motionZ) {
        if (this.worldObj == null || this.worldObj.isRemote) {
            return false;
        }

        LOTREntityNearHaradrimArcher replacement = new LOTREntityNearHaradrimArcher(this.worldObj);
        replacement.onSpawnWithEgg(null);
        replacement.isNPCPersistent = this.isNPCPersistent;
        replacement.setShouldTraderRespawn(false);
        replacement.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        replacement.prevPosX = replacement.posX;
        replacement.prevPosY = replacement.posY;
        replacement.prevPosZ = replacement.posZ;
        replacement.lastTickPosX = replacement.posX;
        replacement.lastTickPosY = replacement.posY;
        replacement.lastTickPosZ = replacement.posZ;
        replacement.prevRotationYaw = this.prevRotationYaw;
        replacement.prevRotationPitch = this.prevRotationPitch;
        replacement.renderYawOffset = this.renderYawOffset;
        replacement.prevRenderYawOffset = this.prevRenderYawOffset;
        replacement.rotationYawHead = this.rotationYawHead;
        replacement.prevRotationYawHead = this.prevRotationYawHead;
        replacement.motionX = motionX;
        replacement.motionY = motionY;
        replacement.motionZ = motionZ;
        replacement.noClip = false;
        replacement.onGround = false;
        replacement.isAirBorne = true;
        replacement.fallDistance = 0.0F;
        replacement.velocityChanged = true;
        replacement.setHealth(MathHelper.clamp_float(this.getHealth(), 1.0F, replacement.getMaxHealth()));
        this.copyEquipmentToReplacement(replacement);
        equipNearHaradBow(replacement);
        if (this.isInvasionSpawned()) {
            replacement.setInvasionID(this.getInvasionID());
            replacement.killBonusFactions.addAll(
                    this.killBonusFactions
            );
            replacement.getEntityData().setInteger(
                    MumakilInvasionFormationRegistry
                            .INVASION_MEMBER_WEIGHT_KEY,
                    this.getEntityData().getInteger(
                            MumakilInvasionFormationRegistry
                                    .INVASION_MEMBER_WEIGHT_KEY
                    )
            );
        }

        MumakilHowdahArcherEventHandler
                .markDetachedArcherLandingCorrection(replacement);
        if (!this.worldObj.spawnEntityInWorld(replacement)) {
            return false;
        }

        this.setDead();
        return true;
    }

    private void copyEquipmentToReplacement(LOTREntityNearHaradrimArcher replacement) {
        for (int slot = 1; slot < 5; ++slot) {
            ItemStack equipment = this.getEquipmentInSlot(slot);
            if (equipment != null) {
                replacement.setCurrentItemOrArmor(slot, equipment.copy());
            }
        }
    }

    @Override
    public void onUpdate() {
        boolean trackPerformance = !this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled();
        long perfStart = trackPerformance ? MumakilPerformanceTracker.startTimer() : 0L;
        LOTREntityMumakil perfMumakil = trackPerformance ? this.getAttachedMumakilForPerformance() : null;

        try {
            if (this.isRuntimeHowdahPassenger()) {
                /*
                 * Runtime passengers only need the base Entity tick for age, previous
                 * position, fire/portal bookkeeping, and tracker stability. Calling the
                 * full LOTR NPC/Living update here runs combat, inventory, senses,
                 * navigation, and AI work that these visual helpers never use.
                 */
                this.onEntityUpdate();
                this.updateHowdahPassengerAttachment();
                return;
            }

            super.onUpdate();

            if (this.detachedFromDeadMumakil && !this.worldObj.isRemote && ++this.detachedTicks > DETACHED_DESPAWN_TICKS) {
                this.setDead();
            }
        } finally {
            if (trackPerformance) {
                if (perfMumakil == null) {
                    perfMumakil = this.getAttachedMumakilForPerformance();
                }
                if (perfMumakil != null) {
                    MumakilPerformanceTracker.recordArcherFullLiving(
                            perfMumakil,
                            System.nanoTime() - perfStart
                    );
                }
            }
        }
    }

    @Override
    public void onLivingUpdate() {
        if (this.isRuntimeHowdahPassenger()) {
            this.updateHowdahPassengerAttachment();
            return;
        }

        if (this.detachedFromDeadMumakil) {
            this.runSuperOnLivingUpdate();
            return;
        }

        this.clearPassengerAI();
        this.runSuperOnLivingUpdate();
        this.updateHowdahPassengerAttachment();
    }

    private void runSuperOnLivingUpdate() {
        boolean trackPerformance = !this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled();
        LOTREntityMumakil perfMumakil = trackPerformance ? this.getAttachedMumakilForPerformance() : null;
        long perfStart = perfMumakil != null ? MumakilPerformanceTracker.startTimer() : 0L;

        try {
            super.onLivingUpdate();
        } finally {
            if (perfMumakil != null) {
                MumakilPerformanceTracker.recordArcherSuperLiving(
                        perfMumakil,
                        System.nanoTime() - perfStart
                );
            }
        }
    }

    private LOTREntityMumakil getAttachedMumakilForPerformance() {
        int mountId = this.getHowdahMountEntityId();
        Entity entity = mountId == 0 || this.worldObj == null ? null : this.worldObj.getEntityByID(mountId);
        return entity instanceof LOTREntityMumakil ? (LOTREntityMumakil)entity : null;
    }

    private void updateHowdahPassengerAttachment() {
        if (this.detachedFromDeadMumakil) {
            return;
        }

        LOTREntityMumakil mumakil = this.getAttachedMumakil();

        if (mumakil == null) {
            this.handleMissingMount();
            return;
        }

        this.missingMountTicks = 0;

        if (!mumakil.isEntityAlive()) {
            this.detachFromHowdahForMumakilDeath(mumakil);
            return;
        }

        if (!mumakil.hasMumakilHowdahEquipped()) {
            this.stopPassengerMotion();

            if (!this.worldObj.isRemote) {
                this.setDead();
            }

            return;
        }

        this.clearPassengerAI();
        this.placeOnHowdah(mumakil, this.getHowdahSlot());
        this.updateHowdahCombatBehavior(mumakil);
    }

    private LOTREntityMumakil getAttachedMumakil() {
        int mountId = this.getHowdahMountEntityId();
        Entity entity = mountId == 0 || this.worldObj == null ? null : this.worldObj.getEntityByID(mountId);
        if (entity instanceof LOTREntityMumakil) {
            return (LOTREntityMumakil)entity;
        }

        return this.findAttachedMumakilByUuid();
    }

    public LOTREntityMumakil getAttachedMumakilForFormationCredit() {
        return this.getAttachedMumakil();
    }

    private LOTREntityMumakil findAttachedMumakilByUuid() {
        String mountUuid = this.getHowdahMountUuid();
        if (this.worldObj == null || mountUuid.length() == 0) {
            return null;
        }

        if (this.missingMountTicks > 0 && this.ticksExisted % UUID_LOOKUP_INTERVAL != 0) {
            return null;
        }

        List loaded = this.worldObj.loadedEntityList;
        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof LOTREntityMumakil)) {
                continue;
            }

            LOTREntityMumakil mumakil = (LOTREntityMumakil)object;
            if (mountUuid.equals(getEntityPersistentIdString(mumakil))) {
                this.howdahMountEntityId = mumakil.getEntityId();
                this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
                return mumakil;
            }
        }

        return null;
    }

    private void handleMissingMount() {
        this.stopPassengerMotion();
        ++this.missingMountTicks;

        if (!this.worldObj.isRemote && this.missingMountTicks >= MOUNT_LOOKUP_GRACE_TICKS) {
            this.setDead();
        }
    }

    private void stopPassengerMotion() {
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.resetHowdahLocomotionFields();
        this.fallDistance = 0.0F;
        this.onGround = true;
        this.isAirBorne = false;
    }

    private void resetHowdahLocomotionFields() {
        this.prevLimbSwingAmount = 0.0F;
        this.limbSwingAmount = 0.0F;
        this.limbSwing = 0.0F;
        this.prevDistanceWalkedModified = 0.0F;
        this.distanceWalkedModified = 0.0F;
        this.moveForward = 0.0F;
        this.moveStrafing = 0.0F;
        this.setSprinting(false);
    }

    /**
     * Damage/knockback packets can update vanilla walk fields after attachment
     * placement. The custom renderer calls this once more immediately before
     * the NPC model consumes them.
     */
    public void resetAttachedLocomotionAnimation() {
        if (this.isFixedHowdahPassenger()) {
            this.resetHowdahLocomotionFields();
        }
    }

    private void placeOnHowdah(LOTREntityMumakil mumakil, int rawSlot) {
        int slot = MathHelper.clamp_int(rawSlot, 0, HOWDAH_ARCHER_OFFSETS.length - 1);
        double[] offset = HOWDAH_ARCHER_OFFSETS[slot];
        double forwardOffset = offset[0];
        double sideOffset = offset[1];
        double verticalOffset = offset[2];
        float currentPlacementYaw = mumakil.renderYawOffset;
        float previousPlacementYaw = mumakil.prevRenderYawOffset;
        boolean snapPrevious = this.shouldSnapPreviousPlacementToCurrent(mumakil);

        if (snapPrevious) {
            previousPlacementYaw = currentPlacementYaw;
        }

        float currentYawRadians = currentPlacementYaw * 3.1415927F / 180.0F;
        float previousYawRadians = previousPlacementYaw * 3.1415927F / 180.0F;

        double currentForwardX = -MathHelper.sin(currentYawRadians) * forwardOffset;
        double currentForwardZ = MathHelper.cos(currentYawRadians) * forwardOffset;
        double currentSideX = MathHelper.cos(currentYawRadians) * sideOffset;
        double currentSideZ = MathHelper.sin(currentYawRadians) * sideOffset;
        double previousForwardX = -MathHelper.sin(previousYawRadians) * forwardOffset;
        double previousForwardZ = MathHelper.cos(previousYawRadians) * forwardOffset;
        double previousSideX = MathHelper.cos(previousYawRadians) * sideOffset;
        double previousSideZ = MathHelper.sin(previousYawRadians) * sideOffset;

        double currentX = mumakil.posX + currentForwardX + currentSideX;
        double currentY = mumakil.posY + verticalOffset;
        double currentZ = mumakil.posZ + currentForwardZ + currentSideZ;
        double previousX = (snapPrevious ? mumakil.posX : mumakil.prevPosX) + previousForwardX + previousSideX;
        double previousY = (snapPrevious ? mumakil.posY : mumakil.prevPosY) + verticalOffset;
        double previousZ = (snapPrevious ? mumakil.posZ : mumakil.prevPosZ) + previousForwardZ + previousSideZ;
        float archerYaw = MathHelper.wrapAngleTo180_float(currentPlacementYaw + (float)offset[3]);
        float previousArcherYaw = normalizePreviousYaw(
                MathHelper.wrapAngleTo180_float(previousPlacementYaw + (float)offset[3]),
                archerYaw
        );

        this.setPosition(currentX, currentY, currentZ);
        this.prevPosX = previousX;
        this.prevPosY = previousY;
        this.prevPosZ = previousZ;
        this.lastTickPosX = previousX;
        this.lastTickPosY = previousY;
        this.lastTickPosZ = previousZ;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.fallDistance = 0.0F;
        this.onGround = true;
        this.isAirBorne = false;
        this.noClip = true;
        this.isCollided = false;
        this.isCollidedHorizontally = false;
        this.isCollidedVertically = false;

        this.rotationYaw = archerYaw;
        this.prevRotationYaw = previousArcherYaw;
        this.rotationPitch = 0.0F;
        this.prevRotationPitch = 0.0F;
        this.renderYawOffset = archerYaw;
        this.prevRenderYawOffset = previousArcherYaw;
        this.rotationYawHead = archerYaw;
        this.prevRotationYawHead = previousArcherYaw;
        this.resetHowdahLocomotionFields();
    }

    /**
     * Places a non-spawned client preview archer with the same immutable slot
     * table and transform used by live howdah passengers.
     */
    public boolean placeOnHowdahForHirePreview(LOTREntityMumakil mumakil, int slot) {
        if (this.worldObj == null
                || !this.worldObj.isRemote
                || mumakil == null
                || !mumakil.isMumakilHirePreview()
                || slot < 0
                || slot >= HOWDAH_ARCHER_OFFSETS.length) {
            return false;
        }

        this.placeOnHowdah(mumakil, slot);
        return true;
    }

    private boolean shouldSnapPreviousPlacementToCurrent(LOTREntityMumakil mumakil) {
        if (this.ticksExisted <= 1 || mumakil.ticksExisted <= 1) {
            return true;
        }

        double dx = mumakil.posX - mumakil.prevPosX;
        double dy = mumakil.posY - mumakil.prevPosY;
        double dz = mumakil.posZ - mumakil.prevPosZ;
        return dx * dx + dy * dy + dz * dz > PREVIOUS_PLACEMENT_SNAP_DISTANCE_SQ;
    }

    private static float normalizePreviousYaw(float previousYaw, float currentYaw) {
        return currentYaw - MathHelper.wrapAngleTo180_float(currentYaw - previousYaw);
    }

    private boolean isFixedHowdahPassenger() {
        return this.isRuntimeHowdahPassenger()
                && !this.detachedFromDeadMumakil
                && this.getHowdahMountEntityId() != 0;
    }

    private void clearHowdahAttachment() {
        this.howdahMountEntityId = 0;
        this.howdahMountUuid = "";
        this.howdahAttachmentDataLoaded = true;
        this.hasHowdahLookRotation = false;
        this.getEntityData().setInteger(NBT_MOUNT_ID, 0);
        this.getEntityData().setString(NBT_MOUNT_UUID, "");
    }

    private void updateHowdahCombatBehavior(LOTREntityMumakil mumakil) {
        boolean trackPerformance =
                !this.worldObj.isRemote
                        && MumakilPerformanceTracker.isEnabled();
        long perfStart = trackPerformance
                ? MumakilPerformanceTracker.startTimer()
                : 0L;
        int perfWorkUnits = 1;
        boolean perfHasTarget = false;

        try {
            if (MumakilPerformanceTracker.DEBUG_DISABLE_HOWDAH_ARCHER_COMBAT) {
                this.updateHowdahIdleLook();
                this.tickHowdahShootCooldown();
                return;
            }

            EntityLivingBase target = this.getAssignedHowdahTarget();
            double targetDistanceSq = target == null
                    ? Double.MAX_VALUE
                    : this.getDistanceSqToEntity(target);
            perfHasTarget = this.canHowdahArcherTrackTarget(
                    mumakil,
                    target,
                    targetDistanceSq
            );

            if (perfHasTarget) {
                ++perfWorkUnits;
                this.updateHowdahLookBehavior(target);
                this.updateHowdahShooting(
                        mumakil,
                        target,
                        targetDistanceSq
                );
                return;
            }

            this.updateHowdahIdleLook();
            this.tickHowdahShootCooldown();
        } finally {
            if (trackPerformance) {
                MumakilPerformanceTracker.recordArcherUpdate(
                        mumakil,
                        this.getHowdahSlot(),
                        perfHasTarget,
                        perfWorkUnits,
                        System.nanoTime() - perfStart
                );
            }
        }
    }

    private void updateHowdahLookBehavior(EntityLivingBase target) {
        double targetX = target.posX - this.posX;
        double targetY = target.posY + (double)target.getEyeHeight() * 0.75D - (this.posY + (double)this.getEyeHeight());
        double targetZ = target.posZ - this.posZ;
        double horizontal = MathHelper.sqrt_double(targetX * targetX + targetZ * targetZ);
        float yaw = (float)(Math.atan2(targetZ, targetX) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float)(-(Math.atan2(targetY, horizontal) * 180.0D / Math.PI));

        this.applyHowdahLookRotation(yaw, MathHelper.clamp_float(pitch, -75.0F, 55.0F), 14.0F, 10.0F);
    }

    private void updateHowdahIdleLook() {
        if (--this.howdahIdleLookTicks <= 0) {
            this.howdahIdleLookTicks = 60 + this.rand.nextInt(80);
            this.howdahIdleYawOffset = -35.0F + this.rand.nextFloat() * 70.0F;
        }

        this.applyHowdahLookRotation(this.rotationYaw + this.howdahIdleYawOffset, 0.0F, 4.0F, 4.0F);
    }

    private void applyHowdahLookRotation(float desiredYaw, float desiredPitch, float maxYawStep, float maxPitchStep) {
        float previousYaw = this.hasHowdahLookRotation ? this.howdahLookYaw : desiredYaw;
        float previousPitch = this.hasHowdahLookRotation ? this.howdahLookPitch : desiredPitch;

        this.howdahLookYaw = this.updateHowdahRotation(previousYaw, desiredYaw, maxYawStep);
        this.howdahLookPitch = this.updateHowdahRotation(previousPitch, desiredPitch, maxPitchStep);
        this.hasHowdahLookRotation = true;

        this.rotationYaw = this.howdahLookYaw;
        this.prevRotationYaw = normalizePreviousYaw(previousYaw, this.howdahLookYaw);
        this.renderYawOffset = this.howdahLookYaw;
        this.prevRenderYawOffset = this.prevRotationYaw;
        this.rotationYawHead = this.howdahLookYaw;
        this.prevRotationYawHead = this.prevRotationYaw;
        this.rotationPitch = this.howdahLookPitch;
        this.prevRotationPitch = previousPitch;
    }

    private float updateHowdahRotation(float current, float desired, float maxStep) {
        float delta = MathHelper.wrapAngleTo180_float(desired - current);
        return current + MathHelper.clamp_float(delta, -maxStep, maxStep);
    }

    private void updateHowdahShooting(
            LOTREntityMumakil mumakil,
            EntityLivingBase target,
            double targetDistanceSq
    ) {
        if (this.tickHowdahShootCooldown() || this.worldObj.isRemote) {
            return;
        }

        /*
         * The parent, target, and attachment were validated immediately before
         * this call. Reuse that distance instead of repeating the same checks.
         */
        if (targetDistanceSq > HOWDAH_ARCHER_SHOOT_RANGE_SQ
                || !this.hasHowdahShotLine(mumakil, target)) {
            return;
        }

        this.shootHowdahArrowAt(mumakil, target);
        this.resetHowdahShootCooldown();
    }

    private boolean tickHowdahShootCooldown() {
        if (this.howdahShootCooldown > 0) {
            --this.howdahShootCooldown;
            return true;
        }

        return false;
    }

    private void primeHowdahShootCooldown() {
        if (this.howdahShootCooldown <= 0) {
            this.howdahShootCooldown = 40 + this.rand.nextInt(80);
        }
    }

    private void resetHowdahShootCooldown() {
        if (this.isPrimaryHowdahShooterSlot()) {
            this.howdahShootCooldown = PRIMARY_SHOOT_COOLDOWN_MIN + this.rand.nextInt(PRIMARY_SHOOT_COOLDOWN_RANDOM);
        } else {
            this.howdahShootCooldown = HIGH_PERCH_SHOOT_COOLDOWN_MIN + this.rand.nextInt(HIGH_PERCH_SHOOT_COOLDOWN_RANDOM);
        }
    }

    private boolean canHowdahArcherTrackTarget(
            LOTREntityMumakil mumakil,
            EntityLivingBase target,
            double targetDistanceSq
    ) {
        return mumakil != null
                && mumakil.isEntityAlive()
                && this.isFixedHowdahPassenger()
                && target != null
                && target != this
                && target != mumakil
                && target.isEntityAlive()
                && targetDistanceSq <= HOWDAH_ARCHER_TRACK_RANGE_SQ;
    }

    private boolean hasHowdahShotLine(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (!this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordArcherVisibilityCheck(mumakil, this.getHowdahSlot());
        }

        if (this.canEntityBeSeen(target)) {
            return true;
        }

        if (!this.isPrimaryHowdahShooterSlot()) {
            return false;
        }

        Vec3 origin = Vec3.createVectorHelper(this.posX, this.posY + (double)this.getEyeHeight(), this.posZ);
        return this.hasClearShotTo(mumakil, origin, target.posY + (double)target.height * 0.65D, target)
                || this.hasClearShotTo(mumakil, origin, target.posY + 0.2D, target);
    }

    private boolean hasClearShotTo(LOTREntityMumakil mumakil, Vec3 origin, double targetY, EntityLivingBase target) {
        if (!this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordArcherVisibilityCheck(mumakil, this.getHowdahSlot());
        }

        Vec3 targetPoint = Vec3.createVectorHelper(target.posX, targetY, target.posZ);
        return this.worldObj.rayTraceBlocks(origin, targetPoint) == null;
    }

    private boolean isPrimaryHowdahShooterSlot() {
        return this.getHowdahSlot() <= 13;
    }

    private void shootHowdahArrowAt(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (!this.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordArrowFired(mumakil, this.getHowdahSlot());
        }

        this.attackEntityWithRangedAttack(target, 1.0F);
    }

    private void ensureNearHaradBowEquipped() {
        equipNearHaradBow(this);
    }

    private static void equipNearHaradBow(LOTREntityNearHaradrimArcher archer) {
        ItemStack bow = new ItemStack(LOTRMod.nearHaradBow);
        if (archer.npcItemsInv != null) {
            archer.npcItemsInv.setRangedWeapon(bow);
            archer.npcItemsInv.setIdleItem(bow);
        }

        ItemStack heldItem = archer.getHeldItem();
        if (heldItem == null || heldItem.getItem() != LOTRMod.nearHaradBow) {
            archer.setCurrentItemOrArmor(0, new ItemStack(LOTRMod.nearHaradBow));
        }

        archer.setEquipmentDropChance(0, 0.0F);
    }

    private void clearPassengerAI() {
        if (this.passengerAICleared && this.getAttackTarget() == null) {
            return;
        }

        if (this.tasks != null && this.tasks.taskEntries != null && !this.tasks.taskEntries.isEmpty()) {
            this.tasks.taskEntries.clear();
        }

        if (this.targetTasks != null && this.targetTasks.taskEntries != null && !this.targetTasks.taskEntries.isEmpty()) {
            this.targetTasks.taskEntries.clear();
        }

        if (this.getAttackTarget() != null) {
            this.setAttackTarget(null);
        }

        this.setRevengeTarget(null);

        PathNavigate navigator = this.getNavigator();
        if (navigator != null && !navigator.noPath()) {
            navigator.clearPathEntity();
        }

        this.passengerAICleared = true;
        this.stopPassengerMotion();
    }

    @Override
    public boolean isAIEnabled() {
        return !this.isRuntimeHowdahPassenger() && super.isAIEnabled();
    }

    @Override
    protected void updateAITasks() {
        if (!this.isFixedHowdahPassenger()) {
            super.updateAITasks();
        }
    }

    @Override
    protected void updateEntityActionState() {
        if (!this.isFixedHowdahPassenger()) {
            super.updateEntityActionState();
        }
    }

    @Override
    protected void updateLeashedState() {
        if (!this.isFixedHowdahPassenger()) {
            super.updateLeashedState();
        }
    }

    @Override
    protected void collideWithEntity(Entity entity) {
        if (!this.isFixedHowdahPassenger()) {
            super.collideWithEntity(entity);
        }
    }

    @Override
    protected void collideWithNearbyEntities() {
        if (!this.isFixedHowdahPassenger()) {
            super.collideWithNearbyEntities();
        }
    }

    @Override
    public boolean canBePushed() {
        return !this.isFixedHowdahPassenger() && super.canBePushed();
    }

    @Override
    public boolean canBeCollidedWith() {
        if (this.isFixedHowdahPassenger()) {
            return true;
        }

        return super.canBeCollidedWith();
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        if (this.isFixedHowdahPassenger()) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            this.fallDistance = 0.0F;
            this.isAirBorne = false;
            return;
        }

        super.moveEntity(x, y, z);
    }

    @Override
    public void addVelocity(double x, double y, double z) {
        if (this.isFixedHowdahPassenger()) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            return;
        }

        super.addVelocity(x, y, z);
    }

    @Override
    public void knockBack(Entity entity, float strength, double xRatio, double zRatio) {
        if (!this.isFixedHowdahPassenger()) {
            super.knockBack(entity, strength, xRatio, zRatio);
        }
    }

    @Override
    protected void fall(float distance) {
        if (!this.isFixedHowdahPassenger()) {
            super.fall(distance);
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (!this.isFixedHowdahPassenger()) {
            return super.attackEntityFrom(source, amount);
        }

        if (source == null || amount <= 0.0F || this.isBlockedFixedPassengerDamage(source)) {
            return false;
        }

        boolean damaged = super.attackEntityFrom(source, amount);

        if (this.isEntityAlive()) {
            this.noClip = true;
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
        }

        return damaged;
    }

    private boolean isBlockedFixedPassengerDamage(DamageSource source) {
        return source == DamageSource.inWall
                || source == DamageSource.fall
                || source == DamageSource.drown
                || source == DamageSource.cactus
                || source == DamageSource.inFire
                || source == DamageSource.onFire
                || source == DamageSource.lava;
    }

    @Override
    public boolean canDespawn() {
        return !this.isFixedHowdahPassenger() && super.canDespawn();
    }

    @Override
    public boolean writeToNBTOptional(NBTTagCompound nbt) {
        return false;
    }

    @Override
    public boolean writeMountToNBT(NBTTagCompound nbt) {
        return false;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger(NBT_MOUNT_ID, this.getHowdahMountEntityId());
        nbt.setString(NBT_MOUNT_UUID, this.getHowdahMountUuid());
        nbt.setInteger(NBT_SLOT, this.getHowdahSlot());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.naturalFormationBootstrapPending = false;
        this.howdahMountEntityId = nbt.getInteger(NBT_MOUNT_ID);
        this.howdahMountUuid = nbt.getString(NBT_MOUNT_UUID);
        this.howdahSlot = nbt.hasKey(NBT_SLOT) ? nbt.getInteger(NBT_SLOT) : nbt.getInteger(LEGACY_NBT_SLOT);
        this.howdahAttachmentDataLoaded = true;
        this.detachedFromDeadMumakil = false;
        this.detachedTicks = 0;
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setString(NBT_MOUNT_UUID, this.howdahMountUuid);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.getHowdahMountEntityId());
        buffer.writeInt(this.getHowdahSlot());
        ByteBufUtils.writeUTF8String(buffer, this.getHowdahMountUuid());
        buffer.writeBoolean(this.isRuntimeHowdahPassenger());
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.howdahMountEntityId = additionalData.readInt();
        this.howdahSlot = additionalData.readInt();
        this.howdahMountUuid = ByteBufUtils.readUTF8String(additionalData);
        this.howdahAttachmentDataLoaded = true;
        this.setRuntimeHowdahPassenger(additionalData.readBoolean());
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setString(NBT_MOUNT_UUID, this.howdahMountUuid);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    private static String getEntityPersistentIdString(Entity entity) {
        return entity == null ? "" : entity.getPersistentID().toString();
    }

    public static int getHowdahArcherSlotCount() {
        return HOWDAH_ARCHER_OFFSETS.length;
    }

    public boolean setAssignedHowdahTarget(EntityLivingBase target) {
        int newTargetId = target == null || target.isDead ? 0 : target.getEntityId();
        if (this.assignedHowdahTargetEntityId == newTargetId) {
            return false;
        }

        this.assignedHowdahTargetEntityId = newTargetId;
        return true;
    }

    public EntityLivingBase getAssignedHowdahTarget() {
        if (this.assignedHowdahTargetEntityId == 0 || this.worldObj == null) {
            return null;
        }

        Entity target = this.worldObj.getEntityByID(this.assignedHowdahTargetEntityId);
        return target instanceof EntityLivingBase ? (EntityLivingBase)target : null;
    }

    /**
     * Offset columns are:
     * 0 = forward/back along the Mumakil body. Positive moves toward the head.
     * 1 = left/right across the Mumakil body. Negative is left, positive is right.
     * 2 = vertical height above the Mumakil entity origin.
     * 3 = archer yaw relative to the Mumakil body yaw.
     */
    private static final double[][] HOWDAH_ARCHER_OFFSETS = new double[][] {
            // Slots 0-11: main howdah deck/perch.
            //Left side from back to front
            { -8.0D, -1.5D, 16.8D, 90.0D },
            { -5.0D, -4.0D, 16.8D, 90.0D },
            { -2.0D, -5.5D, 17.0D, 90.0D },
            { 1.0D, -5.5D, 17.5D, 90.0D },
            { 4.0D, -4.0D, 18.0D, 90.0D },
            {  6.0D, -2.0D, 18.0D, 90.0D },

            //Right side from back to front
            { -8.0D, 1.5D, 16.8D, 90.0D },
            { -5.0D, 4.0D, 16.8D, 90.0D },
            { -2.0D, 5.5D, 17.0D, 90.0D },
            { 1.0D, 5.5D, 17.5D, 90.0D },
            { 4.0D, 4.0D, 18.0D, 90.0D },
            {  6.0D, 2.0D, 18.0D, 90.0D },

            // Slots 12-13: lower side perches.
            {  0.0D, -7.0D, 15.0D, -90.0D },
            {  0.0D,  7.0D, 15.0D,  90.0D },

            // Slots 14-15: middle perch, staggered lengthwise.
            {  2.5D, 0.0D, 21.5D,   0.0D },
            { -4.0D,  0.0D, 21.0D,   0.0D },

            // Slot 16: top lookout perch.
            { -3.0D,  0.0D, 25.5D,   0.0D }
    };
}
