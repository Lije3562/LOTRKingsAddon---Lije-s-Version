package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.pickupfilter.PickupFilterNetwork;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import net.minecraft.entity.player.EntityPlayerMP;

/**
 * Keeps the client's pickup-filter cache synchronized when the player's
 * server-side player entity is created or moved between worlds.
 */
public class PickupFilterConnectionHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.player);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(
            PlayerEvent.PlayerChangedDimensionEvent event
    ) {
        sync(event.player);
    }

    private void sync(net.minecraft.entity.player.EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            PickupFilterNetwork.syncToPlayer(
                    (EntityPlayerMP) player
            );
        }
    }
}