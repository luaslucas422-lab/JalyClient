package fr.darkerdawn.jaly.visual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/**
 * Small helpers for drawing coloured or textured quads in the world (camera-relative coordinates).
 * Uses the game's own render layers, so it works the same on every Minecraft version.
 */
final class WorldDraw {
    private WorldDraw() {}

    static final Matrix4f ID = new Matrix4f();

    private static VertexConsumerProvider.Immediate imm() {
        return MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
    }

    /** Coloured quads. additive = glowy (aurora, stars); otherwise normal see-through colour. */
    static void colored(boolean additive, boolean depthTest, Consumer<VertexConsumer> draw) {
        RenderLayer layer = additive ? fr.darkerdawn.jaly.Compat.lightning() : fr.darkerdawn.jaly.Compat.debugQuads();
        VertexConsumerProvider.Immediate imm = imm();
        draw.accept(imm.getBuffer(layer));
        imm.draw(layer);
    }

    /** Textured + tinted quads (glass rain, frost). */
    static void textured(Identifier tex, boolean depthTest, Consumer<VertexConsumer> draw) {
        RenderLayer layer = fr.darkerdawn.jaly.Compat.translucent(tex);
        VertexConsumerProvider.Immediate imm = imm();
        draw.accept(imm.getBuffer(layer));
        imm.draw(layer);
    }

    static void v(VertexConsumer b, float x, float y, float z, int argb) {
        b.vertex(ID, x, y, z).color(argb);
    }

    static void vt(VertexConsumer b, float x, float y, float z, float u, float vv, int argb) {
        b.vertex(ID, x, y, z).color(argb).texture(u, vv).overlay(OverlayTexture.DEFAULT_UV).light(LightmapTextureManager.MAX_LIGHT_COORDINATE).normal(0f, 1f, 0f);
    }

    static int argb(float a, float r, float g, float bl) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }
}
