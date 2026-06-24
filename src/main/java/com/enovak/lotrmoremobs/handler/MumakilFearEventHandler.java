package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class MumakilFearEventHandler {
    private static final float AVOID_DISTANCE = 12.0F;
    private static final double FAR_SPEED = 1.2D;
    private static final double NEAR_SPEED = 1.5D;

    private final Set<EntityCreature> configuredAvoiders =
            Collections.newSetFromMap(new WeakHashMap<EntityCreature, Boolean>());

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world == null || event.world.isRemote) {
            return;
        }

        Entity entity = event.entity;
        if (!(entity instanceof EntityAnimal)
                || entity instanceof LOTREntityMumakil
                || !(entity instanceof EntityCreature)) {
            return;
        }

        if (entity instanceof EntityTameable && ((EntityTameable)entity).isTamed()) {
            return;
        }

        if (entity instanceof EntityHorse && ((EntityHorse)entity).isTame()) {
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
}
