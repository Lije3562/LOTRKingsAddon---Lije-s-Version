package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.ai.LOTREntityAINearestAttackableTargetHighRider;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.util.Iterator;
import lotr.common.LOTRMod;
import lotr.common.entity.ai.LOTRNPCTargetSelector;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

public class MumakilHiredMountEventHandler {
    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;

    private static final float MUMAKIL_MIN_FOLLOW_DIST = 30.0F;
    private static final float MUMAKIL_MAX_NEAR_DIST = 18.0F;

    /*
     * Matches the vanilla LOTR invasion-spawn behavior:
     * invasion NPCs have their followRange raised to at least 40.0 after spawn.
     */
    private static final double MUMAKIL_DRIVER_DETECTION_RANGE = 40.0D;
    private static final int MUMAKIL_DRIVER_HIGH_Y_TARGET_CHANCE = 20;
    private static final String DRIVER_RANGE_APPLIED_TAG = "LOTRMoreMobsMumakilDriverRangeApplied";
    private static final String DRIVER_RANGE_BASE_TAG = "LOTRMoreMobsMumakilDriverRangeBase";
    private static final String DRIVER_HIGH_Y_TARGET_AI_INSTALLED_TAG = "LOTRMoreMobsMumakilHighYTargetAIInstalled";

    private static final String[] INVENTORY_FIELDS = new String[] {
            "horseChest",
            "mountInventory",
            "horseInventory",
            "inventory"
    };

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event == null || event.world == null || event.world.isRemote) {
            return;
        }

        if (!(event.entity instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)event.entity;

        if (!mumakil.getBelongsToNPC()) {
            return;
        }

        this.equipHiredMumakil(mumakil);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null) {
            return;
        }

        if (event.entityLiving.worldObj.isRemote) {
            return;
        }

        /*
         * Keep this lightweight: boost the mounted driver's normal horizontal
         * followRange, install one high-Y version of LOTR's target AI, and mirror
         * any valid rider target onto the Mumakil.
         */
        if (event.entityLiving instanceof LOTREntityNPC) {
            this.updateMumakilDriverSupport((LOTREntityNPC)event.entityLiving);
        }
    }

    private void equipHiredMumakil(LOTREntityMumakil mumakil) {
        mumakil.saddleMountForWorldGen();
        this.setInventoryStack(mumakil, SADDLE_SLOT, new ItemStack(Items.saddle));
        this.setInventoryStack(mumakil, HOWDAH_SLOT, new ItemStack(Main.mumakilHowdah));
        mumakil.setMumakilHowdahEquipped(true);
        this.tuneHiredMumakilFollowDistance(mumakil);

        System.out.println("[LOTRMoreMobs] Equipped hired Mumakil with saddle and howdah.");
    }

    private void updateMumakilDriverSupport(LOTREntityNPC npc) {
        if (npc.ridingEntity instanceof LOTREntityMumakil) {
            LOTREntityMumakil mumakil = (LOTREntityMumakil)npc.ridingEntity;

            if (mumakil.hasMumakilHowdahEquipped()) {
                this.applyMumakilDriverRange(npc);
                this.ensureMumakilDriverHighYTargetAI(npc);
                this.syncMumakilTargetToDriver(npc, mumakil);
                return;
            }
        }

        this.restoreNormalDriverRange(npc);
    }

    private void applyMumakilDriverRange(LOTREntityNPC npc) {
        IAttributeInstance followRange = npc.getEntityAttribute(SharedMonsterAttributes.followRange);

        if (followRange == null) {
            return;
        }

        NBTTagCompound data = npc.getEntityData();

        if (!data.getBoolean(DRIVER_RANGE_APPLIED_TAG)) {
            double baseRange = followRange.getBaseValue();
            double newRange = Math.max(baseRange, MUMAKIL_DRIVER_DETECTION_RANGE);

            data.setBoolean(DRIVER_RANGE_APPLIED_TAG, true);
            data.setDouble(DRIVER_RANGE_BASE_TAG, baseRange);
            followRange.setBaseValue(newRange);

            System.out.println("[LOTRMoreMobs] Applied Mumakil driver detection range from "
                    + baseRange
                    + " to "
                    + newRange
                    + ".");
        }
    }

    private void ensureMumakilDriverHighYTargetAI(LOTREntityNPC npc) {
        NBTTagCompound data = npc.getEntityData();
        if (data.getBoolean(DRIVER_HIGH_Y_TARGET_AI_INSTALLED_TAG)) {
            return;
        }

        if (npc.targetTasks == null) {
            return;
        }

        data.setBoolean(DRIVER_HIGH_Y_TARGET_AI_INSTALLED_TAG, true);
        npc.targetTasks.addTask(4, new LOTREntityAINearestAttackableTargetHighRider(
                npc,
                EntityPlayer.class,
                MUMAKIL_DRIVER_HIGH_Y_TARGET_CHANCE,
                true
        ));
        npc.targetTasks.addTask(4, new LOTREntityAINearestAttackableTargetHighRider(
                npc,
                EntityLiving.class,
                MUMAKIL_DRIVER_HIGH_Y_TARGET_CHANCE,
                true,
                new LOTRNPCTargetSelector(npc)
        ));

        System.out.println("[LOTRMoreMobs] Installed Mumakil high-vertical rider target AI on "
                + npc.getClass().getName()
                + ".");
    }

    private void restoreNormalDriverRange(LOTREntityNPC npc) {
        NBTTagCompound data = npc.getEntityData();

        if (!data.getBoolean(DRIVER_RANGE_APPLIED_TAG)) {
            return;
        }

        IAttributeInstance followRange = npc.getEntityAttribute(SharedMonsterAttributes.followRange);

        if (followRange != null) {
            followRange.setBaseValue(data.getDouble(DRIVER_RANGE_BASE_TAG));
        }

        data.removeTag(DRIVER_RANGE_APPLIED_TAG);
        data.removeTag(DRIVER_RANGE_BASE_TAG);

        System.out.println("[LOTRMoreMobs] Restored normal Mumakil driver detection range.");
    }

    private void syncMumakilTargetToDriver(LOTREntityNPC npc, LOTREntityMumakil mumakil) {
        EntityLivingBase riderTarget = npc.getAttackTarget();

        if (riderTarget != null && riderTarget.isEntityAlive() && this.canDriverAttackTarget(npc, riderTarget)) {
            if (mumakil.getAttackTarget() != riderTarget) {
                mumakil.setAttackTarget(riderTarget);
            }
            return;
        }

        EntityLivingBase mumakilTarget = mumakil.getAttackTarget();
        if (mumakilTarget != null && (!mumakilTarget.isEntityAlive() || mumakilTarget == npc)) {
            mumakil.setAttackTarget(null);
        }
    }

    private boolean canDriverAttackTarget(LOTREntityNPC npc, EntityLivingBase target) {
        if (target == null || target == npc || !target.isEntityAlive()) {
            return false;
        }

        if (!(npc instanceof EntityCreature)) {
            return false;
        }

        return LOTRMod.canNPCAttackEntity((EntityCreature)npc, target, false);
    }

    private void tuneHiredMumakilFollowDistance(LOTREntityMumakil mumakil) {
        if (mumakil.tasks == null || mumakil.tasks.taskEntries == null) {
            return;
        }

        boolean tuned = false;
        Iterator iterator = mumakil.tasks.taskEntries.iterator();

        while (iterator.hasNext()) {
            Object taskEntry = iterator.next();
            Object aiTask = this.getFieldValue(taskEntry, "action");

            if (aiTask == null) {
                continue;
            }

            String aiName = aiTask.getClass().getName();

            if (aiName.endsWith("LOTREntityAIHorseFollowHiringPlayer")) {
                if (this.setFloatField(aiTask, "minFollowDist", MUMAKIL_MIN_FOLLOW_DIST)) {
                    tuned = true;
                }

                if (this.setFloatField(aiTask, "maxNearDist", MUMAKIL_MAX_NEAR_DIST)) {
                    tuned = true;
                }
            }
        }

        if (tuned) {
            System.out.println("[LOTRMoreMobs] Tuned hired Mumakil follow distance to "
                    + MUMAKIL_MIN_FOLLOW_DIST
                    + "/"
                    + MUMAKIL_MAX_NEAR_DIST
                    + " blocks.");
        }
    }

    private Object getFieldValue(Object object, String name) {
        try {
            Field field = this.findField(object.getClass(), name);
            return field == null ? null : field.get(object);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean setFloatField(Object object, String name, float value) {
        try {
            Field field = this.findField(object.getClass(), name);

            if (field != null) {
                field.setFloat(object, value);
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    private boolean setInventoryStack(LOTREntityMumakil mumakil, int slot, ItemStack stack) {
        IInventory inventory = this.findMountInventory(mumakil);

        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) {
            System.out.println("[LOTRMoreMobs] Could not set hired Mumakil inventory slot " + slot);
            return false;
        }

        inventory.setInventorySlotContents(slot, stack);
        inventory.markDirty();
        return true;
    }

    private IInventory findMountInventory(LOTREntityMumakil mumakil) {
        for (int i = 0; i < INVENTORY_FIELDS.length; ++i) {
            Field field = this.findField(mumakil.getClass(), INVENTORY_FIELDS[i]);

            if (field != null) {
                try {
                    Object value = field.get(mumakil);

                    if (value instanceof IInventory) {
                        return (IInventory)value;
                    }
                } catch (Exception e) {
                }
            }
        }

        return null;
    }

    private Field findField(Class type, String name) {
        Class current = type;

        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }
}
