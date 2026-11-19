package fr.darkerdawn.jaly.ui;

import net.minecraft.client.gui.DrawContext;

/** Four extra animated main menu backgrounds, drawn in code so they scale to any screen. */
public final class MenuScenes {
    private MenuScenes() {}

    public static final String[] IDS = {"lake", "blizzard", "fireworks", "gingertown"};

    public static boolean has(String id) {
        for (String s : IDS) if (s.equals(id)) return true;
        return false;
    }

    private static int hash(int i) {
        int h = i * 0x45d9f3b;
        h = ((h >>> 16) ^ h) * 0x45d9f3b;
        return (h >>> 16) ^ h;
    }

    private static float rnd(int i) { return (hash(i) & 0xFFFF) / 65535f; }

    /** Draws scene `id` filling w x h. t = seconds (scene time), now = ms clock. */
    public static void draw(DrawContext ctx, String id, int w, int h, float t, long now) {
        switch (id) {
            case "lake" -> lake(ctx, w, h, t, now);
            case "blizzard" -> blizzard(ctx, w, h, t, now);
            case "fireworks" -> fireworks(ctx, w, h, t, now);
            default -> gingertown(ctx, w, h, t, now);
        }
    }

    // ---------------------------------------------------------------- Frozen Lake: aurora mirrored on the ice
    private static void lake(DrawContext ctx, int w, int h, float t, long now) {
        int horizon = (int) (h * 0.62f);
        ctx.fillGradient(0, 0, w, horizon, 0xFF040A1E, 0xFF0F2348);
        stars(ctx, w, horizon, now, 80);
        // aurora ribbons
        for (int band = 0; band < 3; band++) {
            for (int x = 0; x < w; x += 2) {
                float f = x / (float) w;
                float wave = (float) (Math.sin(f * 7 + t * 0.4 + band * 1.6) * 0.5 + Math.sin(f * 17 - t * 0.7) * 0.2);
                int top = (int) (h * (0.12f + band * 0.06f) + wave * h * 0.06f);
                int len = (int) (h * (0.22f + 0.08f * Math.sin(f * 5 + t * 0.3 + band)));
                float a = (float) (0.22 + 0.18 * Math.sin(f * 23 + t * 1.3 + band)) * (1f - Math.abs(f - 0.5f) * 1.2f);
                if (a <= 0) continue;
                int col = band == 1 ? 0x9B5CFF : 0x3DFFB0;
                ctx.fillGradient(x, top, x + 2, top + len, Gfx.alpha(0xFF000000 | col, a), 0x00000000 | col);
                // reflection on the ice (dimmer, flipped)
                int rt = horizon + (horizon - top - len);
                if (rt < h) ctx.fillGradient(x, Math.max(horizon, rt), x + 2, Math.min(h, rt + len), 0x00000000 | col, Gfx.alpha(0xFF000000 | col, a * 0.35f));
            }
        }
        // far pine shore
        for (int x = 0; x < w; x += 6) {
            int th = (int) (8 + rnd(x / 6) * 26);
            for (int k = 0; k < th; k++) {
                int half = Math.max(0, (th - k) / 3);
                ctx.fill(x + 3 - half, horizon - k, x + 4 + half, horizon - k + 1, 0xFF071410);
            }
        }
        // ice
        ctx.fillGradient(0, horizon, w, h, 0x99BFD8F0, 0xCC6E8FB8);
        for (int i = 0; i < 12; i++) {
            int y = horizon + 6 + (int) (rnd(i + 99) * (h - horizon - 8));
            int x = (int) ((rnd(i) * w + t * 6 * (i % 3 + 1)) % (w + 120)) - 60;
            ctx.fill(x, y, x + 40 + (int) (rnd(i + 7) * 60), y + 1, 0x44FFFFFF);
        }
        // a lonely cabin with a warm window
        int cx = (int) (w * 0.78f), cy = horizon;
        ctx.fill(cx - 14, cy - 12, cx + 14, cy, 0xFF2A1A10);
        for (int k = 0; k < 9; k++) ctx.fill(cx - 17 + k * 2, cy - 12 - k, cx + 17 - k * 2, cy - 11 - k, 0xFFEEF3F8);
        float flick = 0.75f + 0.25f * (float) Math.sin(now / 180.0);
        ctx.fill(cx - 4, cy - 8, cx + 2, cy - 3, Gfx.alpha(0xFFFFD27A, flick));
        ctx.fillGradient(cx - 10, cy, cx + 6, cy + 18, Gfx.alpha(0xFFFFD27A, 0.25f * flick), 0x00FFD27A);
    }

    // ---------------------------------------------------------------- Blizzard Pines: parallax forest in a snowstorm
    private static void blizzard(DrawContext ctx, int w, int h, float t, long now) {
        ctx.fillGradient(0, 0, w, h, 0xFF8FA6BE, 0xFFDDE7F0);
        int[] colors = {0xFFAEC0D0, 0xFF7E95AA, 0xFF4E6A80, 0xFF2A4256};
        for (int layer = 0; layer < 4; layer++) {
            float speed = 4 + layer * 9;
            int base = (int) (h * (0.55f + layer * 0.12f));
            int spacing = 14 + layer * 8;
            float off = (t * speed) % spacing;
            for (int i = -1; i < w / spacing + 2; i++) {
                int idx = i + (int) (t * speed / spacing);
                int x = (int) (i * spacing - off);
                int th = (int) ((22 + layer * 16) * (0.7f + rnd(idx * 7 + layer) * 0.6f));
                for (int k = 0; k < th; k += 2) {
                    int half = (th - k) * (3 + layer) / 10;
                    ctx.fill(x - half, base - k - 2, x + half + 1, base - k, colors[layer]);
                    if (k % 6 == 0) ctx.fill(x - half, base - k - 2, x + half + 1, base - k - 1, 0x66FFFFFF);
                }
            }
            ctx.fill(0, base, w, h, colors[layer]);
            ctx.fillGradient(0, base - 6, w, base + 4, 0x00FFFFFF, 0x88FFFFFF);
        }
        // the storm: slanted streaks blowing sideways
        for (int i = 0; i < 260; i++) {
            float sp = 0.6f + rnd(i) * 1.6f;
            int x = (int) ((rnd(i * 3) * (w + 200) + t * 180 * sp) % (w + 200)) - 100;
            int y = (int) ((rnd(i * 5) * h + t * 70 * sp) % h);
            int len = (int) (3 + sp * 5);
            int a = (int) (90 + rnd(i * 11) * 140);
            for (int k = 0; k < len; k++) ctx.fill(x - k * 2, y - k, x - k * 2 + 2, y - k + 1, (Math.max(0, a - k * 20) << 24) | 0xFFFFFF);
        }
        float gust = (float) Math.max(0, Math.sin(t * 0.5) * Math.sin(t * 0.23));
        ctx.fill(0, 0, w, h, Gfx.alpha(0xFFFFFFFF, 0.25f * gust));
    }

    // ---------------------------------------------------------------- Fireworks Night over a snowy town
    private static void fireworks(DrawContext ctx, int w, int h, float t, long now) {
        ctx.fillGradient(0, 0, w, h, 0xFF02030E, 0xFF151A3A);
        stars(ctx, w, (int) (h * 0.7f), now, 60);
        int[] cols = {0xFFFF4F6A, 0xFFFFD94A, 0xFF4CC3FF, 0xFF5CFF7A, 0xFFB35CFF, 0xFFFFFFFF};
        for (int f = 0; f < 7; f++) {
            float period = 3.2f + rnd(f) * 2.4f;
            float local = (t + rnd(f + 50) * period) % period;
            int cycle = (int) ((t + rnd(f + 50) * period) / period);
            int bx = (int) (w * (0.12f + rnd(f * 13 + cycle) * 0.76f));
            int by = (int) (h * (0.15f + rnd(f * 17 + cycle) * 0.3f));
            int col = cols[(f + cycle) % cols.length];
            if (local < 0.9f) { // rocket going up
                float k = local / 0.9f;
                int y = (int) (h - (h - by) * k);
                ctx.fill(bx, y, bx + 2, y + 4, 0xFFFFE0A0);
                ctx.fill(bx, y + 4, bx + 1, y + 10, 0x66FFAA55);
            } else if (local < 2.6f) { // burst
                float k = (local - 0.9f) / 1.7f;
                float a = 1f - k;
                int n = 26;
                for (int i = 0; i < n; i++) {
                    double ang = i * Math.PI * 2 / n + rnd(f + i) * 0.2;
                    float r = (float) (k * h * 0.16f * (0.8f + rnd(i + f * 31) * 0.4f));
                    int px = (int) (bx + Math.cos(ang) * r), py = (int) (by + Math.sin(ang) * r + k * k * 18);
                    ctx.fill(px, py, px + 2, py + 2, Gfx.alpha(col, a));
                    if (k < 0.6f) ctx.fill((int) (bx + Math.cos(ang) * r * 0.8), (int) (by + Math.sin(ang) * r * 0.8 + k * k * 14), (int) (bx + Math.cos(ang) * r * 0.8) + 1, (int) (by + Math.sin(ang) * r * 0.8 + k * k * 14) + 1, Gfx.alpha(col, a * 0.5f));
                }
                if (k < 0.15f) ctx.fill(0, 0, w, h, Gfx.alpha(col, 0.05f * (1 - k / 0.15f)));
            }
        }
        // town skyline with lit windows and snowy roofs
        int ground = (int) (h * 0.82f);
        for (int i = 0; i < w / 26 + 1; i++) {
            int bw = 22, bh = (int) (20 + rnd(i * 3) * 46);
            int x = i * 26;
            ctx.fill(x, ground - bh, x + bw, ground, 0xFF0B0E20);
            ctx.fill(x - 1, ground - bh - 2, x + bw + 1, ground - bh, 0xFFE6EEF6);
            for (int wy = ground - bh + 6; wy < ground - 6; wy += 8) {
                for (int wx = x + 4; wx < x + bw - 4; wx += 7) {
                    if (rnd(wx * 31 + wy) < 0.55f) ctx.fill(wx, wy, wx + 3, wy + 4, 0xFFFFD27A);
                }
            }
        }
        ctx.fill(0, ground, w, h, 0xFFE6EEF6);
    }

    // ---------------------------------------------------------------- Gingerbread Town: candy houses scrolling by
    private static void gingertown(DrawContext ctx, int w, int h, float t, long now) {
        ctx.fillGradient(0, 0, w, h, 0xFF2B1840, 0xFFB0587A);
        stars(ctx, w, h / 2, now, 40);
        // big moon
        int mx = (int) (w * 0.2f), my = (int) (h * 0.2f), mr = Math.max(10, h / 14);
        for (int y = -mr; y <= mr; y++) {
            int half = (int) Math.sqrt(mr * mr - y * y);
            ctx.fill(mx - half, my + y, mx + half, my + y + 1, 0xFFFFF4D6);
        }
        int ground = (int) (h * 0.8f);
        float scroll = t * 18;
        int spacing = 70;
        float off = scroll % spacing;
        for (int i = -1; i < w / spacing + 2; i++) {
            int idx = i + (int) (scroll / spacing);
            int x = (int) (i * spacing - off);
            int hw = 42 + (int) (rnd(idx) * 14), hh = 30 + (int) (rnd(idx * 5) * 22);
            int top = ground - hh;
            // gingerbread walls
            ctx.fill(x, top, x + hw, ground, 0xFFB5733A);
            // roof of white icing with candy drips
            int roofH = hw / 2;
            for (int k = 0; k < roofH; k++) ctx.fill(x - 4 + k, top - k, x + hw + 4 - k, top - k + 1, 0xFF8C4E22);
            for (int k = 0; k < roofH; k += 1) ctx.fill(x - 4 + k, top - k - 1, x - 2 + k, top - k + 1, 0xFFF6F4F7);
            for (int k = 0; k < roofH; k += 1) ctx.fill(x + hw + 2 - k, top - k - 1, x + hw + 4 - k, top - k + 1, 0xFFF6F4F7);
            for (int d = x; d < x + hw; d += 5) ctx.fill(d, top, d + 2, top + 2 + (d / 5 % 3), 0xFFF6F4F7);
            // gumdrops on the roof edge
            int[] gum = {0xFFE0384F, 0xFF3FB950, 0xFFFFD94A, 0xFF4CC3FF};
            for (int g = 0; g < 4; g++) ctx.fill(x + 6 + g * (hw - 12) / 3, top - 2, x + 9 + g * (hw - 12) / 3, top + 1, gum[(g + idx) & 3]);
            // windows and a door
            float flick = 0.7f + 0.3f * (float) Math.sin(now / 200.0 + idx);
            ctx.fill(x + 6, top + 8, x + 14, top + 16, Gfx.alpha(0xFFFFD27A, flick));
            ctx.fill(x + hw - 14, top + 8, x + hw - 6, top + 16, Gfx.alpha(0xFFFFD27A, flick));
            ctx.fill(x + hw / 2 - 5, ground - 14, x + hw / 2 + 5, ground, 0xFF6B3A1A);
            ctx.fill(x + hw / 2 - 6, ground - 15, x + hw / 2 + 6, ground - 14, 0xFFF6F4F7);
            // candy cane lamp post
            int lx = x + hw + 12;
            for (int k = 0; k < 26; k++) ctx.fill(lx, ground - k, lx + 2, ground - k + 1, (k / 3) % 2 == 0 ? 0xFFE0384F : 0xFFF6F4F7);
            ctx.fill(lx - 2, ground - 29, lx + 4, ground - 25, Gfx.alpha(0xFFFFE9A0, flick));
        }
        ctx.fill(0, ground, w, h, 0xFFF6F4F7);
        for (int x = 0; x < w; x += 4) ctx.fill(x, ground - 1 - (x / 4 % 2), x + 4, ground, 0xFFF6F4F7);
        // sugar snow
        for (int i = 0; i < 90; i++) {
            int x = (int) ((rnd(i) * w + Math.sin(t + i) * 10) % w);
            int y = (int) ((rnd(i * 9) * h + t * (12 + rnd(i * 3) * 20)) % h);
            ctx.fill(x, y, x + 1 + (i % 3 == 0 ? 1 : 0), y + 1 + (i % 3 == 0 ? 1 : 0), 0xDDFFFFFF);
        }
    }

    private static void stars(DrawContext ctx, int w, int maxY, long now, int n) {
        for (int i = 0; i < n; i++) {
            float tw = (float) (0.5 + 0.5 * Math.sin(now / 400.0 + i * 1.7));
            int x = (int) (rnd(i * 41) * w), y = (int) (rnd(i * 43) * maxY);
            ctx.fill(x, y, x + 1, y + 1, Gfx.alpha(0xFFFFFFFF, 0.3f + 0.7f * tw));
        }
    }
}
