package com.enovak.lotrmoremobs.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class LOTRItemMumakilHowdah extends LOTRItemMumakilEquipment {

    public LOTRItemMumakilHowdah() {
        this.setMaxStackSize(1);
        this.setCreativeTab(CreativeTabs.tabTransport);
        this.setUnlocalizedName("mumakil_howdah");
        this.setTextureName("lotrmoremobs:mumakil_howdah");
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target) {
        /*
         * Player equipment is inventory-only. Entity interaction owns normal
         * mounting and shift-right-click inventory access.
         */
        return false;
    }
}
