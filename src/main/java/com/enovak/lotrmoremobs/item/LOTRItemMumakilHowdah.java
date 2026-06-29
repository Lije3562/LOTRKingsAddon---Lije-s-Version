package com.enovak.lotrmoremobs.item;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class LOTRItemMumakilHowdah extends LOTRItemMumakilEquipment {

    public LOTRItemMumakilHowdah() {
        this.setMaxStackSize(8);
        this.setCreativeTab(CreativeTabs.tabTransport);
        this.setUnlocalizedName("mumakil_howdah");
        this.setTextureName("lotrmoremobs:mumakil_howdah");
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target) {
        if (!this.isMumakil(target)) {
            return false;
        }

        LOTREntityMumakil mumakil = this.asMumakil(target);
        if (this.hasWarEquipmentStack(mumakil)) {
            return false;
        }

        if (mumakil.worldObj.isRemote) {
            return true;
        }

        // The current renderer intentionally shows war/howdah bones only when the saddle layer is also active.
        this.setMountSaddled(mumakil, true);
        this.equipSaddleSlot(mumakil, new ItemStack(Items.saddle));
        this.equipWarEquipmentSlot(mumakil, new ItemStack(Main.mumakilHowdah));
        this.consumeOne(stack, player);
        player.swingItem();

        return true;
    }
}