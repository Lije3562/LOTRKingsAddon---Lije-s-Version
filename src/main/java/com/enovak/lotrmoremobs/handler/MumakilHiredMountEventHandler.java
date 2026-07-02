package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

public class MumakilHiredMountEventHandler {
    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;

    private static final double HOWDAH_RIDER_FORWARD = 9.5D;
    private static final double HOWDAH_RIDER_SIDE = 0.0D;
    private static final double IDLE_YAW_MOTION_THRESHOLD_SQ = 4.0E-4D;

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
        if (event == null || !(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)event.entityLiving;
        if (!mumakil.hasMumakilHowdahEquipped() || mumakil.riddenByEntity == null) {
            return;
        }

        if (!this.isStationaryIdleForHowdahRiderYawLock(mumakil)) {
            return;
        }

        this.applyHowdahRiderPosition(mumakil, mumakil.renderYawOffset);
    }

    private void equipHiredMumakil(LOTREntityMumakil mumakil) {
        mumakil.saddleMountForWorldGen();
        this.setInventoryStack(mumakil, SADDLE_SLOT, new ItemStack(Items.saddle));
        this.setInventoryStack(mumakil, HOWDAH_SLOT, new ItemStack(Main.mumakilHowdah));
        mumakil.setMumakilHowdahEquipped(true);
        this.tuneHiredMumakilFollowDistance(mumakil);
        System.out.println("[LOTRMoreMobs] Equipped hired Mumakil with saddle and howdah.");
    }

    private void applyHowdahRiderPosition(LOTREntityMumakil mumakil, float yaw) {
        Entity rider = mumakil.riddenByEntity;
        if (rider == null) {
            return;
        }

        double verticalOffset = mumakil.getMountedYOffset() + rider.getYOffset();
        float yawRadians = yaw * 3.1415927F / 180.0F;

        double forwardX = -MathHelper.sin(yawRadians) * HOWDAH_RIDER_FORWARD;
        double forwardZ = MathHelper.cos(yawRadians) * HOWDAH_RIDER_FORWARD;

        double sideX = MathHelper.cos(yawRadians) * HOWDAH_RIDER_SIDE;
        double sideZ = MathHelper.sin(yawRadians) * HOWDAH_RIDER_SIDE;

        rider.setPosition(
                mumakil.posX + forwardX + sideX,
                mumakil.posY + verticalOffset,
                mumakil.posZ + forwardZ + sideZ
        );

        rider.rotationYaw = yaw;
        rider.prevRotationYaw = yaw;

        if (rider instanceof EntityLivingBase) {
            EntityLivingBase livingRider = (EntityLivingBase)rider;
            livingRider.rotationYawHead = yaw;
            livingRider.prevRotationYawHead = yaw;
            livingRider.renderYawOffset = yaw;
            livingRider.prevRenderYawOffset = yaw;
        }
    }

    private boolean isStationaryIdleForHowdahRiderYawLock(LOTREntityMumakil mumakil) {
        if (mumakil.getAttackTarget() != null
                || mumakil.isSprinting()
                || !mumakil.onGround
                || Math.abs(mumakil.moveForward) > 0.01F
                || Math.abs(mumakil.moveStrafing) > 0.01F) {
            return false;
        }

        double horizontalMotionSq = mumakil.motionX * mumakil.motionX + mumakil.motionZ * mumakil.motionZ;
        return horizontalMotionSq <= IDLE_YAW_MOTION_THRESHOLD_SQ && mumakil.getNavigator().noPath();
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
                if (this.setFloatField(aiTask, "minFollowDist", 16.0F)) {
                    tuned = true;
                }
                if (this.setFloatField(aiTask, "maxNearDist", 12.0F)) {
                    tuned = true;
                }
            }
        }

        if (tuned) {
            System.out.println("[LOTRMoreMobs] Tuned hired Mumakil follow distance to 16/12 blocks.");
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
