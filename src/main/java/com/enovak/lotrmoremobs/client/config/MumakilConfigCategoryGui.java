package com.enovak.lotrmoremobs.client.config;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.config.MumakilConfig;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

/** Native Forge property editor for one KOME config category. */
@SideOnly(Side.CLIENT)
public final class MumakilConfigCategoryGui extends GuiConfig {

    public MumakilConfigCategoryGui(
            GuiScreen parentScreen,
            String categoryName,
            String categoryTitle
    ) {
        super(
                parentScreen,
                getCategoryElements(categoryName),
                Main.MODID,
                false,
                false,
                "LOTR KOME Addon - " + categoryTitle
        );
    }

    private static List<IConfigElement> getCategoryElements(
            String categoryName
    ) {
        return new ConfigElement(
                MumakilConfig.getConfiguration().getCategory(categoryName)
        ).getChildElements();
    }
}
