package com.enovak.lotrmoremobs.spawning;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.config.MumakilConfig;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.animal.MumakilFormationOrigin;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.handler.MumakilHowdahArcherEventHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lotr.common.LOTRSpawnDamping;
import lotr.common.entity.LOTREntityInvasionSpawner;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.entity.npc.LOTREntitySouthronChampion;
import lotr.common.world.biome.LOTRBiome;
import lotr.common.world.spawning.LOTRSpawnerNPCs;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

/**
 * One server-side construction path for player-hired and autonomous Mumak war
 * formations. Autonomous construction is transactional: any failed member
 * spawn removes every member already created by that attempt.
 */
public final class MumakilWarFormationFactory {
    public static final int FORMATION_ARCHER_COUNT =
            LOTREntityMumakilHowdahArcher.getHowdahArcherSlotCount();
    public static final int FORMATION_NPC_SPAWN_COUNT =
            1 + FORMATION_ARCHER_COUNT;

    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;
    private static final int CLEARANCE_RADIUS = 4;
    private static final int CLEARANCE_HEIGHT = 16;
    private static final int GROUND_SAMPLE_RADIUS = 3;
    private MumakilWarFormationFactory() {
    }

    public static boolean initializeMount(
            LOTREntityMumakil mumakil,
            MumakilFormationOrigin origin
    ) {
        if (mumakil == null || origin == null || origin == MumakilFormationOrigin.NONE) {
            return false;
        }

        mumakil.setFormationOrigin(origin);
        mumakil.setBelongsToNPC(true);
        mumakil.setHiredWarMumakil(true);
        mumakil.setMountable(true);
        mumakil.setGrowingAge(0);
        mumakil.saddleMountForWorldGen();

        boolean saddleEquipped = setInventoryStack(
                mumakil,
                SADDLE_SLOT,
                new ItemStack(Items.saddle)
        );
        boolean howdahEquipped = setInventoryStack(
                mumakil,
                HOWDAH_SLOT,
                new ItemStack(Main.mumakilHowdah)
        );
        mumakil.setMumakilHowdahEquipped(howdahEquipped);

        if (mumakil.worldObj != null && !mumakil.worldObj.isRemote) {
            MumakilHowdahArcherEventHandler.markHiredHowdahArcherCarrier(mumakil);
        }

        return saddleEquipped && howdahEquipped;
    }

    public static boolean canNaturalFormationSpawnAt(
            World world,
            Entity excludedEntity,
            double x,
            double y,
            double z
    ) {
        if (world == null || world.isRemote) {
            return false;
        }

        int baseX = MathHelper.floor_double(x);
        int baseY = MathHelper.floor_double(y);
        int baseZ = MathHelper.floor_double(z);
        if (baseY < 1 || baseY + CLEARANCE_HEIGHT >= world.getActualHeight()) {
            return false;
        }

        int[][] groundOffsets = new int[][] {
                {0, 0},
                {-GROUND_SAMPLE_RADIUS, 0},
                {GROUND_SAMPLE_RADIUS, 0},
                {0, -GROUND_SAMPLE_RADIUS},
                {0, GROUND_SAMPLE_RADIUS}
        };
        for (int i = 0; i < groundOffsets.length; ++i) {
            int groundX = baseX + groundOffsets[i][0];
            int groundZ = baseZ + groundOffsets[i][1];
            if (!world.blockExists(groundX, baseY, groundZ)
                    || !World.doesBlockHaveSolidTopSurface(
                    world,
                    groundX,
                    baseY - 1,
                    groundZ
            )) {
                return false;
            }
        }

        /*
         * Validate the actual adult collision volume rather than the former
         * 11x11x29 solid-block sweep. The old sweep was almost twice the
         * Mumak's height and rejected harmless terrain well outside its body.
         */
        if (!world.blockExists(
                baseX - CLEARANCE_RADIUS,
                baseY + CLEARANCE_HEIGHT,
                baseZ - CLEARANCE_RADIUS
        ) || !world.blockExists(
                baseX + CLEARANCE_RADIUS,
                baseY + CLEARANCE_HEIGHT,
                baseZ + CLEARANCE_RADIUS
        )) {
            return false;
        }

        AxisAlignedBB mumakSpace = AxisAlignedBB.getBoundingBox(
                x - 3.5D,
                y,
                z - 3.5D,
                x + 3.5D,
                y + 15.5D,
                z + 3.5D
        );
        return world.getCollidingBoundingBoxes(
                excludedEntity,
                mumakSpace
        ).isEmpty()
                && !world.isAnyLiquid(mumakSpace)
                && world.getEntitiesWithinAABBExcludingEntity(
                excludedEntity,
                mumakSpace
        ).isEmpty();
    }

    public static boolean hasNaturalFormationSpawnCapacity(
            World world,
            Entity bootstrapArcher
    ) {
        if (world == null || world.isRemote) {
            return false;
        }

        Set<ChunkCoordIntPair> eligibleChunks =
                new HashSet<ChunkCoordIntPair>();
        LOTRSpawnerNPCs.getSpawnableChunks(world, eligibleChunks);
        int npcCap = LOTRSpawnDamping.getNPCSpawnCap(world)
                * eligibleChunks.size()
                / 196;
        int currentNpcCount = 0;

        List loadedEntities = world.loadedEntityList;
        for (int i = 0; i < loadedEntities.size(); ++i) {
            Object loaded = loadedEntities.get(i);
            if (loaded instanceof LOTREntityNPC) {
                currentNpcCount +=
                        ((LOTREntityNPC)loaded).getSpawnCountValue();
            }
        }

        boolean bootstrapAlreadyCounted =
                bootstrapArcher != null
                        && world.loadedEntityList.contains(bootstrapArcher);
        int formationSpawnCost = getAggregateNpcSpawnCount(
                world,
                bootstrapArcher == null ? 0.0D : bootstrapArcher.posX,
                bootstrapArcher == null ? 0.0D : bootstrapArcher.posZ
        );
        int bootstrapSpawnCost = bootstrapAlreadyCounted
                && bootstrapArcher instanceof LOTREntityNPC
                ? ((LOTREntityNPC)bootstrapArcher).getSpawnCountValue()
                : 0;
        int additionalNpcCost =
                formationSpawnCost - bootstrapSpawnCost;
        return currentNpcCount + additionalNpcCost <= npcCap;
    }

    public static int getAggregateNpcSpawnCount(
            World world,
            double x,
            double z
    ) {
        int multiplier = 1;
        if (world != null) {
            BiomeGenBase biome = world.getBiomeGenForCoords(
                    MathHelper.floor_double(x),
                    MathHelper.floor_double(z)
            );
            if (biome instanceof LOTRBiome) {
                multiplier = ((LOTRBiome)biome).spawnCountMultiplier();
            }
        }
        return FORMATION_NPC_SPAWN_COUNT * Math.max(1, multiplier);
    }

    public static boolean createNaturalFormation(
            LOTREntityMumakilHowdahArcher bootstrapArcher,
            boolean conquestSpawn
    ) {
        return createAutonomousFormation(
                bootstrapArcher == null
                        ? null
                        : bootstrapArcher.worldObj,
                bootstrapArcher,
                MumakilFormationOrigin.NATURAL_NEAR_HARAD,
                conquestSpawn,
                null,
                null,
                bootstrapArcher == null
                        ? 0.0D
                        : bootstrapArcher.posX,
                bootstrapArcher == null
                        ? 0.0D
                        : bootstrapArcher.posY,
                bootstrapArcher == null
                        ? 0.0D
                        : bootstrapArcher.posZ,
                bootstrapArcher == null
                        ? 0.0F
                        : bootstrapArcher.rotationYaw,
                bootstrapArcher,
                true,
                false,
                null
        );
    }

    public static boolean createInvasionFormation(
            LOTREntityMumakilHowdahArcher bootstrapArcher
    ) {
        LOTREntityInvasionSpawner spawner =
                MumakilInvasionFormationRegistry.getEligibleSpawner(
                        bootstrapArcher
                );
        if (spawner == null) {
            return false;
        }

        boolean created = createAutonomousFormation(
                bootstrapArcher.worldObj,
                bootstrapArcher,
                MumakilFormationOrigin.INVASION_NEAR_HARAD,
                false,
                bootstrapArcher.getInvasionID(),
                bootstrapArcher.killBonusFactions,
                bootstrapArcher.posX,
                bootstrapArcher.posY,
                bootstrapArcher.posZ,
                bootstrapArcher.rotationYaw,
                bootstrapArcher,
                true,
                false,
                null
        );
        if (created) {
            MumakilInvasionFormationRegistry.markFormationCreated(
                    spawner
            );
        }
        return created;
    }

    /**
     * Creates one persistent, autonomous Near Harad formation from the
     * dedicated creative spawn egg. The egg owner is used only as the
     * collision-validation exclusion; it is never recorded as an owner.
     */
    public static boolean createSpawnEggFormation(
            World world,
            Entity placementActor,
            double x,
            double y,
            double z,
            float yaw,
            String customName
    ) {
        return createAutonomousFormation(
                world,
                null,
                MumakilFormationOrigin.CREATIVE_SPAWN_EGG,
                false,
                null,
                null,
                x,
                y,
                z,
                yaw,
                placementActor,
                false,
                true,
                customName
        );
    }

    private static boolean createAutonomousFormation(
            World world,
            LOTREntityMumakilHowdahArcher bootstrapArcher,
            MumakilFormationOrigin origin,
            boolean conquestSpawn,
            UUID invasionId,
            List invasionBonusFactions,
            double x,
            double y,
            double z,
            float yaw,
            Entity validationExclusion,
            boolean enforceNaturalSpawnCapacity,
            boolean persistentFormation,
            String customName
    ) {
        if (world == null
                || world.isRemote
                || origin == null
                || origin == MumakilFormationOrigin.NONE
                || bootstrapArcher != null
                && (bootstrapArcher.worldObj != world
                || bootstrapArcher.isDead)) {
            return false;
        }

        if (enforceNaturalSpawnCapacity
                && !hasNaturalFormationSpawnCapacity(
                world,
                bootstrapArcher
        )
                || !canNaturalFormationSpawnAt(
                world,
                validationExclusion,
                x,
                y,
                z
        )) {
            return false;
        }

        List<Entity> createdMembers = new ArrayList<Entity>();
        try {
        LOTREntityMumakil mumakil = new LOTREntityMumakil(world);
        mumakil.setLocationAndAngles(
                x,
                y,
                z,
                yaw,
                0.0F
        );
        mumakil.rotationYawHead = yaw;
        mumakil.renderYawOffset = yaw;
        mumakil.onSpawnWithEgg(null);
        if (!initializeMount(mumakil, origin)) {
            removeCreatedMembers(createdMembers);
            return false;
        }
        if (customName != null && !customName.isEmpty()) {
            mumakil.setCustomNameTag(customName);
        }
        if (persistentFormation) {
            mumakil.func_110163_bv();
        }
        if (invasionId != null) {
            mumakil.setMumakilInvasionId(invasionId);
            mumakil.getEntityData().setInteger(
                    MumakilInvasionFormationRegistry
                            .INVASION_MEMBER_WEIGHT_KEY,
                    MumakilConfig.INVASION_MUMAK_BUDGET_VALUE
            );
        }
        if (!world.spawnEntityInWorld(mumakil)) {
            removeCreatedMembers(createdMembers);
            return false;
        }
        createdMembers.add(mumakil);

        LOTREntitySouthronChampion driver =
                new LOTREntitySouthronChampion(world);
        driver.setConquestSpawning(conquestSpawn);
        driver.onSpawnWithEgg(null);
        if (invasionId != null) {
            driver.setInvasionID(invasionId);
            driver.getEntityData().setInteger(
                    MumakilInvasionFormationRegistry
                            .INVASION_MEMBER_WEIGHT_KEY,
                    MumakilConfig.INVASION_DRIVER_BUDGET_VALUE
            );
            if (invasionBonusFactions != null) {
                driver.killBonusFactions.addAll(
                        invasionBonusFactions
                );
            }
        }
        driver.isNPCPersistent = persistentFormation;
        driver.setShouldTraderRespawn(false);
        /*
         * Keep an attached driver from independently distance-despawning.
         * Natural formations retain their nonpersistent LOTR cap state; spawn-
         * egg formations retain the explicit persistence requested above.
         */
        driver.func_110163_bv();
        driver.setCurrentItemOrArmor(0, null);
        mumakil.positionRiderAtMumakilAnchor(driver);
        driver.rotationYaw = mumakil.renderYawOffset;
        driver.prevRotationYaw = driver.rotationYaw;
        driver.renderYawOffset = driver.rotationYaw;
        driver.prevRenderYawOffset = driver.rotationYaw;

        if (!world.spawnEntityInWorld(driver)) {
            removeCreatedMembers(createdMembers);
            return false;
        }
        createdMembers.add(driver);
        driver.setConquestSpawning(false);
        driver.mountEntity(mumakil);
        mumakil.updateRiderPosition();

        int firstSpawnedArcherSlot = 0;
        if (bootstrapArcher != null) {
            MumakilHowdahArcherEventHandler
                    .attachExistingHowdahArcher(
                            mumakil,
                            bootstrapArcher,
                            0,
                            persistentFormation
                    );
            if (invasionId != null) {
                bootstrapArcher.getEntityData().setInteger(
                        MumakilInvasionFormationRegistry
                                .INVASION_MEMBER_WEIGHT_KEY,
                        MumakilConfig
                                .INVASION_ARCHER_BUDGET_VALUE
                );
            }
            createdMembers.add(bootstrapArcher);
            firstSpawnedArcherSlot = 1;
        }

        for (int slot = firstSpawnedArcherSlot;
             slot < FORMATION_ARCHER_COUNT;
             ++slot) {
            LOTREntityMumakilHowdahArcher archer =
                    MumakilHowdahArcherEventHandler
                            .spawnAttachedHowdahArcher(
                                    mumakil,
                                    slot,
                                    persistentFormation,
                                    invasionId,
                                    invasionBonusFactions
            );
            if (archer == null) {
                removeCreatedMembers(createdMembers);
                return false;
            }
            if (invasionId != null) {
                archer.getEntityData().setInteger(
                        MumakilInvasionFormationRegistry
                                .INVASION_MEMBER_WEIGHT_KEY,
                        MumakilConfig.INVASION_ARCHER_BUDGET_VALUE
                );
            }
            createdMembers.add(archer);
        }

        MumakilHowdahArcherEventHandler.markHowdahArcherSetComplete(mumakil);
        if (persistentFormation) {
            mumakil.playLivingSound();
        }
        return true;
        } catch (RuntimeException e) {
            removeCreatedMembers(createdMembers);
            return false;
        }
    }

    public static void removeNaturalFormationMembers(
            LOTREntityMumakil mumakil,
            Entity capturedDriver
    ) {
        if (mumakil == null
                || mumakil.worldObj == null
                || mumakil.worldObj.isRemote
                || !mumakil.isNaturalNearHaradFormation()
                && mumakil.getFormationOrigin()
                != MumakilFormationOrigin.INVASION_NEAR_HARAD) {
            return;
        }

        Entity driver = capturedDriver != null
                ? capturedDriver
                : mumakil.riddenByEntity;
        if (driver != null && !driver.isDead) {
            driver.mountEntity(null);
            driver.setDead();
        }

        String mountUuid = mumakil.getPersistentID().toString();
        List loaded = new ArrayList(mumakil.worldObj.loadedEntityList);
        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof LOTREntityMumakilHowdahArcher)) {
                continue;
            }

            LOTREntityMumakilHowdahArcher archer =
                    (LOTREntityMumakilHowdahArcher)object;
            if (archer.getHowdahMountEntityId() == mumakil.getEntityId()
                    || mountUuid.equals(archer.getHowdahMountUuid())) {
                archer.setDead();
            }
        }
    }

    private static boolean setInventoryStack(
            LOTREntityMumakil mumakil,
            int slot,
            ItemStack stack
    ) {
        IInventory inventory = mumakil.getMumakilMountInventory();
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) {
            return false;
        }

        inventory.setInventorySlotContents(slot, stack);
        inventory.markDirty();
        return true;
    }

    private static void removeCreatedMembers(List<Entity> createdMembers) {
        for (int i = createdMembers.size() - 1; i >= 0; --i) {
            Entity entity = createdMembers.get(i);
            if (entity != null && !entity.isDead) {
                if (entity.ridingEntity != null) {
                    entity.mountEntity(null);
                }
                entity.setDead();
            }
        }
    }

}
