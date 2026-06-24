package com.enovak.lotrmoremobs.model;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

public class LOTRModelMumakil extends ModelBase {
    /**
     * The source Bedrock model uses 2048x2048 texture-space-sized cubes. ModelRenderer builds UV
     * rectangles from Java cube dimensions, so these cubes are expanded by one global factor and
     * scaled back down during render. That keeps the in-game footprint close to the old Java model
     * while making the sampled UV rectangles much closer to LOTRMumakilModel.geo.json.
     */
    private static final float GEO_TO_JAVA_UV_SCALE = 3.0F;
    private static final float JAVA_RENDER_SCALE = 1.0F / GEO_TO_JAVA_UV_SCALE;
    // After the 1.35x entity render scale, negative inflation on visible exterior cubes exposes edge cracks.
    // Keep visible boxes full-sized; handle any future true z-fighting with local offsets instead.
    private static final float Z_FIGHT_EPSILON = 0.0F;
    private static final float SEAM_OVERLAP = 0.05F;

    // Temporary UV diagnostic switches. Leave all false for normal rendering; if more than one is
    // enabled, the first one in renderDebugIsolation() wins. Head/tusk/foot modes render their local
    // groups directly for texture-bleed isolation, not final in-world pose validation.
    private static final boolean DEBUG_RENDER_ONLY_BODY = false;
    private static final boolean DEBUG_RENDER_ONLY_LEGS = false;
    private static final boolean DEBUG_RENDER_ONLY_HEAD = false;
    private static final boolean DEBUG_RENDER_ONLY_TUSKS = false;
    private static final boolean DEBUG_RENDER_ONLY_FEET = false;

    // Optional texture test note: to use assets/lotrmoremobs/textures/mob/mumakil/mumakil_war_uvdebug.png,
    // temporarily point LOTRRenderMumakil's texture ResourceLocation at that file. Do not switch it here;
    // this branch keeps default rendering unchanged unless a debug boolean is manually enabled.

    private final ModelRenderer master;
    private ModelRenderer bodyGroup;
    private ModelRenderer legsGroup;
    private ModelRenderer headGroup;
    private ModelRenderer leftTuskGroup;
    private ModelRenderer rightTuskGroup;
    private ModelRenderer frontRightUpperFootGroup;
    private ModelRenderer frontRightLowerFootGroup;
    private ModelRenderer frontLeftUpperFootGroup;
    private ModelRenderer frontLeftLowerFootGroup;
    private ModelRenderer backLeftUpperFootGroup;
    private ModelRenderer backLeftLowerFootGroup;
    private ModelRenderer backRightUpperFootGroup;
    private ModelRenderer backRightLowerFootGroup;

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
        ModelRenderer sway = emptyChild(this.master, 0.0F, -13.0F, -67.0F);

        // Geo bones: tail, tail_upper, tail_middle, tail_lower. UVs [44,756], [53,844], [60,897].
        ModelRenderer tail = emptyChild(sway, 0.0F, 13.0F, 67.0F);
        ModelRenderer upperTail = part(tail, 44, 756, 2.0F, 17.0F, -2.0F, -5.0F, -21.0F, -1.0F, 6, 21, 4, inflate);
        ModelRenderer middleTail = part(upperTail, 53, 844, -2.0F, 1.0F, 2.0F, -2.0F, -0.9617F, -1.0F, 4, 10, 3, inflate);
        part(middleTail, 60, 897, 0.0F, 10.0383F, 2.0F, -1.0F, -1.0F, -1.0F, 2, 10, 2, inflate);

        // Geo bone: body. Cubes preserve UV [235,244] and [703,43]. The main side box is a high-risk
        // bleed candidate because its scaled UV footprint is large and borders nearby atlas islands.
        ModelRenderer body = part(sway, 235, 244, -2.0F, 1.0F, 17.0F, -18.01F, 1.0F, -14.0F, 39, 20, 60, inflate);
        this.bodyGroup = body;
        // Slightly tucked to reduce flicker where the raised back intersects the main body.
        // Head/body transition candidate: this raised back box can expose neighboring texture pixels if its rounded dimensions overrun the island.
        part(body, 703, 43, 21.0F, 7.0F, -3.0F, -0.1745F, 0.0F, 0.0F, -39.0F, -28.0F, -17.0F, 40, 31, 67, stableInflate(inflate));

        // Geo bone: upper_back. UVs are matched to the cube dimensions from the Geo JSON.
        ModelRenderer upperBack = emptyChild(body, 2.0F, -0.8195F, -19.1583F);
        // Head/body transition candidates: these shoulder/back boxes sit under the head group and should be isolated with DEBUG_RENDER_ONLY_BODY/HEAD.
        part(upperBack, 176, 38, 0.0F, -6.0F, -5.0F, 1.8151F, 0.0F, 0.0F, -19.0F, -17.0F, -14.0F, 38, 32, 27, stableInflate(inflate));
        part(upperBack, 533, 527, 0.0F, 8.6987F, -2.783F, 1.2217F, 0.0F, 0.0F, -18.9899F, -13.5F, -7.5F, 38, 26, 15, stableInflate(inflate));

        // Geo bone: head. UVs are matched to the lower-head/trunk-root dimensions from the Geo JSON.
        ModelRenderer head = emptyChild(upperBack, 0.0F, 1.8195F, -23.8417F);
        this.headGroup = head;
        // Head/trunk transition candidates: these compact boxes are likely places for one-pixel UV bleed at face edges.
        part(head, 235, 546, -0.5F, 15.4231F, -13.1771F, 0.6109F, 0.0F, 0.0F, -10.5F, -7.5F, -8.5F, 20, 11, 22, stableInflate(inflate));
        part(head, 891, 576, 1.0F, 13.6338F, -24.3129F, 1.0036F, 0.0F, 0.0F, -13.0F, -5.0F, -5.0F, 24, 12, 19, stableInflate(inflate));
        part(head, 866, 385, 0.0F, 0.9885F, -3.2061F, 0.5672F, 0.0F, 0.0F, -14.0F, -11.5F, -15.0F, 28, 23, 30, inflate);

        // Geo bones: left_main_tusk and right_main_tusk. Decorative tusk spikes are intentionally not ported yet.
        ModelRenderer leftMainTusk = emptyChild(head, 10.0F, 18.7962F, -26.4405F, -0.173F, 0.1752F, -0.1668F);
        this.leftTuskGroup = leftMainTusk;
        addTuskSegments(leftMainTusk, false, inflate);

        ModelRenderer rightMainTusk = emptyChild(head, -21.0F, 62.7962F, -6.4405F, -0.173F, -0.1752F, 0.1668F);
        this.rightTuskGroup = rightMainTusk;
        addTuskSegments(rightMainTusk, true, inflate);

        // Geo bones: left_ear and right_ear. Main ear cubes preserve UV [246,248]; steering ropes/hooks are skipped.
        ModelRenderer leftEar = emptyChild(head, 13.6766F, -0.0718F, -1.1615F);
        part(leftEar, 246, 248, 4.0F, 0.0F, 0.0F, 2.1378F, 1.0787F, 2.459F, -1.0F, -9.5F, -9.5F, 2, 19, 19, inflate);

        ModelRenderer rightEar = emptyChild(head, -14.6766F, 0.9282F, -1.1615F);
        part(rightEar, 246, 248, -3.0F, -1.0F, 0.0F, 2.1378F, -1.0787F, -2.459F, -1.0F, -9.5F, -9.5F, 2, 19, 19, inflate);

        // Geo bones: trunk and trunk_01 through trunk_05. UVs [721,697], [591,716], [444,731], [306,736], [163,737].
        ModelRenderer trunk = emptyChild(head, 0.0F, 3.0F, -19.0F, -0.2182F, 0.0F, 0.0F);
        ModelRenderer trunk01 = part(trunk, 721, 697, -0.005F, 16.4833F, -2.829F, -0.1309F, 0.0F, 0.0F, -5.995F, -3.5F, -5.5F, 12, 19, 10, inflate);
        ModelRenderer trunk02 = emptyChild(trunk01, 0.0F, 8.8408F, -1.3063F);
        part(trunk02, 591, 716, 2.005F, 29.0F, 2.0F, 0.3054F, 0.0F, 0.0F, -7.0F, -23.0F, 2.0F, 10, 15, 8, inflate);
        ModelRenderer trunk03 = emptyChild(trunk02, 0.0F, 19.0F, 5.0F);
        part(trunk03, 444, 731, 2.005F, 3.0F, -3.0F, -0.7418F, 0.0F, 0.0F, -6.0F, -7.0F, -1.0F, 8, 6, 12, inflate);
        ModelRenderer trunk04 = emptyChild(trunk03, 1.0F, 6.0F, 7.0F);
        part(trunk04, 306, 736, 0.005F, 3.0F, 3.0F, 0.3491F, 0.0F, 0.0F, -4.99F, -7.0F, -4.0F, 8, 6, 11, inflate);
        ModelRenderer trunk05 = emptyChild(trunk04, 1.0F, -3.0F, 7.0F);
        part(trunk05, 163, 737, 0.005F, 1.0F, 5.0F, 1.2217F, 0.0F, 0.0F, -5.0F, -7.0F, -3.0F, 6, 5, 11, inflate);

        // Geo bone: legs. Four main leg groups preserve their original leg/foot UV starts. Ankle spikes are skipped.
        // No separate underside belly connector is present in this restored model; if one is reintroduced, isolate it first because underside bridges are high-risk bleed panels.
        ModelRenderer legs = emptyChild(this.master, 8.0F, 47.0F, -90.0F);
        this.legsGroup = legs;

        // Geo group: front_right_leg. UVs [493,826], [335,813], [952,834], [935,760].
        ModelRenderer frontRightLeg = emptyChild(legs, 7.0F, -47.2038F, 19.5595F);
        // Upper/lower leg candidate: tall scaled cuboids can bleed along their vertical side strips.
        ModelRenderer frontRightUpperLeg = part(frontRightLeg, 493, 826, 6.0F, 0.2038F, 0.4405F, -14.0F, -8.0F, -8.0F, 15, 23, 15, inflate);
        ModelRenderer frontRightLowerLeg = part(frontRightUpperLeg, 335, 813, -13.0F, 19.0F, -4.0F, 1.0F, -8.0F, -3.0F, 11, 32, 12, inflate);
        // Foot/ankle candidates: small height boxes are sensitive to one-pixel Java UV footprint drift.
        ModelRenderer frontRightUpperFoot = part(frontRightLowerLeg, 952, 834, 0.0F, 27.0F - SEAM_OVERLAP, 3.0F, 0.0F, -3.0F, -6.0F, 13, 4, 12, inflate);
        ModelRenderer frontRightLowerFoot = part(frontRightUpperFoot, 935, 760, 0.0F, 4.0F - SEAM_OVERLAP, 1.0F, -1.0F, -3.0F, -9.0F, 15, 3, 15, inflate);
        this.frontRightUpperFootGroup = frontRightUpperFoot;
        this.frontRightLowerFootGroup = frontRightLowerFoot;

        // Geo group: front_left_leg. UVs [493,826], [335,813], [952,834], [935,760].
        ModelRenderer frontLeftLeg = emptyChild(legs, -23.0F, -47.2038F, 19.5595F);
        // Foot/ankle candidates: direct children here make it easy to compare against the nested front-right foot chain.
        this.frontLeftLowerFootGroup = part(frontLeftLeg, 935, 760, 0.0F, 0.0F, 0.0F, -7.0F, 47.2038F - SEAM_OVERLAP, -8.5595F, 15, 3, 15, inflate);
        this.frontLeftUpperFootGroup = part(frontLeftLeg, 952, 834, 0.0F, 0.0F, 0.0F, -6.0F, 43.2038F - SEAM_OVERLAP, -6.5595F, 13, 4, 12, inflate);
        // Upper/lower leg candidate.
        part(frontLeftLeg, 493, 826, 0.0F, 0.0F, 0.0F, -7.0F, -7.7962F, -7.5595F, 15, 23, 15, inflate);
        part(frontLeftLeg, 335, 813, 0.0F, 0.0F, 0.0F, -5.0F, 11.2038F, -6.5595F, 11, 32, 12, inflate);

        // Geo group: back_left_leg. UVs [695,815], [169,829], [949,898], [934,678].
        ModelRenderer backLeftLeg = emptyChild(legs, -26.0F, -43.2038F, 77.5595F);
        // Foot/ankle candidates.
        this.backLeftLowerFootGroup = part(backLeftLeg, 934, 678, 0.0F, 0.0F, 0.0F, -4.0F, 43.2038F - SEAM_OVERLAP, -8.5595F, 15, 3, 16, inflate);
        this.backLeftUpperFootGroup = part(backLeftLeg, 949, 898, 0.0F, 0.0F, 0.0F, -3.0F, 39.2038F - SEAM_OVERLAP, -6.5595F, 13, 4, 13, inflate);
        // Upper/lower leg candidates.
        part(backLeftLeg, 169, 829, 0.0F, 0.0F, 0.0F, -2.0F, 14.2038F, -6.5595F, 11, 25, 13, inflate);
        part(backLeftLeg, 695, 815, 0.0F, 0.0F, 0.0F, -4.0F, -9.7962F, -8.5595F, 15, 24, 18, inflate);

        // Geo group: back_right_leg. UVs [695,815], [169,829], [949,898], [934,678].
        ModelRenderer backRightLeg = emptyChild(legs, 9.0F, -43.2038F, 77.5595F);
        // Foot/ankle candidates.
        this.backRightLowerFootGroup = part(backRightLeg, 934, 678, 0.0F, 0.0F, 0.0F, -10.0F, 43.2038F - SEAM_OVERLAP, -8.5595F, 15, 3, 16, inflate);
        this.backRightUpperFootGroup = part(backRightLeg, 949, 898, 0.0F, 0.0F, 0.0F, -9.0F, 39.2038F - SEAM_OVERLAP, -6.5595F, 13, 4, 13, inflate);
        // Upper/lower leg candidates.
        part(backRightLeg, 169, 829, 0.0F, 0.0F, 0.0F, -8.0F, 14.2038F, -6.5595F, 11, 25, 13, inflate);
        part(backRightLeg, 695, 815, 0.0F, 0.0F, 0.0F, -10.0F, -9.7962F, -8.5595F, 15, 24, 18, inflate);
    }

    private void addTuskSegments(ModelRenderer tusk, boolean mirrored, float inflate) {
        // Approximate original main tusk cube chain. UV starts come from LOTRMumakilModel.geo.json.
        part(tusk, 33, 38, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -29.6301F : -1.7789F,
                -0.3552F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F,
                mirrored ? -3.744F : -3.256F, -4.6306F, -3.4385F, 7, 28, 7, inflate);
        part(tusk, 39, 164, mirrored ? -5.8692F : 6.0354F, mirrored ? -9.3049F : 31.9053F, mirrored ? -42.0529F : -14.2016F,
                -0.4861F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F,
                -3.0F, -10.5F, -2.5F, 6, 20, 6, inflate);
        part(tusk, 51, 665, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -28.6301F : -0.7789F,
                -0.9601F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F,
                mirrored ? -1.6029F : -1.3971F, 41.0388F, 7.8274F, 3, 4, 9, inflate);
        part(tusk, 63, 578, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -29.6301F : -1.7789F,
                -1.6206F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F,
                mirrored ? -2.2847F : -1.7153F, 12.6982F, 36.9549F, 4, 12, 4, inflate);
        part(tusk, 123, 590, mirrored ? 1.7742F : -1.608F, mirrored ? -39.0075F : 2.2027F, mirrored ? -28.6301F : -0.7789F,
                -1.5333F, mirrored ? 0.3135F : -0.3135F, mirrored ? 0.1096F : -0.1096F,
                mirrored ? -1.7508F : -1.2492F, 30.3571F, 28.6119F, 3, 6, 3, inflate);
    }

    private ModelRenderer emptyChild(ModelRenderer parent, float x, float y, float z) {
        return emptyChild(parent, x, y, z, 0.0F, 0.0F, 0.0F);
    }

    private ModelRenderer emptyChild(ModelRenderer parent, float x, float y, float z, float rotX, float rotY, float rotZ) {
        ModelRenderer child = new ModelRenderer(this);
        setPoint(child, x, y, z);
        setRotationAngle(child, rotX, rotY, rotZ);
        parent.addChild(child);
        return child;
    }

    private ModelRenderer part(ModelRenderer parent, int textureU, int textureV, float pointX, float pointY, float pointZ,
                               float boxX, float boxY, float boxZ, int width, int height, int depth, float inflate) {
        return part(parent, textureU, textureV, pointX, pointY, pointZ, 0.0F, 0.0F, 0.0F, boxX, boxY, boxZ, width, height, depth, inflate);
    }

    private ModelRenderer part(ModelRenderer parent, int textureU, int textureV, float pointX, float pointY, float pointZ,
                               float rotX, float rotY, float rotZ, float boxX, float boxY, float boxZ,
                               int width, int height, int depth, float inflate) {
        ModelRenderer child = new ModelRenderer(this, textureU, textureV);
        setPoint(child, pointX, pointY, pointZ);
        setRotationAngle(child, rotX, rotY, rotZ);
        addScaledBox(child, boxX, boxY, boxZ, width, height, depth, inflate);
        parent.addChild(child);
        return child;
    }

    private static void setPoint(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.setRotationPoint(x * GEO_TO_JAVA_UV_SCALE, y * GEO_TO_JAVA_UV_SCALE, z * GEO_TO_JAVA_UV_SCALE);
    }

    /**
     * Java ModelRenderer UV diagnostic note:
     *
     * For a Java box with width W, height H, and depth D, ModelRenderer samples an atlas footprint roughly:
     *   width  = 2 * D + 2 * W
     *   height = D + H
     *
     * This model scales W/H/D by GEO_TO_JAVA_UV_SCALE and then Math.round(...)s the values below. Even a
     * one-pixel mismatch after scaling can push a generated face rectangle into a neighboring texture island,
     * which matches the observed thin wrong-color strips around cube face edges.
     */
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

    private static int getDebugUVFootprintWidth(int width, int depth) {
        return 2 * depth + 2 * width;
    }

    private static int getDebugUVFootprintHeight(int height, int depth) {
        return depth + height;
    }

    private static float stableInflate(float inflate) {
        return inflate - Z_FIGHT_EPSILON;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                       float netHeadYaw, float headPitch, float scale) {
        GL11.glPushMatrix();
        GL11.glScalef(JAVA_RENDER_SCALE, JAVA_RENDER_SCALE, JAVA_RENDER_SCALE);

        if (isDebugIsolationEnabled()) {
            this.renderDebugIsolation(scale);
        } else {
            this.master.render(scale);
        }

        GL11.glPopMatrix();
    }

    private static boolean isDebugIsolationEnabled() {
        return DEBUG_RENDER_ONLY_BODY
                || DEBUG_RENDER_ONLY_LEGS
                || DEBUG_RENDER_ONLY_HEAD
                || DEBUG_RENDER_ONLY_TUSKS
                || DEBUG_RENDER_ONLY_FEET;
    }

    private void renderDebugIsolation(float scale) {
        if (DEBUG_RENDER_ONLY_BODY) {
            this.renderBodyOnly(scale);
        } else if (DEBUG_RENDER_ONLY_LEGS) {
            this.renderLegsOnly(scale);
        } else if (DEBUG_RENDER_ONLY_HEAD) {
            this.renderHeadOnly(scale);
        } else if (DEBUG_RENDER_ONLY_TUSKS) {
            this.renderTusksOnly(scale);
        } else if (DEBUG_RENDER_ONLY_FEET) {
            this.renderFeetOnly(scale);
        }
    }

    private void renderBodyOnly(float scale) {
        boolean oldLegs = this.legsGroup.showModel;
        boolean oldHead = this.headGroup.showModel;
        this.legsGroup.showModel = false;
        this.headGroup.showModel = false;
        this.master.render(scale);
        this.legsGroup.showModel = oldLegs;
        this.headGroup.showModel = oldHead;
    }

    private void renderLegsOnly(float scale) {
        boolean oldBody = this.bodyGroup.showModel;
        this.bodyGroup.showModel = false;
        this.master.render(scale);
        this.bodyGroup.showModel = oldBody;
    }

    private void renderHeadOnly(float scale) {
        boolean oldLeftTusk = this.leftTuskGroup.showModel;
        boolean oldRightTusk = this.rightTuskGroup.showModel;
        this.leftTuskGroup.showModel = false;
        this.rightTuskGroup.showModel = false;
        this.headGroup.render(scale);
        this.leftTuskGroup.showModel = oldLeftTusk;
        this.rightTuskGroup.showModel = oldRightTusk;
    }

    private void renderTusksOnly(float scale) {
        this.leftTuskGroup.render(scale);
        this.rightTuskGroup.render(scale);
    }

    private void renderFeetOnly(float scale) {
        this.renderDebugPart(this.frontRightUpperFootGroup, scale);
        this.renderDebugPart(this.frontRightLowerFootGroup, scale);
        this.renderDebugPart(this.frontLeftUpperFootGroup, scale);
        this.renderDebugPart(this.frontLeftLowerFootGroup, scale);
        this.renderDebugPart(this.backLeftUpperFootGroup, scale);
        this.renderDebugPart(this.backLeftLowerFootGroup, scale);
        this.renderDebugPart(this.backRightUpperFootGroup, scale);
        this.renderDebugPart(this.backRightLowerFootGroup, scale);
    }

    private void renderDebugPart(ModelRenderer modelRenderer, float scale) {
        if (modelRenderer != null) {
            modelRenderer.render(scale);
        }
    }

    private static void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }
}
