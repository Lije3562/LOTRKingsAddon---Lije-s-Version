package com.fuzs.aquaacrobatics.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import com.fuzs.aquaacrobatics.config.ConfigHandler;
import com.fuzs.aquaacrobatics.entity.EntitySize;
import com.fuzs.aquaacrobatics.entity.Pose;
import com.fuzs.aquaacrobatics.integration.efr.EFRIntegration;

/** Shared pose selection, dimensions, and clearance calculations. */
public final class AquaPoseLogic {

    public static final EntitySize STANDING_SIZE = EntitySize.flexible(0.6F, 1.8F);
    public static final EntitySize SLEEPING_SIZE = EntitySize.fixed(0.2F, 0.2F);
    public static final EntitySize SWIMMING_SIZE = EntitySize.flexible(0.6F, 0.6F);
    public static final EntitySize CROUCHING_SIZE = EntitySize.flexible(0.6F, 1.5F);

    private AquaPoseLogic() {}

    public static EntitySize getSize(Pose pose) {
        switch (pose) {
            case SLEEPING:
            case DYING:
                return SLEEPING_SIZE;
            case FALL_FLYING:
            case SWIMMING:
            case SPIN_ATTACK:
                return SWIMMING_SIZE;
            case CROUCHING:
                return CROUCHING_SIZE;
            default:
                return STANDING_SIZE;
        }
    }

    public static Pose choosePose(EntityPlayer player, IPlayerResizeable resizeable) {
        if (resizeable.getShouldBeDead()) return Pose.DYING;
        if (player.isPlayerSleeping()) return Pose.SLEEPING;

        boolean swimmingClear = isPoseClear(player, resizeable, Pose.SWIMMING);
        boolean crouchingClear = isPoseClear(player, resizeable, Pose.CROUCHING);
        boolean standingClear = isPoseClear(player, resizeable, Pose.STANDING);
        if (EFRIntegration.isElytraFlying(player)) return Pose.FALL_FLYING;
        if (resizeable.isForcingCrawling() || resizeable.isSwimming()) return Pose.SWIMMING;
        if (!standingClear) {
            if (canUseCrouchPose(player) && crouchingClear) return Pose.CROUCHING;
            if (ConfigHandler.MovementConfig.enableCrawling && swimmingClear) return Pose.SWIMMING;
            return resizeable.getPose();
        }
        if (resizeable.isActuallySneaking() && canUseCrouchPose(player) && crouchingClear) return Pose.CROUCHING;
        return Pose.STANDING;
    }

    /** Applies the existing post-player-tick pose boundary. */
    public static void updatePose(EntityPlayer player, IPlayerResizeable resizeable) {
        Pose pose = choosePose(player, resizeable);
        if (player.worldObj.isRemote) {
            player.yOffset = pose == Pose.SWIMMING ? 0.28F : 1.62F;
        }
        resizeable.setPose(pose);

        // Preserve Phase 2A's two-step ordering. setPose can synchronously notify
        // the client DataWatcher path, which recalculates size/eye bookkeeping.
        // Re-read the resulting pose only after that mutation has completed rather
        // than carrying the pre-notification local value into camera state.
        updateEyeHeight(player, resizeable);
    }

    private static void updateEyeHeight(EntityPlayer player, IPlayerResizeable resizeable) {
        AquaPlayerState state = resizeable.getAquaPlayerState();
        if (player.eyeHeight != state.previousEyeHeight) {
            Pose pose = resizeable.getPose();
            // Preserve the Phase 2A post-DataWatcher getSize evaluation (including
            // its existing DYING-size Mixin special case) before eye bookkeeping.
            resizeable.getSize(pose);
            state.playerEyeHeight = getEyeHeight(pose, player.eyeHeight, resizeable.isResizingAllowed());
            state.previousEyeHeight = player.eyeHeight;
        }
    }

    public static boolean canUseCrouchPose(EntityPlayer player) {
        return !player.capabilities.isFlying && (player.onGround || !player.isInWater()) && !player.isOnLadder();
    }

    public static boolean isPoseClear(EntityPlayer player, IPlayerResizeable resizeable, Pose pose) {
        return player.worldObj.getCollidingBoundingBoxes(player, getBoundingBox(player, resizeable, pose)).isEmpty();
    }

    public static AxisAlignedBB getBoundingBox(EntityPlayer player, IPlayerResizeable resizeable, Pose pose) {
        EntitySize size = resizeable.getSize(pose);
        float halfWidth = size.width / 2.0F;
        return AxisAlignedBB.getBoundingBox(
            player.posX - halfWidth,
            player.boundingBox.minY,
            player.posZ - halfWidth,
            player.posX + halfWidth,
            player.boundingBox.minY + size.height,
            player.posZ + halfWidth);
    }

    public static float getEyeHeight(Pose pose, float vanillaEyeHeight, boolean resizingAllowed) {
        if (pose == Pose.SLEEPING || pose == Pose.DYING) return 0.2F;
        switch (pose) {
            case SWIMMING:
            case FALL_FLYING:
            case SPIN_ATTACK:
                return vanillaEyeHeight;
            case CROUCHING:
                return resizingAllowed ? 0.35F : 0.08F;
            default:
                return vanillaEyeHeight;
        }
    }
}
