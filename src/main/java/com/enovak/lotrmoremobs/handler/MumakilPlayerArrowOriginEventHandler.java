package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/**
 * Moves only the spawn point of player-fired arrows to the Mumak rider
 * anchor. Bow charge, motion, damage, critical state, enchantments, pickup
 * state, and shooter ownership have already been assigned by the bow item.
 */
public final class MumakilPlayerArrowOriginEventHandler {
    private static final String ORIGIN_ADJUSTED_KEY =
            "lotrmoremobs_mumakRiderArrowOriginAdjusted";
    private static final double NORMAL_BOW_VERTICAL_OFFSET = -0.1D;
    private static final double INITIAL_FORWARD_OFFSET = 0.4D;
    private static final double CLEARANCE_STEP = 0.25D;
    private static final int MAX_CLEARANCE_STEPS = 6;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null
                || event.world == null
                || event.world.isRemote
                || !(event.entity instanceof EntityArrow)) {
            return;
        }

        EntityArrow arrow = (EntityArrow)event.entity;
        Entity shooter = arrow.shootingEntity;
        if (!(shooter instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer player = (EntityPlayer)shooter;
        if (!(player.ridingEntity instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil =
                (LOTREntityMumakil)player.ridingEntity;
        if (!mumakil.isTamedMumakilMode()) {
            return;
        }

        NBTTagCompound arrowData = arrow.getEntityData();
        if (arrowData.getBoolean(ORIGIN_ADJUSTED_KEY)) {
            return;
        }

        Vec3 riderAnchor =
                mumakil.getMumakilRiderAnchor(player);
        if (riderAnchor == null) {
            return;
        }

        /*
         * Build the origin from the visible rider anchor instead of retaining
         * the arrow constructor's offset from a possibly stale player
         * position. In particular, this avoids carrying its yaw-dependent
         * lateral component up from the ground-player position.
         */
        Vec3 look = player.getLookVec();
        if (look == null) {
            return;
        }

        double motionX = arrow.motionX;
        double motionY = arrow.motionY;
        double motionZ = arrow.motionZ;
        double baseY = riderAnchor.yCoord
                + player.getEyeHeight()
                + NORMAL_BOW_VERTICAL_OFFSET;
        Vec3 origin = findSafeOrigin(
                arrow,
                mumakil,
                riderAnchor.xCoord,
                baseY,
                riderAnchor.zCoord,
                look
        );

        arrow.setPosition(origin.xCoord, origin.yCoord, origin.zCoord);
        arrow.prevPosX = origin.xCoord;
        arrow.prevPosY = origin.yCoord;
        arrow.prevPosZ = origin.zCoord;
        arrow.lastTickPosX = origin.xCoord;
        arrow.lastTickPosY = origin.yCoord;
        arrow.lastTickPosZ = origin.zCoord;
        arrow.motionX = motionX;
        arrow.motionY = motionY;
        arrow.motionZ = motionZ;
        arrowData.setBoolean(ORIGIN_ADJUSTED_KEY, true);
    }

    private static Vec3 findSafeOrigin(
            EntityArrow arrow,
            LOTREntityMumakil mumakil,
            double anchorX,
            double eyeY,
            double anchorZ,
            Vec3 look
    ) {
        for (int step = 0; step <= MAX_CLEARANCE_STEPS; ++step) {
            double forward = INITIAL_FORWARD_OFFSET
                    + step * CLEARANCE_STEP;
            double x = anchorX + look.xCoord * forward;
            double y = eyeY + look.yCoord * forward;
            double z = anchorZ + look.zCoord * forward;
            AxisAlignedBB arrowBox = arrowBoxAt(arrow, x, y, z);

            if (!arrowBox.intersectsWith(mumakil.boundingBox)
                    && arrow.worldObj.getCollidingBoundingBoxes(
                    arrow,
                    arrowBox
            ).isEmpty()) {
                return Vec3.createVectorHelper(x, y, z);
            }
        }

        /*
         * Deterministic non-cancelling fallback: retain the intended look
         * offset, but lift only enough to clear the Mumak's own collision box.
         * The normal rider eye point is already above an adult or calf in
         * ordinary terrain, so this branch is reserved for unusual poses.
         */
        double fallbackX =
                anchorX + look.xCoord * INITIAL_FORWARD_OFFSET;
        double fallbackY = Math.max(
                eyeY + look.yCoord * INITIAL_FORWARD_OFFSET,
                mumakil.boundingBox.maxY + 0.1D
        );
        double fallbackZ =
                anchorZ + look.zCoord * INITIAL_FORWARD_OFFSET;
        return Vec3.createVectorHelper(fallbackX, fallbackY, fallbackZ);
    }

    private static AxisAlignedBB arrowBoxAt(
            EntityArrow arrow,
            double x,
            double y,
            double z
    ) {
        double halfWidth = arrow.width * 0.5D;
        return AxisAlignedBB.getBoundingBox(
                x - halfWidth,
                y,
                z - halfWidth,
                x + halfWidth,
                y + arrow.height,
                z + halfWidth
        );
    }
}
