package com.enovak.lotrmoremobs.network;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MumakilOpenGuiPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<MumakilOpenGuiPacket, IMessage> {
        private final Queue<EntityPlayerMP> pendingPlayers = new ConcurrentLinkedQueue<EntityPlayerMP>();

        public Handler() {
            FMLCommonHandler.instance().bus().register(this);
        }

        @Override
        public IMessage onMessage(MumakilOpenGuiPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;

            if (player != null) {
                this.pendingPlayers.offer(player);
            }

            return null;
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) {
                return;
            }

            EntityPlayerMP player;
            while ((player = this.pendingPlayers.poll()) != null) {
                if (player.isDead
                        || !player.isEntityAlive()
                        || player.worldObj == null
                        || player.worldObj.isRemote
                        || player.worldObj.getEntityByID(player.getEntityId()) != player
                        || !(player.ridingEntity instanceof LOTREntityMumakil)) {
                    continue;
                }

                LOTREntityMumakil mumakil = (LOTREntityMumakil) player.ridingEntity;
                if (mumakil.isDead
                        || !mumakil.isEntityAlive()
                        || mumakil.worldObj != player.worldObj
                        || mumakil.worldObj.getEntityByID(mumakil.getEntityId()) != mumakil
                        || !mumakil.canPlayerUseMumakilInventory(player)) {
                    continue;
                }

                System.out.println("[LOTRMoreMobs] Server received Mumakil open GUI packet. Opening GUI.");

                mumakil.openGUI(player);
            }
        }
    }
}
