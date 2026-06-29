package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Keeps Mumakil war equipment restricted to the custom Mumakil Howdah item.
 *
 * The inherited LOTR/horse inventory is intentionally reused so the existing renderer can keep reading
 * the saddle and armor slots normally. Any non-howdah item that reaches the armor slot is ejected.
 */
public class MumakilEquipmentEventHandler {
    private static final int WAR_EQUIPMENT_SLOT = 1;

    private static final String[] INVENTORY_FIELDS = new String[] {
            "horseChest",
            "mountInventory",
            "horseInventory",
            "inventory"
    };

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.entityLiving instanceof LOTREntityMumakil) || event.entityLiving.worldObj.isRemote) {
            return;
        }

        LOTREntityMumakil mumakil = (LOTREntityMumakil) event.entityLiving;
        IInventory inventory = this.findMountInventory(mumakil);
        if (inventory == null || WAR_EQUIPMENT_SLOT >= inventory.getSizeInventory()) {
            return;
        }

        ItemStack stack = inventory.getStackInSlot(WAR_EQUIPMENT_SLOT);
        if (stack == null || stack.getItem() == null || stack.getItem() == Main.mumakilHowdah) {
            return;
        }

        ItemStack rejected = stack.copy();
        inventory.setInventorySlotContents(WAR_EQUIPMENT_SLOT, null);
        inventory.markDirty();

        Entity entity = mumakil;
        if (rejected.stackSize > 0) {
            entity.entityDropItem(rejected, entity.height * 0.5F);
        }
    }

    private IInventory findMountInventory(LOTREntityMumakil mumakil) {
        for (int i = 0; i < INVENTORY_FIELDS.length; ++i) {
            Field field = this.findField(mumakil.getClass(), INVENTORY_FIELDS[i]);
            if (field != null) {
                try {
                    Object value = field.get(mumakil);
                    if (value instanceof IInventory) {
                        return (IInventory) value;
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