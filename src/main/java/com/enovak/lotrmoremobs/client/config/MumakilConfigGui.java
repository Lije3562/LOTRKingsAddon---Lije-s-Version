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
                getFlatConfigElements(),
                Main.MODID,
                false,
                false,
                "LOTR KOME Addon"
        );
    }

    private static List<IConfigElement> getFlatConfigElements() {
        List<IConfigElement> elements =
                new ArrayList<IConfigElement>();
        elements.addAll(
                new ConfigElement(
                        MumakilConfig.getConfiguration().getCategory(
                                MumakilConfig.CATEGORY_GAMEPLAY
                        )
                ).getChildElements()
        );
        elements.addAll(
                new ConfigElement(
                        MumakilConfig.getConfiguration().getCategory(
                                MumakilConfig.CATEGORY_SPAWNING
                        )
                ).getChildElements()
        );
        return elements;
    }
}
