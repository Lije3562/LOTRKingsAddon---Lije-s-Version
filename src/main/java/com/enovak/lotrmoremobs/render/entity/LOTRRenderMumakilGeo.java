package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.mumakil.LOTRGeoModelMumakil;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
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
}
