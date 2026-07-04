package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Safe first-pass passive howdah archers for hired Mumakil.
 *
 * Minecraft 1.7.10 only supports one normal riddenByEntity per mount. The
 * Southron Champion driver uses that real rider slot, so the howdah archers are
 * custom passive passenger entities that are tagged with a mount UUID and hard
 * positioned onto fixed howdah offsets at the end of each world tick.
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

    private static final int ARCHER_COUNT_CHECK_INTERVAL = 40;

    private static final String[] SOUTHON_ARCHER_CLASS_NAMES = new String[] {
            "lotr.common.entity.npc.LOTREntitySouthronArcher",
            "lotr.common.entity.npc.LOTREntityNearHaradrimArcher",
            "lotr.common.entity.npc.LOTREntityNearHaradArcher",
            "lotr.common.entity.npc.LOTREntityHaradrimArcher"
    };

    /**
     * Each row is: forward offset, side offset, vertical offset, yaw offset.
     * Forward = toward Mumakil head. Side = positive to Mumakil right.
     *
     * Requested 15-slot starting layout:
     * - slots 0-5:  three on each side of the wide howdah body
     * - slots 6-9:  two on each lower side perch
     * - slots 10-13: four on the middle perch
     * - slot 14:    one on the top perch
     */
    private static final double[][] HOWDAH_ARCHER_OFFSETS = new double[][] {
            // Wide howdah body: left side, front to rear.
            { 7.3D, -4.35D, 16.85D, -90.0D },
            { 9.7D, -4.55D, 16.85D, -90.0D },
            {12.1D, -4.35D, 16.85D, -90.0D },

            // Wide howdah body: right side, front to rear.
            { 7.3D,  4.35D, 16.85D,  90.0D },
            { 9.7D,  4.55D, 16.85D,  90.0D },
            {12.1D,  4.35D, 16.85D,  90.0D },

            // Lower side perches: two on each lower perch.
            { 8.2D, -5.45D, 14.45D, -90.0D },
            {11.6D, -5.45D, 14.45D, -90.0D },
            { 8.2D,  5.45D, 14.45D,  90.0D },
            {11.6D,  5.45D, 14.45D,  90.0D },

            // Middle perch: four across the middle upper perch/bridge area.
            { 6.3D, -1.8D, 18.35D,   0.0D },
            { 7.8D, -0.6D, 18.35D,   0.0D },
            { 7.8D,  0.6D, 18.35D,   0.0D },
            { 6.3D,  1.8D, 18.35D,   0.0D },

            // Top perch: one lookout.
            { 9.8D,  0.0D, 21.35D,   0.0D }
    };

    private static boolean loggedMissingArcherClass;
    private static boolean loggedResolvedArcherClass;

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
            makeArcherPassive((EntityLiving)living);
        }
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event == null || event.world == null || event.world.isRemote || event.phase != TickEvent.Phase.END) {
            return;
        }

        updateAllTaggedArchersAtEndOfTick(event.world);
    }

    private static void updateAllTaggedArchersAtEndOfTick(World world) {
        List loaded = world.loadedEntityList;

        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);

            if (object instanceof EntityLiving && isTaggedHowdahArcher((Entity)object)) {
                updateTaggedArcher((EntityLiving)object);
            }
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
                if (valid < HOWDAH_ARCHER_OFFSETS.length) {
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

        System.out.println("[LOTRMoreMobs] Attempting to spawn " + HOWDAH_ARCHER_OFFSETS.length + " passive Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");

        for (int slot = 0; slot < HOWDAH_ARCHER_OFFSETS.length; ++slot) {
            EntityLiving archer = createSouthronArcher(world);
            if (archer == null) {
                break;
            }

            archer.onSpawnWithEgg(null);
            tagArcher(archer, mumakil, mountUuid, slot);
            makeArcherPassive(archer);
            placeArcherOnHowdah(archer, mumakil, slot);

            if (world.spawnEntityInWorld(archer)) {
                ++spawned;
            }
        }

        System.out.println("[LOTRMoreMobs] Spawned " + spawned + " passive Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");
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
        boolean[] seenSlots = new boolean[HOWDAH_ARCHER_OFFSETS.length];
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
            if (slot < 0 || slot >= HOWDAH_ARCHER_OFFSETS.length || seenSlots[slot]) {
                archer.setDead();
                ++removed;
                continue;
            }

            seenSlots[slot] = true;
            ++valid;
            archer.getEntityData().setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
            makeArcherPassive(archer);
            placeArcherOnHowdah(archer, mumakil, slot);
        }

        if (removed > 0) {
            System.out.println("[LOTRMoreMobs] Removed " + removed + " duplicate passive Mumakil howdah archers for mount=" + mumakil.getEntityId() + ".");
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

    private static EntityLiving createSouthronArcher(World world) {
        for (int i = 0; i < SOUTHON_ARCHER_CLASS_NAMES.length; ++i) {
            try {
                Class archerClass = Class.forName(SOUTHON_ARCHER_CLASS_NAMES[i]);
                Constructor constructor = archerClass.getConstructor(new Class[] { World.class });
                Object created = constructor.newInstance(new Object[] { world });

                if (created instanceof EntityLiving) {
                    if (!loggedResolvedArcherClass) {
                        loggedResolvedArcherClass = true;
                        System.out.println("[LOTRMoreMobs] Using howdah archer class: " + SOUTHON_ARCHER_CLASS_NAMES[i]);
                    }

                    return (EntityLiving)created;
                }
            } catch (Exception e) {
            }
        }

        if (!loggedMissingArcherClass) {
            loggedMissingArcherClass = true;
            System.out.println("[LOTRMoreMobs] Could not find a Southron archer entity class for passive Mumakil howdah archers.");
            System.out.println("[LOTRMoreMobs] Tried: LOTREntitySouthronArcher, LOTREntityNearHaradrimArcher, LOTREntityNearHaradArcher, LOTREntityHaradrimArcher");
        }

        return null;
    }

    private static void tagArcher(EntityLiving archer, LOTREntityMumakil mumakil, String mountUuid, int slot) {
        NBTTagCompound data = archer.getEntityData();
        data.setBoolean(ARCHER_TAG_KEY, true);
        data.setInteger(ARCHER_SLOT_KEY, slot);
        data.setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
        data.setString(ARCHER_MOUNT_UUID_KEY, mountUuid);
    }

    private static void updateTaggedArcher(EntityLiving archer) {
        NBTTagCompound data = archer.getEntityData();
        LOTREntityMumakil mumakil = findMountForArcher(archer, data);

        if (mumakil == null || !mumakil.isEntityAlive() || !mumakil.hasMumakilHowdahEquipped()) {
            makeArcherPassive(archer);

            /*
             * If the mount is not loaded yet during chunk-load ordering, give it
             * a few seconds to appear before cleaning up the orphan passenger.
             */
            if (archer.ticksExisted > 100) {
                archer.setDead();
            }

            return;
        }

        int slot = MathHelper.clamp_int(data.getInteger(ARCHER_SLOT_KEY), 0, HOWDAH_ARCHER_OFFSETS.length - 1);
        data.setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());

        makeArcherPassive(archer);
        placeArcherOnHowdah(archer, mumakil, slot);
    }

    private static LOTREntityMumakil findMountForArcher(EntityLiving archer, NBTTagCompound data) {
        int mountId = data.getInteger(ARCHER_MOUNT_ID_KEY);
        Entity byId = mountId > 0 ? archer.worldObj.getEntityByID(mountId) : null;

        if (byId instanceof LOTREntityMumakil) {
            return (LOTREntityMumakil)byId;
        }

        if (archer.ticksExisted % 20 != 0) {
            return null;
        }

        String mountUuid = data.getString(ARCHER_MOUNT_UUID_KEY);
        if (mountUuid == null || mountUuid.length() == 0) {
            return null;
        }

        List loaded = archer.worldObj.loadedEntityList;
        for (int i = 0; i < loaded.size(); ++i) {
            Object object = loaded.get(i);
            if (object instanceof LOTREntityMumakil) {
                LOTREntityMumakil candidate = (LOTREntityMumakil)object;
                if (mountUuid.equals(getEntityPersistentIdString(candidate))) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static void placeArcherOnHowdah(EntityLiving archer, LOTREntityMumakil mumakil, int slot) {
        double[] offset = HOWDAH_ARCHER_OFFSETS[slot];
        double forwardOffset = offset[0];
        double sideOffset = offset[1];
        double verticalOffset = offset[2];
        float placementYaw = mumakil.renderYawOffset;
        float yawRadians = placementYaw * 3.1415927F / 180.0F;

        double forwardX = -MathHelper.sin(yawRadians) * forwardOffset;
        double forwardZ = MathHelper.cos(yawRadians) * forwardOffset;
        double sideX = MathHelper.cos(yawRadians) * sideOffset;
        double sideZ = MathHelper.sin(yawRadians) * sideOffset;

        double x = mumakil.posX + forwardX + sideX;
        double y = mumakil.posY + verticalOffset;
        double z = mumakil.posZ + forwardZ + sideZ;
        float archerYaw = MathHelper.wrapAngleTo180_float(placementYaw + (float)offset[3]);

        archer.setPositionAndRotation(x, y, z, archerYaw, 0.0F);
        archer.prevPosX = x;
        archer.prevPosY = y;
        archer.prevPosZ = z;
        archer.lastTickPosX = x;
        archer.lastTickPosY = y;
        archer.lastTickPosZ = z;
        archer.motionX = 0.0D;
        archer.motionY = 0.0D;
        archer.motionZ = 0.0D;
        archer.fallDistance = 0.0F;
        archer.onGround = true;
        archer.isAirBorne = false;

        archer.rotationYaw = archerYaw;
        archer.prevRotationYaw = archerYaw;
        archer.rotationPitch = 0.0F;
        archer.prevRotationPitch = 0.0F;
        archer.renderYawOffset = archerYaw;
        archer.prevRenderYawOffset = archerYaw;
        archer.rotationYawHead = archerYaw;
        archer.prevRotationYawHead = archerYaw;
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
