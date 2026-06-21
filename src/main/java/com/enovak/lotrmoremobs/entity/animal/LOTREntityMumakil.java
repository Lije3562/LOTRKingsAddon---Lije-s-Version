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
import net.minecraft.block.material.Material;
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
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class LOTREntityMumakil extends LOTREntityHorse {
    private static final double MAX_HEALTH = 120.0D;
    private static final double MOVEMENT_SPEED = 0.30D;
    private static final double KNOCKBACK_RESISTANCE = 0.75D;
    private static final double ATTACK_DAMAGE = 16.0D;
    private static final double WILD_ATTACK_SPEED = 1.30D;
    private static final float CHARGE_MIN_SPEED = 0.24F;
    private static final float MAX_CHARGE_DAMAGE = 36.0F;
    private static final int MOB_TARGET_CHECK_INTERVAL = 20;
    private static final int MOB_TARGET_CHECK_CHANCE = 2;
    private static final double MOB_TARGET_RANGE = 18.0D;
    private static final double MOB_TARGET_VERTICAL_RANGE = 8.0D;
    private static final int LEAF_CLEAR_INTERVAL = 2;
    private static final int MAX_LEAVES_PER_PASS = 96;
    private static final int TRAMPLE_SCAN_INTERVAL = 2;
    private static final int TRAMPLE_COOLDOWN_TICKS = 20;
    private static final float TRAMPLE_MIN_SPEED = 0.10F;
    private static final float TRAMPLE_DAMAGE = 8.0F;

    private final Map<Integer, Integer> trampleCooldowns = new HashMap<Integer, Integer>();

    public LOTREntityMumakil(World world) {
        super(world);
        this.setSize(3.0F, 4.0F);
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
    }

    private boolean isWildMumakil() {
        return !this.isTame() && this.riddenByEntity == null;
    }

    public double getMountedYOffset() {
        return 5.0D;
    }

    protected boolean isMountHostile() {
        return true;
    }

    protected EntityAIBase createMountAttackAI() {
        return new LOTREntityAIAttackOnCollide(this, WILD_ATTACK_SPEED, true);
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
        if (!this.worldObj.isRemote) {
            this.clearLeavesForMovement();
            this.tryAcquireWildMobTarget();
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
                                    float knockback = Math.min(strength * 0.04F, 1.0F);
                                    entity.addVelocity(
                                            (double)(-MathHelper.sin(this.rotationYaw * (float)Math.PI / 180.0F) * knockback),
                                            (double)knockback,
                                            (double)(MathHelper.cos(this.rotationYaw * (float)Math.PI / 180.0F) * knockback)
                                    );
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
                        this.worldObj.playSoundAtEntity(
                                this,
                                "lotr:troll.ologHai_hammer",
                                1.0F,
                                (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F
                        );
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

    private void tryAcquireWildMobTarget() {
        if (!this.isWildMumakil()
                || this.getAttackTarget() != null
                || this.ticksExisted % MOB_TARGET_CHECK_INTERVAL != 0
                || this.rand.nextInt(MOB_TARGET_CHECK_CHANCE) != 0) {
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
                || target.getClass() == this.getClass()
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
            if (npc.hiredNPCInfo.isActive) {
                return false;
            }

            LOTRFaction targetFaction = LOTRMod.getNPCFaction(npc);
            return LOTRFaction.NEAR_HARAD.isBadRelation(targetFaction);
        }

        return target instanceof EntityCreature;
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
                this.applyTrampleKnockback(target, directionX, directionZ);
            }
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

        target.addVelocity(deltaX * 0.9D, 0.25D, deltaZ * 0.9D);
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

    private void clearLeavesForMovement() {
        if (this.worldObj.isRemote || this.ticksExisted % LEAF_CLEAR_INTERVAL != 0) {
            return;
        }

        double horizontalSpeedSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
        boolean riderTryingToMove = this.riddenByEntity instanceof EntityLivingBase
                && (Math.abs(((EntityLivingBase)this.riddenByEntity).moveForward) > 0.01F
                || Math.abs(((EntityLivingBase)this.riddenByEntity).moveStrafing) > 0.01F);
        boolean tryingToMove = horizontalSpeedSq > 0.0004D
                || riderTryingToMove
                || this.isMountEnraged();

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

        int remaining = MAX_LEAVES_PER_PASS;
        remaining -= this.clearLeavesInBox(headAndTusksBox, remaining);

        if (remaining > 0) {
            remaining -= this.clearLeavesInBox(trunkBox, remaining);
        }

        if (remaining > 0) {
            this.clearLeavesInBox(bodyBox, remaining);
        }
    }

    private int clearLeavesInBox(AxisAlignedBB leafBox, int maximum) {
        int minX = MathHelper.floor_double(leafBox.minX);
        int maxX = MathHelper.floor_double(leafBox.maxX);
        int minY = MathHelper.floor_double(leafBox.minY);
        int maxY = MathHelper.floor_double(leafBox.maxY);
        int minZ = MathHelper.floor_double(leafBox.minZ);
        int maxZ = MathHelper.floor_double(leafBox.maxZ);
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
                    if (block.getMaterial() == Material.leaves) {
                        this.worldObj.setBlockToAir(x, y, z);
                        ++broken;
                    }
                }
            }
        }

        return broken;
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
