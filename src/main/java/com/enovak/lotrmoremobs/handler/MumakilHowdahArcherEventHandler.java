package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import com.enovak.lotrmoremobs.util.MumakilPerformanceTracker;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import java.util.List;
import lotr.common.LOTRMod;
import lotr.common.entity.ai.LOTRNPCTargetSelector;
import lotr.common.entity.npc.LOTREntityNPC;
import lotr.common.fac.LOTRFaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Safe first-pass passive howdah archers for hired Mumakil.
 *
 * Minecraft 1.7.10 only supports one normal riddenByEntity per mount. The
 * Southron Champion driver uses that real rider slot, so the howdah archers are
 * custom passive passenger entities tagged to a Mumakil and slot. The custom
 * archer entity handles no-gravity attachment on both server and client.
 *
 * Combat is deliberately lightweight: one slow target scan per Mumakil, no pathing,
 * no chase AI, and fixed passengers decide only whether to look/fire.
 */
public class MumakilHowdahArcherEventHandler {
    private static final String MUMAKIL_ARCHER_CARRIER_KEY = "LOTRMoreMobsHiredHowdahArcherCarrier";
    private static final String MUMAKIL_ARCHER_CARRIER_VERSION_KEY = "LOTRMoreMobsHiredHowdahArcherCarrierVersion";
    private static final String MUMAKIL_ARCHERS_SPAWNED_KEY = "LOTRMoreMobsHowdahArchersSpawned";
    private static final String MUMAKIL_ARCHERS_NEXT_CHECK_KEY = "LOTRMoreMobsHowdahArchersNextCheck";
    private static final String MUMAKIL_ARCHERS_NEXT_REPAIR_KEY = "LOTRMoreMobsHowdahArchersNextRepair";
    private static final String MUMAKIL_ARCHERS_NEXT_TARGET_SCAN_KEY = "LOTRMoreMobsHowdahArchersNextTargetScan";
    private static final String ARCHER_TAG_KEY = "LOTRMoreMobsHowdahArcher";
    private static final String ARCHER_SLOT_KEY = "LOTRMoreMobsHowdahArcherSlot";
    private static final String LEGACY_ARCHER_SLOT_KEY = "LOTRMoreMobsHowdahSlot";
    private static final String ARCHER_MOUNT_ID_KEY = "LOTRMoreMobsHowdahMountId";
    private static final String ARCHER_MOUNT_UUID_KEY = "LOTRMoreMobsHowdahMountUuid";
    private static final String DEAD_ARCHER_SLOTS_KEY = "LOTRMoreMobsDeadHowdahArcherSlots";

    private static final int HOWDAH_ARCHER_COUNT = LOTREntityMumakilHowdahArcher.getHowdahArcherSlotCount();
    private static final int CURRENT_CARRIER_VERSION = 2;
    private static final int ARCHER_COUNT_CHECK_INTERVAL = 200;
    private static final int ARCHER_REPAIR_COOLDOWN = 400;
    private static final int TARGET_SCAN_MIN_INTERVAL = 40;
    private static final int TARGET_SCAN_RANDOM_INTERVAL = 21;
    private static final double TARGET_SCAN_RANGE = 36.0D;
    private static final double TARGET_SCAN_VERTICAL_RANGE = 28.0D;

    public static void markHiredHowdahArcherCarrier(LOTREntityMumakil mumakil) {
        if (mumakil == null || mumakil.worldObj == null || mumakil.worldObj.isRemote) {
            return;
        }

        /*
         * Do not reset MUMAKIL_ARCHERS_SPAWNED_KEY here. Resetting it on every
         * EntityJoinWorld pass was what allowed old hired Mumakil to spawn another
         * full archer set when a world/chunk reloaded.
         */
        mumakil.getEntityData().setBoolean(MUMAKIL_ARCHER_CARRIER_KEY, true);
        mumakil.getEntityData().setInteger(MUMAKIL_ARCHER_CARRIER_VERSION_KEY, CURRENT_CARRIER_VERSION);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || event.world.isRemote) {
            return;
        }

        if (event.entity instanceof LOTREntityMumakilHowdahArcher) {
            LOTREntityMumakilHowdahArcher archer = (LOTREntityMumakilHowdahArcher)event.entity;
            if (!archer.isRuntimeHowdahPassenger()) {
                archer.setDead();
                return;
            }

            makeArcherPassive(archer);
            return;
        }

        if (isTaggedHowdahArcher(event.entity) && event.entity instanceof EntityLiving) {
            ((EntityLiving)event.entity).setDead();
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null) {
            return;
        }

        if (event.entityLiving.worldObj.isRemote) {
            if (event.entityLiving instanceof LOTREntityMumakil
                    && ((LOTREntityMumakil)event.entityLiving).hasMumakilHowdahEquipped()) {
                updateHowdahArcherSharedTarget(
                        (LOTREntityMumakil)event.entityLiving,
                        event.entityLiving.worldObj.getTotalWorldTime()
                );
            }

            return;
        }

        EntityLivingBase living = event.entityLiving;

        if (living instanceof LOTREntityMumakil) {
            LOTREntityMumakil mumakil = (LOTREntityMumakil)living;
            long perfStart = MumakilPerformanceTracker.startTimer();

            try {
                updateHiredMumakilArchers(mumakil);
            } finally {
                if (MumakilPerformanceTracker.isEnabled()) {
                    MumakilPerformanceTracker.recordArcherHandler(
                            mumakil,
                            System.nanoTime() - perfStart
                    );
                }
            }
            return;
        }

        if (living instanceof LOTREntityMumakilHowdahArcher
                && !((LOTREntityMumakilHowdahArcher)living).isRuntimeHowdahPassenger()
                && !((LOTREntityMumakilHowdahArcher)living).isDetachedFromDeadMumakil()) {
            living.setDead();
            return;
        }

        if (living instanceof EntityLiving && isTaggedHowdahArcher(living)) {
            /*
             * Any old generic archer entities left over from prior test builds are
             * cleaned up. New passengers use LOTREntityMumakilHowdahArcher, which
             * attaches itself and disables gravity on both server and client.
             */
            if (!(living instanceof LOTREntityMumakilHowdahArcher)) {
                living.setDead();
            }
        }
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event == null
                || event.entityLiving == null
                || event.entityLiving.worldObj == null
                || event.entityLiving.worldObj.isRemote) {
            return;
        }

        if (event.entityLiving instanceof LOTREntityMumakilHowdahArcher) {
            markDeadHowdahArcherSlot((LOTREntityMumakilHowdahArcher)event.entityLiving);
            return;
        }

        if (!(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        detachHowdahArchersForDeadMumakil((LOTREntityMumakil)event.entityLiving);
    }

    private static void detachHowdahArchersForDeadMumakil(LOTREntityMumakil mumakil) {
        List loaded = mumakil.worldObj.loadedEntityList;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof LOTREntityMumakilHowdahArcher)) {
                continue;
            }

            LOTREntityMumakilHowdahArcher archer = (LOTREntityMumakilHowdahArcher)object;
            if (!archer.isDead && archer.isRuntimeHowdahPassenger() && isArcherAssignedToMount(archer, mumakil)) {
                archer.detachFromHowdahForMumakilDeath(mumakil);
            }
        }
    }

    private static void updateHiredMumakilArchers(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();

        if (!data.getBoolean(MUMAKIL_ARCHER_CARRIER_KEY)
                || data.getInteger(MUMAKIL_ARCHER_CARRIER_VERSION_KEY) != CURRENT_CARRIER_VERSION) {
            return;
        }

        if (!mumakil.hasMumakilHowdahEquipped()) {
            LOTREntityMumakilHowdahArcher.setSharedHowdahTarget(mumakil, null);
            return;
        }

        /*
         * Wait a few ticks so the normal LOTR hired-unit spawn path has time to
         * attach the Southron Champion driver and the existing hired-mount handler
         * has time to finish saddle/howdah sync.
         */
        if (mumakil.ticksExisted < 5) {
            return;
        }

        long worldTime = mumakil.worldObj.getTotalWorldTime();
        updateHowdahArcherSharedTarget(mumakil, worldTime);

        if (!mumakil.getBelongsToNPC()) {
            return;
        }

        if (!data.getBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY)) {
            SlotScanResult result = normalizeExistingArchersForMumakil(mumakil);
            int spawned = spawnMissingHowdahArchers(mumakil, result.seenSlots, true);

            data.setBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY, true);
            data.setLong(MUMAKIL_ARCHERS_NEXT_CHECK_KEY, worldTime + ARCHER_COUNT_CHECK_INTERVAL);
            data.setLong(MUMAKIL_ARCHERS_NEXT_REPAIR_KEY, worldTime + ARCHER_REPAIR_COOLDOWN);

            System.out.println("[LOTRMoreMobs] Initial Mumakil howdah archer set for mount="
                    + mumakil.getEntityId()
                    + ": kept="
                    + result.valid
                    + ", spawned="
                    + spawned
                    + ".");
            return;
        }

        long nextCheck = data.getLong(MUMAKIL_ARCHERS_NEXT_CHECK_KEY);
        if (nextCheck > worldTime) {
            return;
        }

        data.setLong(MUMAKIL_ARCHERS_NEXT_CHECK_KEY, worldTime + ARCHER_COUNT_CHECK_INTERVAL);
        SlotScanResult result = normalizeExistingArchersForMumakil(mumakil);

        if (result.valid >= HOWDAH_ARCHER_COUNT) {
            data.setLong(MUMAKIL_ARCHERS_NEXT_REPAIR_KEY, 0L);
            return;
        }

        long nextRepair = data.getLong(MUMAKIL_ARCHERS_NEXT_REPAIR_KEY);
        if (nextRepair <= worldTime) {
            spawnMissingHowdahArchers(mumakil, result.seenSlots, false);
            data.setLong(MUMAKIL_ARCHERS_NEXT_REPAIR_KEY, worldTime + ARCHER_REPAIR_COOLDOWN);
        }
    }

    private static void updateHowdahArcherSharedTarget(LOTREntityMumakil mumakil, long worldTime) {
        if (MumakilPerformanceTracker.DEBUG_DISABLE_HOWDAH_ARCHER_COMBAT) {
            LOTREntityMumakilHowdahArcher.setSharedHowdahTarget(mumakil, null);
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        long nextScan = data.getLong(MUMAKIL_ARCHERS_NEXT_TARGET_SCAN_KEY);
        if (nextScan > worldTime) {
            return;
        }

        data.setLong(
                MUMAKIL_ARCHERS_NEXT_TARGET_SCAN_KEY,
                worldTime + TARGET_SCAN_MIN_INTERVAL + mumakil.worldObj.rand.nextInt(TARGET_SCAN_RANDOM_INTERVAL)
        );

        EntityLivingBase target = findBestHowdahArcherTarget(mumakil);
        LOTREntityMumakilHowdahArcher.setSharedHowdahTarget(mumakil, target);
    }

    private static EntityLivingBase findBestHowdahArcherTarget(LOTREntityMumakil mumakil) {
        long perfStart = mumakil.worldObj.isRemote ? 0L : MumakilPerformanceTracker.startTimer();
        List nearby = mumakil.worldObj.getEntitiesWithinAABB(
                EntityLivingBase.class,
                mumakil.boundingBox.expand(TARGET_SCAN_RANGE, TARGET_SCAN_VERTICAL_RANGE, TARGET_SCAN_RANGE)
        );
        EntityLiving selectorOwner = findHowdahTargetSelectorOwner(mumakil);
        LOTRNPCTargetSelector targetSelector = selectorOwner == null ? null : new LOTRNPCTargetSelector(selectorOwner);

        EntityLivingBase bestTarget = null;
        int bestPriority = Integer.MAX_VALUE;
        double bestDistanceSq = Double.MAX_VALUE;

        for (int i = 0; i < nearby.size(); ++i) {
            EntityLivingBase candidate = (EntityLivingBase)nearby.get(i);
            if (!canHowdahArchersTarget(mumakil, candidate, targetSelector)) {
                continue;
            }

            int priority = getHowdahTargetPriority(candidate);
            double distanceSq = mumakil.getDistanceSqToEntity(candidate);
            if (priority < bestPriority || priority == bestPriority && distanceSq < bestDistanceSq) {
                bestTarget = candidate;
                bestPriority = priority;
                bestDistanceSq = distanceSq;
            }
        }

        if (!mumakil.worldObj.isRemote && MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordArcherTargetScan(mumakil, nearby.size(), System.nanoTime() - perfStart);
        }

        return bestTarget;
    }

    private static EntityLiving findHowdahTargetSelectorOwner(LOTREntityMumakil mumakil) {
        List loaded = mumakil.worldObj.loadedEntityList;
        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof LOTREntityMumakilHowdahArcher)) {
                continue;
            }

            LOTREntityMumakilHowdahArcher archer = (LOTREntityMumakilHowdahArcher)object;
            if (!archer.isDead && archer.isRuntimeHowdahPassenger() && isArcherAssignedToMount(archer, mumakil)) {
                return archer;
            }
        }

        return mumakil.riddenByEntity instanceof EntityLiving ? (EntityLiving)mumakil.riddenByEntity : null;
    }

    private static boolean canHowdahArchersTarget(LOTREntityMumakil mumakil, EntityLivingBase target, LOTRNPCTargetSelector targetSelector) {
        if (MumakilPerformanceTracker.isEnabled()) {
            MumakilPerformanceTracker.recordArcherCandidateCheck(mumakil);
        }

        if (target == null
                || target == mumakil
                || target == mumakil.riddenByEntity
                || target instanceof LOTREntityMumakil
                || target instanceof LOTREntityMumakilHowdahArcher
                || !target.isEntityAlive()
                || target.riddenByEntity != null) {
            return false;
        }

        if (target instanceof EntityPlayer) {
            return !((EntityPlayer)target).capabilities.isCreativeMode
                    && targetSelector != null
                    && targetSelector.isEntityApplicable(target);
        }

        if (!(target instanceof LOTREntityNPC)) {
            return false;
        }

        LOTREntityNPC npc = (LOTREntityNPC)target;
        if (npc.hiredNPCInfo.isActive) {
            return false;
        }

        if (targetSelector != null) {
            return targetSelector.isEntityApplicable(target);
        }

        LOTRFaction targetFaction = LOTRMod.getNPCFaction(npc);
        if (targetFaction == null || !LOTRFaction.NEAR_HARAD.isBadRelation(targetFaction)) {
            return false;
        }

        if (mumakil.riddenByEntity instanceof EntityCreature
                && !LOTRMod.canNPCAttackEntity((EntityCreature)mumakil.riddenByEntity, target, false)) {
            return false;
        }

        return true;
    }

    private static int getHowdahTargetPriority(EntityLivingBase target) {
        if (target instanceof LOTREntityNPC) {
            return 0;
        }

        if (target instanceof EntityPlayer) {
            return 1;
        }

        return 2;
    }

    private static int spawnMissingHowdahArchers(LOTREntityMumakil mumakil, boolean[] seenSlots, boolean initialSpawn) {
        if (MumakilPerformanceTracker.DEBUG_DO_NOT_SPAWN_HOWDAH_ARCHERS) {
            return 0;
        }

        int spawned = 0;
        StringBuilder resultSlots = new StringBuilder();

        for (int slot = 0; slot < HOWDAH_ARCHER_COUNT; ++slot) {
            if (seenSlots[slot] || isHowdahArcherSlotDead(mumakil, slot)) {
                continue;
            }

            if (spawnPassiveHowdahArcher(mumakil, slot)) {
                seenSlots[slot] = true;
                ++spawned;

                if (!initialSpawn) {
                    resultSlots.append(resultSlots.length() == 0 ? "" : ",").append(slot);
                }
            }
        }

        if (!initialSpawn && spawned > 0) {
            System.out.println("[LOTRMoreMobs] Spawned "
                    + spawned
                    + " missing howdah archer slots for mount="
                    + mumakil.getEntityId()
                    + " slots="
                    + resultSlots
                    + ".");
        }

        return spawned;
    }

    private static boolean spawnPassiveHowdahArcher(LOTREntityMumakil mumakil, int slot) {
        World world = mumakil.worldObj;
        String mountUuid = getEntityPersistentIdString(mumakil);

        LOTREntityMumakilHowdahArcher archer = new LOTREntityMumakilHowdahArcher(world);
        archer.onSpawnWithEgg(null);
        archer.setRuntimeHowdahPassenger(true);
        archer.setHowdahAttachment(mumakil, slot);
        tagArcher(archer, mumakil, mountUuid, slot);
        makeArcherPassive(archer);

        return world.spawnEntityInWorld(archer);
    }

    private static SlotScanResult normalizeExistingArchersForMumakil(LOTREntityMumakil mumakil) {
        SlotScanResult result = new SlotScanResult();
        List loaded = mumakil.worldObj.loadedEntityList;
        String mountUuid = getEntityPersistentIdString(mumakil);
        int invalidRemoved = 0;
        int duplicateRemoved = 0;
        int staleRemoved = 0;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof EntityLiving) || !isTaggedHowdahArcher((Entity)object)) {
                continue;
            }

            EntityLiving archer = (EntityLiving)object;
            if (archer.isDead) {
                continue;
            }

            if (!isArcherAssignedToMount(archer, mumakil)) {
                continue;
            }

            if (archer instanceof LOTREntityMumakilHowdahArcher
                    && !((LOTREntityMumakilHowdahArcher)archer).isRuntimeHowdahPassenger()) {
                archer.setDead();
                ++staleRemoved;
                continue;
            }

            int slot = getArcherSlot(archer);
            if (slot < 0 || slot >= HOWDAH_ARCHER_COUNT || !(archer instanceof LOTREntityMumakilHowdahArcher)) {
                archer.setDead();
                ++invalidRemoved;
                continue;
            }

            if (result.seenSlots[slot]) {
                archer.setDead();
                ++duplicateRemoved;
                continue;
            }

            result.seenSlots[slot] = true;
            ++result.valid;
            tagArcher((LOTREntityMumakilHowdahArcher)archer, mumakil, mountUuid, slot);
            ((LOTREntityMumakilHowdahArcher)archer).setHowdahAttachment(mumakil, slot);
            makeArcherPassive(archer);
        }

        if (invalidRemoved > 0 || duplicateRemoved > 0 || staleRemoved > 0) {
            System.out.println("[LOTRMoreMobs] Cleaned passive Mumakil howdah archers for mount="
                    + mumakil.getEntityId()
                    + ": stale="
                    + staleRemoved
                    + ", invalid="
                    + invalidRemoved
                    + ", duplicate="
                    + duplicateRemoved
                    + ".");
        }

        return result;
    }

    private static boolean isArcherAssignedToMount(EntityLiving archer, LOTREntityMumakil mumakil) {
        NBTTagCompound data = archer.getEntityData();
        int mountId = data.getInteger(ARCHER_MOUNT_ID_KEY);
        if (mountId == mumakil.getEntityId()) {
            return true;
        }

        String archerMountUuid = data.getString(ARCHER_MOUNT_UUID_KEY);
        return archerMountUuid != null
                && archerMountUuid.length() > 0
                && archerMountUuid.equals(getEntityPersistentIdString(mumakil));
    }

    private static void markDeadHowdahArcherSlot(LOTREntityMumakilHowdahArcher archer) {
        if (archer == null
                || !archer.isRuntimeHowdahPassenger()
                || archer.isDetachedFromDeadMumakil()) {
            return;
        }

        int slot = archer.getHowdahSlot();
        if (slot < 0 || slot >= HOWDAH_ARCHER_COUNT || slot >= 32) {
            return;
        }

        LOTREntityMumakil mumakil = findAssignedMumakilForArcher(archer);
        if (mumakil != null) {
            markHowdahArcherSlotDead(mumakil, slot);
        }
    }

    private static LOTREntityMumakil findAssignedMumakilForArcher(LOTREntityMumakilHowdahArcher archer) {
        int mountId = archer.getHowdahMountEntityId();
        Entity entity = mountId == 0 || archer.worldObj == null ? null : archer.worldObj.getEntityByID(mountId);
        if (entity instanceof LOTREntityMumakil) {
            return (LOTREntityMumakil)entity;
        }

        String mountUuid = archer.getHowdahMountUuid();
        if (archer.worldObj == null || mountUuid == null || mountUuid.length() == 0) {
            return null;
        }

        List loaded = archer.worldObj.loadedEntityList;
        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (object instanceof LOTREntityMumakil
                    && mountUuid.equals(getEntityPersistentIdString((Entity)object))) {
                return (LOTREntityMumakil)object;
            }
        }

        return null;
    }

    private static boolean isHowdahArcherSlotDead(LOTREntityMumakil mumakil, int slot) {
        return slot >= 0
                && slot < 32
                && (mumakil.getEntityData().getInteger(DEAD_ARCHER_SLOTS_KEY) & (1 << slot)) != 0;
    }

    private static void markHowdahArcherSlotDead(LOTREntityMumakil mumakil, int slot) {
        mumakil.getEntityData().setInteger(
                DEAD_ARCHER_SLOTS_KEY,
                mumakil.getEntityData().getInteger(DEAD_ARCHER_SLOTS_KEY) | (1 << slot)
        );
    }

    private static void tagArcher(LOTREntityMumakilHowdahArcher archer, LOTREntityMumakil mumakil, String mountUuid, int slot) {
        NBTTagCompound data = archer.getEntityData();
        data.setBoolean(ARCHER_TAG_KEY, true);
        data.setInteger(ARCHER_SLOT_KEY, slot);
        data.setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
        data.setString(ARCHER_MOUNT_UUID_KEY, mountUuid);
    }

    private static int getArcherSlot(EntityLiving archer) {
        NBTTagCompound data = archer.getEntityData();
        if (data.hasKey(ARCHER_SLOT_KEY)) {
            return data.getInteger(ARCHER_SLOT_KEY);
        }

        if (data.hasKey(LEGACY_ARCHER_SLOT_KEY)) {
            int slot = data.getInteger(LEGACY_ARCHER_SLOT_KEY);
            data.setInteger(ARCHER_SLOT_KEY, slot);
            return slot;
        }

        return -1;
    }

    private static void makeArcherPassive(EntityLiving archer) {
        if (archer.tasks != null && archer.tasks.taskEntries != null && !archer.tasks.taskEntries.isEmpty()) {
            archer.tasks.taskEntries.clear();
        }

        if (archer.targetTasks != null && archer.targetTasks.taskEntries != null && !archer.targetTasks.taskEntries.isEmpty()) {
            archer.targetTasks.taskEntries.clear();
        }

        if (archer instanceof EntityCreature) {
            EntityCreature creature = (EntityCreature)archer;
            creature.setAttackTarget(null);

            if (!(archer instanceof LOTREntityMumakilHowdahArcher) && creature.getNavigator() != null) {
                creature.getNavigator().clearPathEntity();
            }
        }

        archer.setRevengeTarget(null);
        archer.motionX = 0.0D;
        archer.motionY = 0.0D;
        archer.motionZ = 0.0D;
        archer.fallDistance = 0.0F;
        archer.onGround = true;
        archer.isAirBorne = false;

        if (!(archer instanceof LOTREntityMumakilHowdahArcher)) {
            invokeNoArgMethod(archer, "enablePersistence", "func_110163_bv");
        }
    }

    private static boolean isTaggedHowdahArcher(Entity entity) {
        return entity != null && entity.getEntityData().getBoolean(ARCHER_TAG_KEY);
    }

    private static String getEntityPersistentIdString(Entity entity) {
        Object id = invokeNoArgMethod(entity, "getPersistentID", "getUniqueID", "func_110124_au");
        return id == null ? "" : id.toString();
    }

    private static Object invokeNoArgMethod(Object target, String... methodNames) {
        for (int i = 0; i < methodNames.length; ++i) {
            try {
                Method method = findNoArgMethod(target.getClass(), methodNames[i]);
                if (method != null) {
                    return method.invoke(target, new Object[0]);
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private static Method findNoArgMethod(Class type, String name) {
        Class current = type;
        while (current != null && current != Object.class) {
            try {
                Method method = current.getDeclaredMethod(name, new Class[0]);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private static class SlotScanResult {
        private final boolean[] seenSlots = new boolean[HOWDAH_ARCHER_COUNT];
        private int valid;
    }
}
