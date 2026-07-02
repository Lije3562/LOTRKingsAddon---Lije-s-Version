package com.enovak.lotrmoremobs.hiring;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import java.lang.reflect.Field;
import lotr.common.entity.npc.LOTREntitySouthronChampion;
import lotr.common.entity.npc.LOTRUnitTradeEntry;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class LOTRUnitTradeEntryMumakil extends LOTRUnitTradeEntry {
    public static final int MUMAKIL_HIRE_COST = 500;
    public static final float MUMAKIL_ALIGNMENT_REQUIRED = 3000.0F;

    private static final int SADDLE_SLOT = 0;
    private static final int HOWDAH_SLOT = 1;
    private static final String[] INVENTORY_FIELDS = new String[] {
            "horseChest",
            "mountInventory",
            "horseInventory",
            "inventory"
    };

    public LOTRUnitTradeEntryMumakil() {
        super(
                LOTREntitySouthronChampion.class,
                LOTREntityMumakil.class,
                "Mumakil_Howdah",
                MUMAKIL_HIRE_COST,
                MUMAKIL_ALIGNMENT_REQUIRED
        );
        this.setMountArmor(Main.mumakilHowdah, 1.0F);
        this.setExtraInfo("Requires 3000 Near Harad alignment. Includes a saddle and howdah.");
    }

    @Override
    public String getUnitTradeName() {
        return "Mumakil with Howdah";
    }

    @Override
    public EntityLiving createHiredMount(World world) {
        LOTREntityMumakil mumakil = new LOTREntityMumakil(world);
        mumakil.onSpawnWithEgg(null);
        mumakil.setBelongsToNPC(true);
        mumakil.setMountable(true);
        this.equipMumakilForHire(mumakil);
        return mumakil;
    }

    private void equipMumakilForHire(LOTREntityMumakil mumakil) {
        mumakil.saddleMountForWorldGen();
        this.setInventoryStack(mumakil, SADDLE_SLOT, new ItemStack(Items.saddle));
        this.setInventoryStack(mumakil, HOWDAH_SLOT, new ItemStack(Main.mumakilHowdah));
        mumakil.setMumakilHowdahEquipped(true);
    }

    private boolean setInventoryStack(LOTREntityMumakil mumakil, int slot, ItemStack stack) {
        IInventory inventory = this.findMountInventory(mumakil);
        if (inventory == null || slot < 0 || slot >= inventory.getSizeInventory()) {
            System.out.println("[LOTRMoreMobs] Could not equip hired Mumakil inventory slot " + slot);
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
