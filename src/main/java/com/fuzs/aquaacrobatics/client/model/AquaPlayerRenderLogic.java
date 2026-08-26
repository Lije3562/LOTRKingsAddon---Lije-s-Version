package com.fuzs.aquaacrobatics.client.model;

import net.minecraft.client.model.ModelRenderer;

import com.fuzs.aquaacrobatics.entity.Pose;

/** Modern crouch render values shared by current Mixins and future ASM hooks. */
public final class AquaPlayerRenderLogic {

    public static final float CROUCHING_RENDER_OFFSET = -0.125F;

    private AquaPlayerRenderLogic() {}

    public static void applyPosePivots(Pose pose, ModelRenderer head, ModelRenderer headwear, ModelRenderer body,
        ModelRenderer rightArm, ModelRenderer leftArm, ModelRenderer rightLeg, ModelRenderer leftLeg) {

        if (pose == Pose.CROUCHING) {
            head.rotationPointY = 4.2F;
            headwear.rotationPointY = 4.2F;
            body.rotationPointY = 3.2F;
            rightArm.rotationPointY = 5.2F;
            leftArm.rotationPointY = 5.2F;
            rightLeg.rotationPointY = 12.0F;
            leftLeg.rotationPointY = 12.0F;
        } else if (pose == Pose.STANDING) {
            head.rotationPointY = 0.0F;
            headwear.rotationPointY = 0.0F;
            body.rotationPointY = 0.0F;
            rightArm.rotationPointY = 2.0F;
            leftArm.rotationPointY = 2.0F;
            rightLeg.rotationPointY = 12.0F;
            leftLeg.rotationPointY = 12.0F;
        }
    }

    public static double getCrouchingRenderY(double ySize, boolean hasLegacyRemoteSneakOffset) {
        return ySize + CROUCHING_RENDER_OFFSET + (hasLegacyRemoteSneakOffset ? 0.125D : 0.0D);
    }
}
