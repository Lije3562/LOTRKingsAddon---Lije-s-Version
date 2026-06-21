//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.LOTRModelMumakil;
import com.enovak.lotrmoremobs.model.LOTRModelMumakilHowdah;
import lotr.client.render.entity.LOTRRenderHorse;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class LOTRRenderMumakil extends RenderLiving {
    private static final ResourceLocation mumakilTexture = new ResourceLocation("lotrmoremobs:mob/mumakil/mumakil.png");
    private static final ResourceLocation howdahTexture = mumakilTexture;

    public LOTRRenderMumakil() {
        super(new LOTRModelMumakil(), 0.5F);
        this.setRenderPassModel(new LOTRModelMumakilHowdah());
    }

    @Override
    protected int shouldRenderPass(EntityLivingBase entity, int pass, float partialTicks) {
        if (pass == 0 && entity instanceof LOTREntityMumakil
                && ((LOTREntityMumakil)entity).isMountSaddled()) {
            this.bindTexture(howdahTexture);
            return 1;
        }

        return -1;
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        LOTREntityMumakil mumakil = (LOTREntityMumakil) entity;
        return LOTRRenderHorse.getLayeredMountTexture(mumakil, mumakilTexture);
    }
}
