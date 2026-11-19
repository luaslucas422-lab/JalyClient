package fr.darkerdawn.jaly.cosmetic;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/** Your skin + cosmetics spinning in the menus, no world needed. */
public final class AvatarPreview {
    private AvatarPreview() {}

    private static PlayerEntityModel<AbstractClientPlayerEntity> wide, slim;

    private static PlayerEntityModel<AbstractClientPlayerEntity> model(boolean isSlim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (isSlim) {
            if (slim == null) slim = new PlayerEntityModel<>(mc.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
            return slim;
        }
        if (wide == null) wide = new PlayerEntityModel<>(mc.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER), false);
        return wide;
    }

    /** Draws you standing at (x, feetY), `size` pixels per block, turned by yawDeg. */
    public static void draw(DrawContext ctx, int x, int feetY, float size, float yawDeg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        SkinTextures st = mc.getSkinProvider().getSkinTextures(mc.getGameProfile());
        boolean isSlim = st.model() == SkinTextures.Model.SLIM;
        PlayerEntityModel<AbstractClientPlayerEntity> m = model(isSlim);
        float anim = (Util.getMeasuringTimeMs() % 1_000_000L) / 50f;
        // relaxed idle pose
        m.child = false;
        m.sneaking = false;
        m.head.pitch = 0.05f;
        m.head.yaw = MathHelper.sin(anim * 0.03f) * 0.25f;
        m.leftArm.pitch = MathHelper.sin(anim * 0.067f) * 0.05f;
        m.rightArm.pitch = -MathHelper.sin(anim * 0.067f) * 0.05f;
        m.leftArm.roll = -0.08f - MathHelper.cos(anim * 0.09f) * 0.05f;
        m.rightArm.roll = 0.08f + MathHelper.cos(anim * 0.09f) * 0.05f;
        m.leftLeg.pitch = 0;
        m.rightLeg.pitch = 0;
        m.hat.copyTransform(m.head);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftPants.copyTransform(m.leftLeg);
        m.rightPants.copyTransform(m.rightLeg);
        m.jacket.copyTransform(m.body);

        MatrixStack ms = ctx.getMatrices();
        ms.push();
        ms.translate(x, feetY, 120);
        ms.scale(size, size, -size);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
        // same setup the game uses for players: y down, origin at the neck
        ms.scale(-1f, -1f, 1f);
        ms.scale(0.9375f, 0.9375f, 0.9375f);
        ms.translate(0f, -1.501f, 0f);
        RenderSystem.setShaderLights(new Vector3f(0.2f, -1f, -1f).normalize(), new Vector3f(-0.2f, -1f, 0f).normalize());
        VertexConsumerProvider.Immediate vc = ctx.getVertexConsumers();
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        m.render(ms, vc.getBuffer(RenderLayer.getEntityTranslucent(st.texture())), light, OverlayTexture.DEFAULT_UV);
        // cape
        String cape = Cosmetics.selected(Cosmetics.Type.CAPE);
        if (!cape.equals("none")) {
            Identifier tex = Cosmetics.ANIMATED_CAPES.contains(cape) ? Cosmetics.capeTexture(cape + "_f" + (Util.getMeasuringTimeMs() / 110 % 8)) : Cosmetics.capeTexture(cape);
            ms.push();
            ms.translate(0f, 0f, 0.125f);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6f + MathHelper.sin(anim * 0.05f) * 3f));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            m.renderCape(ms, vc.getBuffer(RenderLayer.getEntitySolid(tex)), light, OverlayTexture.DEFAULT_UV);
            ms.pop();
        }
        // hat
        String hat = Cosmetics.selected(Cosmetics.Type.HAT);
        var hatPart = hat.equals("none") ? null : Cosmetics.model(hat);
        if (hatPart != null) {
            ms.push();
            m.head.rotate(ms);
            if (hat.equals("halo")) ms.translate(0, -0.75f + MathHelper.sin(anim * 0.08f) * 0.03f, 0);
            hatPart.render(ms, vc.getBuffer(RenderLayer.getEntityCutoutNoCull(Cosmetics.hatTexture(hat))), light, OverlayTexture.DEFAULT_UV);
            ms.pop();
        }
        PlayerExtrasFeature.renderWings(ms, vc, light, m.body, anim, true);
        PlayerExtrasFeature.renderPet(ms, vc, light, anim, anim * 0.45f, 0.5f, 0f); // the pet walks on the spot
        ctx.draw();
        ms.pop();
        DiffuseLighting.enableGuiDepthLighting();
    }
}
