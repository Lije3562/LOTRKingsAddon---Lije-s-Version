package com.enovak.lotrmoremobs.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;

public class LOTRModelMumakil extends ModelBase {
    private final ModelRenderer Master;
    private final ModelRenderer Sway;
    private final ModelRenderer tail;
    private final ModelRenderer Upper;
    private final ModelRenderer Middle;
    private final ModelRenderer Lower;
    private final ModelRenderer body;
    private final ModelRenderer back_r1;
    private final ModelRenderer upper_back;
    private final ModelRenderer hump_r1;
    private final ModelRenderer beastplate_r1;
    private final ModelRenderer head;
    private final ModelRenderer mouth_r1;
    private final ModelRenderer trunktuskroot_r1;
    private final ModelRenderer skull_r1;
    private final ModelRenderer left_small_tusk;
    private final ModelRenderer left_small_tusk_r1;
    private final ModelRenderer left_tusk_root_r1;
    private final ModelRenderer right_small_tusks;
    private final ModelRenderer right_small_root_r1;
    private final ModelRenderer right_tusk_root_r1;
    private final ModelRenderer left_main_tusk;
    private final ModelRenderer t5_r1;
    private final ModelRenderer t4_r1;
    private final ModelRenderer t3_r1;
    private final ModelRenderer t1_r1;
    private final ModelRenderer t1_r2;
    private final ModelRenderer right_main_tusk;
    private final ModelRenderer t5_r2;
    private final ModelRenderer t4_r2;
    private final ModelRenderer t3_r2;
    private final ModelRenderer t1_r3;
    private final ModelRenderer t1_r4;
    private final ModelRenderer left_ear;
    private final ModelRenderer right_ear_r1;
    private final ModelRenderer right_ear;
    private final ModelRenderer left_ear_r1;
    private final ModelRenderer trunk;
    private final ModelRenderer t1;
    private final ModelRenderer t2;
    private final ModelRenderer t2_r1;
    private final ModelRenderer t3;
    private final ModelRenderer t3_r3;
    private final ModelRenderer t4;
    private final ModelRenderer t4_r3;
    private final ModelRenderer t5;
    private final ModelRenderer t5_r3;
    private final ModelRenderer legs;
    private final ModelRenderer front_right_foot;
    private final ModelRenderer leg_front_upper;
    private final ModelRenderer leg_front_lower;
    private final ModelRenderer foot_upper;
    private final ModelRenderer foot_lower;
    private final ModelRenderer front_left_foot;
    private final ModelRenderer back_left_foot;
    private final ModelRenderer back_right_foot;

    public LOTRModelMumakil() {
        this(0.0F);
    }

    public LOTRModelMumakil(float f) {
        textureWidth = 512;
        textureHeight = 512;

        Master = new ModelRenderer(this);
        Master.setRotationPoint(0.0F, -26.0F, 50.0F);

        Sway = new ModelRenderer(this);
        Sway.setRotationPoint(0.0F, -13.0F, -67.0F);
        Master.addChild(Sway);

        tail = new ModelRenderer(this);
        tail.setRotationPoint(0.0F, 13.0F, 67.0F);
        Sway.addChild(tail);

        Upper = new ModelRenderer(this);
        Upper.setRotationPoint(2.0F, 17.0F, -2.0F);
        tail.addChild(Upper);
        Upper.addBox(-5.0F, -21.0F, -1.0F, 6, 21, 4, 0.0F);

        Middle = new ModelRenderer(this);
        Middle.setRotationPoint(-2.0F, 1.0F, 2.0F);
        Upper.addChild(Middle);
        Middle.addBox(-2.0F, -0.9617F, -1.0F, 4, 10, 3, 0.0F);

        Lower = new ModelRenderer(this);
        Lower.setRotationPoint(0.0F, 10.0383F, 2.0F);
        Middle.addChild(Lower);
        Lower.addBox(-1.0F, -1.0F, -1.0F, 2, 10, 2, 0.0F);

        body = new ModelRenderer(this);
        body.setRotationPoint(-2.0F, 1.0F, 17.0F);
        Sway.addChild(body);
        body.addBox(-18.01F, 1.0F, -14.0F, 39, 20, 60, 0.0F);

        back_r1 = new ModelRenderer(this);
        back_r1.setRotationPoint(21.0F, 7.0F, -3.0F);
        body.addChild(back_r1);
        setRotationAngle(back_r1, -0.1745F, 0.0F, 0.0F);
        back_r1.addBox(-39.0F, -28.0F, -17.0F, 40, 31, 67, 0.0F);

        upper_back = new ModelRenderer(this);
        upper_back.setRotationPoint(2.0F, -0.8195F, -19.1583F);
        body.addChild(upper_back);

        hump_r1 = new ModelRenderer(this);
        hump_r1.setRotationPoint(0.0F, -6.0F, -5.0F);
        upper_back.addChild(hump_r1);
        setRotationAngle(hump_r1, 1.8151F, 0.0F, 0.0F);
        hump_r1.addBox(-19.0F, -17.0F, -14.0F, 38, 32, 27, 0.0F);

        beastplate_r1 = new ModelRenderer(this);
        beastplate_r1.setRotationPoint(0.0F, 8.6987F, -2.783F);
        upper_back.addChild(beastplate_r1);
        setRotationAngle(beastplate_r1, 1.2217F, 0.0F, 0.0F);
        beastplate_r1.addBox(-18.9899F, -13.5F, -7.5F, 38, 26, 15, 0.0F);

        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, 1.8195F, -23.8417F);
        upper_back.addChild(head);

        mouth_r1 = new ModelRenderer(this);
        mouth_r1.setRotationPoint(-0.5F, 15.4231F, -13.1771F);
        head.addChild(mouth_r1);
        setRotationAngle(mouth_r1, 0.6109F, 0.0F, 0.0F);
        mouth_r1.addBox(-10.5F, -7.5F, -8.5F, 20, 11, 22, 0.0F);

        trunktuskroot_r1 = new ModelRenderer(this);
        trunktuskroot_r1.setRotationPoint(1.0F, 13.6338F, -24.3129F);
        head.addChild(trunktuskroot_r1);
        setRotationAngle(trunktuskroot_r1, 1.0036F, 0.0F, 0.0F);
        trunktuskroot_r1.addBox(-13.0F, -5.0F, -5.0F, 24, 12, 19, 0.0F);

        skull_r1 = new ModelRenderer(this);
        skull_r1.setRotationPoint(0.0F, 0.9885F, -3.2061F);
        head.addChild(skull_r1);
        setRotationAngle(skull_r1, 0.5672F, 0.0F, 0.0F);
        skull_r1.addBox(-14.0F, -11.5F, -15.0F, 28, 23, 30, 0.0F);

        left_small_tusk = new ModelRenderer(this);
        left_small_tusk.setRotationPoint(-17.0F, 50.7962F, 21.5595F);
        head.addChild(left_small_tusk);

        left_small_tusk_r1 = new ModelRenderer(this);
        left_small_tusk_r1.setRotationPoint(27.5F, -37.7938F, -30.7753F);
        left_small_tusk.addChild(left_small_tusk_r1);
        setRotationAngle(left_small_tusk_r1, 1.1043F, -0.1062F, -0.109F);
        left_small_tusk_r1.addBox(-0.3534F, -2.301F, -24.9481F, 4, 5, 13, 0.0F);

        left_tusk_root_r1 = new ModelRenderer(this);
        left_tusk_root_r1.setRotationPoint(27.5F, -37.7938F, -31.7753F);
        left_small_tusk.addChild(left_tusk_root_r1);
        setRotationAngle(left_tusk_root_r1, 1.1043F, -0.1062F, -0.109F);
        left_tusk_root_r1.addBox(-1.4112F, -3.7102F, -14.3851F, 6, 9, 17, 0.0F);

        right_small_tusks = new ModelRenderer(this);
        right_small_tusks.setRotationPoint(17.0F, 50.7962F, 21.5595F);
        head.addChild(right_small_tusks);

        right_small_root_r1 = new ModelRenderer(this);
        right_small_root_r1.setRotationPoint(-27.5F, -36.7938F, -31.7753F);
        right_small_tusks.addChild(right_small_root_r1);
        setRotationAngle(right_small_root_r1, 1.1043F, 0.1062F, 0.109F);
        right_small_root_r1.addBox(-3.6466F, -2.301F, -24.9481F, 4, 5, 13, 0.0F);

        right_tusk_root_r1 = new ModelRenderer(this);
        right_tusk_root_r1.setRotationPoint(-27.5F, -37.7938F, -31.7753F);
        right_small_tusks.addChild(right_tusk_root_r1);
        setRotationAngle(right_tusk_root_r1, 1.1043F, 0.1062F, 0.109F);
        right_tusk_root_r1.addBox(-4.5888F, -3.7102F, -14.3851F, 6, 9, 17, 0.0F);

        left_main_tusk = new ModelRenderer(this);
        left_main_tusk.setRotationPoint(10.0F, 18.7962F, -26.4405F);
        head.addChild(left_main_tusk);
        setRotationAngle(left_main_tusk, -0.173F, 0.1752F, -0.1668F);

        t5_r1 = new ModelRenderer(this);
        t5_r1.setRotationPoint(-1.608F, 2.2027F, -0.7789F);
        left_main_tusk.addChild(t5_r1);
        setRotationAngle(t5_r1, -1.5333F, -0.3135F, -0.1096F);
        t5_r1.addBox(-1.2492F, 30.3571F, 28.6119F, 3, 6, 3, 0.0F);

        t4_r1 = new ModelRenderer(this);
        t4_r1.setRotationPoint(-1.608F, 2.2027F, -1.7789F);
        left_main_tusk.addChild(t4_r1);
        setRotationAngle(t4_r1, -1.6206F, -0.3135F, -0.1096F);
        t4_r1.addBox(-1.7153F, 12.6982F, 36.9549F, 4, 12, 4, 0.0F);

        t3_r1 = new ModelRenderer(this);
        t3_r1.setRotationPoint(-1.608F, 2.2027F, -0.7789F);
        left_main_tusk.addChild(t3_r1);
        setRotationAngle(t3_r1, -0.9601F, -0.3135F, -0.1096F);
        t3_r1.addBox(-1.3971F, 41.0388F, 7.8274F, 3, 4, 9, 0.0F);

        t1_r1 = new ModelRenderer(this);
        t1_r1.setRotationPoint(6.0354F, 31.9053F, -14.2016F);
        left_main_tusk.addChild(t1_r1);
        setRotationAngle(t1_r1, -0.4861F, -0.3135F, -0.1096F);
        t1_r1.addBox(-3.0F, -10.5F, -2.5F, 6, 20, 6, 0.0F);

        t1_r2 = new ModelRenderer(this);
        t1_r2.setRotationPoint(-1.608F, 2.2027F, -1.7789F);
        left_main_tusk.addChild(t1_r2);
        setRotationAngle(t1_r2, -0.3552F, -0.3135F, -0.1096F);
        t1_r2.addBox(-3.256F, -4.6306F, -3.4385F, 7, 28, 7, 0.0F);

        right_main_tusk = new ModelRenderer(this);
        right_main_tusk.setRotationPoint(-21.0F, 62.7962F, -6.4405F);
        head.addChild(right_main_tusk);
        setRotationAngle(right_main_tusk, -0.173F, -0.1752F, 0.1668F);

        t5_r2 = new ModelRenderer(this);
        t5_r2.setRotationPoint(1.7742F, -39.0075F, -28.6301F);
        right_main_tusk.addChild(t5_r2);
        setRotationAngle(t5_r2, -1.5333F, 0.3135F, 0.1096F);
        t5_r2.addBox(-1.7508F, 30.3571F, 28.6119F, 3, 6, 3, 0.0F);

        t4_r2 = new ModelRenderer(this);
        t4_r2.setRotationPoint(1.7742F, -39.0075F, -29.6301F);
        right_main_tusk.addChild(t4_r2);
        setRotationAngle(t4_r2, -1.6206F, 0.3135F, 0.1096F);
        t4_r2.addBox(-2.2847F, 12.6982F, 36.9549F, 4, 12, 4, 0.0F);

        t3_r2 = new ModelRenderer(this);
        t3_r2.setRotationPoint(1.7742F, -39.0075F, -28.6301F);
        right_main_tusk.addChild(t3_r2);
        setRotationAngle(t3_r2, -0.9601F, 0.3135F, 0.1096F);
        t3_r2.addBox(-1.6029F, 41.0388F, 7.8274F, 3, 4, 9, 0.0F);

        t1_r3 = new ModelRenderer(this);
        t1_r3.setRotationPoint(-5.8692F, -9.3049F, -42.0529F);
        right_main_tusk.addChild(t1_r3);
        setRotationAngle(t1_r3, -0.4861F, 0.3135F, 0.1096F);
        t1_r3.addBox(-3.0F, -10.5F, -2.5F, 6, 20, 6, 0.0F);

        t1_r4 = new ModelRenderer(this);
        t1_r4.setRotationPoint(1.7742F, -39.0075F, -29.6301F);
        right_main_tusk.addChild(t1_r4);
        setRotationAngle(t1_r4, -0.3552F, 0.3135F, 0.1096F);
        t1_r4.addBox(-3.744F, -4.6306F, -3.4385F, 7, 28, 7, 0.0F);

        left_ear = new ModelRenderer(this);
        left_ear.setRotationPoint(13.6766F, -0.0718F, -1.1615F);
        head.addChild(left_ear);

        right_ear_r1 = new ModelRenderer(this);
        right_ear_r1.setRotationPoint(4.0F, 0.0F, 0.0F);
        left_ear.addChild(right_ear_r1);
        setRotationAngle(right_ear_r1, 2.1378F, 1.0787F, 2.459F);
        right_ear_r1.addBox(-2.0F, -9.5F, -9.5F, 4, 19, 19, 0.0F);

        right_ear = new ModelRenderer(this);
        right_ear.setRotationPoint(-14.6766F, 0.9282F, -1.1615F);
        head.addChild(right_ear);

        left_ear_r1 = new ModelRenderer(this);
        left_ear_r1.setRotationPoint(-3.0F, -1.0F, 0.0F);
        right_ear.addChild(left_ear_r1);
        setRotationAngle(left_ear_r1, 2.1378F, -1.0787F, -2.459F);
        left_ear_r1.addBox(-2.0F, -9.5F, -9.5F, 4, 19, 19, 0.0F);

        trunk = new ModelRenderer(this);
        trunk.setRotationPoint(0.0F, 3.0F, -19.0F);
        head.addChild(trunk);
        setRotationAngle(trunk, -0.2182F, 0.0F, 0.0F);

        t1 = new ModelRenderer(this);
        t1.setRotationPoint(-0.005F, 16.4833F, -2.829F);
        trunk.addChild(t1);
        setRotationAngle(t1, -0.1309F, 0.0F, 0.0F);
        t1.addBox(-5.995F, -3.5F, -5.5F, 12, 19, 10, 0.0F);

        t2 = new ModelRenderer(this);
        t2.setRotationPoint(0.0F, 8.8408F, -1.3063F);
        t1.addChild(t2);

        t2_r1 = new ModelRenderer(this);
        t2_r1.setRotationPoint(2.005F, 29.0F, 2.0F);
        t2.addChild(t2_r1);
        setRotationAngle(t2_r1, 0.3054F, 0.0F, 0.0F);
        t2_r1.addBox(-7.0F, -23.0F, 2.0F, 10, 15, 8, 0.0F);

        t3 = new ModelRenderer(this);
        t3.setRotationPoint(0.0F, 19.0F, 5.0F);
        t2.addChild(t3);

        t3_r3 = new ModelRenderer(this);
        t3_r3.setRotationPoint(2.005F, 3.0F, -3.0F);
        t3.addChild(t3_r3);
        setRotationAngle(t3_r3, -0.7418F, 0.0F, 0.0F);
        t3_r3.addBox(-6.0F, -7.0F, -1.0F, 8, 6, 12, 0.0F);

        t4 = new ModelRenderer(this);
        t4.setRotationPoint(1.0F, 6.0F, 7.0F);
        t3.addChild(t4);

        t4_r3 = new ModelRenderer(this);
        t4_r3.setRotationPoint(0.005F, 3.0F, 3.0F);
        t4.addChild(t4_r3);
        setRotationAngle(t4_r3, 0.3491F, 0.0F, 0.0F);
        t4_r3.addBox(-4.99F, -7.0F, -4.0F, 8, 6, 11, 0.0F);

        t5 = new ModelRenderer(this);
        t5.setRotationPoint(1.0F, -3.0F, 7.0F);
        t4.addChild(t5);

        t5_r3 = new ModelRenderer(this);
        t5_r3.setRotationPoint(0.005F, 1.0F, 5.0F);
        t5.addChild(t5_r3);
        setRotationAngle(t5_r3, 1.2217F, 0.0F, 0.0F);
        t5_r3.addBox(-5.0F, -7.0F, -3.0F, 6, 5, 11, 0.0F);

        legs = new ModelRenderer(this);
        legs.setRotationPoint(8.0F, 47.0F, -90.0F);
        Master.addChild(legs);

        front_right_foot = new ModelRenderer(this);
        front_right_foot.setRotationPoint(7.0F, -47.2038F, 19.5595F);
        legs.addChild(front_right_foot);

        leg_front_upper = new ModelRenderer(this);
        leg_front_upper.setRotationPoint(6.0F, 0.2038F, 0.4405F);
        front_right_foot.addChild(leg_front_upper);
        leg_front_upper.addBox(-14.0F, -8.0F, -8.0F, 15, 23, 15, 0.0F);

        leg_front_lower = new ModelRenderer(this);
        leg_front_lower.setRotationPoint(-13.0F, 19.0F, -4.0F);
        leg_front_upper.addChild(leg_front_lower);
        leg_front_lower.addBox(1.0F, -8.0F, -3.0F, 11, 32, 12, 0.0F);

        foot_upper = new ModelRenderer(this);
        foot_upper.setRotationPoint(0.0F, 27.0F, 3.0F);
        leg_front_lower.addChild(foot_upper);
        foot_upper.addBox(0.0F, -3.0F, -6.0F, 13, 4, 12, 0.0F);

        foot_lower = new ModelRenderer(this);
        foot_lower.setRotationPoint(0.0F, 4.0F, 1.0F);
        foot_upper.addChild(foot_lower);
        foot_lower.addBox(-1.0F, -3.0F, -9.0F, 15, 3, 15, 0.0F);

        front_left_foot = new ModelRenderer(this);
        front_left_foot.setRotationPoint(-23.0F, -47.2038F, 19.5595F);
        legs.addChild(front_left_foot);
        front_left_foot.addBox(-7.0F, 47.2038F, -8.5595F, 15, 3, 15, 0.0F);
        front_left_foot.addBox(-6.0F, 43.2038F, -6.5595F, 13, 4, 12, 0.0F);
        front_left_foot.addBox(-7.0F, -7.7962F, -7.5595F, 15, 23, 15, 0.0F);
        front_left_foot.addBox(-5.0F, 11.2038F, -6.5595F, 11, 32, 12, 0.0F);

        back_left_foot = new ModelRenderer(this);
        back_left_foot.setRotationPoint(-26.0F, -43.2038F, 77.5595F);
        legs.addChild(back_left_foot);
        back_left_foot.addBox(-4.0F, 43.2038F, -8.5595F, 15, 3, 16, 0.0F);
        back_left_foot.addBox(-3.0F, 39.2038F, -6.5595F, 13, 4, 13, 0.0F);
        back_left_foot.addBox(-2.0F, 14.2038F, -6.5595F, 11, 25, 13, 0.0F);
        back_left_foot.addBox(-4.0F, -9.7962F, -8.5595F, 15, 24, 18, 0.0F);

        back_right_foot = new ModelRenderer(this);
        back_right_foot.setRotationPoint(9.0F, -43.2038F, 77.5595F);
        legs.addChild(back_right_foot);
        back_right_foot.addBox(-10.0F, 43.2038F, -8.5595F, 15, 3, 16, 0.0F);
        back_right_foot.addBox(-9.0F, 39.2038F, -6.5595F, 13, 4, 13, 0.0F);
        back_right_foot.addBox(-8.0F, 14.2038F, -6.5595F, 11, 25, 13, 0.0F);
        back_right_foot.addBox(-10.0F, -9.7962F, -8.5595F, 15, 24, 18, 0.0F);
    }

    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        // this.setRotationAngles(f, f1, f2, f3, f4, f5, entity); //does this do anything?
        Master.render(f5);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

}