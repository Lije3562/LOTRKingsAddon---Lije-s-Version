package com.enovak.lotrmoremobs.item;

import com.enovak.lotrmoremobs.Main;
import lotr.common.LOTRCreativeTabs;
import net.minecraft.item.ItemFood;

/**
 * MUMAKIL_SHANK_SYSTEM_V1_1
 *
 * Held movement effects are managed by MumakilShankHeldEffectHandler.
 */
public class LOTRItemMumakilShank extends ItemFood {
    private static final String RAW_TEXTURE =
            Main.MODID + ":mumak_shank";
    private static final String COOKED_TEXTURE =
            Main.MODID + ":mumak_shank_cooked";

    public LOTRItemMumakilShank(boolean cooked) {
        super(
                cooked ? 8 : 3,
                cooked ? 0.8F : 0.3F,
                true
        );

        this.setUnlocalizedName(
                cooked
                        ? "cooked_mumakil_shank"
                        : "mumakil_shank"
        );
        this.setCreativeTab(LOTRCreativeTabs.tabFood);
        this.setTextureName(
                cooked ? COOKED_TEXTURE : RAW_TEXTURE
        );
    }
}
