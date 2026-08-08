package com.enovak.lotrmoremobs.network;

import com.enovak.lotrmoremobs.pickupfilter.PickupFilterNetwork;
import com.enovak.lotrmoremobs.pickupfilter.PlayerPickupFilterData;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

/**
 * Client -> server request to toggle one item in the player's pickup filter.
 *
 * The server remains authoritative and sends the updated complete filter
 * back to the client after applying the change.
 */
public class PickupFilterTogglePacket implements IMessage {

    private ItemStack stack;

    public PickupFilterTogglePacket() {
    }

    public PickupFilterTogglePacket(ItemStack stack) {
        if (stack != null) {
            this.stack = stack.copy();
            this.stack.stackSize = 1;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stack = ByteBufUtils.readItemStack(buf);

        if (stack != null) {
            stack.stackSize = 1;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeItemStack(buf, stack);
    }

    public static class Handler
            implements IMessageHandler<PickupFilterTogglePacket, IMessage> {

        @Override
        public IMessage onMessage(
                PickupFilterTogglePacket message,
                MessageContext ctx
        ) {
            if (message.stack == null) {
                return null;
            }

            EntityPlayerMP player =
                    ctx.getServerHandler().playerEntity;

            if (player == null) {
                return null;
            }

            if (PlayerPickupFilterData.isExcluded(
                    player,
                    message.stack
            )) {
                PlayerPickupFilterData.removeExcludedItem(
                        player,
                        message.stack
                );
            } else {
                PlayerPickupFilterData.addExcludedItem(
                        player,
                        message.stack
                );
            }

            PickupFilterNetwork.syncToPlayer(player);

            return null;
        }
    }
}