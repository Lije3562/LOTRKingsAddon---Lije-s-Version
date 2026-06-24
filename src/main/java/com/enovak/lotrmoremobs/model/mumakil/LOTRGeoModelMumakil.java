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
    private static final ResourceLocation ANIMATION =
            new ResourceLocation("lotrmoremobs", "animations/entity/mumakil/LOTRMumakil.animations.json");
    private static final ResourceLocation MODEL =
            new ResourceLocation("lotrmoremobs", "geo/entity/mumakil/LOTRMumakilModel.geo.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    @Override
    public ResourceLocation getAnimationFileLocation(LOTREntityMumakil entity) {
        return ANIMATION;
    }

    @Override
    public ResourceLocation getModelLocation(LOTREntityMumakil entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureLocation(LOTREntityMumakil entity) {
        return TEXTURE;
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
