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
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

/** Your skin + cosmetics spinning in the menus, no world needed. */
public final class AvatarPreview {
    private AvatarPreview() {}

    //#if >=1.21.2
    private static PlayerEntityModel wide, slim;
    //#if >=1.21.9
    private static net.minecraft.client.render.entity.model.PlayerCapeModel capeModel;
    //#else
    private static net.minecraft.client.render.entity.model.PlayerCapeModel<net.minecraft.client.render.entity.state.PlayerEntityRenderState> capeModel;
    //#endif

    private static PlayerEntityModel model(boolean isSlim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (isSlim) {
            //#if >=1.21.11
            if (slim == null) slim = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false); // TODO slim arms on 1.21.11
            //#else
            if (slim == null) slim = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER_SLIM), true);
            //#endif
            return slim;
        }
        if (wide == null) wide = new PlayerEntityModel(mc.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
        return wide;
    }
    //#else
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
    //#endif

    /** Draws you standing at (x, feetY), `size` pixels per block, turned by yawDeg. */
    public static void draw(DrawContext ctx, int x, int feetY, float size, float yawDeg) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier st = fr.darkerdawn.jaly.Compat.profileSkin(mc.getGameProfile());
        boolean isSlim = fr.darkerdawn.jaly.Compat.profileSlim(mc.getGameProfile());
        //#if >=1.21.2
        PlayerEntityModel m = model(isSlim);
        //#else
        PlayerEntityModel<AbstractClientPlayerEntity> m = model(isSlim);
        //#endif
        float anim = (Util.getMeasuringTimeMs() % 1_000_000L) / 50f;
        // relaxed idle pose
        //#if <1.21.2
        m.child = false;
        m.sneaking = false;
        //#endif
        m.head.pitch = 0.05f;
        m.head.yaw = MathHelper.sin(anim * 0.03f) * 0.25f;
        m.leftArm.pitch = MathHelper.sin(anim * 0.067f) * 0.05f;
        m.rightArm.pitch = -MathHelper.sin(anim * 0.067f) * 0.05f;
        m.leftArm.roll = -0.08f - MathHelper.cos(anim * 0.09f) * 0.05f;
        m.rightArm.roll = 0.08f + MathHelper.cos(anim * 0.09f) * 0.05f;
        m.leftLeg.pitch = 0;
        m.rightLeg.pitch = 0;
        //#if <1.21.2
        m.hat.copyTransform(m.head);
        m.leftSleeve.copyTransform(m.leftArm);
        m.rightSleeve.copyTransform(m.rightArm);
        m.leftPants.copyTransform(m.leftLeg);
        m.rightPants.copyTransform(m.rightLeg);
        m.jacket.copyTransform(m.body);
        //#endif

        //#if >=1.21.6
        int half = Math.round(size * 1.3f);
        int y1 = Math.round(feetY - size * 2.3f), y2 = Math.round(feetY + size * 0.2f);
        ctx.state.addSpecialElement(new State(m, st, anim, yawDeg, x - half, y1, x + half, y2, size, feetY - (y1 + y2) / 2f, null,
                net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState.createBounds(x - half, y1, x + half, y2, null)));
        //#else
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
        //#if >=1.21.2
        ctx.draw(vc -> renderAll(ms, vc, m, st, anim));
        //#else
        VertexConsumerProvider.Immediate vc = ctx.getVertexConsumers();
        renderAll(ms, vc, m, st, anim);
        ctx.draw();
        //#endif
        ms.pop();
        DiffuseLighting.enableGuiDepthLighting();
        //#endif
    }

    //#if >=1.21.6
    /** What the GUI needs to draw the spinning avatar later this frame (1.21.6+ draws 3D GUI bits off-screen). */
    public record State(PlayerEntityModel model, Identifier skin, float anim, float yaw, int x1, int y1, int x2, int y2, float scale,
                        float feetOffset, net.minecraft.client.gui.ScreenRect scissorArea, net.minecraft.client.gui.ScreenRect bounds)
            implements net.minecraft.client.gui.render.state.special.SpecialGuiElementRenderState {}

    public static final class Renderer extends net.minecraft.client.gui.render.SpecialGuiElementRenderer<State> {
        public Renderer(VertexConsumerProvider.Immediate vc) {
            super(vc);
        }

        @Override
        public Class<State> getElementClass() {
            return State.class;
        }

        @Override
        protected float getYOffset(int height, int guiScale) {
            return height / 2f;
        }

        @Override
        protected void render(State s, MatrixStack ms) {
            MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);
            ms.translate(0f, s.feetOffset() / s.scale(), 0f);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(s.yaw()));
            ms.scale(-1f, -1f, 1f);
            ms.scale(0.9375f, 0.9375f, 0.9375f);
            ms.translate(0f, -1.501f, 0f);
            renderAll(ms, vertexConsumers, s.model(), s.skin(), s.anim());
            vertexConsumers.draw();
        }

        @Override
        protected String getName() {
            return "jalyclient avatar";
        }
    }
    //#endif

    //#if >=1.21.2
    private static void renderAll(MatrixStack ms, VertexConsumerProvider vc, PlayerEntityModel m, Identifier st, float anim) {
    //#else
    private static void renderAll(MatrixStack ms, VertexConsumerProvider vc, PlayerEntityModel<AbstractClientPlayerEntity> m, Identifier st, float anim) {
    //#endif
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        m.render(ms, vc.getBuffer(fr.darkerdawn.jaly.Compat.translucent(st)), light, OverlayTexture.DEFAULT_UV);
        // cape
        String cape = Cosmetics.selected(Cosmetics.Type.CAPE);
        if (!cape.equals("none")) {
            Identifier tex = Cosmetics.ANIMATED_CAPES.contains(cape) ? Cosmetics.capeTexture(cape + "_f" + (Util.getMeasuringTimeMs() / 110 % 8)) : Cosmetics.capeTexture(cape);
            ms.push();
            //#if <1.21.2
            ms.translate(0f, 0f, 0.125f);
            ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(6f + MathHelper.sin(anim * 0.05f) * 3f));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));
            //#endif
            //#if >=1.21.2
            //#if >=1.21.9
            if (capeModel == null) capeModel = new net.minecraft.client.render.entity.model.PlayerCapeModel(MinecraftClient.getInstance().getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER_CAPE));
            //#else
            if (capeModel == null) capeModel = new net.minecraft.client.render.entity.model.PlayerCapeModel<>(MinecraftClient.getInstance().getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER_CAPE));
            //#endif
            capeModel.render(ms, vc.getBuffer(fr.darkerdawn.jaly.Compat.solid(tex)), light, OverlayTexture.DEFAULT_UV);
            //#else
            m.renderCape(ms, vc.getBuffer(fr.darkerdawn.jaly.Compat.solid(tex)), light, OverlayTexture.DEFAULT_UV);
            //#endif
            ms.pop();
        }
        // hat
        String hat = Cosmetics.selected(Cosmetics.Type.HAT);
        var hatPart = hat.equals("none") ? null : Cosmetics.model(hat);
        if (hatPart != null) {
            ms.push();
            fr.darkerdawn.jaly.Compat.applyTransform(m.head, ms);
            if (hat.equals("halo")) ms.translate(0, -0.75f + MathHelper.sin(anim * 0.08f) * 0.03f, 0);
            hatPart.render(ms, vc.getBuffer(fr.darkerdawn.jaly.Compat.cutoutNoCull(Cosmetics.hatTexture(hat))), light, OverlayTexture.DEFAULT_UV);
            ms.pop();
        }
        PlayerExtrasFeature.renderWings(ms, PartSink.of(vc), light, m.body, anim, true);
        PlayerExtrasFeature.compact = true;
        PlayerExtrasFeature.renderPet(ms, PartSink.of(vc), light, anim, anim * 0.45f, 0.5f, 0f); // the pet walks on the spot
        PlayerExtrasFeature.compact = false;
    }
}
