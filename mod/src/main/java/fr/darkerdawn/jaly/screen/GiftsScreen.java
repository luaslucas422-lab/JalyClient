package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Festive;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** 25 presents: one new present a day, each with a card (joke, tip or fact). Swipe-card style, all free. */
public class GiftsScreen extends JalyScreen {
    private static final int[] BOX = {0xFFB11A33, 0xFF2E7D32, 0xFF2C6FB0, 0xFFC99E02, 0xFF7B3FA0, 0xFFE07A1F};
    private static final int[] RIBBON = {0xFFFDE68A, 0xFFE0384F, 0xFFF6F4F7, 0xFFB11A33, 0xFFFDE68A, 0xFF2E7D32};
    private final List<float[]> confetti = new ArrayList<>();
    private final Random rnd = new Random();
    private int opening = -1;
    private long openingAt;
    private int card = -1;
    private long cardAt;
    private boolean reveal;
    private long last = -1;

    public GiftsScreen(Screen parent) {
        super("Gifts", parent);
    }

    @Override
    protected void build() {}

    private int tile() { return Math.max(18, Math.min(40, Math.min((cw() - 4 * 6) / 5, (ch() - 26 - 4 * 5) / 5))); }

    private int gx() { return cx() + (cw() - (5 * tile() + 4 * 6)) / 2; }

    private int gy() { return cy() + 2; }

    private int[] tilePos(int door) {
        int i = door - 1;
        return new int[]{gx() + (i % 5) * (tile() + 6), gy() + (i / 5) * (tile() + 5)};
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        JalyConfig c = JalyConfig.get();
        LocalDate today = LocalDate.now();
        int avail = Festive.doorsAvailable(today, c.firstDay);
        int t = tile();
        for (int d = 1; d <= 25; d++) {
            int[] p = tilePos(d);
            boolean opened = c.opened.contains(d);
            boolean ready = !opened && d <= avail;
            boolean hov = card < 0 && mouseX >= p[0] && mouseX < p[0] + t && mouseY >= p[1] && mouseY < p[1] + t;
            int bounce = ready ? (int) Math.round(Math.abs(Math.sin(now / 300.0 + d)) * 2) : 0;
            int x = p[0], y = p[1] - bounce - (hov ? 1 : 0);
            int col = BOX[d % BOX.length], rib = RIBBON[d % RIBBON.length];
            if (ready) Gfx.round(ctx, x - 2, y - 2, t + 4, t + 4, Gfx.alpha(Gfx.GOLD_LIGHT, 0.25f + 0.2f * (float) Math.sin(now / 200.0)));
            if (opened) {
                Gfx.round(ctx, x, y + t / 4, t, t - t / 4, Gfx.lerpColor(col, 0xFF000000, 0.35f));
                ctx.fill(x + 3, y + t / 4 + 3, x + t - 3, y + t - 3, 0xFF1A1208);
                ctx.fill(x + t / 2 - 5, y + t / 4 + 1, x + t / 2 + 6, y + t / 4 + 10, 0xFFF6F4F7);
                ctx.fill(x + t / 2 - 3, y + t / 4 + 4, x + t / 2 + 4, y + t / 4 + 5, 0xFF8FA088);
                ctx.fill(x + t / 2 - 3, y + t / 4 + 7, x + t / 2 + 2, y + t / 4 + 8, 0xFF8FA088);
            } else {
                float lidUp = d == opening ? Math.min(1f, (now - openingAt) / 400f) : 0f;
                Gfx.round(ctx, x, y + 6, t, t - 6, col);
                ctx.fill(x + t / 2 - 2, y + 6, x + t / 2 + 2, y + t, rib);
                int ly = y - (int) (lidUp * 18);
                Gfx.round(ctx, x - 2, ly + 2, t + 4, 6, Gfx.lerpColor(col, 0xFFFFFFFF, 0.12f));
                ctx.fill(x + t / 2 - 2, ly + 2, x + t / 2 + 2, ly + 8, rib);
                ctx.fill(x + t / 2 - 6, ly - 2, x + t / 2 - 1, ly + 2, rib);
                ctx.fill(x + t / 2 + 1, ly - 2, x + t / 2 + 6, ly + 2, rib);
                if (!ready) {
                    Gfx.round(ctx, x - 2, y - 2, t + 4, t + 4, 0x88DDE8F5);
                    Gfx.snowCap(ctx, x - 1, y, t + 2, 0.5f, 1f);
                }
            }
            String num = String.valueOf(d);
            Gfx.centered(ctx, num, x + t / 2, y + t / 2 - 1, opened ? 0xFF8FA088 : Gfx.SNOW);
            if (hov && !opened && !ready) Gfx.centered(ctx, "Day " + d, x + t / 2, y + t + 1, 0xFFDCE6F2);
        }
        if (opening > 0 && now - openingAt > 420) {
            card = opening;
            cardAt = now;
            reveal = false;
            opening = -1;
        }
        int done = c.opened.size();
        long secs = Festive.secondsToNextDoor(LocalDateTime.now(), c.firstDay);
        String foot = "Opened " + done + "/25" + (secs >= 0 ? "   Next present in " + Festive.formatDuration(secs) : "   Merry Christmas!");
        Gfx.centered(ctx, foot, width / 2, py + ph - 14, Gfx.GOLD_LIGHT);
        // confetti
        for (int i = confetti.size() - 1; i >= 0; i--) {
            float[] f = confetti.get(i);
            f[0] += f[2] * dt;
            f[1] += f[3] * dt;
            f[3] += 260 * dt;
            f[5] -= dt;
            if (f[5] <= 0) { confetti.remove(i); continue; }
            ctx.fill((int) f[0], (int) f[1], (int) f[0] + 2, (int) f[1] + 2, Gfx.alpha(Gfx.BULBS[(int) f[4]], Math.min(1, f[5] * 2)));
        }
    }

    @Override
    protected void drawOverlay(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (card < 1) return;
        long now = Gfx.now();
        float k = Gfx.easeOutBack(Math.min(1f, (now - cardAt) / 380f));
        String[] c = Festive.CARDS[card - 1];
        String reward = Festive.REWARDS[card - 1];
        int w = Math.min(250, width - 30), h = 150;
        int x = (width - w) / 2, y = (height - h) / 2;
        ctx.fill(0, 0, width, height, Gfx.alpha(0x99000000, Math.min(1, k)));
        Gfx.push(ctx);
        Gfx.translate(ctx, 0, 0);
        Gfx.translate(ctx, width / 2f, y + h / 2f + (1 - k) * 80);
        Gfx.scale(ctx, Math.max(0.01f, k), Math.max(0.01f, k));
        Gfx.translate(ctx, -width / 2f, -(y + h / 2f));
        Gfx.round(ctx, x + 3, y + 4, w, h, 0x77000000);
        Gfx.round(ctx, x, y, w, h, 0xFFF6F4F7);
        Gfx.candy(ctx, x + 1, y + 1, w - 2, 6, (int) (now / 70 % 8), 1f);
        Gfx.outline(ctx, x, y, w, h, Gfx.GOLD);
        String badge = c[0] + "  -  Day " + card;
        Gfx.text(ctx, badge, x + 10, y + 14, 0xFFB11A33, false);
        List<net.minecraft.text.OrderedText> q = textRenderer.wrapLines(net.minecraft.text.Text.literal(c[1]), w - 20);
        int ty = y + 30;
        for (var line : q) { ctx.drawText(textRenderer, line, x + 10, ty, 0xFF1A2414, false); ty += 10; }
        ty += 6;
        if (reveal || !c[0].equals("Joke")) {
            for (var line : textRenderer.wrapLines(net.minecraft.text.Text.literal(c[2]), w - 20)) { ctx.drawText(textRenderer, line, x + 10, ty, 0xFF2E7D32, false); ty += 10; }
        } else {
            Gfx.text(ctx, "Click to reveal the answer!", x + 10, ty, 0xFF8A6A00, false);
        }
        // the gift inside
        String[] rp = reward.split(":");
        fr.darkerdawn.jaly.cosmetic.Cosmetics.Type rt = fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.HAT;
        for (var t2 : fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.values()) if (t2.key.equals(rp[0])) rt = t2;
        int ry = y + h - 40;
        Gfx.round(ctx, x + 8, ry, w - 16, 26, 0xFF183018);
        Gfx.outline(ctx, x + 8, ry, w - 16, 26, Gfx.GOLD);
        if (rt == fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.CAPE) Gfx.tex(ctx, fr.darkerdawn.jaly.cosmetic.Cosmetics.capeTexture(rp[1]), x + 13, ry + 2, 14, 22, 4, 4, 40, 64, 256, 128, 1f);
        else Gfx.tex(ctx, fr.darkerdawn.jaly.cosmetic.Cosmetics.icon(rt, rp[1]), x + 10, ry + 1, 24, 24, 0, 0, 32, 32, 32, 32, 1f);
        Gfx.text(ctx, "You unwrapped:", x + 38, ry + 4, 0xFF8FA088, false);
        Gfx.text(ctx, Gfx.trim(fr.darkerdawn.jaly.cosmetic.Cosmetics.rewardName(reward), w - 50), x + 38, ry + 14, Gfx.GOLD_LIGHT, true);
        Gfx.text(ctx, "Click outside to close", x + w - 10 - textRenderer.getWidth("Click outside to close"), y + h - 12, 0xFF9AA3A0, false);
        Gfx.pop(ctx);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (card > 0) {
            int w = Math.min(250, width - 30), h = 150, x = (width - w) / 2, y = (height - h) / 2;
            boolean inside = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            if (inside && !reveal && Festive.CARDS[card - 1][0].equals("Joke")) {
                reveal = true;
                Sounds.play(Sounds.JINGLE, 1.2f, 0.6f);
            } else if (!inside) {
                card = -1;
            }
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        int t = tile();
        for (int d = 1; d <= 25; d++) {
            int[] p = tilePos(d);
            if (mouseX >= p[0] && mouseX < p[0] + t && mouseY >= p[1] - 3 && mouseY < p[1] + t) {
                return openDoor(d);
            }
        }
        return false;
    }

    /** Opens a present (or re-shows its card). Public for the smoke test. */
    public boolean openDoor(int d) {
        JalyConfig c = JalyConfig.get();
        if (c.opened.contains(d)) {
            card = d;
            cardAt = Gfx.now();
            reveal = true;
            return true;
        }
        if (!Festive.canOpen(d, LocalDate.now(), c.firstDay, c.opened)) {
            Sounds.play(Sounds.CLICK, 0.6f, 0.5f);
            return false;
        }
        c.opened.add(d);
        JalyConfig.save();
        opening = d;
        openingAt = Gfx.now();
        Sounds.play(Sounds.UNWRAP, 1f, 0.9f);
        int[] p = tilePos(d);
        for (int i = 0; i < 60; i++) {
            float ang = (float) (rnd.nextFloat() * Math.PI * 2);
            float sp = 60 + rnd.nextFloat() * 140;
            confetti.add(new float[]{p[0] + tile() / 2f, p[1] + tile() / 2f, (float) Math.cos(ang) * sp, (float) Math.sin(ang) * sp - 120, rnd.nextInt(Gfx.BULBS.length), 1.2f + rnd.nextFloat()});
        }
        return true;
    }
}
