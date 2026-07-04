package com.enovak.lotrmoremobs.entity.npc;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import io.netty.buffer.ByteBuf;
import lotr.common.entity.npc.LOTREntityNearHaradrimArcher;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Visual/passive howdah passenger for the custom hired Mumakil.
 *
 * This is intentionally a subclass of the normal Near Haradrim archer so it can
 * reuse the LOTR archer renderer/model/texture, but it disables normal gravity,
 * pathing, knockback, and combat while attached to its Mumakil.
 */
public class LOTREntityMumakilHowdahArcher extends LOTREntityNearHaradrimArcher implements IEntityAdditionalSpawnData {
    private static final String NBT_MOUNT_ID = "LOTRMoreMobsHowdahMountId";
    private static final String NBT_SLOT = "LOTRMoreMobsHowdahSlot";

    private int howdahMountEntityId;
    private int howdahSlot;

    public LOTREntityMumakilHowdahArcher(World world) {
        super(world);
        this.clearPassengerAI();
        this.noClip = true;
        this.isImmuneToFire = true;
    }

    public void setHowdahAttachment(LOTREntityMumakil mumakil, int slot) {
        this.howdahMountEntityId = mumakil == null ? 0 : mumakil.getEntityId();
        this.howdahSlot = slot;
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    public int getHowdahMountEntityId() {
        if (this.howdahMountEntityId == 0) {
            this.howdahMountEntityId = this.getEntityData().getInteger(NBT_MOUNT_ID);
        }
        return this.howdahMountEntityId;
    }

    public int getHowdahSlot() {
        this.howdahSlot = this.getEntityData().getInteger(NBT_SLOT);
        return this.howdahSlot;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        this.updateHowdahPassengerAttachment();
    }

    @Override
    public void onLivingUpdate() {
        this.clearPassengerAI();
        super.onLivingUpdate();
        this.updateHowdahPassengerAttachment();
    }

    private void updateHowdahPassengerAttachment() {
        LOTREntityMumakil mumakil = this.getAttachedMumakil();

        if (mumakil == null || !mumakil.isEntityAlive() || !mumakil.hasMumakilHowdahEquipped()) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;

            if (!this.worldObj.isRemote) {
                this.setDead();
            }

            return;
        }

        this.clearPassengerAI();
        this.placeOnHowdah(mumakil, this.getHowdahSlot());
    }

    private LOTREntityMumakil getAttachedMumakil() {
        int mountId = this.getHowdahMountEntityId();
        Entity entity = mountId == 0 || this.worldObj == null ? null : this.worldObj.getEntityByID(mountId);
        return entity instanceof LOTREntityMumakil ? (LOTREntityMumakil)entity : null;
    }

    private void placeOnHowdah(LOTREntityMumakil mumakil, int rawSlot) {
        int slot = MathHelper.clamp_int(rawSlot, 0, HOWDAH_ARCHER_OFFSETS.length - 1);
        double[] offset = HOWDAH_ARCHER_OFFSETS[slot];
        double forwardOffset = offset[0];
        double sideOffset = offset[1];
        double verticalOffset = offset[2];
        float placementYaw = mumakil.renderYawOffset;
        float yawRadians = placementYaw * 3.1415927F / 180.0F;

        double forwardX = -MathHelper.sin(yawRadians) * forwardOffset;
        double forwardZ = MathHelper.cos(yawRadians) * forwardOffset;
        double sideX = MathHelper.cos(yawRadians) * sideOffset;
        double sideZ = MathHelper.sin(yawRadians) * sideOffset;

        double x = mumakil.posX + forwardX + sideX;
        double y = mumakil.posY + verticalOffset;
        double z = mumakil.posZ + forwardZ + sideZ;
        float archerYaw = MathHelper.wrapAngleTo180_float(placementYaw + (float)offset[3]);

        this.setPositionAndRotation(x, y, z, archerYaw, 0.0F);
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
        this.lastTickPosX = x;
        this.lastTickPosY = y;
        this.lastTickPosZ = z;
        this.motionX = 0.0D;
        this.motionY = 0.0D;
        this.motionZ = 0.0D;
        this.fallDistance = 0.0F;
        this.onGround = true;
        this.isAirBorne = false;
        this.noClip = true;

        this.rotationYaw = archerYaw;
        this.prevRotationYaw = archerYaw;
        this.rotationPitch = 0.0F;
        this.prevRotationPitch = 0.0F;
        this.renderYawOffset = archerYaw;
        this.prevRenderYawOffset = archerYaw;
        this.rotationYawHead = archerYaw;
        this.prevRotationYawHead = archerYaw;
    }

    private void clearPassengerAI() {
        if (this.tasks != null && this.tasks.taskEntries != null) {
            this.tasks.taskEntries.clear();
        }

        if (this.targetTasks != null && this.targetTasks.taskEntries != null) {
            this.targetTasks.taskEntries.clear();
        }

        this.setAttackTarget(null);
        this.setRevengeTarget(null);

        if (this.getNavigator() != null) {
            this.getNavigator().clearPathEntity();
        }
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        if (this.getHowdahMountEntityId() != 0) {
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
        if (this.getHowdahMountEntityId() != 0) {
            this.motionX = 0.0D;
            this.motionY = 0.0D;
            this.motionZ = 0.0D;
            return;
        }

        super.addVelocity(x, y, z);
    }

    @Override
    public void knockBack(Entity entity, float strength, double xRatio, double zRatio) {
        if (this.getHowdahMountEntityId() == 0) {
            super.knockBack(entity, strength, xRatio, zRatio);
        }
    }

    @Override
    protected void fall(float distance) {
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        return this.getHowdahMountEntityId() == 0 && super.attackEntityFrom(source, amount);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound nbt) {
        super.writeEntityToNBT(nbt);
        nbt.setInteger(NBT_MOUNT_ID, this.getHowdahMountEntityId());
        nbt.setInteger(NBT_SLOT, this.getHowdahSlot());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound nbt) {
        super.readEntityFromNBT(nbt);
        this.howdahMountEntityId = nbt.getInteger(NBT_MOUNT_ID);
        this.howdahSlot = nbt.getInteger(NBT_SLOT);
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeInt(this.getHowdahMountEntityId());
        buffer.writeInt(this.getHowdahSlot());
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        this.howdahMountEntityId = additionalData.readInt();
        this.howdahSlot = additionalData.readInt();
        this.getEntityData().setInteger(NBT_MOUNT_ID, this.howdahMountEntityId);
        this.getEntityData().setInteger(NBT_SLOT, this.howdahSlot);
    }

    /**
     * Same grouping as the handler: 6 wide-body, 4 lower-perch, 4 mid-perch, 1 top-perch.
     * These are slightly higher and closer to the Mumakil center than the previous pass.
     */
    private static final double[][] HOWDAH_ARCHER_OFFSETS = new double[][] {
            // Wide howdah body: left side, front to rear.
            { 7.5D, -3.35D, 17.65D, -90.0D },
            { 9.7D, -3.55D, 17.65D, -90.0D },
            {11.9D, -3.35D, 17.65D, -90.0D },

            // Wide howdah body: right side, front to rear.
            { 7.5D,  3.35D, 17.65D,  90.0D },
            { 9.7D,  3.55D, 17.65D,  90.0D },
            {11.9D,  3.35D, 17.65D,  90.0D },

            // Lower side perches: two on each lower perch.
            { 8.4D, -4.25D, 15.55D, -90.0D },
            {11.2D, -4.25D, 15.55D, -90.0D },
            { 8.4D,  4.25D, 15.55D,  90.0D },
            {11.2D,  4.25D, 15.55D,  90.0D },

            // Middle perch: four across the upper middle area.
            { 6.7D, -1.25D, 19.25D,   0.0D },
            { 8.1D, -0.4D, 19.25D,   0.0D },
            { 8.1D,  0.4D, 19.25D,   0.0D },
            { 6.7D,  1.25D, 19.25D,   0.0D },

            // Top perch: one lookout.
            { 9.8D,  0.0D, 22.35D,   0.0D }
    };
}
