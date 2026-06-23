package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.mumakil.LOTRGeoModelMumakil;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.core.util.Color;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class LOTRRenderMumakil extends GeoEntityRenderer<LOTREntityMumakil> {
    private static final ResourceLocation MUMAKIL_WAR_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    public LOTRRenderMumakil() {
        super(new LOTRGeoModelMumakil());
        this.shadowSize = 0.5F;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return MUMAKIL_WAR_TEXTURE;
    }

    public Color getRenderColor(LOTREntityMumakil animatable, float partialTicks) {
        return Color.ofRGBA(255, 255, 255, 255);
    }
}