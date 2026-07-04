package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.npc.LOTREntityMumakilHowdahArcher;
import lotr.common.entity.npc.LOTREntityNearHaradrimArcher;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Renderer for the custom howdah archer passenger.
 *
 * It first tries to delegate to LOTR's normal Near Haradrim archer renderer at
 * actual render time. If that renderer is still unavailable, it falls back to a
 * biped renderer using a LOTR Southron/Near Harad texture path instead of Steve.
 */
public class LOTRRenderMumakilHowdahArcher extends RenderBiped {
    private static final ResourceLocation SOUTHON_FALLBACK_TEXTURE = new ResourceLocation("lotr:mob/nearHarad/southron.png");

    private Render deferredNearHaradrimRenderer;
    private boolean attemptedDeferredLookup;

    public LOTRRenderMumakilHowdahArcher() {
        super(new ModelBiped(), 0.5F);
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        Render lotrRenderer = this.getDeferredNearHaradrimRenderer();

        if (lotrRenderer != null && lotrRenderer != this) {
            lotrRenderer.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }

        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    private Render getDeferredNearHaradrimRenderer() {
        if (!this.attemptedDeferredLookup) {
            this.attemptedDeferredLookup = true;
            this.deferredNearHaradrimRenderer = (Render)RenderManager.instance.entityRenderMap.get(LOTREntityNearHaradrimArcher.class);

            if (this.deferredNearHaradrimRenderer != null) {
                System.out.println("[LOTRMoreMobs] Deferred Mumakil howdah archer renderer lookup found LOTR Near Haradrim renderer.");
            } else {
                System.out.println("[LOTRMoreMobs] Deferred Mumakil howdah archer renderer lookup using Southron texture fallback.");
            }
        }

        return this.deferredNearHaradrimRenderer;
    }

    protected ResourceLocation getHowdahArcherTexture(LOTREntityMumakilHowdahArcher entity) {
        return SOUTHON_FALLBACK_TEXTURE;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.getHowdahArcherTexture((LOTREntityMumakilHowdahArcher)entity);
    }
}
