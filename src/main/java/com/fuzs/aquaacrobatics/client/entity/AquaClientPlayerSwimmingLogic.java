package com.fuzs.aquaacrobatics.client.entity;

import com.enovak.lotrmoremobs.config.PlayerMovementMode;
import net.minecraft.client.entity.EntityClientPlayerMP;

import com.fuzs.aquaacrobatics.config.ConfigHandler;
import com.fuzs.aquaacrobatics.entity.Pose;
import com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable;

public final class AquaClientPlayerSwimmingLogic {
    private AquaClientPlayerSwimmingLogic() {}
    public static boolean isActuallySneaking(EntityClientPlayerMP player) { return player.isSneaking(); }
    public static boolean isForcedDown(EntityClientPlayerMP player) { return PlayerMovementMode.useModernPlayerMovement(player) && ((IPlayerResizeable) player).isResizingAllowed() && !player.capabilities.isFlying && (((IPlayerResizeable) player).getPose() == Pose.CROUCHING || ((IPlayerResizeable) player).isVisuallySwimming()); }
    public static boolean isUsingSwimmingAnimation(EntityClientPlayerMP player) { return PlayerMovementMode.useModernPlayerMovement(player) && ((IPlayerSPSwimming) player).isUsingSwimmingAnimation(player.movementInput.moveForward, player.movementInput.moveStrafe); }
    public static boolean isUsingSwimmingAnimation(EntityClientPlayerMP player, float forward, float strafe) { if (!PlayerMovementMode.useModernPlayerMovement(player)) return false; IPlayerSPSwimming swimming = (IPlayerSPSwimming) player; return swimming.canSwim() ? swimming.isMovingForward(forward, strafe) : (ConfigHandler.MovementConfig.sidewaysSprinting ? forward >= 0.8F || Math.abs(strafe) > 0.8F : forward >= 0.8F); }
    public static boolean canSwim(EntityClientPlayerMP player) { return PlayerMovementMode.useModernPlayerMovement(player) && ((IPlayerResizeable) player).getEyesInWaterPlayer(); }
    public static boolean isMovingForward(EntityClientPlayerMP player, float forward, float strafe) { return PlayerMovementMode.useModernPlayerMovement(player) && (forward > 1.0E-5F || ConfigHandler.MovementConfig.sidewaysSwimming && Math.abs(strafe) > 1.0E-5F); }
    public static boolean canPerformElytraTakeoff(EntityClientPlayerMP player) { return false; }
}
