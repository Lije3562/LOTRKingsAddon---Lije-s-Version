package com.enovak.lotrmoremobs.client;

import com.enovak.lotrmoremobs.client.gui.LOTRGuiUnitTradePledgeNavigation;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import lotr.client.gui.LOTRGuiUnitTrade;
import lotr.common.entity.npc.LOTRUnitTradeable;
import lotr.common.inventory.LOTRContainerUnitTrade;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.GuiOpenEvent;

/**
 * Replaces only LOTR's native unit-trade screen with an addon subclass that
 * owns a real pledge-navigation button. No Post overlay, tooltip repaint, or
 * low-level GL state handling remains here.
 */
public final class UnitTradePledgeNavigationHandler {
    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.gui == null
                || event.gui.getClass()
                != LOTRGuiUnitTrade.class) {
            return;
        }

        LOTRGuiUnitTrade original =
                (LOTRGuiUnitTrade)event.gui;
        if (!(original.inventorySlots
                instanceof LOTRContainerUnitTrade)) {
            return;
        }
        LOTRContainerUnitTrade container =
                (LOTRContainerUnitTrade)original.inventorySlots;
        if (!(container.theUnitTrader
                instanceof LOTRUnitTradeable)) {
            return;
        }

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null
                || minecraft.thePlayer == null
                || minecraft.theWorld == null) {
            return;
        }

        event.gui = new LOTRGuiUnitTradePledgeNavigation(
                minecraft.thePlayer,
                (LOTRUnitTradeable)container.theUnitTrader,
                minecraft.theWorld
        );
    }
}
