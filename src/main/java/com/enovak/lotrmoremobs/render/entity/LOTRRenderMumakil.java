package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.model.LOTRModelMumakil;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class LOTRRenderMumakil extends RenderLiving {
    private static final ResourceLocation MUMAKIL_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");
    private static final float MUMAKIL_RENDER_SCALE = 1.35F;

    public LOTRRenderMumakil() {
        super(new LOTRModelMumakil(), 2.0F);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTickTime) {
        GL11.glScalef(MUMAKIL_RENDER_SCALE, MUMAKIL_RENDER_SCALE, MUMAKIL_RENDER_SCALE);
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return MUMAKIL_TEXTURE;
    }
}
