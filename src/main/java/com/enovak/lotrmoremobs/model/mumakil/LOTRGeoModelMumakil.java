package com.enovak.lotrmoremobs.model.mumakil;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.model.AnimatedGeoModel;

public class LOTRGeoModelMumakil extends AnimatedGeoModel<LOTREntityMumakil> {
    @Override
    public ResourceLocation getAnimationFileLocation(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "animations/entity/mumakil/LOTRMumakil.animations.json"
        );
    }

    @Override
    public ResourceLocation getModelLocation(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "geo/entity/mumakil/LOTRMumakilModel.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureLocation(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "textures/mob/mumakil/mumakil_war.png"
        );
    }
}
