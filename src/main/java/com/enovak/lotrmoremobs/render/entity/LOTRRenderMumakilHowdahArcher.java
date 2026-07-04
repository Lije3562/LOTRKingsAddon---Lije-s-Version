package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Simple fallback renderer for the custom howdah archer passenger.
 *
 * The ideal renderer is LOTR's own Near Haradrim archer renderer, but it is not
 * available in RenderManager's map during our renderer registration point in the
 * current dev environment. This fallback keeps the entity visible while we tune
 * the attachment/positioning system.
 */
public class LOTRRenderMumakilHowdahArcher extends RenderBiped {
    private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("textures/entity/steve.png");

    public LOTRRenderMumakilHowdahArcher() {
        super(new ModelBiped(), 0.5F);
    }

    protected ResourceLocation getHowdahArcherTexture(LOTREntityMumakilHowdahArcher entity) {
        return FALLBACK_TEXTURE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.getHowdahArcherTexture((LOTREntityMumakilHowdahArcher)entity);
    }
}
