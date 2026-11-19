package fr.darkerdawn.jaly.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Your wings (flapping) and your pet (floating by your shoulder). Only you see them. */
//#if >=1.21.2
public class PlayerExtrasFeature extends FeatureRenderer<net.minecraft.client.render.entity.state.PlayerEntityRenderState, PlayerEntityModel> {
    private static ModelPart wings;

    public PlayerExtrasFeature(FeatureRendererContext<net.minecraft.client.render.entity.state.PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, net.minecraft.client.render.entity.state.PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || state.id != mc.player.getId() || state.invisible) return;
        renderWings(matrices, vertexConsumers, light, this.getContextModel().body, state.age, mc.player.isOnGround());
        renderPet(matrices, vertexConsumers, light, state.age, state.limbFrequency, state.limbAmplitudeMultiplier, fr.darkerdawn.jaly.Features.petAir);
    }
//#else
public class PlayerExtrasFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static ModelPart wings;

    public PlayerExtrasFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }
//#endif

    private static ModelPart wings() {
        if (wings == null) {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();
            root.addChild("left", ModelPartBuilder.create().uv(0, 0).cuboid(-12f, -2f, 0f, 12f, 16f, 1f), fr.darkerdawn.jaly.Compat.at(-1f, 1f, 2.5f));
            root.addChild("right", ModelPartBuilder.create().uv(0, 0).mirrored().cuboid(0f, -2f, 0f, 12f, 16f, 1f), fr.darkerdawn.jaly.Compat.at(1f, 1f, 2.5f));
            wings = TexturedModelData.of(data, 64, 32).createModel();
        }
        return wings;
    }

//#if <1.21.2
    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player != MinecraftClient.getInstance().player || player.isInvisible()) return;
        renderWings(matrices, vertexConsumers, light, this.getContextModel().body, animationProgress, player.isOnGround());
        renderPet(matrices, vertexConsumers, light, animationProgress, limbAngle, limbDistance, fr.darkerdawn.jaly.Features.petAir);
    }
//#endif

    /** Wings on the back, flapping (faster in the air). Also used by the menu preview. */
    public static void renderWings(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ModelPart body, float anim, boolean onGround) {
        String w = Cosmetics.selected(Cosmetics.Type.WINGS);
        if (w.equals("none")) return;
        ModelPart m = wings();
        float flap = MathHelper.sin(anim * 0.11f) * 0.22f + (onGround ? 0f : 0.25f);
        ModelPart l = m.getChild("left"), r = m.getChild("right");
        l.yaw = 0.45f + flap;
        r.yaw = -0.45f - flap;
        l.roll = -0.2f;
        r.roll = 0.2f;
        matrices.push();
        fr.darkerdawn.jaly.Compat.applyTransform(body, matrices);
        m.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.wingTexture(w))), light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    /** Pets that fly (they flap their wings or are pulled by magic reindeer). The rest walk on the floor next to you. */
    /** Menu preview: keep the pet a bit closer so it fits in the frame. */
    public static boolean compact;

    public static final java.util.Set<String> FLYERS = java.util.Set.of("sleigh", "frost_dragon", "owl");
    /** Pets with no legs hop along instead. */
    public static final java.util.Set<String> HOPPERS = java.util.Set.of("present", "snowman", "og", "tree");

    /** Your pet, walking beside your feet (or flying by your shoulder) with its own animation. Also used by the menu preview. */
    public static void renderPet(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float anim, float limbAngle, float limbDistance, float airborne) {
        String p = Cosmetics.selected(Cosmetics.Type.PET);
        if (p.equals("none")) return;
        ModelPart part = Cosmetics.model("pet_" + p);
        if (part == null) return;
        String key = "pet_" + p;
        boolean flyer = FLYERS.contains(p), hopper = HOPPERS.contains(p);
        float amt = MathHelper.clamp(limbDistance * 1.6f, 0f, 1f);
        float walk = limbAngle * 0.9f;
        matrices.push();
        if (flyer) {
            // hovering by your shoulder, bobbing on each wing beat
            float beat = anim * (p.equals("frost_dragon") ? 0.32f : 0.45f);
            float bob = MathHelper.sin(beat) * 0.05f;
            matrices.translate(compact ? -0.7f : -0.95f, 0.05f + bob, 0.2f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-8f * amt - 4f));
            matrices.scale(0.55f, 0.55f, 0.55f);
            animate(part, key, anim * 0.55f, 1f, beat, p);
        } else {
            // on the floor next to your feet (it stays down when you jump)
            matrices.translate(compact ? -0.62f : -0.85f, 1.5f + airborne, 0.1f);
            float lift = 0f, roll = 0f, squash = 1f;
            if (hopper) {
                float h = Math.abs(MathHelper.sin(walk * 0.5f));
                lift = -h * 0.22f * amt;
                squash = 1f - (1f - h) * 0.12f * amt;
            } else {
                lift = -Math.abs(MathHelper.sin(walk)) * 0.03f * amt;
                if (p.equals("penguin")) roll = MathHelper.sin(walk) * 12f * amt; // waddle
            }
            float idle = MathHelper.sin(anim * 0.08f) * 0.012f * (1f - amt); // breathing
            matrices.translate(0, lift - idle, 0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
            if (amt < 0.05f) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(anim * 0.02f) * 25f)); // looks around
            matrices.scale(0.72f, 0.72f * squash, 0.72f);
            animate(part, key, walk, amt, anim * 0.35f, p);
        }
        part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.hatTexture(key))), light, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }

    private static void animate(ModelPart part, String key, float walk, float amt, float beat, String pet) {
        String[] rig = HatModels.RIG.get(key);
        if (rig == null) return;
        float flap = pet.equals("penguin") ? 0.15f + MathHelper.sin(walk) * 0.35f * amt : MathHelper.sin(beat) * (pet.equals("frost_dragon") ? 0.55f : 0.75f);
        for (String n : rig) {
            if (!part.hasChild(n)) continue;
            ModelPart c = part.getChild(n);
            switch (n.substring(n.indexOf('_') + 1)) {
                case "L" -> c.pitch = MathHelper.cos(walk) * 0.9f * amt;
                case "R" -> c.pitch = MathHelper.cos(walk + MathHelper.PI) * 0.9f * amt;
                case "AL" -> c.pitch = MathHelper.cos(walk + MathHelper.PI) * 0.7f * amt;
                case "AR" -> c.pitch = MathHelper.cos(walk) * 0.7f * amt;
                case "WL" -> c.roll = flap;
                case "WR" -> c.roll = -flap;
                case "T" -> c.yaw = MathHelper.sin(beat * 0.7f) * 0.45f;
                default -> { }
            }
        }
    }
}
