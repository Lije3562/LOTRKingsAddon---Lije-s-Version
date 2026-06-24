package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.WeakHashMap;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class MumakilFearEventHandler {
    private static final float AVOID_DISTANCE = 16.0F;
    private static final double FAR_SPEED = 1.2D;
    private static final double NEAR_SPEED = 1.5D;
    private static final String[] LOTR_CIVILIAN_NAME_HINTS = new String[] {
            "hobbit", "villager", "farmer", "trader", "bartender", "child", "woman", "man", "civilian"
    };
    private static final String[] LOTR_WARRIOR_NAME_HINTS = new String[] {
            "soldier", "warrior", "archer", "guard", "knight", "orc", "uruk", "ranger", "warg", "troll", "bandit", "raider"
    };

    private final Set<EntityCreature> configuredAvoiders =
            Collections.newSetFromMap(new WeakHashMap<EntityCreature, Boolean>());

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world == null || event.world.isRemote) {
            return;
        }

        Entity entity = event.entity;
        if (!this.shouldFearMumakil(entity)) {
            return;
        }

        EntityCreature creature = (EntityCreature)entity;
        if (!this.configuredAvoiders.add(creature)) {
            return;
        }

        creature.tasks.addTask(
                3,
                new EntityAIAvoidEntity(
                        creature,
                        LOTREntityMumakil.class,
                        AVOID_DISTANCE,
                        FAR_SPEED,
                        NEAR_SPEED
                )
        );
    }

    private boolean shouldFearMumakil(Entity entity) {
        if (!(entity instanceof EntityCreature)
                || entity instanceof LOTREntityMumakil
                || entity instanceof IMob) {
            return false;
        }

        if (entity instanceof EntityTameable && ((EntityTameable)entity).isTamed()) {
            return false;
        }

        if (entity instanceof EntityHorse && ((EntityHorse)entity).isTame()) {
            return false;
        }

        if (entity instanceof EntityAnimal || entity instanceof EntityVillager) {
            return true;
        }

        if (entity instanceof LOTREntityNPC) {
            return this.isPassiveLOTRNpc((LOTREntityNPC)entity);
        }

        return false;
    }

    private boolean isPassiveLOTRNpc(LOTREntityNPC npc) {
        if (npc.hiredNPCInfo.isActive) {
            return false;
        }

        String className = npc.getClass().getName().toLowerCase(Locale.ROOT);
        if (this.containsAny(className, LOTR_WARRIOR_NAME_HINTS)) {
            return false;
        }

        if (this.containsAny(className, LOTR_CIVILIAN_NAME_HINTS)) {
            return true;
        }

        ItemStack heldItem = npc.getHeldItem();
        if (heldItem != null) {
            return false;
        }

        IAttributeInstance attackDamage = npc.getEntityAttribute(SharedMonsterAttributes.attackDamage);
        return attackDamage == null || attackDamage.getAttributeValue() <= 2.0D;
    }

    private boolean containsAny(String value, String[] terms) {
        for (int i = 0; i < terms.length; ++i) {
            if (value.contains(terms[i])) {
                return true;
            }
        }

        return false;
    }
}
