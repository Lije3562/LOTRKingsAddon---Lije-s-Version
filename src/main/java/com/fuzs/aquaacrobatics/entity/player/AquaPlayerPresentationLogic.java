package com.fuzs.aquaacrobatics.entity.player;

import com.fuzs.aquaacrobatics.entity.Pose;
import com.fuzs.aquaacrobatics.util.math.MathHelperNew;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;

/** Shared pre-post-tick player swim presentation bookkeeping. */
public final class AquaPlayerPresentationLogic {

    private AquaPlayerPresentationLogic() {}

    public static void updatePrePostTick(EntityPlayer player) {
        updateSwimAnimation(player);
        updateEyesInWaterPlayer(player);
    }

    /** Phase 2K: shared read-only presentation API used by the ASM-owned EntityPlayer facades. */
    public static boolean getEyesInWaterPlayer(EntityPlayer player) {
        return ((IAquaPlayerStateHolder) player).getAquaPlayerState().eyesInWaterPlayer;
    }

    /** Preserves the accepted previous-to-current partial-tick interpolation direction. */
    public static float getSwimAnimation(EntityPlayer player, float partialTicks) {
        AquaPlayerState state = ((IAquaPlayerStateHolder) player).getAquaPlayerState();
        return MathHelperNew.lerp(partialTicks, state.lastSwimAnimation, state.swimAnimation);
    }

    public static boolean isActuallySwimming(EntityPlayer player) {
        Pose pose = ((IPlayerResizeable) player).getPose();
        return pose == Pose.SWIMMING || pose == Pose.FALL_FLYING;
    }

    /** Preserves virtual isActuallySwimming dispatch before consulting vanilla water state. */
    public static boolean isVisuallySwimming(EntityPlayer player) {
        return ((IPlayerResizeable) player).isActuallySwimming() && !player.isInWater();
    }

    private static void updateSwimAnimation(EntityPlayer player) {
        AquaPlayerState state = ((IAquaPlayerStateHolder) player).getAquaPlayerState();
        state.lastSwimAnimation = state.swimAnimation;
        if (((IPlayerResizeable) player).isActuallySwimming()) {
            state.swimAnimation = Math.min(1.0F, state.swimAnimation + 0.09F);
        } else {
            state.swimAnimation = Math.max(0.0F, state.swimAnimation - 0.09F);
        }
    }

    private static void updateEyesInWaterPlayer(EntityPlayer player) {
        ((IAquaPlayerStateHolder) player).getAquaPlayerState().eyesInWaterPlayer = player
            .isInsideOfMaterial(Material.water);
    }
}
