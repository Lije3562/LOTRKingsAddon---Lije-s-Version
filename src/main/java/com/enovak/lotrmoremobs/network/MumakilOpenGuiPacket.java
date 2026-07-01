package com.enovak.lotrmoremobs.network;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class MumakilOpenGuiPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<MumakilOpenGuiPacket, IMessage> {
        @Override
        public IMessage onMessage(MumakilOpenGuiPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            if (player != null && player.ridingEntity instanceof LOTREntityMumakil) {
                LOTREntityMumakil mumakil = (LOTREntityMumakil) player.ridingEntity;

                System.out.println("[LOTRMoreMobs] Server received Mumakil open GUI packet. Opening GUI.");

                mumakil.openGUI(player);
            }

            return null;
        }
    }
}