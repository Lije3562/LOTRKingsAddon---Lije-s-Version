package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
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
 * This deliberately does not add target AI, attack AI, arrow shooting, downward
 * AABB searches, or combat relays. The archers are ordinary LOTR Southron archer
 * entities that are tagged, stripped of AI tasks, and repositioned onto fixed
 * howdah offsets each tick.
 */
public class MumakilHowdahArcherEventHandler {
    private static final String MUMAKIL_ARCHERS_SPAWNED_KEY = "LOTRMoreMobsHowdahArchersSpawned";
    private static final String ARCHER_TAG_KEY = "LOTRMoreMobsHowdahArcher";
    private static final String ARCHER_SLOT_KEY = "LOTRMoreMobsHowdahArcherSlot";
    private static final String ARCHER_MOUNT_ID_KEY = "LOTRMoreMobsHowdahMountId";
    private static final String ARCHER_MOUNT_UUID_KEY = "LOTRMoreMobsHowdahMountUuid";

    private static final String[] SOUTHON_ARCHER_CLASS_NAMES = new String[] {
            "lotr.common.entity.npc.LOTREntitySouthronArcher",
            "lotr.common.entity.npc.LOTREntityNearHaradArcher",
            "lotr.common.entity.npc.LOTREntityHaradrimArcher"
    };

    /**
     * Each row is: forward offset, side offset, vertical offset, yaw offset.
     * Forward = toward Mumakil head. Side = positive to Mumakil right.
     */
    private static final double[][] HOWDAH_ARCHER_OFFSETS = new double[][] {
            { 6.2D, -2.7D, 16.75D,   0.0D },
            { 6.2D,  2.7D, 16.75D,   0.0D },
            {13.1D, -2.7D, 16.75D, 180.0D },
            {13.1D,  2.7D, 16.75D, 180.0D }
    };

    private boolean loggedMissingArcherClass;

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || event.world.isRemote) {
            return;
        }

        if (this.isTaggedHowdahArcher(event.entity) && event.entity instanceof EntityLiving) {
            this.makeArcherPassive((EntityLiving)event.entity);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null || event.entityLiving.worldObj.isRemote) {
            return;
        }

        EntityLivingBase living = event.entityLiving;

        if (living instanceof LOTREntityMumakil) {
            this.updateHiredMumakilArchers((LOTREntityMumakil)living);
            return;
        }

        if (living instanceof EntityLiving && this.isTaggedHowdahArcher(living)) {
            this.updateTaggedArcher((EntityLiving)living);
        }
    }

    private void updateHiredMumakilArchers(LOTREntityMumakil mumakil) {
        if (!mumakil.getBelongsToNPC() || !mumakil.hasMumakilHowdahEquipped()) {
            return;
        }

        NBTTagCompound data = mumakil.getEntityData();
        if (data.getBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY)) {
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

        data.setBoolean(MUMAKIL_ARCHERS_SPAWNED_KEY, true);
        this.spawnPassiveHowdahArchers(mumakil);
    }

    private void spawnPassiveHowdahArchers(LOTREntityMumakil mumakil) {
        World world = mumakil.worldObj;
        String mountUuid = this.getEntityPersistentIdString(mumakil);
        int spawned = 0;

        for (int slot = 0; slot < HOWDAH_ARCHER_OFFSETS.length; ++slot) {
            EntityLiving archer = this.createSouthronArcher(world);
            if (archer == null) {
                return;
            }

            archer.onSpawnWithEgg(null);
            this.tagArcher(archer, mumakil, mountUuid, slot);
            this.makeArcherPassive(archer);
            this.placeArcherOnHowdah(archer, mumakil, slot);

            if (world.spawnEntityInWorld(archer)) {
                ++spawned;
            }
        }

        System.out.println("[LOTRMoreMobs] Spawned " + spawned + " passive Mumakil howdah archers.");
    }

    private EntityLiving createSouthronArcher(World world) {
        for (int i = 0; i < SOUTHON_ARCHER_CLASS_NAMES.length; ++i) {
            try {
                Class archerClass = Class.forName(SOUTHON_ARCHER_CLASS_NAMES[i]);
                Constructor constructor = archerClass.getConstructor(new Class[] { World.class });
                Object created = constructor.newInstance(new Object[] { world });

                if (created instanceof EntityLiving) {
                    return (EntityLiving)created;
                }
            } catch (Exception e) {
            }
        }

        if (!this.loggedMissingArcherClass) {
            this.loggedMissingArcherClass = true;
            System.out.println("[LOTRMoreMobs] Could not find a Southron archer entity class for passive Mumakil howdah archers.");
        }

        return null;
    }

    private void tagArcher(EntityLiving archer, LOTREntityMumakil mumakil, String mountUuid, int slot) {
        NBTTagCompound data = archer.getEntityData();
        data.setBoolean(ARCHER_TAG_KEY, true);
        data.setInteger(ARCHER_SLOT_KEY, slot);
        data.setInteger(ARCHER_MOUNT_ID_KEY, mumakil.getEntityId());
        data.setString(ARCHER_MOUNT_UUID_KEY, mountUuid);
    }

    private void updateTaggedArcher(EntityLiving archer) {
        NBTTagCompound data = archer.getEntityData();
        LOTREntityMumakil mumakil = this.findMountForArcher(archer, data);

        if (mumakil == null || !mumakil.isEntityAlive() || !mumakil.hasMumakilHowdahEquipped()) {
            this.makeArcherPassive(archer);

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

        this.makeArcherPassive(archer);
        this.placeArcherOnHowdah(archer, mumakil, slot);
    }

    private LOTREntityMumakil findMountForArcher(EntityLiving archer, NBTTagCompound data) {
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
                if (mountUuid.equals(this.getEntityPersistentIdString(candidate))) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private void placeArcherOnHowdah(EntityLiving archer, LOTREntityMumakil mumakil, int slot) {
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
        archer.motionX = 0.0D;
        archer.motionY = 0.0D;
        archer.motionZ = 0.0D;
        archer.fallDistance = 0.0F;
        archer.onGround = true;

        archer.rotationYaw = archerYaw;
        archer.prevRotationYaw = archerYaw;
        archer.rotationPitch = 0.0F;
        archer.prevRotationPitch = 0.0F;
        archer.renderYawOffset = archerYaw;
        archer.prevRenderYawOffset = archerYaw;
        archer.rotationYawHead = archerYaw;
        archer.prevRotationYawHead = archerYaw;
    }

    private void makeArcherPassive(EntityLiving archer) {
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
        this.invokeNoArgMethod(archer, "enablePersistence", "func_110163_bv");
    }

    private boolean isTaggedHowdahArcher(Entity entity) {
        return entity != null && entity.getEntityData().getBoolean(ARCHER_TAG_KEY);
    }

    private String getEntityPersistentIdString(Entity entity) {
        Object id = this.invokeNoArgMethod(entity, "getPersistentID", "getUniqueID", "func_110124_au");
        return id == null ? "" : id.toString();
    }

    private Object invokeNoArgMethod(Object target, String... methodNames) {
        for (int i = 0; i < methodNames.length; ++i) {
            try {
                Method method = this.findNoArgMethod(target.getClass(), methodNames[i]);
                if (method != null) {
                    return method.invoke(target, new Object[0]);
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    private Method findNoArgMethod(Class type, String name) {
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
