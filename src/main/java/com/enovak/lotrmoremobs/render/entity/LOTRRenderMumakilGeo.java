package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.mumakil.LOTRGeoModelMumakil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

/**
 * Experimental renderer that bypasses the Java ModelRenderer conversion and renders the exported .geo.json.
 *
 * Keep LOTRRenderMumakil available as the stable RenderLiving/ModelBase fallback while this branch tests
 * whether the original Geo model fixes the UV bleed at the root.
 */
public class LOTRRenderMumakilGeo extends GeoEntityRenderer<LOTREntityMumakil> {
    private static final ResourceLocation MUMAKIL_WAR_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    public LOTRRenderMumakilGeo() {
        super(new LOTRGeoModelMumakil());
        this.shadowSize = 2.0F;
    }

    /**
     * RenderManager can enter through the raw Entity signature. Keep this generic path and route only real
     * Mumakil instances into the safe static renderer instead of allowing dispatch to GeoEntityRenderer#doRender.
     */
    @Override
    public void doRender(Entity entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof LOTREntityMumakil)) {
            return;
        }

        this.doRenderMumakil((LOTREntityMumakil)entity, x, y, z, entityYaw, partialTicks);
    }

    /**
     * LOTR's mob-spawner inventory preview can enter through the EntityLivingBase signature. If this exact
     * overload is missing, runtime dispatch can still reach GeckoLib's GeoEntityRenderer#doRender and its
     * obfuscated EntityLivingBase.func_98034_c(EntityPlayer) invisibility call. Route it through the same safe
     * static helper and never call super.doRender(...).
     */
    @Override
    public void doRender(EntityLivingBase entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!(entity instanceof LOTREntityMumakil)) {
            return;
        }

        this.doRenderMumakil((LOTREntityMumakil)entity, x, y, z, entityYaw, partialTicks);
    }

    private void doRenderMumakil(LOTREntityMumakil entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (entity == null || entity.isInvisible()) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_CULL_FACE);

        try {
            float bodyYaw = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
            float headYaw = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
            float netHeadYaw = headYaw - bodyYaw;
            float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            float ageInTicks = entity.ticksExisted + partialTicks;
            float limbSwingAmount = entity.prevLimbSwingAmount
                    + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
            float limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

            if (entity.isChild()) {
                limbSwing *= 3.0F;
            }

            if (limbSwingAmount > 1.0F) {
                limbSwingAmount = 1.0F;
            }

            GL11.glTranslatef((float)x, (float)y, (float)z);
            GL11.glRotatef(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glScalef(-1.0F, -1.0F, 1.0F);
            GL11.glTranslatef(0.0F, -1.5078125F, 0.0F);

            this.bindEntityTexture(entity);
            this.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, 0.0625F);
        } finally {
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glPopMatrix();
        }
    }

    /**
     * GeckoLib-Unofficial's default GeoEntityRenderer#getUniqueID path calls EntityLivingBase.getUniqueID()
     * through a name that crashes in this ForgeGradle 1.2 / 1.7.10 deobfuscated runtime. LOTR's creative
     * mob-spawner preview also renders temporary entities, so use the vanilla entity id as a safe per-instance
     * animation key instead of entering the broken func_110124_au call path.
     */
    @Override
    public Integer getUniqueID(LOTREntityMumakil animatable) {
        return Integer.valueOf(animatable == null ? 0 : animatable.getEntityId());
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return MUMAKIL_WAR_TEXTURE;
    }

    public Color getRenderColor(LOTREntityMumakil animatable, float partialTicks) {
        return Color.ofRGBA(255, 255, 255, 255);
    }

    private float interpolateRotation(float previousYaw, float yaw, float partialTicks) {
        float delta = yaw - previousYaw;

        while (delta < -180.0F) {
            delta += 360.0F;
        }

        while (delta >= 180.0F) {
            delta -= 360.0F;
        }

        return previousYaw + partialTicks * delta;
    }
}
