package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.mumakil.LOTRGeoModelMumakil;
import java.util.Collections;
import net.geckominecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.geo.render.built.GeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;
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
    private static boolean loggedSafeRenderStart;
    private static boolean loggedModelRequest;
    private static boolean loggedMissingGeoModel;
    private static boolean loggedTextureRequest;
    private static boolean loggedBeforeGeoRender;

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
        if (entity == null) {
            return;
        }

        if (!loggedSafeRenderStart) {
            loggedSafeRenderStart = true;
            System.out.println("[LOTRMoreMobs] Mumakil safe Geo render path started for entityId=" + entity.getEntityId());
        }

        if (entity.isInvisible()) {
            return;
        }

        ResourceLocation modelLocation = this.modelProvider.getModelLocation(entity);
        if (!loggedModelRequest) {
            loggedModelRequest = true;
            System.out.println("[LOTRMoreMobs] Mumakil Geo model requested: " + modelLocation
                    + " resourceFound=" + this.canLoadResource(modelLocation));
        }

        GeoModel geoModel = this.modelProvider.getModel(modelLocation);
        if (geoModel == null) {
            if (!loggedMissingGeoModel) {
                loggedMissingGeoModel = true;
                System.out.println("[LOTRMoreMobs] Mumakil Geo renderer could not load model: " + modelLocation);
            }
            return;
        }

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, z);

            boolean isSitting = entity.ridingEntity != null && entity.ridingEntity.shouldRiderSit();
            EntityModelData entityModelData = new EntityModelData();
            entityModelData.isSitting = isSitting;
            entityModelData.isChild = entity.isChild();

            Pair<Float, Float> rotations = this.calculateRotations(entity, partialTicks, isSitting);
            float headPitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
            float ageInTicks = entity.ticksExisted + partialTicks;
            this.applyRotations(entity, ageInTicks, rotations.getKey().floatValue(), partialTicks);

            float limbSwingAmount = 0.0F;
            float limbSwing = 0.0F;
            if (!isSitting && entity.isEntityAlive()) {
                limbSwingAmount = entity.prevLimbSwingAmount
                        + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
                limbSwing = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

                if (entity.isChild()) {
                    limbSwing *= 3.0F;
                }

                if (limbSwingAmount > 1.0F) {
                    limbSwingAmount = 1.0F;
                }
            }

            entityModelData.headPitch = -headPitch;
            entityModelData.netHeadYaw = -rotations.getValue().floatValue();

            AnimationEvent<LOTREntityMumakil> animationEvent = new AnimationEvent<LOTREntityMumakil>(
                    entity,
                    limbSwing,
                    limbSwingAmount,
                    partialTicks,
                    limbSwingAmount >= 0.15F,
                    Collections.<Object>singletonList(entityModelData)
            );
            this.modelProvider.setLivingAnimations(entity, this.getUniqueID(entity), animationEvent);

            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(0.0F, 0.01F, 0.0F);

                ResourceLocation textureLocation = this.getEntityTexture(entity);
                if (!loggedTextureRequest) {
                    loggedTextureRequest = true;
                    System.out.println("[LOTRMoreMobs] Mumakil Geo texture requested: " + textureLocation
                            + " resourceFound=" + this.canLoadResource(textureLocation));
                }
                Minecraft.getMinecraft().renderEngine.bindTexture(textureLocation);

                Color renderColor = this.getRenderColor(entity, partialTicks);

                // Deliberately skip GeckoLib's EntityLivingBase.isInvisibleToPlayer(...) branch; in this
                // 1.7.10 dev runtime it dispatches to the missing obfuscated func_98034_c method.
                if (!loggedBeforeGeoRender) {
                    loggedBeforeGeoRender = true;
                    System.out.println("[LOTRMoreMobs] Mumakil calling GeckoLib render(GeoModel, ...) with modelLoaded=true");
                }
                this.render(
                        geoModel,
                        entity,
                        partialTicks,
                        renderColor.getRed() / 255.0F,
                        renderColor.getGreen() / 255.0F,
                        renderColor.getBlue() / 255.0F,
                        renderColor.getAlpha() / 255.0F
                );
            } finally {
                GlStateManager.popMatrix();
            }
        } finally {
            GlStateManager.popMatrix();
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

    private boolean canLoadResource(ResourceLocation resourceLocation) {
        try {
            return Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
