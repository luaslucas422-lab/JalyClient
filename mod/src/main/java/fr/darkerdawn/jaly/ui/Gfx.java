package fr.darkerdawn.jaly.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/** Drawing helpers shared by every JalyClient screen. Dark pine glass + gold, with festive trims. */
public final class Gfx {
    private Gfx() {}

    public static final int PINE = 0xFF192C0C;
    public static final int PINE_GLASS = 0xE0122312;
    public static final int PINE_LIGHT = 0xFF2A4A1E;
    public static final int GOLD = 0xFFC99E02;
    public static final int GOLD_LIGHT = 0xFFFDE68A;
    public static final int RED = 0xFFB11A33;
    public static final int RED_LIGHT = 0xFFE0384F;
    public static final int SNOW = 0xFFF6F4F7;
    public static final int ICE = 0xFF9BC3E7;
    public static final int MUTED = 0xFFA9B8A0;
    public static final int GREEN_ON = 0xFF3FB950;
    public static final int[] BULBS = {0xFFE0384F, 0xFFFDE68A, 0xFF4CC3FF, 0xFF5CFF7A, 0xFFF6F4F7};

    public static long now() { return Util.getMeasuringTimeMs(); }

    public static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    public static int alpha(int color, float a) {
        int al = (int) (((color >>> 24) & 0xFF) * Math.max(0, Math.min(1, a)));
        return (al << 24) | (color & 0xFFFFFF);
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    /** Smooth step towards a target using real frame time; frame-rate independent. */
    public static float approach(float current, float target, float speed, float dtSeconds) {
        float k = 1f - (float) Math.exp(-speed * dtSeconds);
        return current + (target - current) * k;
    }

    public static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
    }

    /** Rectangle with 1px cut corners (reads as rounded at GUI scale). */
    public static void round(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 2 || h <= 2) { ctx.fill(x, y, x + w, y + h, color); return; }
        ctx.fill(x + 1, y, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x + 1, y, x + w - 1, y + 1, color);
        ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** The JalyClient glass panel: dark pine, light top edge, gold border, soft shadow. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, float alpha) {
        round(ctx, x + 2, y + 3, w, h, alpha(0x66000000, alpha));
        round(ctx, x, y, w, h, alpha(PINE_GLASS, alpha));
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, alpha(0x40FFFFFF, alpha));
        outline(ctx, x, y, w, h, alpha(0xAAC99E02, alpha));
    }

    /** Diagonal red/white candy stripes inside a rectangle, scrolling with offset. */
    public static void candy(DrawContext ctx, int x, int y, int w, int h, int offset, float alpha) {
        int red = alpha(RED, alpha), white = alpha(SNOW, alpha);
        for (int row = 0; row < h; row++) {
            int shift = (row + offset) % 8;
            for (int sx = -8; sx < w; sx += 8) {
                int a = x + sx + shift, b = a + 4;
                int ca = Math.max(a, x), cb = Math.min(b, x + w);
                if (cb > ca) ctx.fill(ca, y + row, cb, y + row + 1, red);
                int c2a = Math.max(b, x), c2b = Math.min(b + 4, x + w);
                if (c2b > c2a) ctx.fill(c2a, y + row, c2b, y + row + 1, white);
            }
        }
    }

    /** Lumpy snow sitting on top of an edge. */
    public static void snowCap(DrawContext ctx, int x, int y, int w, float grow, float alpha) {
        int c = alpha(SNOW, alpha);
        ctx.fill(x + 1, y - 1, x + w - 1, y + 1, c);
        for (int i = 2; i < w - 2; i += 3) {
            int hgt = (int) ((((i * 7) % 5) / 2 + 1) * (0.6f + grow * 0.6f));
            if (hgt > 0) ctx.fill(x + i, y - 1 - hgt, x + i + 2, y - 1, c);
        }
    }

    /** Twinkling string of lights along y between x1 and x2. */
    public static void lights(DrawContext ctx, int x1, int x2, int y, int spacing, float lit, long time, float alpha) {
        int wire = alpha(0xFF1E3A1A, alpha);
        int n = Math.max(1, (x2 - x1) / spacing);
        for (int i = 0; i <= n; i++) {
            int bx = x1 + i * spacing;
            if (i < n) {
                for (int s = 0; s < spacing; s += 2) {
                    float t = s / (float) spacing;
                    int sag = (int) (Math.sin(t * Math.PI) * 3);
                    ctx.fill(bx + s, y + sag, bx + s + 2, y + sag + 1, wire);
                }
            }
            boolean on = lit >= (i + 1f) / (n + 1f) || lit >= 1f;
            int col = BULBS[i % BULBS.length];
            float tw = 0.65f + 0.35f * (float) Math.sin(time / 380.0 + i * 1.7);
            if (!on) col = 0xFF3A3A3A;
            ctx.fill(bx, y, bx + 2, y + 1, alpha(0xFF2A2A2A, alpha));
            if (on) ctx.fill(bx - 1, y + 1, bx + 3, y + 5, alpha(col, 0.22f * tw * alpha));
            ctx.fill(bx, y + 1, bx + 2, y + 4, alpha(col, (on ? tw : 1f) * alpha));
        }
    }

    /** Draws a small pixel-art sprite from strings; '.' is transparent. */
    public static void sprite(DrawContext ctx, String[] rows, int[] palette, String keys, int x, int y, int px, float alpha) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int c = 0;
            while (c < row.length()) {
                char ch = row.charAt(c);
                int start = c;
                while (c < row.length() && row.charAt(c) == ch) c++;
                int idx = keys.indexOf(ch);
                if (ch != '.' && idx >= 0) ctx.fill(x + start * px, y + r * px, x + c * px, y + (r + 1) * px, alpha(palette[idx], alpha));
            }
        }
    }

    public static final String[] SNOWFLAKE = {"..#..", "#.#.#", ".###.", "#.#.#", "..#.."};
    public static final String[] STAR = {"..#..", ".###.", "#####", ".###.", ".#.#."};
    public static final String[] GEAR = {".#.#.", "#####", "##.##", "#####", ".#.#."};
    public static final String[] TREE = {"...#...", "..###..", ".#####.", "..###..", ".#####.", "#######", "...#..."};

    // ---- GUI matrix helpers (1.21.6 switched the GUI to 2D matrices)
    public static void push(DrawContext ctx) {
        //#if >=1.21.6
        ctx.getMatrices().pushMatrix();
        //#else
        ctx.getMatrices().push();
        //#endif
    }

    public static void pop(DrawContext ctx) {
        //#if >=1.21.6
        ctx.getMatrices().popMatrix();
        //#else
        pop(ctx);
        //#endif
    }

    public static void translate(DrawContext ctx, double x, double y) {
        //#if >=1.21.6
        ctx.getMatrices().translate((float) x, (float) y);
        //#else
        ctx.getMatrices().translate(x, y, 0);
        //#endif
    }

    public static void scale(DrawContext ctx, float x, float y) {
        //#if >=1.21.6
        ctx.getMatrices().scale(x, y);
        //#else
        ctx.getMatrices().scale(x, y, 1f);
        //#endif
    }

    public static void rotate(DrawContext ctx, float degrees) {
        //#if >=1.21.6
        ctx.getMatrices().rotate((float) Math.toRadians(degrees));
        //#else
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        //#endif
    }

    public static void tex(DrawContext ctx, Identifier id, int x, int y, int w, int h, float u, float v, int rw, int rh, int tw, int th, float alpha) {
        //#if >=1.21.2
        int col = ((int) (Math.max(0f, Math.min(1f, alpha)) * 255) << 24) | 0xFFFFFF;
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, w, h, rw, rh, tw, th, col);
        //#else
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ctx.setShaderColor(1f, 1f, 1f, alpha);
        ctx.drawTexture(id, x, y, w, h, u, v, rw, rh, tw, th);
        ctx.setShaderColor(1f, 1f, 1f, 1f);
        //#endif
    }

    public static void text(DrawContext ctx, String s, int x, int y, int color, boolean shadow) {
        if (((color >>> 24) & 0xFF) < 6) return;
        ctx.drawText(font(), s, x, y, color, shadow);
    }

    public static void centered(DrawContext ctx, String s, int cx, int y, int color) {
        text(ctx, s, cx - font().getWidth(s) / 2, y, color, true);
    }

    public static void scaledText(DrawContext ctx, String s, float x, float y, float scale, int color, boolean shadow) {
        push(ctx);
        translate(ctx, x, y);
        scale(ctx, scale, scale);
        text(ctx, s, 0, 0, color, shadow);
        pop(ctx);
    }

    public static String trim(String s, int width) {
        TextRenderer f = font();
        if (f.getWidth(s) <= width) return s;
        return f.trimToWidth(s, Math.max(0, width - f.getWidth("..."))) + "...";
    }

    /** Animated on/off switch. */
    public static void toggle(DrawContext ctx, int x, int y, float knob, float alpha) {
        int track = lerpColor(0xFF4A4A4A, GREEN_ON, knob);
        round(ctx, x, y, 18, 9, alpha(track, alpha));
        int kx = x + 1 + (int) (knob * 9);
        round(ctx, kx, y + 1, 8, 7, alpha(SNOW, alpha));
    }
}
