package com.enovak.lotrmoremobs.client;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.Timer;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/** Client-side companion for the temporary mounted-arrow origin log. */
@SideOnly(Side.CLIENT)
public final class MumakilPlayerArrowCameraDiagnosticHandler {
    private static final double POSITION_EPSILON = 1.0E-7D;

    @SubscribeEvent(
            priority = EventPriority.LOWEST,
            receiveCanceled = true
    )
    public void logFirstPersonCameraSource(
            EntityJoinWorldEvent event
    ) {
        if (!LOTREntityMumakil.isPlayerSeatDiagnosticsEnabled()) {
            return;
        }

        if (event == null
                || event.world == null
                || !event.world.isRemote
                || !(event.entity instanceof EntityArrow)) {
            return;
        }

        EntityArrow arrow = (EntityArrow)event.entity;
        if (!(arrow.shootingEntity instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player =
                (EntityPlayer)arrow.shootingEntity;
        if (!(player.ridingEntity instanceof LOTREntityMumakil)) {
            return;
        }
        LOTREntityMumakil parentMumak =
                (LOTREntityMumakil)player.ridingEntity;
        Vec3 seatAnchor = parentMumak.calculatePlayerSeatPosition(
                player
        );

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer != player
                || minecraft.renderViewEntity == null) {
            return;
        }

        Entity cameraEntity = minecraft.renderViewEntity;
        float partialTicks = getRenderPartialTicks(minecraft);
        double cameraX = interpolate(
                cameraEntity.prevPosX,
                cameraEntity.posX,
                partialTicks
        );
        double cameraY = interpolate(
                cameraEntity.prevPosY,
                cameraEntity.posY,
                partialTicks
        ) - (cameraEntity.yOffset - 1.62F);
        double cameraZ = interpolate(
                cameraEntity.prevPosZ,
                cameraEntity.posZ,
                partialTicks
        );
        double interpolatedPlayerEyeX = interpolate(
                player.prevPosX,
                player.posX,
                partialTicks
        );
        double interpolatedPlayerEyeY = interpolate(
                player.prevPosY,
                player.posY,
                partialTicks
        ) + player.getEyeHeight();
        double interpolatedPlayerEyeZ = interpolate(
                player.prevPosZ,
                player.posZ,
                partialTicks
        );
        boolean firstPerson =
                minecraft.gameSettings.thirdPersonView == 0;
        boolean cameraUsesPlayer = cameraEntity == player;
        boolean cameraMatchesRequestedPlayerEye =
                positionsMatch(
                        cameraX,
                        cameraY,
                        cameraZ,
                        interpolatedPlayerEyeX,
                        interpolatedPlayerEyeY,
                        interpolatedPlayerEyeZ
                );

        System.out.println(
                "[LOTRMoreMobs][MumakPlayerSeat]"
                        + " reason=ARROW_SPAWN_CAMERA"
                        + " worldTick="
                        + event.world.getTotalWorldTime()
                        + " logicalSide=CLIENT"
                        + " playerEntityId=" + player.getEntityId()
                        + " mumakEntityId="
                        + parentMumak.getEntityId()
                        + " mumakPos=" + formatPosition(
                        parentMumak.posX,
                        parentMumak.posY,
                        parentMumak.posZ
                )
                        + " mumakPrevPos=" + formatPosition(
                        parentMumak.prevPosX,
                        parentMumak.prevPosY,
                        parentMumak.prevPosZ
                )
                        + " mumakRotationYaw="
                        + parentMumak.rotationYaw
                        + " mumakRenderYawOffset="
                        + parentMumak.renderYawOffset
                        + " playerViewYaw=" + player.rotationYaw
                        + " arrowEntityId=" + arrow.getEntityId()
                        + " shootingEntityClass="
                        + player.getClass().getName()
                        + " shootingEntityId="
                        + player.getEntityId()
                        + " playerPos="
                        + formatPosition(
                        player.posX,
                        player.posY,
                        player.posZ
                )
                        + " playerPrevPos="
                        + formatPosition(
                        player.prevPosX,
                        player.prevPosY,
                        player.prevPosZ
                )
                        + " playerEyePos="
                        + formatPosition(
                        player.posX,
                        player.posY + player.getEyeHeight(),
                        player.posZ
                )
                        + " renderedFirstPersonCameraPos="
                        + formatPosition(cameraX, cameraY, cameraZ)
                        + " renderPartialTicks=" + partialTicks
                        + " interpolatedPlayerEyePos="
                        + formatPosition(
                        interpolatedPlayerEyeX,
                        interpolatedPlayerEyeY,
                        interpolatedPlayerEyeZ
                )
                        + " arrowClientPos="
                        + formatPosition(
                        arrow.posX,
                        arrow.posY,
                        arrow.posZ
                )
                        + " calculatedSeatAnchor="
                        + formatPosition(
                        seatAnchor.xCoord,
                        seatAnchor.yCoord,
                        seatAnchor.zCoord
                )
                        + " playerMinusSeatAnchor="
                        + formatPosition(
                        player.posX - seatAnchor.xCoord,
                        player.posY - seatAnchor.yCoord,
                        player.posZ - seatAnchor.zCoord
                )
                        + " arrowPosBeforeAddonHandling="
                        + formatPosition(
                        arrow.posX,
                        arrow.posY,
                        arrow.posZ
                )
                        + " arrowPosAfterAddonHandling="
                        + formatPosition(
                        arrow.posX,
                        arrow.posY,
                        arrow.posZ
                )
                        + " arrowChanged=false"
                        + " arrowPositionChangeCount=0"
                        + " addonPositionCorrectionPath=NONE"
                        + " firstPerson=" + firstPerson
                        + " cameraEntityClass="
                        + cameraEntity.getClass().getName()
                        + " cameraEntityId="
                        + cameraEntity.getEntityId()
                        + " cameraUsesPlayer=" + cameraUsesPlayer
                        + " cameraMatchesRequestedPlayerEye="
                        + cameraMatchesRequestedPlayerEye
                        + " parentMumakEntityId="
                        + parentMumak.getEntityId()
                        + " playerRidingEntityId="
                        + player.ridingEntity.getEntityId()
        );
    }

    private static double interpolate(
            double previous,
            double current,
            float partialTicks
    ) {
        return previous + (current - previous) * partialTicks;
    }

    private static float getRenderPartialTicks(
            Minecraft minecraft
    ) {
        try {
            Timer timer = ReflectionHelper.getPrivateValue(
                    Minecraft.class,
                    minecraft,
                    "timer",
                    "field_71428_T"
            );
            return timer == null ? 1.0F : timer.renderPartialTicks;
        } catch (RuntimeException ignored) {
            return 1.0F;
        }
    }

    private static boolean positionsMatch(
            double firstX,
            double firstY,
            double firstZ,
            double secondX,
            double secondY,
            double secondZ
    ) {
        return Math.abs(firstX - secondX) <= POSITION_EPSILON
                && Math.abs(firstY - secondY) <= POSITION_EPSILON
                && Math.abs(firstZ - secondZ) <= POSITION_EPSILON;
    }

    private static String formatPosition(
            double x,
            double y,
            double z
    ) {
        return "(" + x + "," + y + "," + z + ")";
    }
}
