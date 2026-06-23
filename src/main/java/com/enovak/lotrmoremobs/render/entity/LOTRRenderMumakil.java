package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.model.LOTRModelMumakil;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

public class LOTRRenderMumakil extends RenderLiving {
    private static final ResourceLocation MUMAKIL_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");

    public LOTRRenderMumakil() {
        super(new LOTRModelMumakil(), 1.5F);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return MUMAKIL_TEXTURE;
    }
}
