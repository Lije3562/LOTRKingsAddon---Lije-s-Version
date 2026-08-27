package com.fuzs.aquaacrobatics.client.render;
import com.enovak.lotrmoremobs.client.config.ClientServerGameplayState;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.Entity;
import com.fuzs.aquaacrobatics.client.model.FirstPersonArmRenderContext;
import com.fuzs.aquaacrobatics.client.model.IModelBipedSwimming;
import com.fuzs.aquaacrobatics.client.model.AquaPlayerRenderLogic;
import com.fuzs.aquaacrobatics.entity.Pose;
import com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable;
import com.fuzs.aquaacrobatics.integration.efr.EFRIntegration;
import com.fuzs.aquaacrobatics.util.math.MathHelperNew;
import org.lwjgl.opengl.GL11;
public final class AquaRenderPlayerLogic { private AquaRenderPlayerLogic(){}
 public static void resetFirstPerson(ModelBiped m){((IModelBipedSwimming)m).setSwimAnimation(0);}
 public static void firstPersonAngles(ModelBiped m,float a,float b,float c,float d,float e,float f,Entity g){resetFirstPerson(m);FirstPersonArmRenderContext.push();try{m.setRotationAngles(a,b,c,d,e,f,g);}finally{FirstPersonArmRenderContext.pop();}}
 public static double crouchingY(AbstractClientPlayer p,double original){if(!ClientServerGameplayState.useModernPlayerAnimations())return original;return ((IPlayerResizeable)p).getPose()!=Pose.CROUCHING?original:original+AquaPlayerRenderLogic.getCrouchingRenderY(p.ySize,p.isSneaking()&&!(p instanceof EntityPlayerSP));}
 public static void rotations(AbstractClientPlayer p,float a,float yaw,float partial){if(!ClientServerGameplayState.useModernPlayerAnimations())return;if(!EFRIntegration.isElytraFlying(p)){float f=((IPlayerResizeable)p).getSwimAnimation(partial);float target=p.isInWater()?-90F-p.rotationPitch:-90F;GL11.glRotatef(MathHelperNew.lerp(f,0,target),1,0,0);if(((IPlayerResizeable)p).isActuallySwimming())GL11.glTranslatef(0,-1,.3F);}}
}
