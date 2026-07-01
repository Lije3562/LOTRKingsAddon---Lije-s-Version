package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;

/**
 * Small wrapper around the stable Mumakil Geo renderer.
 *
 * World rendering stays untouched. The extra scale only applies when Minecraft is rendering the Mumakil
 * as an inventory-style GUI preview.
 */
public class LOTRRenderMumakilGeoInventoryScaled extends LOTRRenderMumakilGeo {
    private static final float INVENTORY_PREVIEW_SCALE = 0.18F;
    private static boolean loggedInventoryPreviewScale;

    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity instanceof LOTREntityMumakil && this.shouldScaleInventoryPreview(x, y, z)) {
            this.doScaledInventoryPreview(entity, x, y, z, entityYaw, partialTicks);
            return;
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity instanceof LOTREntityMumakil && this.shouldScaleInventoryPreview(x, y, z)) {
            this.doScaledInventoryPreview(entity, x, y, z, entityYaw, partialTicks);
            return;
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void doScaledInventoryPreview(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(INVENTORY_PREVIEW_SCALE, INVENTORY_PREVIEW_SCALE, INVENTORY_PREVIEW_SCALE);

            if (!loggedInventoryPreviewScale) {
                loggedInventoryPreviewScale = true;
                System.out.println("[LOTRMoreMobs] Scaling Mumakil inventory preview by " + INVENTORY_PREVIEW_SCALE
                        + " renderCoords=" + x + "," + y + "," + z);
            }

            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private boolean shouldScaleInventoryPreview(double x, double y, double z) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.currentScreen == null) {
            return false;
        }

        GuiScreen screen = minecraft.currentScreen;
        String screenClassName = screen.getClass().getName();

        boolean inventoryScreen = screenClassName.indexOf("GuiInventory") >= 0
                || screenClassName.indexOf("GuiScreenHorseInventory") >= 0
                || screenClassName.indexOf("GuiHorse") >= 0
                || screenClassName.indexOf("GuiContainer") >= 0
                || screenClassName.indexOf("Inventory") >= 0;

        if (!inventoryScreen) {
            return false;
        }

        /*
         * Most GUI previews render the entity at 0,0,0, but some horse/inventory paths can pass
         * nonzero GUI-space values. If an inventory GUI is active, scale the Mumakil preview.
         */
        return true;
    }
}