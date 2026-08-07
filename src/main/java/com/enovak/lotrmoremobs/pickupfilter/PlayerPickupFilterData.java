package com.enovak.lotrmoremobs.pickupfilter;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Persistent per-player storage for the pickup filter.
 *
 * The data lives under EntityPlayer.PERSISTED_NBT_TAG so it survives normal
 * player saves and is copied when the player respawns.
 */
public final class PlayerPickupFilterData {
    private static final String FILTER_TAG = "lotrmoremobsPickupFilter";
    private static final String EXCLUDED_ITEMS_TAG = "excludedItems";
    private static final int TAG_COMPOUND = 10;

    private PlayerPickupFilterData() {
    }

    public static List<ItemStack> getExcludedItems(EntityPlayer player) {
        List<ItemStack> excludedItems = new ArrayList<ItemStack>();
        NBTTagCompound filterData = getFilterData(player, false);

        if (filterData == null) {
            return excludedItems;
        }

        NBTTagList itemList = filterData.getTagList(EXCLUDED_ITEMS_TAG, TAG_COMPOUND);

        for (int i = 0; i < itemList.tagCount(); ++i) {
            ItemStack stack = ItemStack.loadItemStackFromNBT(itemList.getCompoundTagAt(i));

            if (stack != null) {
                excludedItems.add(stack);
            }
        }

        return excludedItems;
    }

    public static boolean isExcluded(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null) {
            return false;
        }

        for (ItemStack excluded : getExcludedItems(player)) {
            if (sameItem(excluded, stack)) {
                return true;
            }
        }

        return false;
    }

    public static boolean addExcludedItem(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null || isExcluded(player, stack)) {
            return false;
        }

        List<ItemStack> excludedItems = getExcludedItems(player);
        ItemStack storedStack = stack.copy();
        storedStack.stackSize = 1;
        excludedItems.add(storedStack);
        writeExcludedItems(player, excludedItems);
        return true;
    }

    public static boolean removeExcludedItem(EntityPlayer player, ItemStack stack) {
        if (player == null || stack == null) {
            return false;
        }

        List<ItemStack> excludedItems = getExcludedItems(player);
        boolean removed = false;
        Iterator<ItemStack> iterator = excludedItems.iterator();

        while (iterator.hasNext()) {
            if (sameItem(iterator.next(), stack)) {
                iterator.remove();
                removed = true;
            }
        }

        if (removed) {
            writeExcludedItems(player, excludedItems);
        }

        return removed;
    }

    public static void clearExcludedItems(EntityPlayer player) {
        if (player != null) {
            writeExcludedItems(player, new ArrayList<ItemStack>());
        }
    }

    private static boolean sameItem(ItemStack first, ItemStack second) {
        return first != null && second != null && first.isItemEqual(second);
    }

    private static void writeExcludedItems(EntityPlayer player, List<ItemStack> excludedItems) {
        NBTTagCompound filterData = getFilterData(player, true);
        NBTTagList itemList = new NBTTagList();

        for (ItemStack stack : excludedItems) {
            if (stack == null) {
                continue;
            }

            ItemStack storedStack = stack.copy();
            storedStack.stackSize = 1;
            NBTTagCompound stackTag = new NBTTagCompound();
            storedStack.writeToNBT(stackTag);
            itemList.appendTag(stackTag);
        }

        filterData.setTag(EXCLUDED_ITEMS_TAG, itemList);
    }

    private static NBTTagCompound getFilterData(EntityPlayer player, boolean create) {
        if (player == null) {
            return null;
        }

        NBTTagCompound entityData = player.getEntityData();
        NBTTagCompound persistedData;

        if (entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, TAG_COMPOUND)) {
            persistedData = entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
        } else {
            if (!create) {
                return null;
            }

            persistedData = new NBTTagCompound();
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, persistedData);
        }

        if (persistedData.hasKey(FILTER_TAG, TAG_COMPOUND)) {
            return persistedData.getCompoundTag(FILTER_TAG);
        }

        if (!create) {
            return null;
        }

        NBTTagCompound filterData = new NBTTagCompound();
        persistedData.setTag(FILTER_TAG, filterData);
        return filterData;
    }
}
