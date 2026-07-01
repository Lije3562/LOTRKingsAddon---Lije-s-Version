package com.enovak.lotrmoremobs.client;

import com.enovak.lotrmoremobs.Main;
import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.network.MumakilOpenGuiPacket;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class MumakilInventoryKeyHandler {
    private boolean wasInventoryKeyDown;
    private boolean hadScreenLastTick;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        if (mc == null || mc.thePlayer == null || mc.gameSettings == null) {
            return;
        }

        boolean screenOpen = mc.currentScreen != null;
        boolean inventoryKeyDown = Keyboard.isKeyDown(mc.gameSettings.keyBindInventory.getKeyCode());

        if (!inventoryKeyDown) {
            this.wasInventoryKeyDown = false;
            this.hadScreenLastTick = screenOpen;
            return;
        }

        if (screenOpen) {
            this.wasInventoryKeyDown = true;
            this.hadScreenLastTick = true;
            return;
        }

        if (this.hadScreenLastTick) {
            this.wasInventoryKeyDown = true;
            return;
        }

        if (this.wasInventoryKeyDown) {
            return;
        }

        this.wasInventoryKeyDown = true;

        if (!(mc.thePlayer.ridingEntity instanceof LOTREntityMumakil)) {
            return;
        }

        Main.network.sendToServer(new MumakilOpenGuiPacket());
    }
}