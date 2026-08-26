package com.fuzs.aquaacrobatics.entity.player;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import com.fuzs.aquaacrobatics.entity.EntitySize;
import com.fuzs.aquaacrobatics.entity.Pose;
import com.fuzs.aquaacrobatics.integration.IntegrationManager;
import com.fuzs.aquaacrobatics.integration.morph.MorphIntegration;

/** Shared physical player-size and AABB policy; ASM provides only the public facades. */
public final class AquaPlayerResizeLogic {

    private AquaPlayerResizeLogic() {}

    public static void recalculateSize(EntityPlayer player, IPlayerResizeable resizeable) {
        EntitySize oldSize = resizeable.getAquaPlayerState().size;
        Pose pose = resizeable.getPose();
        EntitySize newSize = resizeable.getSize(pose);
        if (resizeable.isResizingAllowed()) {
            recalculateSize(player, oldSize, newSize);
            player.width = newSize.width;
            player.height = newSize.height;
        }

        // The resize gate intentionally observes the old size through getWidth/getHeight.
        resizeable.getAquaPlayerState().size = newSize;
    }

    private static void recalculateSize(EntityPlayer player, EntitySize oldSize, EntitySize newSize) {
        if (newSize.width < oldSize.width) {
            double d0 = (double) newSize.width / 2.0;
            player.boundingBox.setBB(
                AxisAlignedBB.getBoundingBox(
                    player.posX - d0,
                    player.posY,
                    player.posZ - d0,
                    player.posX + d0,
                    player.posY + (double) newSize.height,
                    player.posZ + d0));
        } else {
            AxisAlignedBB axisalignedbb = player.boundingBox;
            player.boundingBox.setBB(
                AxisAlignedBB.getBoundingBox(
                    axisalignedbb.minX,
                    axisalignedbb.minY,
                    axisalignedbb.minZ,
                    axisalignedbb.minX + (double) newSize.width,
                    axisalignedbb.minY + (double) newSize.height,
                    axisalignedbb.minZ + (double) newSize.width));
            if (newSize.width > oldSize.width && !player.firstUpdate && !player.worldObj.isRemote) {
                float distance = oldSize.width - newSize.width;
                player.moveEntity(distance, 0.0, distance);
            }
        }
    }

    public static boolean isResizingAllowed(EntityPlayer player, IPlayerResizeable resizeable) {
        if (IntegrationManager.isMorphEnabled() && MorphIntegration.isMorphing(player)) return false;

        // Is another mod interfering?
        final float delta = 0.025F;
        AxisAlignedBB bb = player.boundingBox;
        // Something is not right.
        if (player.width < delta || player.height < delta || bb.maxX - bb.minX < delta || bb.maxY - bb.minY < delta) {
            return true;
        }

        boolean sizeIsOk = Math.abs(player.width / resizeable.getWidth() - 1.0F) < delta
            && Math.abs(player.height / resizeable.getHeight() - 1.0F) < delta;
        boolean boundingBoxIsOk = Math.abs((bb.maxX - bb.minX) / resizeable.getWidth() - 1.0F) < delta
            && Math.abs((bb.maxY - bb.minY) / resizeable.getHeight() - 1.0F) < delta;
        return sizeIsOk && boundingBoxIsOk;
    }
}
