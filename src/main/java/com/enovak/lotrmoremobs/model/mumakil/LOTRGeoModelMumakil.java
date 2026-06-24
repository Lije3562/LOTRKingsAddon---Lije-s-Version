package com.enovak.lotrmoremobs.model.mumakil;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.model.AnimatedGeoModel;

/**
 * Experimental GeckoLib model provider for rendering the exported Blockbench/Bedrock geometry directly.
 *
 * This branch intentionally keeps the Mumakil static. The goal is to test whether the original .geo.json
 * geometry and matching texture solve the UV bleed without passing through the Java ModelRenderer conversion.
 */
public class LOTRGeoModelMumakil extends AnimatedGeoModel<LOTREntityMumakil> {
    public static final ResourceLocation PLAIN_MODEL =
            new ResourceLocation("lotrmoremobs", "geo/entity/mumakil/LOTRMumakilModel.geo.json");
    public static final ResourceLocation WAR_MODEL =
            new ResourceLocation("lotrmoremobs", "geo/entity/mumakil/LOTRMumakilWarModel.geo.json");
    public static final ResourceLocation PLAIN_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil.png");
    public static final ResourceLocation WAR_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    private static final ResourceLocation ANIMATION =
            new ResourceLocation("lotrmoremobs", "animations/entity/mumakil/LOTRMumakil.animations.json");

    @Override
    public ResourceLocation getAnimationFileLocation(LOTREntityMumakil entity) {
        return ANIMATION;
    }

    @Override
    public ResourceLocation getModelLocation(LOTREntityMumakil entity) {
        return shouldRenderWarEquipment(entity) ? WAR_MODEL : PLAIN_MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(LOTREntityMumakil entity) {
        return shouldRenderWarEquipment(entity) ? WAR_TEXTURE : PLAIN_TEXTURE;
    }

    public static boolean shouldRenderWarEquipment(LOTREntityMumakil entity) {
        return entity != null && entity.isMountSaddled();
    }

    /**
     * GeckoLib-Unofficial's AnimatedGeoModel#setLivingAnimations calls its AnimationProcessor and Molang setup.
     * In this ForgeGradle 1.2 / Minecraft 1.7.10 deobfuscated runtime that path still references obfuscated
     * World.field_72996_f, causing a NoSuchFieldError while rendering. For this UV experiment we do not need
     * animations, so leave the baked Geo model in its exported static pose and skip the unsafe setup entirely.
     */
    @Override
    public void setLivingAnimations(LOTREntityMumakil entity, Integer uniqueID, AnimationEvent customPredicate) {
    }
}
