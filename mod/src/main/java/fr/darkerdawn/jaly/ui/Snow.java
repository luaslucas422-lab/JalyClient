package fr.darkerdawn.jaly.ui;

import fr.darkerdawn.jaly.JalyConfig;
import net.minecraft.client.gui.DrawContext;

import java.util.Random;

/** A fixed pool of falling flakes in screen space. No allocation per frame. */
public final class Snow {
    private final float[] x, y, speed, drift, phase;
    private final byte[] size;
    private long last = -1;

    public Snow(int count, long seed) {
        Random r = new Random(seed);
        x = new float[count];
        y = new float[count];
        speed = new float[count];
        drift = new float[count];
        phase = new float[count];
        size = new byte[count];
        for (int i = 0; i < count; i++) {
            x[i] = r.nextFloat();
            y[i] = r.nextFloat();
            float depth = r.nextFloat();
            speed[i] = 0.035f + depth * 0.09f;
            drift[i] = 0.004f + r.nextFloat() * 0.01f;
            phase[i] = r.nextFloat() * 6.28f;
            size[i] = (byte) (depth > 0.85f ? 2 : 1);
        }
    }

    public void render(DrawContext ctx, int left, int top, int w, int h, float amount, float alpha) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        int n = (int) (x.length * Math.max(0, Math.min(1, amount)));
        float t = now / 1000f;
        for (int i = 0; i < x.length; i++) {
            y[i] += speed[i] * dt;
            x[i] += (float) Math.sin(t * 0.9f + phase[i]) * drift[i] * dt;
            if (y[i] > 1.02f) { y[i] = -0.02f; x[i] = (x[i] * 7.31f + 0.37f) % 1f; }
            if (x[i] < 0) x[i] += 1;
            if (x[i] > 1) x[i] -= 1;
            if (i >= n) continue;
            int px = left + (int) (x[i] * w), py = top + (int) (y[i] * h);
            int s = size[i];
            int col = Gfx.alpha(s > 1 ? 0xFFFFFFFF : 0xCCE8F0FF, alpha);
            ctx.fill(px, py, px + s, py + s, col);
            if (s > 1) {
                ctx.fill(px - 1, py, px, py + 2, Gfx.alpha(0x55FFFFFF, alpha));
                ctx.fill(px + 2, py, px + 3, py + 2, Gfx.alpha(0x55FFFFFF, alpha));
            }
        }
    }

    public void renderFull(DrawContext ctx, int w, int h, float alpha) {
        render(ctx, 0, 0, w, h, JalyConfig.get().menuSnow ? JalyConfig.get().snowAmount() : 0f, alpha);
    }
}
