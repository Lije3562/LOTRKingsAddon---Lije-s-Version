package com.enovak.lotrmoremobs.render.entity;

import com.enovak.lotrmoremobs.entity.animal.LOTREntityMumakil;
import com.enovak.lotrmoremobs.model.LOTRModelMumakil;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class LOTRRenderMumakil extends RenderLiving {
    private static final ResourceLocation MUMAKIL_WILD_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_wild.png");
    private static final ResourceLocation MUMAKIL_SADDLED_TEXTURE =
            new ResourceLocation("lotrmoremobs", "textures/mob/mumakil/mumakil_saddled.png");
    private static final ResourceLocation MUMAKIL_WAR_TEXTURE =
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
        if (entity instanceof LOTREntityMumakil) {
            LOTREntityMumakil mumakil = (LOTREntityMumakil)entity;
            if (!isMumakilSaddled(mumakil)) {
                return MUMAKIL_WILD_TEXTURE;
            }

            return isMumakilArmored(mumakil) ? MUMAKIL_WAR_TEXTURE : MUMAKIL_SADDLED_TEXTURE;
        }

        return MUMAKIL_WILD_TEXTURE;
    }

    private static boolean isMumakilSaddled(LOTREntityMumakil mumakil) {
        Boolean lotrSaddleState = callBooleanNoArg(mumakil, "isMountSaddled");
        if (lotrSaddleState != null) {
            return lotrSaddleState.booleanValue();
        }

        return mumakil.isHorseSaddled();
    }

    private static boolean isMumakilArmored(LOTREntityMumakil mumakil) {
        Integer horseArmorIndex = callIntNoArg(mumakil, "getHorseArmorIndexSynced");
        if (horseArmorIndex != null) {
            return horseArmorIndex.intValue() > 0;
        }

        Integer obfuscatedHorseArmorIndex = callIntNoArg(mumakil, "func_110241_cb");
        if (obfuscatedHorseArmorIndex != null) {
            return obfuscatedHorseArmorIndex.intValue() > 0;
        }

        Object horseArmorStack = callObjectNoArg(mumakil, "getHorseArmorStack");
        return horseArmorStack instanceof ItemStack;
    }

    private static Boolean callBooleanNoArg(Object target, String methodName) {
        Object result = callObjectNoArg(target, methodName);
        return result instanceof Boolean ? (Boolean)result : null;
    }

    private static Integer callIntNoArg(Object target, String methodName) {
        Object result = callObjectNoArg(target, methodName);
        return result instanceof Integer ? (Integer)result : null;
    }

    private static Object callObjectNoArg(Object target, String methodName) {
        Class currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Method method = currentClass.getDeclaredMethod(methodName);
                if (method.getParameterTypes().length != 0) {
                    return null;
                }

                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException e) {
                currentClass = currentClass.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }
}
