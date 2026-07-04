package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Safe first-pass passive howdah archers for hired Mumakil.
 *
 * Minecraft 1.7.10 only supports one normal riddenByEntity per mount. The
 * Southron Champion driver uses that real rider slot, so the howdah archers are
 * custom passive passenger entities tagged to a Mumakil and slot. The custom
 * archer entity handles no-gravity attachment on both server and client.
 *
 * This deliberately does not add target AI, attack AI, arrow shooting, downward
 * AABB searches, or combat relays.
 */
public class MumakilHowdahArcherEventHandler {
    private static final String MUMAKIL_ARCHER_CARRIER_KEY = "LOTRMoreMobsHiredHowdahArcherCarrier";
    private static final String MUMAKIL_ARCHERS_SPAWNED_KEY = "LOTRMoreMobsHowdahArchersSpawned";
    private static final String ARCHER_TAG_KEY = "LOTRMoreMobsHowdahArcher";
    private static final String ARCHER_SLOT_KEY = "LOTRMoreMobsHowdahArcherSlot";
    private static final String ARCHER_MOUNT_ID_KEY = "LOTRMoreMobsHowdahMountId";
    private static final String ARCHER_MOUNT_UUID_KEY = "LOTRMoreMobsHowdahMountUuid";

    private static final int HOWDAH_ARCHER_COUNT = 15;
    private static final int ARCHER_COUNT_CHECK_INTERVAL = 40;

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
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || event.world.isRemote) {
            return;
        }

        if (isTaggedHowdahArcher(event.entity) && event.entity instanceof EntityLiving) {
            makeArcherPassive((EntityLiving)event.entity);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null || event.entityLiving.worldObj.isRemote) {
            return;
        }

        EntityLivingBase living = event.entityLiving;

        if (living instanceof LOTREntityMumakil) {
            updateHiredMumakilArchers((LOTREntityMumakil)living);
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
                return;
            }

            makeArcherPassive((EntityLiving)living);
        }
    }

    private static void updateHiredMumakilArchers(LOTREntityMumakil mumakil) {
        NBTTagCompound data = mumakil.getEntityData();

        if (!data.getBoolean(MUMAKIL_ARCHER_CARRIER_KEY)) {
            return;
        }

        if (!mumakil.getBelongsToNPC() || !mumakil.hasMumakilHowdahEquipped()) {
            return;
        }

        if (data.getBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY)) {
            if (mumakil.ticksExisted % ARCHER_COUNT_CHECK_INTERVAL == 0) {
                int valid = normalizeExistingArchersForMumakil(mumakil);
                if (valid < HOWDAH_ARCHER_COUNT) {
                    data.setBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY, false);
                }
            }
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

        int existing = countExistingArchersForMumakil(mumakil);
        if (existing > 0) {
            int removed = removeExistingArchersForMumakil(mumakil);
            System.out.println("[LOTRMoreMobs] Removed " + removed + " old passive Mumakil howdah archers before respawn.");
        }

        int spawned = spawnPassiveHowdahArchers(mumakil);
        if (spawned > 0) {
            data.setBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY, true);
        }
    }

    private static int spawnPassiveHowdahArchers(LOTREntityMumakil mumakil) {
        World world = mumakil.worldObj;
        String mountUuid = getEntityPersistentIdString(mumakil);
        int spawned = 0;

        System.out.println("[LOTRMoreMobs] Attempting to spawn " + HOWDAH_ARCHER_COUNT + " gravity-proof Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");

        for (int slot = 0; slot < HOWDAH_ARCHER_COUNT; ++slot) {
            LOTREntityMumakilHowdahArcher archer = new LOTREntityMumakilHowdahArcher(world);
            archer.onSpawnWithEgg(null);
            archer.setHowdahAttachment(mumakil, slot);
            tagArcher(archer, mumakil, mountUuid, slot);
            makeArcherPassive(archer);

            if (world.spawnEntityInWorld(archer)) {
                ++spawned;
            }
        }

        System.out.println("[LOTRMoreMobs] Spawned " + spawned + " gravity-proof Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");
        return spawned;
    }

    private static int countExistingArchersForMumakil(LOTREntityMumakil mumakil) {
        int count = 0;
        List loaded = mumakil.worldObj.loadedEntityList;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (object instanceof EntityLiving && isTaggedHowdahArcher((Entity)object) && isArcherAssignedToMount((EntityLiving)object, mumakil)) {
                ++count;
            }
        }

        return count;
    }

    private static int removeExistingArchersForMumakil(LOTREntityMumakil mumakil) {
        int removed = 0;
        List loaded = mumakil.worldObj.loadedEntityList;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (object instanceof EntityLiving && isTaggedHowdahArcher((Entity)object) && isArcherAssignedToMount((EntityLiving)object, mumakil)) {
                ((EntityLiving)object).setDead();
                ++removed;
            }
        }

        return removed;
    }

    private static int normalizeExistingArchersForMumakil(LOTREntityMumakil mumakil) {
        boolean[] seenSlots = new boolean[HOWDAH_ARCHER_COUNT];
        int valid = 0;
        int removed = 0;
        List loaded = mumakil.worldObj.loadedEntityList;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (!(object instanceof EntityLiving) || !isTaggedHowdahArcher((Entity)object)) {
                continue;
            }

            EntityLiving archer = (EntityLiving)object;
            if (!isArcherAssignedToMount(archer, mumakil)) {
                continue;
            }

            int slot = archer.getEntityData().getInteger(ARCHER_SLOT_KEY);
            if (slot < 0 || slot >= HOWDAH_ARCHER_COUNT || seenSlots[slot] || !(archer instanceof LOTREntityMumakilHowdahArcher)) {
                archer.setDead();
                ++removed;
                continue;
            }

            seenSlots[slot] = true;
            ++valid;
            archer.getEntityData().setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
            ((LOTREntityMumakilHowdahArcher)archer).setHowdahAttachment(mumakil, slot);
            makeArcherPassive(archer);
        }

        if (removed > 0) {
            System.out.println("[LOTRMoreMobs] Removed " + removed + " duplicate/legacy passive Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");
        }

        return valid;
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

    private static void tagArcher(LOTREntityMumakilHowdahArcher archer, LOTREntityMumakil mumakil, String mountUuid, int slot) {
        NBTTagCompound data = archer.getEntityData();
        data.setBoolean(ARCHER_TAG_KEY, true);
        data.setInteger(ARCHER_SLOT_KEY, slot);
        data.setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
        data.setString(ARCHER_MOUNT_UUID_KEY, mountUuid);
    }

    private static void makeArcherPassive(EntityLiving archer) {
        if (archer.tasks != null && archer.tasks.taskEntries != null) {
            archer.tasks.taskEntries.clear();
        }

        if (archer.targetTasks != null && archer.targetTasks.taskEntries != null) {
            archer.targetTasks.taskEntries.clear();
        }

        if (archer instanceof EntityCreature) {
            EntityCreature creature = (EntityCreature)archer;
            creature.setAttackTarget(null);

            if (creature.getNavigator() != null) {
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
        invokeNoArgMethod(archer, "enablePersistence", "func_110163_bv");
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
}
