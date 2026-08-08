package com.enovak.lotrmoremobs.network;

import com.enovak.lotrmoremobs.client.pickupfilter.ClientPickupFilterState;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client packet containing the player's complete pickup-filter list.
 *
 * The server remains authoritative; this only updates the client's cached
 * copy for display in the future GUI.
 */
public class PickupFilterSyncPacket implements IMessage {

    private List<ItemStack> excludedItems =
            new ArrayList<ItemStack>();

    public PickupFilterSyncPacket() {
    }

    public PickupFilterSyncPacket(List<ItemStack> excludedItems) {
        if (excludedItems == null) {
            return;
        }

        for (ItemStack stack : excludedItems) {
            if (stack != null) {
                ItemStack copy = stack.copy();
                copy.stackSize = 1;
                this.excludedItems.add(copy);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        excludedItems.clear();

        int count = buf.readInt();

        for (int i = 0; i < count; ++i) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);

            if (stack != null) {
                stack.stackSize = 1;
                excludedItems.add(stack);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(excludedItems.size());

        for (ItemStack stack : excludedItems) {
            ByteBufUtils.writeItemStack(buf, stack);
        }
    }

    public static class Handler
            implements IMessageHandler<PickupFilterSyncPacket, IMessage> {

        @Override
        public IMessage onMessage(
                PickupFilterSyncPacket message,
                MessageContext ctx
        ) {
            ClientPickupFilterState.setExcludedItems(
                    message.excludedItems
            );

            return null;
        }
    }
}