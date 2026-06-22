package com.enovak.lotrmoremobs.model.mumakil;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import net.minecraft.util.ResourceLocation;

public class LOTRGeoModelMumakil extends GeoModel<LOTREntityMumakil> {
    @Override
    public ResourceLocation getModelResource(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "geo/entity/mumakil/LOTRMumakilModel.geo.json"
        );
    }

    @Override
    public ResourceLocation getTextureResource(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "textures/mob/mumakil/mumakil_war.png"
        );
    }

    @Override
    public ResourceLocation getAnimationResource(LOTREntityMumakil entity) {
        return new ResourceLocation(
                "lotrmoremobs",
                "animations/entity/mumakil/LOTRMumakilModel.animation.json"
        );
    }
}