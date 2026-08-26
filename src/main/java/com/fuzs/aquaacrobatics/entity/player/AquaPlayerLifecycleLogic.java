package com.fuzs.aquaacrobatics.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.fuzs.aquaacrobatics.entity.EntitySize;
import com.fuzs.aquaacrobatics.entity.Pose;

/** Shared lifecycle and sleep state semantics; ASM retains the required super dispatch. */
public final class AquaPlayerLifecycleLogic {

    private AquaPlayerLifecycleLogic() {}

    /** Runs before EntityPlayer's inherited preparePlayerToSpawn implementation. */
    public static void beforePreparePlayerToSpawn(EntityPlayer player) {
        player.yOffset = 1.62F;
        ((IPlayerResizeable) player).setPose(Pose.STANDING);
    }

    /** Runs after EntityPlayer's inherited preparePlayerToSpawn implementation. */
    public static void afterPreparePlayerToSpawn(EntityPlayer player) {
        player.setHealth(player.getMaxHealth());
        player.deathTime = 0;
    }

    /** Replaces only sleepInBedAt's accepted vanilla setSize(0.2F, 0.2F) invocation. */
    public static void onSleepSetSize(EntityPlayer player, float width, float height) {
        ((IPlayerResizeable) player).setPose(Pose.SLEEPING);
    }

    public static void onDeath(EntityPlayer player) {
        ((IPlayerResizeable) player).setPose(Pose.DYING);
    }

    public static float defaultEyeHeight(EntityPlayer player) {
        return ((IPlayerResizeable) player).getPose() == Pose.SWIMMING ? 0.4F : 1.62F;
    }

    public static float defaultEyeHeight(EntityPlayerMP player) {
        return defaultEyeHeight((EntityPlayer) player);
    }

    public static boolean hasSwimmingEyeHeight(EntityPlayer player) {
        return ((IPlayerResizeable) player).getPose() == Pose.SWIMMING;
    }

    public static boolean hasSwimmingEyeHeight(EntityPlayerMP player) {
        return hasSwimmingEyeHeight((EntityPlayer) player);
    }

    public static EntitySize resizeSize(EntityPlayer player, Pose pose) {
        return ((IPlayerResizeable) player).getSize(pose);
    }

    public static EntitySize resizeSize(EntityPlayerMP player, Pose pose) {
        return resizeSize((EntityPlayer) player, pose);
    }
}
