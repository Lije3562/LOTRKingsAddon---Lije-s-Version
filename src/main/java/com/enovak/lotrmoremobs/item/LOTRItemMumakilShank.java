package com.enovak.lotrmoremobs.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.world.World;

public class LOTRItemMumakilShank extends ItemFood {
    private static final int HELD_SLOWNESS_REFRESH_INTERVAL = 10;
    private static final int HELD_SLOWNESS_DURATION = 30;

    public LOTRItemMumakilShank() {
        super(4, 0.4F, true);
        this.setUnlocalizedName("mumakil_shank");
        this.setTextureName("lotrmoremobs:mumakil_shank");
    }

    @Override
    public void onUpdate(ItemStack itemstack, World world, Entity entity, int slot, boolean isHeld) {
        super.onUpdate(itemstack, world, entity, slot, isHeld);

        if (!world.isRemote
                && isHeld
                && entity instanceof EntityPlayer
                && entity.ticksExisted % HELD_SLOWNESS_REFRESH_INTERVAL == 0) {
            ((EntityPlayer)entity).addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, HELD_SLOWNESS_DURATION, 0));
        }
    }
}
