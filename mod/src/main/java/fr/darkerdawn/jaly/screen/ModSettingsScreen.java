package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.hud.Hud;
import fr.darkerdawn.jaly.hud.HudModule;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

/** A list of settings rows (switches, pickers, sliders, colour swatches, buttons) with an optional live HUD preview. */
public class ModSettingsScreen extends JalyScreen {
    private static final int ROW = 22, CTRL_W = 120;
    private final String desc;
    private final List<Mods.Setting> rows;
    private final HudModule hud;
    private final float[] knobs;
    private int dragging = -1;
    private double scroll;

    public ModSettingsScreen(Screen parent, Mods.Entry entry) {
        this(parent, entry.name, entry.desc, withEnabled(entry), entry.hud);
    }

    public ModSettingsScreen(Screen parent, String title, String desc, List<Mods.Setting> rows, HudModule hud) {
        super(title, parent);
        this.desc = desc;
        this.rows = rows;
        this.hud = hud;
        this.knobs = new float[rows.size()];
        for (int i = 0; i < rows.size(); i++) if (rows.get(i) instanceof Mods.Toggle t) knobs[i] = t.get().get() ? 1f : 0f;
    }

    private static List<Mods.Setting> withEnabled(Mods.Entry e) {
        List<Mods.Setting> l = new ArrayList<>();
        l.add(new Mods.Toggle("Enabled", e::on, e::set));
        l.addAll(e.settings);
        return l;
    }

    @Override
    protected void build() {}

    private int listW() { return hud != null ? cw() - 150 : cw(); }

    private int listTop() { return cy() + 16; }

    private int listBottom() { return py + ph - 6; }

    private int rowY(int i) { return listTop() + i * ROW - (int) scroll; }

    private int ctrlX() { return cx() + listW() - CTRL_W - 4; }

    private int maxScroll() { return Math.max(0, rows.size() * ROW - (listBottom() - listTop())); }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Gfx.text(ctx, Gfx.trim(desc, cw()), cx() + 4, cy() + 2, 0xFF8FA088, false);
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        ctx.enableScissor(cx(), listTop(), cx() + listW(), listBottom());
        for (int i = 0; i < rows.size(); i++) {
            Mods.Setting s = rows.get(i);
            int y = rowY(i);
            if (y + ROW < listTop() || y > listBottom()) continue;
            boolean hov = mouseX >= cx() && mouseX < cx() + listW() && mouseY >= y && mouseY < y + ROW - 2 && mouseY >= listTop() && mouseY < listBottom();
            Gfx.round(ctx, cx(), y, listW(), ROW - 2, hov ? 0xC0203A18 : 0xA0162A12);
            Gfx.text(ctx, s.name(), cx() + 8, y + 6, Gfx.SNOW, true);
            int x = ctrlX();
            switch (s) {
                case Mods.Toggle t -> {
                    knobs[i] = knobs[i] + ((t.get().get() ? 1f : 0f) - knobs[i]) * 0.35f;
                    Gfx.toggle(ctx, x + CTRL_W - 20, y + 5, knobs[i], 1f);
                }
                case Mods.Cycle c -> {
                    String label = c.get().get();
                    for (int k = 0; k < c.values().length; k++) if (c.values()[k].equals(label)) label = c.labels()[k];
                    Gfx.round(ctx, x, y + 3, CTRL_W, 14, 0xFF0E1C0A);
                    Gfx.outline(ctx, x, y + 3, CTRL_W, 14, hov ? Gfx.GOLD_LIGHT : 0x88C99E02);
                    Gfx.text(ctx, "<", x + 4, y + 6, Gfx.GOLD_LIGHT, false);
                    Gfx.text(ctx, ">", x + CTRL_W - 9, y + 6, Gfx.GOLD_LIGHT, false);
                    Gfx.centered(ctx, Gfx.trim(label, CTRL_W - 24), x + CTRL_W / 2, y + 6, Gfx.SNOW);
                }
                case Mods.Slider sl -> {
                    double v = sl.get().getAsDouble();
                    float f = (float) ((v - sl.min()) / (sl.max() - sl.min()));
                    int tw = CTRL_W - 34;
                    Gfx.round(ctx, x, y + 8, tw, 4, 0xFF0E1C0A);
                    int fill = (int) (tw * Math.max(0, Math.min(1, f)));
                    if (fill > 0) Gfx.candy(ctx, x, y + 8, fill, 4, 0, 1f);
                    Gfx.round(ctx, x + fill - 3, y + 5, 6, 10, Gfx.GOLD_LIGHT);
                    String val = sl.unit().equals("%") ? Math.round(v * 100) + "%" : String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "") + sl.unit();
                    Gfx.text(ctx, val, x + tw + 5, y + 6, Gfx.GOLD_LIGHT, false);
                }
                case Mods.Swatch sw -> {
                    int cur = sw.get().getAsInt();
                    for (int k = 0; k < sw.colors().length; k++) {
                        int sx = x + k * 15;
                        ctx.fill(sx, y + 4, sx + 12, y + 16, 0xFF000000);
                        ctx.fill(sx + 1, y + 5, sx + 11, y + 15, sw.colors()[k]);
                        if (sw.colors()[k] == cur) Gfx.outline(ctx, sx - 1, y + 3, 14, 14, Gfx.GOLD_LIGHT);
                    }
                }
                case Mods.Action a -> {
                    int bw = textRenderer.getWidth(a.button()) + 16;
                    int bx = x + CTRL_W - bw;
                    boolean bh = hov && mouseX >= bx;
                    Gfx.round(ctx, bx, y + 3, bw, 14, bh ? 0xFFB11A33 : 0xFF213E1A);
                    Gfx.outline(ctx, bx, y + 3, bw, 14, bh ? Gfx.GOLD_LIGHT : 0x88C99E02);
                    Gfx.centered(ctx, a.button(), bx + bw / 2, y + 6, Gfx.SNOW);
                }
            }
        }
        ctx.disableScissor();
        if (maxScroll() > 0) {
            int trackH = listBottom() - listTop();
            int barH = Math.max(14, trackH * trackH / (trackH + maxScroll()));
            int barY = listTop() + (int) ((trackH - barH) * (scroll / maxScroll()));
            ctx.fill(cx() + listW() + 2, barY, cx() + listW() + 4, barY + barH, Gfx.GOLD);
        }
        if (hud != null) {
            int bx = cx() + cw() - 142, by = listTop();
            Gfx.round(ctx, bx, by, 142, 110, 0xA0060E06);
            Gfx.outline(ctx, bx, by, 142, 110, 0x66C99E02);
            Gfx.text(ctx, "Preview", bx + 5, by + 4, 0xFF8FA088, false);
            ctx.enableScissor(bx + 1, by + 14, bx + 141, by + 109);
            int[] b = Hud.size(client, hud, true);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx + 71 - b[0] / 2, by + 62 - b[1] / 2, 0);
            Hud.drawAt(ctx, client, hud, true);
            ctx.getMatrices().pop();
            ctx.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseY < listTop() || mouseY >= listBottom()) return false;
        for (int i = 0; i < rows.size(); i++) {
            int y = rowY(i);
            if (mouseY < y || mouseY >= y + ROW - 2 || mouseX < cx() || mouseX >= cx() + listW()) continue;
            Mods.Setting s = rows.get(i);
            int x = ctrlX();
            switch (s) {
                case Mods.Toggle t -> { t.set().accept(!t.get().get()); Sounds.play(Sounds.CLICK, 1.1f, 0.6f); }
                case Mods.Cycle c -> {
                    int idx = 0;
                    for (int k = 0; k < c.values().length; k++) if (c.values()[k].equals(c.get().get())) idx = k;
                    int dir = mouseX < x + CTRL_W / 2.0 ? -1 : 1;
                    c.set().accept(c.values()[Math.floorMod(idx + dir, c.values().length)]);
                    Sounds.play(Sounds.CLICK, 1.2f, 0.6f);
                }
                case Mods.Slider sl -> { dragging = i; slide(sl, mouseX); }
                case Mods.Swatch sw -> {
                    int k = (int) Math.floor((mouseX - x) / 15);
                    if (k >= 0 && k < sw.colors().length) { sw.set().accept(sw.colors()[k]); Sounds.play(Sounds.CLICK, 1.3f, 0.5f); }
                }
                case Mods.Action a -> { a.run().run(); Sounds.play(Sounds.CHIME, 1.2f, 0.5f); }
            }
            return true;
        }
        return false;
    }

    private void slide(Mods.Slider sl, double mouseX) {
        int tw = CTRL_W - 34;
        double f = Math.max(0, Math.min(1, (mouseX - ctrlX()) / tw));
        double v = sl.min() + f * (sl.max() - sl.min());
        v = Math.round(v / sl.step()) * sl.step();
        sl.set().accept(Math.max(sl.min(), Math.min(sl.max(), v)));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging >= 0 && rows.get(dragging) instanceof Mods.Slider sl) {
            slide(sl, mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 22;
        return true;
    }
}
