package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.ui.Gfx;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** Draws the current Spotify lyric line as giant glowing text in the sky in front of you. */
public final class SkyLyrics {
    private SkyLyrics() {}

    private static float yaw = Float.NaN;
    private static String shown = "";
    private static long changedAt;

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Mods.on("sky_lyrics") || mc.player == null || mc.options.hudHidden) return;
        String line = Music.currentLine();
        if (Music.current == null || line.isEmpty()) return;
        if (!line.equals(shown)) {
            shown = line;
            changedAt = System.currentTimeMillis();
        }
        JalyConfig c = JalyConfig.get();
        Camera cam = context.camera();
        float target = cam.getYaw();
        if (Float.isNaN(yaw)) yaw = target;
        yaw += MathHelper.wrapDegrees(target - yaw) * 0.04f;
        double dist = 60;
        double rad = Math.toRadians(yaw);
        Vec3d at = new Vec3d(-Math.sin(rad) * dist, c.skyLyricsHeight, Math.cos(rad) * dist);

        MatrixStack ms = new MatrixStack();
        ms.multiplyPositionMatrix(context.positionMatrix());
        ms.translate(at.x, at.y, at.z);
        ms.multiply(cam.getRotation());
        float k = 0.22f * c.skyLyricsSize;
        ms.scale(k, -k, k);

        float fade = Math.min(1f, (System.currentTimeMillis() - changedAt) / 350f);
        TextRenderer tr = mc.textRenderer;
        VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();
        int col = Gfx.alpha(c.skyLyricsColor, Math.max(0.05f, fade));
        float w = tr.getWidth(line);
        tr.draw(line, -w / 2f, -4, col, true, ms.peek().getPositionMatrix(), imm, TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        if (c.skyLyricsNext) {
            String next = nextLine();
            if (!next.isEmpty()) {
                ms.push();
                ms.translate(0, 13, 0);
                ms.scale(0.6f, 0.6f, 0.6f);
                tr.draw(next, -tr.getWidth(next) / 2f, 0, 0x88FFFFFF, false, ms.peek().getPositionMatrix(), imm, TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
                ms.pop();
            }
        }
        String song = "~ " + Music.current.title() + " ~";
        ms.translate(0, -16, 0);
        ms.scale(0.5f, 0.5f, 0.5f);
        tr.draw(song, -tr.getWidth(song) / 2f, 0, 0xAA9BC3E7, false, ms.peek().getPositionMatrix(), imm, TextRenderer.TextLayerType.SEE_THROUGH, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        imm.draw();
    }

    private static String nextLine() {
        List<Music.Line> l = Music.lyrics;
        long pos = Music.positionMs() + 200;
        for (Music.Line line : l) if (line.timeMs() > pos && !line.text().isEmpty()) return line.text();
        return "";
    }
}
