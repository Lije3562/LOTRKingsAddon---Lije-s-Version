package com.enovak.lotrmoremobs.handler;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import java.lang.reflect.Field;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

public class MumakilHiredMountEventHandler {
    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;
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

    private void equipHiredMumakil(LOTREntityMumakil mumakil) {
        mumakil.saddleMountForWorldGen();
        this.setInventoryStack(mumakil, SADDLE_SLOT, new ItemStack(Items.saddle));
        this.setInventoryStack(mumakil, HOWDAH_SLOT, new ItemStack(Main.mumakilHowdah));
        mumakil.setMumakilHowdahEquipped(true);
        System.out.println("[LOTRMoreMobs] Equipped hired Mumakil with saddle and howdah.");
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
