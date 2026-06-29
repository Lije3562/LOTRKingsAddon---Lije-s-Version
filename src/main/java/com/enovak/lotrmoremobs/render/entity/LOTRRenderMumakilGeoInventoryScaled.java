package com.enovak.lotrmoremobs.render.entity;

import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;

/**
 * Small wrapper around the stable Mumakil Geo renderer.
 *
 * World rendering stays untouched. The extra scale only applies when Minecraft is rendering the Mumakil
 * as the zero-position entity preview inside an inventory-style GUI.
 */
public class LOTRRenderMumakilGeoInventoryScaled extends LOTRRenderMumakilGeo {
    private static final float INVENTORY_PREVIEW_SCALE = 0.35F;
    private static boolean loggedInventoryPreviewScale;

    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (this.shouldScaleInventoryPreview(x, y, z)) {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.scale(INVENTORY_PREVIEW_SCALE, INVENTORY_PREVIEW_SCALE, INVENTORY_PREVIEW_SCALE);

                if (!loggedInventoryPreviewScale) {
                    loggedInventoryPreviewScale = true;
                    System.out.println("[LOTRMoreMobs] Scaling Mumakil inventory preview by " + INVENTORY_PREVIEW_SCALE);
                }

                super.doRender(entity, x, y, z, entityYaw, partialTicks);
            } finally {
                GlStateManager.popMatrix();
            }
            return;
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private boolean shouldScaleInventoryPreview(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.currentScreen == null) {
            return false;
        }

        if (Math.abs(x) > 1.0E-4D || Math.abs(y) > 1.0E-4D || Math.abs(z) > 1.0E-4D) {
            return false;
        }

        GuiScreen screen = minecraft.currentScreen;
        String screenClassName = screen.getClass().getName();
        return screenClassName.indexOf("GuiInventory") >= 0
                || screenClassName.indexOf("GuiScreenHorseInventory") >= 0
                || screenClassName.indexOf("GuiHorse") >= 0
                || screenClassName.indexOf("Inventory") >= 0;
    }
}