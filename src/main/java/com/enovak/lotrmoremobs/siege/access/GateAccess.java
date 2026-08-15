package com.enovak.lotrmoremobs.siege.access;

import com.enovak.lotrmoremobs.siege.tile.TileEntitySiegeGate;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;

public final class GateAccess {

    private GateAccess() {
    }

    public static boolean isAdministrativePlayer(EntityPlayerMP player) {
        return player != null
                && (player.capabilities.isCreativeMode
                || player.canCommandSenderUseCommand(2, "siegegate"));
    }

    public static void deny(
            EntityPlayerMP player,
            TileEntitySiegeGate gate
    ) {
        if (player == null || gate == null || gate.getWorldObj() == null) {
            return;
        }
        player.addChatMessage(new ChatComponentText(
                "You are not authorized to operate "
                        + gate.getGateName()
                        + "."
        ));
        if (gate.getWorldObj() instanceof WorldServer) {
            ((WorldServer)gate.getWorldObj()).func_147487_a(
                    "reddust",
                    gate.xCoord + 0.5D,
                    gate.yCoord + 1.0D,
                    gate.zCoord + 0.5D,
                    12,
                    0.45D,
                    0.65D,
                    0.45D,
                    0.0D
            );
        }
    }
}
