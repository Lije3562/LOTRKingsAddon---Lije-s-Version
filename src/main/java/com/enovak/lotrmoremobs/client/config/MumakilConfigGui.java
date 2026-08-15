package com.enovak.lotrmoremobs.client.config;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.config.MumakilConfig;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

@SideOnly(Side.CLIENT)
public final class MumakilConfigGui extends GuiConfig {
    public MumakilConfigGui(GuiScreen parentScreen) {
        super(
                parentScreen,
                getCategoryElements(),
                Main.MODID,
                false,
                false,
                "LOTR KOME Addon"
        );
    }

    /**
     * Keep the first screen clean: each top-level element opens one focused
     * feature category instead of flattening every property into one list.
     */
    private static List<IConfigElement> getCategoryElements() {
        List<IConfigElement> elements = new ArrayList<IConfigElement>();
        addCategory(elements, MumakilConfig.CATEGORY_MUMAKIL);
        addCategory(elements, MumakilConfig.CATEGORY_PICKUP_FILTER);
        addCategory(elements, MumakilConfig.CATEGORY_MORTAL_GANDALF);
        addCategory(elements, MumakilConfig.CATEGORY_SIEGE_GATES);
        addCategory(elements, MumakilConfig.CATEGORY_BATTLE_RAMS);
        return elements;
    }

    private static void addCategory(
            List<IConfigElement> elements,
            String categoryName
    ) {
        elements.add(
                new ConfigElement(
                        MumakilConfig.getConfiguration().getCategory(
                                categoryName
                        )
                )
        );
    }
}
