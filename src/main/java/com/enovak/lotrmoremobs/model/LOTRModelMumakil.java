package com.enovak.lotrmoremobs.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class LOTRModelMumakil extends ModelBase {
    private final ModelRenderer master;
    private final ModelRenderer sway;
    private final ModelRenderer tail;
    private final ModelRenderer body;
    private final ModelRenderer upperBack;
    private final ModelRenderer head;
    private final ModelRenderer trunk;
    private final ModelRenderer leftMainTusk;
    private final ModelRenderer rightMainTusk;
    private final ModelRenderer leftEar;
    private final ModelRenderer rightEar;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer backRightLeg;
    private final ModelRenderer backLeftLeg;

    public LOTRModelMumakil() {
        this(0.0F);
    }

    public LOTRModelMumakil(float inflate) {
        this.textureWidth = 512;
        this.textureHeight = 512;

        this.master = new ModelRenderer(this);
        this.master.setRotationPoint(0.0F, -26.0F, 50.0F);

        this.sway = new ModelRenderer(this);
        this.sway.setRotationPoint(0.0F, -13.0F, -67.0F);
        this.master.addChild(this.sway);

        this.body = new ModelRenderer(this, 235, 244);
        this.body.setRotationPoint(-2.0F, 1.0F, 17.0F);
        this.body.addBox(-18.0F, -20.0F, -20.0F, 40, 42, 70, inflate);
        this.sway.addChild(this.body);

        ModelRenderer back = new ModelRenderer(this, 703, 43);
        back.setRotationPoint(1.0F, -17.0F, 10.0F);
        setRotationAngle(back, -0.1745F, 0.0F, 0.0F);
        back.addBox(-21.0F, -17.0F, -33.0F, 42, 22, 74, inflate);
        this.body.addChild(back);

        this.upperBack = new ModelRenderer(this, 176, 38);
        this.upperBack.setRotationPoint(0.0F, -18.0F, -24.0F);
        setRotationAngle(this.upperBack, 0.35F, 0.0F, 0.0F);
        this.upperBack.addBox(-18.0F, -20.0F, -22.0F, 36, 34, 34, inflate);
        this.body.addChild(this.upperBack);

        this.head = new ModelRenderer(this, 866, 385);
        this.head.setRotationPoint(0.0F, 3.0F, -22.0F);
        setRotationAngle(this.head, 0.35F, 0.0F, 0.0F);
        this.head.addBox(-14.0F, -12.0F, -20.0F, 28, 24, 30, inflate);
        this.upperBack.addChild(this.head);

        ModelRenderer mouth = new ModelRenderer(this, 891, 576);
        mouth.setRotationPoint(0.0F, 9.0F, -17.0F);
        setRotationAngle(mouth, 0.35F, 0.0F, 0.0F);
        mouth.addBox(-10.0F, -3.0F, -16.0F, 20, 10, 20, inflate);
        this.head.addChild(mouth);

        this.trunk = new ModelRenderer(this, 721, 697);
        this.trunk.setRotationPoint(0.0F, 8.0F, -19.0F);
        setRotationAngle(this.trunk, -0.2F, 0.0F, 0.0F);
        this.trunk.addBox(-6.0F, -1.0F, -8.0F, 12, 32, 10, inflate);
        this.head.addChild(this.trunk);

        ModelRenderer trunkTip = new ModelRenderer(this, 591, 716);
        trunkTip.setRotationPoint(0.0F, 27.0F, -2.0F);
        setRotationAngle(trunkTip, 0.45F, 0.0F, 0.0F);
        trunkTip.addBox(-4.0F, -1.0F, -4.0F, 8, 20, 8, inflate);
        this.trunk.addChild(trunkTip);

        this.leftMainTusk = new ModelRenderer(this, 33, 38);
        this.leftMainTusk.setRotationPoint(8.0F, 7.0F, -21.0F);
        setRotationAngle(this.leftMainTusk, -0.45F, 0.25F, -0.15F);
        this.leftMainTusk.addBox(-3.0F, -2.0F, -36.0F, 6, 6, 38, inflate);
        this.head.addChild(this.leftMainTusk);

        this.rightMainTusk = new ModelRenderer(this, 33, 38);
        this.rightMainTusk.setRotationPoint(-8.0F, 7.0F, -21.0F);
        setRotationAngle(this.rightMainTusk, -0.45F, -0.25F, 0.15F);
        this.rightMainTusk.addBox(-3.0F, -2.0F, -36.0F, 6, 6, 38, inflate);
        this.head.addChild(this.rightMainTusk);

        this.leftEar = new ModelRenderer(this, 246, 248);
        this.leftEar.setRotationPoint(13.0F, -3.0F, -4.0F);
        setRotationAngle(this.leftEar, 0.2F, 0.65F, 0.35F);
        this.leftEar.addBox(0.0F, -9.0F, -9.0F, 4, 18, 18, inflate);
        this.head.addChild(this.leftEar);

        this.rightEar = new ModelRenderer(this, 246, 248);
        this.rightEar.setRotationPoint(-13.0F, -3.0F, -4.0F);
        setRotationAngle(this.rightEar, 0.2F, -0.65F, -0.35F);
        this.rightEar.addBox(-4.0F, -9.0F, -9.0F, 4, 18, 18, inflate);
        this.head.addChild(this.rightEar);

        this.tail = new ModelRenderer(this, 44, 756);
        this.tail.setRotationPoint(0.0F, -4.0F, 49.0F);
        setRotationAngle(this.tail, 0.45F, 0.0F, 0.0F);
        this.tail.addBox(-3.0F, -1.0F, -1.0F, 6, 28, 4, inflate);
        this.body.addChild(this.tail);

        this.frontRightLeg = makeLeg(7.0F, 22.0F, -16.0F, false, inflate);
        this.sway.addChild(this.frontRightLeg);

        this.frontLeftLeg = makeLeg(-23.0F, 22.0F, -16.0F, true, inflate);
        this.sway.addChild(this.frontLeftLeg);

        this.backRightLeg = makeLeg(8.0F, 22.0F, 38.0F, false, inflate);
        this.sway.addChild(this.backRightLeg);

        this.backLeftLeg = makeLeg(-24.0F, 22.0F, 38.0F, true, inflate);
        this.sway.addChild(this.backLeftLeg);
    }

    private ModelRenderer makeLeg(float x, float y, float z, boolean mirror, float inflate) {
        ModelRenderer leg = new ModelRenderer(this, 493, 826);
        leg.mirror = mirror;
        leg.setRotationPoint(x, y, z);
        leg.addBox(-7.0F, -8.0F, -7.0F, 14, 24, 14, inflate);

        ModelRenderer lower = new ModelRenderer(this, 335, 813);
        lower.mirror = mirror;
        lower.setRotationPoint(0.0F, 16.0F, 0.0F);
        lower.addBox(-5.5F, 0.0F, -5.5F, 11, 25, 11, inflate);
        leg.addChild(lower);

        ModelRenderer foot = new ModelRenderer(this, 935, 760);
        foot.mirror = mirror;
        foot.setRotationPoint(0.0F, 25.0F, 0.0F);
        foot.addBox(-7.5F, -1.0F, -9.0F, 15, 5, 17, inflate);
        lower.addChild(foot);

        return leg;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        this.master.render(scale);
    }

    private static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
