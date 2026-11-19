package fr.darkerdawn.jaly.visual;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.function.Consumer;

/** Small helpers for drawing coloured or textured quads in the world (camera-relative coordinates). */
final class WorldDraw {
    private WorldDraw() {}

    static final Matrix4f ID = new Matrix4f();

    /** Coloured quads. additive = glowy (aurora, stars); otherwise normal alpha blending. */
    static void colored(boolean additive, boolean depthTest, Consumer<BufferBuilder> draw) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        draw.accept(b);
        BuiltBuffer built = b.endNullable();
        if (built == null) return;
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        setup(additive, depthTest);
        BufferRenderer.drawWithGlobalProgram(built);
        restore();
    }

    /** Textured + tinted quads (glass rain, frost). */
    static void textured(Identifier tex, boolean depthTest, Consumer<BufferBuilder> draw) {
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        draw.accept(b);
        BuiltBuffer built = b.endNullable();
        if (built == null) return;
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, tex);
        setup(false, depthTest);
        BufferRenderer.drawWithGlobalProgram(built);
        restore();
    }

    private static void setup(boolean additive, boolean depthTest) {
        RenderSystem.enableBlend();
        if (additive) RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        else RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        if (depthTest) RenderSystem.enableDepthTest();
        else RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void restore() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    static void v(BufferBuilder b, float x, float y, float z, int argb) {
        b.vertex(ID, x, y, z).color(argb);
    }

    static void vt(BufferBuilder b, float x, float y, float z, float u, float vv, int argb) {
        b.vertex(ID, x, y, z).texture(u, vv).color(argb);
    }

    static int argb(float a, float r, float g, float bl) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }
}
