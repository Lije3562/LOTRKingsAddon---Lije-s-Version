package com.enovak.lotrmoremobs.item;

import com.enovak.lotrmoremobs.Main;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

public class LOTRItemMumakilTusk extends Item {
    private static final String TUSK_ICON_NAME = Main.MODID + ":mumakil_tusk";

    public LOTRItemMumakilTusk() {
        this.setMaxStackSize(16);
        this.setCreativeTab(CreativeTabs.tabMaterials);
        this.setUnlocalizedName("mumakil_tusk");
        this.setTextureName(TUSK_ICON_NAME);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        System.out.println("[LOTRMoreMobs] Registering Mumakil tusk item icon: " + TUSK_ICON_NAME);
        this.itemIcon = iconRegister.registerIcon(TUSK_ICON_NAME);
    }
}
