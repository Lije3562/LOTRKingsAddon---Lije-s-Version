package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.LOTRModelMumakil;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class LOTRRenderMumakil extends RenderLiving {
    private static final ResourceLocation MUMAKIL_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_war.png");
    private static final ResourceLocation MUMAKIL_WILD_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_wild.png");
    private static final ResourceLocation MUMAKIL_SADDLED_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_saddled.png");
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
        if (entity instanceof LOTREntityMumakil) {
            LOTREntityMumakil mumakil = (LOTREntityMumakil)entity;
            return isMumakilSaddled(mumakil) ? MUMAKIL_SADDLED_TEXTURE : MUMAKIL_WILD_TEXTURE;
        }

        return MUMAKIL_TEXTURE;
    }

    private static boolean isMumakilSaddled(LOTREntityMumakil mumakil) {
        Boolean lotrSaddleState = callBooleanNoArg(mumakil, "isMountSaddled");
        if (lotrSaddleState != null) {
            return lotrSaddleState.booleanValue();
        }

        return mumakil.isHorseSaddled();
    }

    private static Boolean callBooleanNoArg(Object target, String methodName) {
        Class currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Method method = currentClass.getDeclaredMethod(methodName);
                Class returnType = method.getReturnType();
                if (returnType != Boolean.TYPE && returnType != Boolean.class) {
                    return null;
                }

                method.setAccessible(true);
                Object result = method.invoke(target);
                return result instanceof Boolean ? (Boolean)result : null;
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }
}
