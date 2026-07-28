package com.enovak.lotrmoremobs.item;

import com.enovak.lotrmoremobs.Main;
import net.minecraft.item.ItemFood;

/**
 * MUMAKIL_SHANK_SYSTEM_V1_1
 *
 * Raw and cooked Mumakil shanks share the current raw-shank texture.
 * Held movement effects are managed by MumakilShankHeldEffectHandler.
 */
public class LOTRItemMumakilShank extends ItemFood {
    private static final String SHARED_TEXTURE =
            Main.MODID + ":mumakil_shank";

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

        /*
         * Temporary art choice: the cooked item intentionally reuses the
         * raw Mumakil shank icon until a separate cooked texture is made.
         */
        this.setTextureName(SHARED_TEXTURE);
    }
}