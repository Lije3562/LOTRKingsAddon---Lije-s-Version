package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.mumakil.LOTRGeoModelMumakil;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class LOTRRenderMumakil extends GeoEntityRenderer<LOTREntityMumakil> {
    public LOTRRenderMumakil() {
        super(new LOTRGeoModelMumakil());
        this.shadowSize = 0.5F;
    }

    public Color getRenderColor(LOTREntityMumakil animatable, float partialTicks) {
        return Color.ofRGBA(255, 255, 255, 255);
    }
}
