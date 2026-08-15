package com.enovak.lotrmoremobs.siege.creation;

import com.enovak.lotrmoremobs.siege.SiegeRegistry;
import lotr.common.block.LOTRBlockGateDwarvenIthildin;
import lotr.common.tileentity.LOTRTileEntityDwarvenDoor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public final class GateSourceBlockValidator {

    private GateSourceBlockValidator() {
    }

    public static boolean isValid(World world, int x, int y, int z) {
        if (world == null || !world.blockExists(x, y, z)) {
            return false;
        }

        Block block = world.getBlock(x, y, z);
        int metadata = world.getBlockMetadata(x, y, z);

        return isValidDefinition(world, x, y, z, block, metadata)
                && hasSupportedNewSourceTileEntity(
                world,
                x,
                y,
                z,
                block,
                metadata
        );
    }

    static boolean matchesSelection(
            World world,
            GateSelectionData selection
    ) {
        if (world == null || selection == null) {
            return false;
        }
        GateBlockPosition position = selection.getPosition();
        if (!isValid(
                world,
                position.getX(),
                position.getY(),
                position.getZ()
        )) {
            return false;
        }
        return world.getBlock(
                position.getX(),
                position.getY(),
                position.getZ()
        ) == selection.getSourceBlock()
                && world.getBlockMetadata(
                        position.getX(),
                        position.getY(),
                        position.getZ()
                ) == selection.getSourceMetadata();
    }

    public static boolean isValidDefinition(
            World world,
            int x,
            int y,
            int z,
            Block block,
            int metadata
    ) {
        if (world == null
                || block == null
                || block == Blocks.air
                || block == Blocks.tnt
                || block == SiegeRegistry.gateController
                || block == SiegeRegistry.gatePart
                || block instanceof BlockFalling) {
            return false;
        }

        try {
            /*
             * TileEntity admission is deliberately NOT decided here. This
             * definition check is also used by durable restoration/recovery
             * for legacy saved gates, so tightening it would make old worlds
             * unrecoverable. New source selection is restricted separately by
             * hasSupportedNewSourceTileEntity().
             */

            /*
             * Gate source blocks become inert appearance data after finalization.
             *
             * Their original movement, redstone, activation, ticking, collision,
             * and other gameplay behavior will not survive conversion to GateParts,
             * so those properties are not reasons to reject them here.
             */
            if (block.getBlockHardness(world, x, y, z) < 0.0F) {
                return false;
            }

            /*
             * Deliberately do NOT require:
             *
             *   renderAsNormalBlock()
             *   render type 0
             *   a full/non-null collision box
             *
             * Those old restrictions excluded native metadata-driven geometry
             * such as fences, fence gates, stairs, slabs, panes, walls, doors,
             * trapdoors, and compatible modded equivalents.
             *
             * Once accepted into a finalized siege gate, this source block is
             * appearance data only. The physical world contains a GatePart, so
             * the source block itself cannot tick, activate, provide collision,
             * run redstone logic, or otherwise retain normal functionality.
             */
            return getRegisteredName(block) != null;

        } catch (RuntimeException ignored) {
            return false;
        }
    }

    /**
     * New gate selections reject generic TileEntities. The sole exception is
     * LOTR's Ithildin dwarven-door block paired with its exact visual TE.
     *
     * This is intentionally separate from isValidDefinition(), which remains
     * compatible with server-side restoration of legacy saved gate sources.
     */
    private static boolean hasSupportedNewSourceTileEntity(
            World world,
            int x,
            int y,
            int z,
            Block block,
            int metadata
    ) {
        try {
            boolean declaresTileEntity = block.hasTileEntity(metadata);
            TileEntity tileEntity = world.getTileEntity(x, y, z);

            if (!declaresTileEntity && tileEntity == null) {
                return true;
            }

            if (!(block instanceof LOTRBlockGateDwarvenIthildin)) {
                return false;
            }

            return declaresTileEntity
                    && tileEntity instanceof LOTRTileEntityDwarvenDoor;

        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static String getRegisteredName(Block block) {
        if (block == null || block == Blocks.air) {
            return null;
        }
        String registeredName =
                Block.blockRegistry.getNameForObject(block);
        return registeredName == null || registeredName.isEmpty()
                ? null
                : registeredName;
    }
}
