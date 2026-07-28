package com.enovak.lotrmoremobs.handler;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.common.entity.npc.LOTREntityGandalf;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

/**
 * GANDALF_SURVIVAL_DAMAGE_V1
 *
 * LOTREntityGandalf deliberately replaces all non-creative damage with zero
 * before calling EntityLivingBase. Forge's LivingHurtEvent still runs inside
 * the superclass damage path, so this handler restores only damage caused by
 * a real non-creative player.
 *
 * Direct melee damage is captured before Gandalf zeroes it. Player-fired
 * arrow damage is reconstructed from the arrow's speed and damage value.
 * Other mobs and environmental sources remain unable to kill Gandalf.
 */
public final class GandalfSurvivalDamageEventHandler {
    private static final ThreadLocal<PendingMeleeAttack> PENDING_MELEE =
            new ThreadLocal<PendingMeleeAttack>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerAttack(AttackEntityEvent event) {
        if (!(event.target instanceof LOTREntityGandalf)) {
            return;
        }

        EntityPlayer player = event.entityPlayer;

        if (player == null
                || player.worldObj == null
                || player.worldObj.isRemote
                || player.capabilities.isCreativeMode) {
            return;
        }

        LOTREntityGandalf gandalf =
                (LOTREntityGandalf)event.target;

        float damage = (float)player.getEntityAttribute(
                SharedMonsterAttributes.attackDamage
        ).getAttributeValue();

        float enchantmentDamage =
                EnchantmentHelper.getEnchantmentModifierLiving(
                        player,
                        gandalf
                );

        boolean critical =
                player.fallDistance > 0.0F
                        && !player.onGround
                        && !player.isOnLadder()
                        && !player.isInWater()
                        && !player.isPotionActive(Potion.blindness)
                        && player.ridingEntity == null;

        if (critical && damage > 0.0F) {
            damage *= 1.5F;
        }

        damage += enchantmentDamage;

        if (damage <= 0.0F) {
            PENDING_MELEE.remove();
            return;
        }

        PENDING_MELEE.set(
                new PendingMeleeAttack(
                        gandalf.getEntityId(),
                        player.getEntityId(),
                        player.worldObj.getTotalWorldTime(),
                        damage
                )
        );
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGandalfHurt(LivingHurtEvent event) {
        if (!(event.entityLiving instanceof LOTREntityGandalf)
                || event.entityLiving.worldObj == null
                || event.entityLiving.worldObj.isRemote
                || event.ammount > 0.0F) {
            return;
        }

        Entity responsibleEntity = event.source.getEntity();

        if (!(responsibleEntity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer)responsibleEntity;

        /*
         * Creative-mode damage already passes through Gandalf's own override
         * normally, so it must not be replaced here.
         */
        if (player.capabilities.isCreativeMode) {
            return;
        }

        float restoredDamage = this.consumePendingMeleeDamage(
                (LOTREntityGandalf)event.entityLiving,
                player
        );

        if (restoredDamage <= 0.0F) {
            restoredDamage = this.getPlayerArrowDamage(
                    event.source.getSourceOfDamage(),
                    player
            );
        }

        if (restoredDamage > 0.0F) {
            event.ammount = restoredDamage;
        }
    }

    private float consumePendingMeleeDamage(
            LOTREntityGandalf gandalf,
            EntityPlayer player
    ) {
        PendingMeleeAttack pending = PENDING_MELEE.get();

        if (pending == null) {
            return 0.0F;
        }

        PENDING_MELEE.remove();

        if (pending.gandalfEntityId != gandalf.getEntityId()
                || pending.playerEntityId != player.getEntityId()
                || pending.worldTick
                != player.worldObj.getTotalWorldTime()) {
            return 0.0F;
        }

        return pending.damage;
    }

    private float getPlayerArrowDamage(
            Entity directDamageEntity,
            EntityPlayer player
    ) {
        if (!(directDamageEntity instanceof EntityArrow)) {
            return 0.0F;
        }

        EntityArrow arrow = (EntityArrow)directDamageEntity;

        if (arrow.shootingEntity != player) {
            return 0.0F;
        }

        double speed = MathHelper.sqrt_double(
                arrow.motionX * arrow.motionX
                        + arrow.motionY * arrow.motionY
                        + arrow.motionZ * arrow.motionZ
        );

        int damage = MathHelper.ceiling_double_int(
                speed * arrow.getDamage()
        );

        if (arrow.getIsCritical() && damage > 0) {
            damage += arrow.worldObj.rand.nextInt(damage / 2 + 2);
        }

        return (float)Math.max(0, damage);
    }

    private static final class PendingMeleeAttack {
        private final int gandalfEntityId;
        private final int playerEntityId;
        private final long worldTick;
        private final float damage;

        private PendingMeleeAttack(
                int gandalfEntityId,
                int playerEntityId,
                long worldTick,
                float damage
        ) {
            this.gandalfEntityId = gandalfEntityId;
            this.playerEntityId = playerEntityId;
            this.worldTick = worldTick;
            this.damage = damage;
        }
    }
}