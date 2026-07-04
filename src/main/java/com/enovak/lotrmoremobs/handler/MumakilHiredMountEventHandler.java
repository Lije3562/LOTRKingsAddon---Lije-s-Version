package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.ai.LOTREntityAINearestAttackableTargetDrivenMumakil;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import java.util.Iterator;
import lotr.common.LOTRMod;
import lotr.common.entity.npc.LOTREntityNPC;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

public class MumakilHiredMountEventHandler {
    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;

    private static final float MUMAKIL_MIN_FOLLOW_DIST = 30.0F;
    private static final float MUMAKIL_MAX_NEAR_DIST = 18.0F;

    /*
     * This is vanilla-style target AI chance, not the old custom relay interval.
     * Lower numbers make the Mumakil notice enemies more consistently. Keep it
     * above 1 so we do not turn this into an every-tick scan.
     */
    private static final int DRIVEN_MUMAKIL_TARGET_CHANCE = 8;
    private static final double DRIVEN_MUMAKIL_COMBAT_FOLLOW_RANGE = 36.0D;

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
        this.applyDrivenMumakilCombatRange(mumakil);
        this.ensureDrivenMumakilTargetAI(mumakil);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event == null || event.entityLiving == null || event.entityLiving.worldObj == null) {
            return;
        }

        if (event.entityLiving.worldObj.isRemote || !(event.entityLiving instanceof LOTREntityMumakil)) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil)event.entityLiving;
        LOTREntityNPC driver = this.getMumakilDriver(mumakil);

        if (driver == null || !mumakil.hasMumakilHowdahEquipped() || event.source == null) {
            return;
        }

        Entity attacker = event.source.getEntity();

        if (attacker instanceof EntityLivingBase && this.canDriverAttackTarget(driver, (EntityLivingBase)attacker)) {
            driver.setAttackTarget((EntityLivingBase)attacker);
            mumakil.setAttackTarget((EntityLivingBase)attacker);
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

    private void applyDrivenMumakilCombatRange(LOTREntityMumakil mumakil) {
        IAttributeInstance followRange = mumakil.getEntityAttribute(SharedMonsterAttributes.followRange);

        if (followRange == null) {
            return;
        }

        if (followRange.getBaseValue() != DRIVEN_MUMAKIL_COMBAT_FOLLOW_RANGE) {
            followRange.setBaseValue(DRIVEN_MUMAKIL_COMBAT_FOLLOW_RANGE);
            System.out.println("[LOTRMoreMobs] Applied driven Mumakil combat follow range "
                    + DRIVEN_MUMAKIL_COMBAT_FOLLOW_RANGE
                    + ".");
        }
    }

    private void ensureDrivenMumakilTargetAI(LOTREntityMumakil mumakil) {
        if (mumakil.targetTasks == null || mumakil.targetTasks.taskEntries == null) {
            return;
        }

        Iterator iterator = mumakil.targetTasks.taskEntries.iterator();

        while (iterator.hasNext()) {
            Object taskEntry = iterator.next();
            Object aiTask = this.getFieldValue(taskEntry, "action");

            if (aiTask instanceof LOTREntityAINearestAttackableTargetDrivenMumakil) {
                return;
            }
        }

        mumakil.targetTasks.addTask(2, new LOTREntityAINearestAttackableTargetDrivenMumakil(
                mumakil,
                DRIVEN_MUMAKIL_TARGET_CHANCE
        ));

        System.out.println("[LOTRMoreMobs] Installed driven Mumakil Southron target AI. targetChance="
                + DRIVEN_MUMAKIL_TARGET_CHANCE
                + ".");
    }

    private LOTREntityNPC getMumakilDriver(LOTREntityMumakil mumakil) {
        if (mumakil != null && mumakil.riddenByEntity instanceof LOTREntityNPC) {
            LOTREntityNPC npc = (LOTREntityNPC)mumakil.riddenByEntity;

            if (npc.ridingEntity == mumakil) {
                return npc;
            }
        }

        return null;
    }

    private boolean canDriverAttackTarget(LOTREntityNPC driver, EntityLivingBase target) {
        if (driver == null || target == null || target == driver || !target.isEntityAlive()) {
            return false;
        }

        if (!(driver instanceof EntityCreature)) {
            return false;
        }

        return LOTRMod.canNPCAttackEntity((EntityCreature)driver, target, false);
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
