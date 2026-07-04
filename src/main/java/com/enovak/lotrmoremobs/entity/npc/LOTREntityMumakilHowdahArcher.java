package com.enovak.lotrmoremobs.entity.npc;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Method;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNearHaradrimArcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.EntityLivingBase;
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
    private int howdahIdleLookTicks;
    private float howdahIdleYawOffset;
    private float howdahLookYaw;
    private float howdahLookPitch;
    private boolean runtimeHowdahPassenger;
    private boolean passengerAICleared;
    private boolean detachedFromDeadMumakil;
    private boolean hasHowdahLookRotation;

    public LOTREntityMumakilHowdahArcher(World world) {
        super(world);
        this.clearPassengerAI();
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    @Override
    public IEntityLivingData onSpawnWithEgg(IEntityLivingData data) {
        data = super.onSpawnWithEgg(data);
        this.ensureNearHaradBowEquipped();
        return data;
    }

    public void setHowdahAttachment(LOTREntityMumakil mumakil, int slot) {
        this.howdahMountEntityId = mumakil == null ? 0 : mumakil.getEntityId();
        this.howdahMountUuid = mumakil == null ? "" : getEntityPersistentIdString(mumakil);
        this.howdahSlot = slot;
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
        if (this.howdahMountEntityId == 0) {
            this.howdahMountEntityId = this.getEntityData().getInteger(NBT_MOUNT_ID);
        }
        return this.howdahMountEntityId;
    }

    public int getHowdahSlot() {
        NBTTagCompound data = this.getEntityData();
        if (data.hasKey(NBT_SLOT)) {
            this.howdahSlot = data.getInteger(NBT_SLOT);
        } else if (data.hasKey(LEGACY_NBT_SLOT)) {
            this.howdahSlot = data.getInteger(LEGACY_NBT_SLOT);
            data.setInteger(NBT_SLOT, this.howdahSlot);
        }
        return this.howdahSlot;
    }

    public String getHowdahMountUuid() {
        if ((this.howdahMountUuid == null || this.howdahMountUuid.length() == 0) && this.getEntityData().hasKey(NBT_MOUNT_UUID)) {
            this.howdahMountUuid = this.getEntityData().getString(NBT_MOUNT_UUID);
        }

        return this.howdahMountUuid == null ? "" : this.howdahMountUuid;
    }

    public void setRuntimeHowdahPassenger(boolean runtimeHowdahPassenger) {
        this.runtimeHowdahPassenger = runtimeHowdahPassenger;
        if (runtimeHowdahPassenger) {
            this.detachedFromDeadMumakil = false;
            this.detachedTicks = 0;
            this.primeHowdahShootCooldown();
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

        double outwardX = mumakil == null ? 0.0D : this.posX - mumakil.posX;
        double outwardZ = mumakil == null ? 0.0D : this.posZ - mumakil.posZ;
        double outwardDistanceSq = outwardX * outwardX + outwardZ * outwardZ;

        if (outwardDistanceSq < 1.0E-4D && mumakil != null) {
            float yawRadians = mumakil.renderYawOffset * 3.1415927F / 180.0F;
            outwardX = -MathHelper.sin(yawRadians);
            outwardZ = MathHelper.cos(yawRadians);
            outwardDistanceSq = 1.0D;
        }

        if (outwardDistanceSq > 1.0E-4D) {
            double outwardDistance = MathHelper.sqrt_double(outwardDistanceSq);
            outwardX /= outwardDistance;
            outwardZ /= outwardDistance;
        }

        double detachMotionX = (mumakil == null ? 0.0D : mumakil.motionX) + outwardX * 0.22D;
        double detachMotionY = 0.32D;
        double detachMotionZ = (mumakil == null ? 0.0D : mumakil.motionZ) + outwardZ * 0.22D;

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
        replacement.setLocationAndAngles(this.posX, this.posY, this.posZ, this.rotationYaw, this.rotationPitch);
        replacement.prevPosX = this.prevPosX;
        replacement.prevPosY = this.prevPosY;
        replacement.prevPosZ = this.prevPosZ;
        replacement.lastTickPosX = this.lastTickPosX;
        replacement.lastTickPosY = this.lastTickPosY;
        replacement.lastTickPosZ = this.lastTickPosZ;
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
    }

    @Override
    public void onLivingUpdate() {
        if (this.isRuntimeHowdahPassenger()) {
            this.updateHowdahPassengerAttachment();
            return;
        }

        if (this.detachedFromDeadMumakil) {
            super.onLivingUpdate();
            return;
        }

        this.clearPassengerAI();
        super.onLivingUpdate();
        this.updateHowdahPassengerAttachment();
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

    private LOTREntityMumakil findAttachedMumakilByUuid() {
        if (this.worldObj == null || this.getHowdahMountUuid().length() == 0) {
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
            if (this.getHowdahMountUuid().equals(getEntityPersistentIdString(mumakil))) {
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
        this.fallDistance = 0.0F;
        this.onGround = true;
        this.isAirBorne = false;
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
        this.hasHowdahLookRotation = false;
        this.getEntityData().setInteger(NBT_MOUNT_ID, 0);
        this.getEntityData().setString(NBT_MOUNT_UUID, "");
    }

    private void updateHowdahCombatBehavior(LOTREntityMumakil mumakil) {
        EntityLivingBase target = getSharedHowdahTarget(mumakil);
        if (this.canHowdahArcherTrackTarget(mumakil, target)) {
            this.updateHowdahLookBehavior(target);
            this.updateHowdahShooting(mumakil, target);
            return;
        }

        this.updateHowdahIdleLook();
        this.tickHowdahShootCooldown();
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

    private void updateHowdahShooting(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (this.tickHowdahShootCooldown() || this.worldObj.isRemote) {
            return;
        }

        if (!this.canHowdahArcherShootTarget(mumakil, target)) {
            return;
        }

        this.shootHowdahArrowAt(target);
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

    private boolean canHowdahArcherTrackTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        return mumakil != null
                && mumakil.isEntityAlive()
                && this.isFixedHowdahPassenger()
                && target != null
                && target != this
                && target != mumakil
                && target.isEntityAlive()
                && this.getDistanceSqToEntity(target) <= HOWDAH_ARCHER_TRACK_RANGE * HOWDAH_ARCHER_TRACK_RANGE;
    }

    private boolean canHowdahArcherShootTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        return this.canHowdahArcherTrackTarget(mumakil, target)
                && this.getDistanceSqToEntity(target) <= HOWDAH_ARCHER_SHOOT_RANGE * HOWDAH_ARCHER_SHOOT_RANGE
                && this.hasHowdahShotLine(target);
    }

    private boolean hasHowdahShotLine(EntityLivingBase target) {
        if (this.canEntityBeSeen(target)) {
            return true;
        }

        if (!this.isPrimaryHowdahShooterSlot()) {
            return false;
        }

        Vec3 origin = Vec3.createVectorHelper(this.posX, this.posY + (double)this.getEyeHeight(), this.posZ);
        return this.hasClearShotTo(origin, target.posY + (double)target.height * 0.65D, target)
                || this.hasClearShotTo(origin, target.posY + 0.2D, target);
    }

    private boolean hasClearShotTo(Vec3 origin, double targetY, EntityLivingBase target) {
        Vec3 targetPoint = Vec3.createVectorHelper(target.posX, targetY, target.posZ);
        return this.worldObj.rayTraceBlocks(origin, targetPoint) == null;
    }

    private boolean isPrimaryHowdahShooterSlot() {
        return this.getHowdahSlot() <= 13;
    }

    private void shootHowdahArrowAt(EntityLivingBase target) {
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
            this.stopPassengerMotion();
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
        return !this.isFixedHowdahPassenger() && super.canBeCollidedWith();
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
        return !this.isFixedHowdahPassenger() && super.attackEntityFrom(source, amount);
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
        this.howdahMountEntityId = nbt.getInteger(NBT_MOUNT_ID);
        this.howdahMountUuid = nbt.getString(NBT_MOUNT_UUID);
        this.howdahSlot = nbt.hasKey(NBT_SLOT) ? nbt.getInteger(NBT_SLOT) : nbt.getInteger(LEGACY_NBT_SLOT);
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
        this.setRuntimeHowdahPassenger(additionalData.readBoolean());
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setString(NBT_MOUNT_UUID, this.howdahMountUuid);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    private static String getEntityPersistentIdString(Entity entity) {
        Object id = invokeNoArgMethod(entity, "getPersistentID", "getUniqueID", "func_110124_au");
        return id == null ? "" : id.toString();
    }

    private static Object invokeNoArgMethod(Object target, String... methodNames) {
        for (int i = 0; i < methodNames.length; ++i) {
            try {
                Method method = findNoArgMethod(target.getClass(), methodNames[i]);
                if (method != null) {
                    return method.invoke(target, new Object[0]);
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private static Method findNoArgMethod(Class type, String name) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, new Class[0]);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public static int getHowdahArcherSlotCount() {
        return HOWDAH_ARCHER_OFFSETS.length;
    }

    public static void setSharedHowdahTarget(LOTREntityMumakil mumakil, EntityLivingBase target) {
        if (mumakil == null) {
            return;
        }

        mumakil.getEntityData().setInteger(
                MUMAKIL_SHARED_TARGET_ID_KEY,
                target == null || target.isDead ? 0 : target.getEntityId()
        );
    }

    private static EntityLivingBase getSharedHowdahTarget(LOTREntityMumakil mumakil) {
        if (mumakil == null || mumakil.worldObj == null) {
            return null;
        }

        int targetId = mumakil.getEntityData().getInteger(MUMAKIL_SHARED_TARGET_ID_KEY);
        if (targetId == 0) {
            return null;
        }

        Entity target = mumakil.worldObj.getEntityByID(targetId);
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
