//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.enovak.lotrmoremobs.entity.animal;

import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.LOTRReflection;
import lotr.common.entity.ai.LOTREntityAIAttackOnCollide;
import lotr.common.entity.animal.LOTREntityHorse;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class LOTREntityMumakil extends LOTREntityHorse {
    private static final double MAX_HEALTH = 120.0D;
    private static final double MOVEMENT_SPEED = 0.28D;
    private static final double KNOCKBACK_RESISTANCE = 0.75D;
    private static final double ATTACK_DAMAGE = 12.0D;
    private static final float CHARGE_MIN_SPEED = 0.24F;
    private static final float MAX_CHARGE_DAMAGE = 36.0F;

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
        return !this.isMountSaddled() && this.riddenByEntity == null;
    }

    public double getMountedYOffset() {
        return 4.2D;
    }

    protected boolean isMountHostile() {
        return true;
    }

    protected EntityAIBase createMountAttackAI() {
        return new LOTREntityAIAttackOnCollide(this, 1.0D, true);
    }

    public int getHorseType() {
        return 0;
    }

    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(MOVEMENT_SPEED);
        this.getEntityAttribute(SharedMonsterAttributes.knockbackResistance).setBaseValue(KNOCKBACK_RESISTANCE);
        this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(ATTACK_DAMAGE);
    }

    protected void onLOTRHorseSpawn() {
        this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setBaseValue(MAX_HEALTH);
        this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setBaseValue(MOVEMENT_SPEED);

        double jumpStrength = this.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).getAttributeValue();
        jumpStrength *= 0.5D;
        this.getEntityAttribute(LOTRReflection.getHorseJumpStrength()).setBaseValue(jumpStrength);

        this.setHealth(this.getMaxHealth());
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
            this.breakLeavesAroundBody();

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

    private void breakLeavesAroundBody() {
        if (this.worldObj.isRemote || this.ticksExisted % 2 != 0) {
            return;
        }

        double horizontalSpeedSq = this.motionX * this.motionX + this.motionZ * this.motionZ;
        if (horizontalSpeedSq < 0.01D && !this.isSprinting()) {
            return;
        }

        AxisAlignedBB leafBox = this.boundingBox
                .expand(0.25D, 0.0D, 0.25D)
                .addCoord(this.motionX * 2.0D, 0.0D, this.motionZ * 2.0D);

        int minX = MathHelper.floor_double(leafBox.minX);
        int maxX = MathHelper.floor_double(leafBox.maxX);
        int minY = MathHelper.floor_double(this.boundingBox.minY + 1.0D);
        int maxY = MathHelper.floor_double(this.boundingBox.maxY + 0.25D);
        int minZ = MathHelper.floor_double(leafBox.minZ);
        int maxZ = MathHelper.floor_double(leafBox.maxZ);

        for(int x = minX; x <= maxX; ++x) {
            for(int y = minY; y <= maxY; ++y) {
                for(int z = minZ; z <= maxZ; ++z) {
                    if (!this.worldObj.blockExists(x, y, z)) {
                        continue;
                    }

                    Block block = this.worldObj.getBlock(x, y, z);
                    if (this.canBreakLeaf(block, x, y, z)) {
                        this.worldObj.setBlockToAir(x, y, z);
                    }
                }
            }
        }
    }

    private boolean canBreakLeaf(Block block, int x, int y, int z) {
        if (block.getMaterial() != Material.leaves) {
            return false;
        }

        return !(block instanceof BlockLeaves)
                || (this.worldObj.getBlockMetadata(x, y, z) & 4) == 0;
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
