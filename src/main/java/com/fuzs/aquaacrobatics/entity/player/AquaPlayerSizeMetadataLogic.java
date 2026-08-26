package com.fuzs.aquaacrobatics.entity.player;

import net.minecraft.entity.player.EntityPlayer;

import com.fuzs.aquaacrobatics.entity.EntitySize;
import com.fuzs.aquaacrobatics.entity.Pose;

/** Shared size metadata semantics used by the ASM-owned EntityPlayer facades. */
public final class AquaPlayerSizeMetadataLogic {

    private AquaPlayerSizeMetadataLogic() {}

    /**
     * Preserves EntityPlayerMixin's constructor-return order: size first, then
     * the derived eye height whose resize gate reads that size through getWidth/Height.
     */
    public static void initialize(EntityPlayer player) {
        AquaPlayerState state = ((IAquaPlayerStateHolder) player).getAquaPlayerState();
        state.size = AquaPoseLogic.STANDING_SIZE;
        state.playerEyeHeight = getEyeHeight(player, Pose.STANDING, state.size);
    }

    public static float getEyeHeight(EntityPlayer player, Pose pose, EntitySize size) {
        return AquaPoseLogic.getEyeHeight(
            pose,
            player.eyeHeight,
            ((IPlayerResizeable) player).isResizingAllowed());
    }

    /** Preserves the former getSize body plus its inseparable DYING Mixin override. */
    public static EntitySize getSize(Pose pose) {
        if (pose == Pose.DYING) return new EntitySize(0.6F, 1.8F, false);
        return AquaPoseLogic.getSize(pose);
    }
}
