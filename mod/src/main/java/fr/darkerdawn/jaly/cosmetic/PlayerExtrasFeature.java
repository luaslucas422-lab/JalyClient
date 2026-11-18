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
public class PlayerExtrasFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    private static ModelPart wings;

    public PlayerExtrasFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    private static ModelPart wings() {
        if (wings == null) {
            ModelData data = new ModelData();
            ModelPartData root = data.getRoot();
            root.addChild("left", ModelPartBuilder.create().uv(0, 0).cuboid(-12f, -2f, 0f, 12f, 16f, 1f), ModelTransform.pivot(-1f, 1f, 2.5f));
            root.addChild("right", ModelPartBuilder.create().uv(0, 0).mirrored().cuboid(0f, -2f, 0f, 12f, 16f, 1f), ModelTransform.pivot(1f, 1f, 2.5f));
            wings = TexturedModelData.of(data, 64, 32).createModel();
        }
        return wings;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, AbstractClientPlayerEntity player,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player != MinecraftClient.getInstance().player || player.isInvisible()) return;
        String w = Cosmetics.selected(Cosmetics.Type.WINGS);
        if (!w.equals("none")) {
            ModelPart m = wings();
            float flap = MathHelper.sin(animationProgress * 0.11f) * 0.22f + (player.isOnGround() ? 0f : 0.25f);
            ModelPart l = m.getChild("left"), r = m.getChild("right");
            l.yaw = 0.45f + flap;
            r.yaw = -0.45f - flap;
            l.roll = -0.2f;
            r.roll = 0.2f;
            matrices.push();
            this.getContextModel().body.rotate(matrices);
            m.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.wingTexture(w))), light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
        String p = Cosmetics.selected(Cosmetics.Type.PET);
        if (!p.equals("none")) {
            ModelPart part = Cosmetics.model("pet_" + p);
            if (part == null) return;
            float bob = MathHelper.sin(animationProgress * 0.08f) * 0.06f;
            matrices.push();
            matrices.translate(-0.8f, -0.15f + bob, 0.25f);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(animationProgress * 0.03f) * 20f));
            matrices.scale(0.6f, 0.6f, 0.6f);
            part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.hatTexture("pet_" + p))), light, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
    }
}
