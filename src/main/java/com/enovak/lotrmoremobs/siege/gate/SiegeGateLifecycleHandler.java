package com.enovak.lotrmoremobs.siege.gate;

import com.enovak.lotrmoremobs.config.MumakilConfig;
import com.enovak.lotrmoremobs.siege.SiegeRegistry;
import com.enovak.lotrmoremobs.siege.banner.SiegeGateBannerAttachmentData;
import com.enovak.lotrmoremobs.siege.creation.GateCreationManager;
import com.enovak.lotrmoremobs.siege.repair.GateManagementManager;
import com.enovak.lotrmoremobs.siege.tile.TileEntitySiegeGate;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

/** Server transaction reconciliation and player-removal protection. */
public final class SiegeGateLifecycleHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.world == null || event.world.isRemote) {
            return;
        }
        if (event.block == SiegeRegistry.gatePart) {
            EntityPlayer player = event.getPlayer();

            /*
             * Finalized GateParts are normally protected from direct player
             * removal, but Creative mode intentionally supports surgical
             * structure editing. Do not cancel the Forge break event here;
             * BlockSiegeGatePart.removedByPlayer(...) owns the authoritative
             * server-side transaction and will reject unsafe/non-closed edits.
             */
            if (player != null
                    && player.capabilities.isCreativeMode) {
                return;
            }

            event.setCanceled(true);
            sendMessage(
                    player,
                    "GateParts can only be removed through their controller."
            );
            return;
        }
        if (event.block != SiegeRegistry.gateController) {
            return;
        }

        TileEntity tileEntity = event.world.getTileEntity(
                event.x,
                event.y,
                event.z
        );
        if (!(tileEntity instanceof TileEntitySiegeGate)) {
            if (GateRegistry.hasDurableController(
                    event.world,
                    event.x,
                    event.y,
                    event.z
            )) {
                event.setCanceled(true);
                sendMessage(
                        event.getPlayer(),
                        "This controller has durable gate ownership but no "
                                + "valid controller data; manual recovery is required."
                );
            }
            return;
        }

        TileEntitySiegeGate gate = (TileEntitySiegeGate)tileEntity;
        boolean protectedGate = gate.isFinalized()
                || gate.isGateStructureQuarantined()
                || GateRegistry.hasDurableController(
                        event.world,
                        event.x,
                        event.y,
                        event.z
                );
        if (!protectedGate) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        if (gate.isGateStructureQuarantined()) {
            event.setCanceled(true);
            sendMessage(
                    player,
                    "This quarantined controller requires manual recovery "
                            + "and cannot be dismantled automatically."
            );
            return;
        }
        if (!(player instanceof EntityPlayerMP)
                || !gate.canDismantle((EntityPlayerMP)player)) {
            event.setCanceled(true);
            sendMessage(
                    player,
                    "Only the gate owner or an administrator may dismantle "
                            + gate.getGateName() + "."
            );
            return;
        }
        if (!gate.canPrepareControllerRemovalTransaction()) {
            event.setCanceled(true);
            sendMessage(
                    player,
                    "The durable gate-removal journal is unavailable or full; "
                            + "the controller was preserved."
            );
        }
    }

    /**
     * Sneak-right-click keeps block placement available while making the gate
     * GUI accessible with an empty hand or a non-block item.
     *
     * Forge normally gives a sneaking held item first chance to use itself,
     * which means Block#onBlockActivated may never run for items such as
     * weapons or tools. Intercept the server-side interaction event so those
     * non-placeable clicks still open the appropriate gate GUI.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.world == null
                || event.world.isRemote
                || event.action != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK
                || !MumakilConfig.enableSiegeGates) {
            return;
        }

        EntityPlayer player = event.entityPlayer;
        if (!(player instanceof EntityPlayerMP)
                || !player.isSneaking()) {
            return;
        }

        /*
         * A held block keeps normal sneak-placement semantics. Everything
         * else -- including an empty hand -- is treated as a GUI request.
         */
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof ItemBlock) {
            return;
        }

        TileEntitySiegeGate controller = null;

        if (event.world.getBlock(event.x, event.y, event.z)
                == SiegeRegistry.gateController) {

            TileEntity tileEntity = event.world.getTileEntity(
                    event.x,
                    event.y,
                    event.z
            );
            if (tileEntity instanceof TileEntitySiegeGate) {
                controller = (TileEntitySiegeGate)tileEntity;
            }

        } else if (event.world.getBlock(event.x, event.y, event.z)
                == SiegeRegistry.gatePart) {

            controller = GateRegistry.getController(
                    event.world,
                    event.x,
                    event.y,
                    event.z
            );
        } else {
            return;
        }

        if (controller == null) {
            return;
        }

        event.setCanceled(true);

        EntityPlayerMP serverPlayer = (EntityPlayerMP)player;
        if (controller.isFinalized()) {
            GateManagementManager.open(serverPlayer, controller);
        } else {
            GateCreationManager.openControls(serverPlayer, controller);
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        World world = event.world;
        if (world == null) {
            return;
        }
        GateRegistry.notifyPartChunkAvailabilityChanged(
                world,
                event.getChunk().xPosition,
                event.getChunk().zPosition
        );
        if (!world.isRemote) {
            SiegeGateOwnershipData data =
                    SiegeGateOwnershipData.get(world, false);
            if (data != null) {
                data.onChunkLoaded(
                        event.getChunk().xPosition,
                        event.getChunk().zPosition
                );
            }
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        GateRegistry.notifyPartChunkAvailabilityChanged(
                event.world,
                event.getChunk().xPosition,
                event.getChunk().zPosition
        );
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        if (event.world != null && !event.world.isRemote) {
            SiegeGateOwnershipData.get(event.world, false);
            SiegeGateBannerAttachmentData.get(event.world, false);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        SiegeGateOwnershipData data = event.world == null
                || event.world.isRemote
                ? null
                : SiegeGateOwnershipData.get(event.world, false);
        if (data != null) {
            data.clearTransientQueue();
        }
        GateRegistry.clearWorld(event.world);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.world == null
                || event.world.isRemote) {
            return;
        }
        SiegeGateOwnershipData data =
                SiegeGateOwnershipData.get(event.world, false);
        if (data != null) {
            data.processReconciliation(event.world);
            data.processEditCommitReconciliation(event.world);
        }

        /*
         * Banner restoration must run after the ordinary gate journal so a
         * native entity never respawns before its support block is back.
         */
        SiegeGateBannerAttachmentData.process(event.world);
    }

    private static void sendMessage(EntityPlayer player, String message) {
        if (player != null && message != null) {
            player.addChatMessage(new ChatComponentText(message));
        }
    }
}
