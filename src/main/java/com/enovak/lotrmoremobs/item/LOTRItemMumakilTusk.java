package com.enovak.lotrmoremobs.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class LOTRItemMumakilTusk extends Item {

    public LOTRItemMumakilTusk() {
        this.setMaxStackSize(64);
        this.setCreativeTab(CreativeTabs.tabMaterials);
        this.setUnlocalizedName("mumakil_tusk");
        this.setTextureName("lotrmoremobs:mumakil_tusk");
    }
}
