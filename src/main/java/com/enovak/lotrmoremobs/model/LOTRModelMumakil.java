package com.enovak.lotrmoremobs.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public class LOTRModelMumakil extends ModelBase {
    /**
     * The Bedrock/Geo model is authored in much larger texture-space units.  ModelRenderer derives
     * UV island size from cube dimensions, so the Java cubes are expanded with one consistent scale
     * factor and then scaled back down at render time. This preserves the in-game footprint while
     * making the UV rectangles closer to the 2048x2048 mumakil_war texture layout.
     */
    private static final float GEO_TO_JAVA_UV_SCALE = 3.0F;
    private static final float JAVA_RENDER_SCALE = 1.0F / GEO_TO_JAVA_UV_SCALE;

    private final ModelRenderer master;
    private final ModelRenderer sway;
    private final ModelRenderer tail;
    private final ModelRenderer upperTail;
    private final ModelRenderer middleTail;
    private final ModelRenderer lowerTail;
    private final ModelRenderer body;
    private final ModelRenderer bodyBack;
    private final ModelRenderer upperBack;
    private final ModelRenderer upperBackHump;
    private final ModelRenderer upperBackFront;
    private final ModelRenderer head;
    private final ModelRenderer lowerHead;
    private final ModelRenderer trunkTuskRoot;
    private final ModelRenderer skull;
    private final ModelRenderer leftMainTusk;
    private final ModelRenderer rightMainTusk;
    private final ModelRenderer leftEar;
    private final ModelRenderer rightEar;
    private final ModelRenderer trunk;
    private final ModelRenderer trunk01;
    private final ModelRenderer trunk02;
    private final ModelRenderer trunk03;
    private final ModelRenderer trunk04;
    private final ModelRenderer trunk05;
    private final ModelRenderer legs;
    private final ModelRenderer frontRightLeg;
    private final ModelRenderer frontRightUpperLeg;
    private final ModelRenderer frontRightLowerLeg;
    private final ModelRenderer frontRightUpperFoot;
    private final ModelRenderer frontRightLowerFoot;
    private final ModelRenderer frontLeftLeg;
    private final ModelRenderer backLeftLeg;
    private final ModelRenderer backRightLeg;

    public LOTRModelMumakil() {
        this(0.0F);
    }

    public LOTRModelMumakil(float inflate) {
        this.textureWidth = 2048;
        this.textureHeight = 2048;

        // Geo bone: root
        this.master = new ModelRenderer(this);
        setPoint(this.master, 0.0F, -26.0F, 50.0F);

        // Geo bone: body_sway
        this.sway = new ModelRenderer(this);
        setPoint(this.sway, 0.0F, -13.0F, -67.0F);
        this.master.addChild(this.sway);

        // Geo bone: tail / tail_upper / tail_middle / tail_lower; UVs [44,756], [53,844], [60,897]
        this.tail = new ModelRenderer(this);
        setPoint(this.tail, 0.0F, 13.0F, 67.0F);
        this.sway.addChild(this.tail);

        this.upperTail = new ModelRenderer(this, 44, 756);
        setPoint(this.upperTail, 2.0F, 17.0F, -2.0F);
        addScaledBox(this.upperTail, -5.0F, -21.0F, -1.0F, 6, 21, 4, inflate);
        this.tail.addChild(this.upperTail);

        this.middleTail = new ModelRenderer(this, 53, 844);
        setPoint(this.middleTail, -2.0F, 1.0F, 2.0F);
        addScaledBox(this.middleTail, -2.0F, -0.9617F, -1.0F, 4, 10, 3, inflate);
        this.upperTail.addChild(this.middleTail);

        this.lowerTail = new ModelRenderer(this, 60, 897);
        setPoint(this.lowerTail, 0.0F, 10.0383F, 2.0F);
        addScaledBox(this.lowerTail, -1.0F, -1.0F, -1.0F, 2, 10, 2, inflate);
        this.middleTail.addChild(this.lowerTail);

        // Geo bone: body. Cube 0 UV [235,244], cube 1 UV [703,43].
        this.body = new ModelRenderer(this, 235, 244);
        setPoint(this.body, -2.0F, 1.0F, 17.0F);
        addScaledBox(this.body, -18.01F, 1.0F, -14.0F, 39, 20, 60, inflate);
        this.sway.addChild(this.body);

        this.bodyBack = new ModelRenderer(this, 703, 43);
        setPoint(this.bodyBack, 21.0F, 7.0F, -3.0F);
        setRotationAngle(this.bodyBack, -0.1745F, 0.0F, 0.0F);
        addScaledBox(this.bodyBack, -39.0F, -28.0F, -17.0F, 40, 31, 67, inflate);
        this.body.addChild(this.bodyBack);

        // Geo bone: upper_back. Cubes UV [533,527] and [176,38].
        this.upperBack = new ModelRenderer(this);
        setPoint(this.upperBack, 2.0F, -0.8195F, -19.1583F);
        this.body.addChild(this.upperBack);

        this.upperBackHump = new ModelRenderer(this, 533, 527);
        setPoint(this.upperBackHump, 0.0F, -6.0F, -5.0F);
        setRotationAngle(this.upperBackHump, 1.8151F, 0.0F, 0.0F);
        addScaledBox(this.upperBackHump, -19.0F, -17.0F, -14.0F, 38, 32, 27, inflate);
        this.upperBack.addChild(this.upperBackHump);

        this.upperBackFront = new ModelRenderer(this, 176, 38);
        setPoint(this.upperBackFront, 0.0F, 8.6987F, -2.783F);
        setRotationAngle(this.upperBackFront, 1.2217F, 0.0F, 0.0F);
        addScaledBox(this.upperBackFront, -18.9899F, -13.5F, -7.5F, 38, 26, 15, inflate);
        this.upperBack.addChild(this.upperBackFront);

        // Geo bone: head. Main head/lower head/trunk root/skull cubes use UVs [866,385], [891,576], [235,546].
        this.head = new ModelRenderer(this);
        setPoint(this.head, 0.0F, 1.8195F, -23.8417F);
        this.upperBack.addChild(this.head);

        this.lowerHead = new ModelRenderer(this, 891, 576);
        setPoint(this.lowerHead, -0.5F, 15.4231F, -13.1771F);
        setRotationAngle(this.lowerHead, 0.6109F, 0.0F, 0.0F);
        addScaledBox(this.lowerHead, -10.5F, -7.5F, -8.5F, 20, 11, 22, inflate);
        this.head.addChild(this.lowerHead);

        this.trunkTuskRoot = new ModelRenderer(this, 235, 546);
        setPoint(this.trunkTuskRoot, 1.0F, 13.6338F, -24.3129F);
        setRotationAngle(this.trunkTuskRoot, 1.0036F, 0.0F, 0.0F);
        addScaledBox(this.trunkTuskRoot, -13.0F, -5.0F, -5.0F, 24, 12, 19, inflate);
        this.head.addChild(this.trunkTuskRoot);

        this.skull = new ModelRenderer(this, 866, 385);
        setPoint(this.skull, 0.0F, 0.9885F, -3.2061F);
        setRotationAngle(this.skull, 0.5672F, 0.0F, 0.0F);
        addScaledBox(this.skull, -14.0F, -11.5F, -15.0F, 28, 23, 30, inflate);
        this.head.addChild(this.skull);

        // Geo bones: left_main_tusk / right_main_tusk. Main tusk segments preserve UVs [33,38], [39,164], [51,665], [63,578], [123,590].
        this.leftMainTusk = new ModelRenderer(this);
        setPoint(this.leftMainTusk, 10.0F, 18.7962F, -26.4405F);
        setRotationAngle(this.leftMainTusk, -0.173F, 0.1752F, -0.1668F);
        this.head.addChild(this.leftMainTusk);
        addTuskSegments(this.leftMainTusk, false, inflate);

        this.rightMainTusk = new ModelRenderer(this);
        setPoint(this.rightMainTusk, -21.0F, 62.7962F, -6.4405F);
        setRotationAngle(this.rightMainTusk, -0.173F, -0.1752F, 0.1668F);
        this.head.addChild(this.rightMainTusk);
        addTuskSegments(this.rightMainTusk, true, inflate);

        // Geo bones: left_ear / right_ear. Main ear cube UV [246,248].
        this.leftEar = new ModelRenderer(this, 246, 248);
        setPoint(this.leftEar, 13.6766F, -0.0718F, -1.1615F);
        this.head.addChild(this.leftEar);
        ModelRenderer leftEarCube = new ModelRenderer(this, 246, 248);
        setPoint(leftEarCube, 4.0F, 0.0F, 0.0F);
        setRotationAngle(leftEarCube, 2.1378F, 1.0787F, 2.459F);
        addScaledBox(leftEarCube, -2.0F, -9.5F, -9.5F, 4, 19, 19, inflate);
        this.leftEar.addChild(leftEarCube);

        this.rightEar = new ModelRenderer(this, 246, 248);
        setPoint(this.rightEar, -14.6766F, 0.9282F, -1.1615F);
        this.head.addChild(this.rightEar);
        ModelRenderer rightEarCube = new ModelRenderer(this, 246, 248);
        setPoint(rightEarCube, -3.0F, -1.0F, 0.0F);
        setRotationAngle(rightEarCube, 2.1378F, -1.0787F, -2.459F);
        addScaledBox(rightEarCube, -2.0F, -9.5F, -9.5F, 4, 19, 19, inflate);
        this.rightEar.addChild(rightEarCube);

        // Geo bones: trunk, trunk_01 through trunk_05. UVs [721,697], [591,716], [444,731], [306,736], [163,737].
        this.trunk = new ModelRenderer(this);
        setPoint(this.trunk, 0.0F, 3.0F, -19.0F);
        setRotationAngle(this.trunk, -0.2182F, 0.0F, 0.0F);
        this.head.addChild(this.trunk);

        this.trunk01 = new ModelRenderer(this, 721, 697);
        setPoint(this.trunk01, -0.005F, 16.4833F, -2.829F);
        setRotationAngle(this.trunk01, -0.1309F, 0.0F, 0.0F);
        addScaledBox(this.trunk01, -5.995F, -3.5F, -5.5F, 12, 19, 10, inflate);
        this.trunk.addChild(this.trunk01);

        this.trunk02 = new ModelRenderer(this);
        setPoint(this.trunk02, 0.0F, 8.8408F, -1.3063F);
        this.trunk01.addChild(this.trunk02);
        ModelRenderer trunk02Cube = new ModelRenderer(this, 591, 716);
        setPoint(trunk02Cube, 2.005F, 29.0F, 2.0F);
        setRotationAngle(trunk02Cube, 0.3054F, 0.0F, 0.0F);
        addScaledBox(trunk02Cube, -7.0F, -23.0F, 2.0F, 10, 15, 8, inflate);
        this.trunk02.addChild(trunk02Cube);

        this.trunk03 = new ModelRenderer(this);
        setPoint(this.trunk03, 0.0F, 19.0F, 5.0F);
        this.trunk02.addChild(this.trunk03);
        ModelRenderer trunk03Cube = new ModelRenderer(this, 444, 731);
        setPoint(trunk03Cube, 2.005F, 3.0F, -3.0F);
        setRotationAngle(trunk03Cube, -0.7418F, 0.0F, 0.0F);
        addScaledBox(trunk03Cube, -6.0F, -7.0F, -1.0F, 8, 6, 12, inflate);
        this.trunk03.addChild(trunk03Cube);

        this.trunk04 = new ModelRenderer(this);
        setPoint(this.trunk04, 1.0F, 6.0F, 7.0F);
        this.trunk03.addChild(this.trunk04);
        ModelRenderer trunk04Cube = new ModelRenderer(this, 306, 736);
        setPoint(trunk04Cube, 0.005F, 3.0F, 3.0F);
        setRotationAngle(trunk04Cube, 0.3491F, 0.0F, 0.0F);
        addScaledBox(trunk04Cube, -4.99F, -7.0F, -4.0F, 8, 6, 11, inflate);
        this.trunk04.addChild(trunk04Cube);

        this.trunk05 = new ModelRenderer(this);
        setPoint(this.trunk05, 1.0F, -3.0F, 7.0F);
        this.trunk04.addChild(this.trunk05);
        ModelRenderer trunk05Cube = new ModelRenderer(this, 163, 737);
        setPoint(trunk05Cube, 0.005F, 1.0F, 5.0F);
        setRotationAngle(trunk05Cube, 1.2217F, 0.0F, 0.0F);
        addScaledBox(trunk05Cube, -5.0F, -7.0F, -3.0F, 6, 5, 11, inflate);
        this.trunk05.addChild(trunk05Cube);

        // Geo bones: legs and the four main leg/foot groups. Decorative ankle spikes are intentionally not ported yet.
        this.legs = new ModelRenderer(this);
        setPoint(this.legs, 8.0F, 47.0F, -90.0F);
        this.master.addChild(this.legs);

        // Geo bone group: front_right_leg. UVs [493,826], [335,813], [952,834], [935,760].
        this.frontRightLeg = new ModelRenderer(this);
        setPoint(this.frontRightLeg, 7.0F, -47.2038F, 19.5595F);
        this.legs.addChild(this.frontRightLeg);

        this.frontRightUpperLeg = new ModelRenderer(this, 493, 826);
        setPoint(this.frontRightUpperLeg, 6.0F, 0.2038F, 0.4405F);
        addScaledBox(this.frontRightUpperLeg, -14.0F, -8.0F, -8.0F, 15, 23, 15, inflate);
        this.frontRightLeg.addChild(this.frontRightUpperLeg);

        this.frontRightLowerLeg = new ModelRenderer(this, 335, 813);
        setPoint(this.frontRightLowerLeg, -13.0F, 19.0F, -4.0F);
        addScaledBox(this.frontRightLowerLeg, 1.0F, -8.0F, -3.0F, 11, 32, 12, inflate);
        this.frontRightUpperLeg.addChild(this.frontRightLowerLeg);

        this.frontRightUpperFoot = new ModelRenderer(this, 952, 834);
        setPoint(this.frontRightUpperFoot, 0.0F, 27.0F, 3.0F);
        addScaledBox(this.frontRightUpperFoot, 0.0F, -3.0F, -6.0F, 13, 4, 12, inflate);
        this.frontRightLowerLeg.addChild(this.frontRightUpperFoot);

        this.frontRightLowerFoot = new ModelRenderer(this, 935, 760);
        setPoint(this.frontRightLowerFoot, 0.0F, 4.0F, 1.0F);
        addScaledBox(this.frontRightLowerFoot, -1.0F, -3.0F, -9.0F, 15, 3, 15, inflate);
        this.frontRightUpperFoot.addChild(this.frontRightLowerFoot);

        // Geo bone group: front_left_leg. UVs [493,826], [335,813], [952,834], [935,760].
        this.frontLeftLeg = new ModelRenderer(this);
        setPoint(this.frontLeftLeg, -23.0F, -47.2038F, 19.5595F);
        addScaledBox(this.frontLeftLeg, -7.0F, 47.2038F, -8.5595F, 15, 3, 15, inflate);
        addScaledBox(this.frontLeftLeg, -6.0F, 43.2038F, -6.5595F, 13, 4, 12, inflate);
        addScaledBox(this.frontLeftLeg, -7.0F, -7.7962F, -7.5595F, 15, 23, 15, inflate);
        addScaledBox(this.frontLeftLeg, -5.0F, 11.2038F, -6.5595F, 11, 32, 12, inflate);
        this.legs.addChild(this.frontLeftLeg);

        // Geo bone group: back_left_leg. UVs [695,815], [169,829], [949,898], [934,678].
        this.backLeftLeg = new ModelRenderer(this);
        setPoint(this.backLeftLeg, -26.0F, -43.2038F, 77.5595F);
        addScaledBox(this.backLeftLeg, -4.0F, 43.2038F, -8.5595F, 15, 3, 16, inflate);
        addScaledBox(this.backLeftLeg, -3.0F, 39.2038F, -6.5595F, 13, 4, 13, inflate);
        addScaledBox(this.backLeftLeg, -2.0F, 14.2038F, -6.5595F, 11, 25, 13, inflate);
        addScaledBox(this.backLeftLeg, -4.0F, -9.7962F, -8.5595F, 15, 24, 18, inflate);
        this.legs.addChild(this.backLeftLeg);

        // Geo bone group: back_right_leg. UVs [695,815], [169,829], [949,898], [934,678].
        this.backRightLeg = new ModelRenderer(this);
        setPoint(this.backRightLeg, 9.0F, -43.2038F, 77.5595F);
        addScaledBox(this.backRightLeg, -10.0F, 43.2038F, -8.5595F, 15, 3, 16, inflate);
        addScaledBox(this.backRightLeg, -9.0F, 39.2038F, -6.5595F, 13, 4, 13, inflate);
        addScaledBox(this.backRightLeg, -8.0F, 14.2038F, -6.5595F, 11, 25, 13, inflate);
        addScaledBox(this.backRightLeg, -10.0F, -9.7962F, -8.5595F, 15, 24, 18, inflate);
        this.legs.addChild(this.backRightLeg);
    }

    private void addTuskSegments(ModelRenderer tusk, boolean mirrored, float inflate) {
        ModelRenderer segment1 = new ModelRenderer(this, 33, 38);
        setPoint(segment1, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -29.6301F : -1.7789F);
        setRotationAngle(segment1, -0.3552F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F);
        addScaledBox(segment1, mirrored ? -3.744F : -3.256F, -4.6306F, -3.4385F, 7, 28, 7, inflate);
        tusk.addChild(segment1);

        ModelRenderer segment2 = new ModelRenderer(this, 33, 38);
        setPoint(segment2, mirrored ? -5.8692F : 6.0354F, mirrored ? -9.3049F : 31.9053F, mirrored ? -42.0529F : -14.2016F);
        setRotationAngle(segment2, -0.4861F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F);
        addScaledBox(segment2, -3.0F, -10.5F, -2.5F, 6, 20, 6, inflate);
        tusk.addChild(segment2);

        ModelRenderer segment3 = new ModelRenderer(this, 51, 665);
        setPoint(segment3, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -28.6301F : -0.7789F);
        setRotationAngle(segment3, -0.9601F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F);
        addScaledBox(segment3, mirrored ? -1.6029F : -1.3971F, 41.0388F, 7.8274F, 3, 4, 9, inflate);
        tusk.addChild(segment3);

        ModelRenderer segment4 = new ModelRenderer(this, 39, 164);
        setPoint(segment4, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -29.6301F : -1.7789F);
        setRotationAngle(segment4, -1.6206F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F);
        addScaledBox(segment4, mirrored ? -2.2847F : -1.7153F, 12.6982F, 36.9549F, 4, 12, 4, inflate);
        tusk.addChild(segment4);

        ModelRenderer segment5 = new ModelRenderer(this, 63, 578);
        setPoint(segment5, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -28.6301F : -0.7789F);
        setRotationAngle(segment5, -1.5333F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F);
        addScaledBox(segment5, mirrored ? -1.7508F : -1.2492F, 30.3571F, 28.6119F, 3, 6, 3, inflate);
        tusk.addChild(segment5);
    }

    private static void setPoint(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.setRotationPoint(x * GEO_TO_JAVA_UV_SCALE, y * GEO_TO_JAVA_UV_SCALE, z * GEO_TO_JAVA_UV_SCALE);
    }

    private static void addScaledBox(ModelRenderer modelRenderer, float x, float y, float z,
                                     int width, int height, int depth, float inflate) {
        modelRenderer.addBox(
                x * GEO_TO_JAVA_UV_SCALE,
                y * GEO_TO_JAVA_UV_SCALE,
                z * GEO_TO_JAVA_UV_SCALE,
                Math.round(width * GEO_TO_JAVA_UV_SCALE),
                Math.round(height * GEO_TO_JAVA_UV_SCALE),
                Math.round(depth * GEO_TO_JAVA_UV_SCALE),
                inflate * GEO_TO_JAVA_UV_SCALE
        );
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(JAVA_RENDER_SCALE, JAVA_RENDER_SCALE, JAVA_RENDER_SCALE);
        this.master.render(scale);
        GL11.glPopMatrix();
    }

    private static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
