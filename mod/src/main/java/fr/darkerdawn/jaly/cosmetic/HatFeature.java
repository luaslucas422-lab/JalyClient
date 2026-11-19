package fr.darkerdawn.jaly.cosmetic;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.RotationAxis;

//#if >=1.21.2
/** Draws the chosen festive hat on any biped (players, zombies, skeletons...). */
public class HatFeature extends FeatureRenderer<net.minecraft.client.render.entity.state.BipedEntityRenderState, BipedEntityModel<net.minecraft.client.render.entity.state.BipedEntityRenderState>> {
    public HatFeature(FeatureRendererContext<net.minecraft.client.render.entity.state.BipedEntityRenderState, BipedEntityModel<net.minecraft.client.render.entity.state.BipedEntityRenderState>> context) {
        super(context);
    }

    @Override
    //#if >=1.21.9
    public void render(MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, int light, net.minecraft.client.render.entity.state.BipedEntityRenderState state, float limbAngle, float limbDistance) {
        PartSink sink = PartSink.of(queue);
    //#else
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, net.minecraft.client.render.entity.state.BipedEntityRenderState state, float limbAngle, float limbDistance) {
        PartSink sink = PartSink.of(vertexConsumers);
    //#endif
        if (state.invisible || state.baby) return;
        String hat = Cosmetics.hatForState(state);
        if (hat == null) return;
        ModelPart part = Cosmetics.model(hat);
        if (part == null) return;
        matrices.push();
        fr.darkerdawn.jaly.Compat.applyTransform(this.getContextModel().head, matrices);
        int l = light;
        if (hat.equals("halo")) {
            float t = (Util.getMeasuringTimeMs() % 4000L) / 4000f;
            matrices.translate(0, -0.75f + (float) Math.sin(t * Math.PI * 2) * 0.03f, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 360f));
            l = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        sink.part(part, matrices, RenderLayer.getEntityCutoutNoCull(Cosmetics.hatTexture(hat)), l);
        matrices.pop();
    }
}
//#else
/** Draws the chosen festive hat on any biped (players, zombies, skeletons...). */
public class HatFeature extends FeatureRenderer<LivingEntity, BipedEntityModel<LivingEntity>> {
    public HatFeature(FeatureRendererContext<LivingEntity, BipedEntityModel<LivingEntity>> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, LivingEntity entity,
                       float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (entity.isInvisible() || entity.isBaby()) return;
        String hat = Cosmetics.hatFor(entity);
        if (hat == null) return;
        ModelPart part = Cosmetics.model(hat);
        if (part == null) return;
        matrices.push();
        fr.darkerdawn.jaly.Compat.applyTransform(this.getContextModel().head, matrices);
        if (!entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty() && !hat.equals("antlers")) {
            matrices.translate(0, -0.07f, 0);
            matrices.scale(1.12f, 1.12f, 1.12f);
        }
        int l = light;
        if (hat.equals("halo")) {
            float t = (Util.getMeasuringTimeMs() % 4000L) / 4000f;
            matrices.translate(0, -0.75f + (float) Math.sin(t * Math.PI * 2) * 0.03f, 0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 360f));
            l = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        }
        part.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.hatTexture(hat))), l, OverlayTexture.DEFAULT_UV);
        matrices.pop();
    }
}
//#endif
