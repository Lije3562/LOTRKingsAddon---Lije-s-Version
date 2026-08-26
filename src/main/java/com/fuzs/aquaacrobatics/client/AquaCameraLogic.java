package com.fuzs.aquaacrobatics.client;

import com.fuzs.aquaacrobatics.entity.Pose;
/** Modern pose-driven camera values, independent of the hook used to apply them. */
public final class AquaCameraLogic {

    public static final float STANDING_EYE_HEIGHT = 1.62F;
    public static final float CROUCHING_EYE_HEIGHT = 1.27F;
    public static final float SWIMMING_EYE_HEIGHT = 0.4F;

    private AquaCameraLogic() {}

    public static float getPoseEyeHeight(Pose pose) {
        switch (pose) {
            case CROUCHING:
                return CROUCHING_EYE_HEIGHT;
            case SWIMMING:
            case FALL_FLYING:
            case SPIN_ATTACK:
                return SWIMMING_EYE_HEIGHT;
            default:
                return STANDING_EYE_HEIGHT;
        }
    }

    public static float interpolate(float current, float target) {
        return current + (target - current) * 0.5F;
    }

}
