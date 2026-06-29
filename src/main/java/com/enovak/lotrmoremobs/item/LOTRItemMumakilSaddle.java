package com.enovak.lotrmoremobs.item;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class LOTRItemMumakilSaddle extends LOTRItemMumakilEquipment {

    public LOTRItemMumakilSaddle() {
        this.setMaxStackSize(16);
        this.setCreativeTab(CreativeTabs.tabTransport);
        this.setUnlocalizedName("mumakil_saddle");
        this.setTextureName("lotrmoremobs:mumakil_saddle");
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target) {
        if (!this.isMumakil(target)) {
            return false;
        }

        LOTREntityMumakil mumakil = this.asMumakil(target);
        if (mumakil.isMountSaddled()) {
            return false;
        }

        if (mumakil.worldObj.isRemote) {
            return true;
        }

        this.setMountSaddled(mumakil, true);
        this.equipSaddleSlot(mumakil, new ItemStack(Items.saddle));
        this.consumeOne(stack, player);
        player.swingItem();

        return true;
    }
}