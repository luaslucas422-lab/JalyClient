package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Card grid of every built-in mod, with search, category chips, animated switches and an Advanced page. */
public class ModsScreen extends JalyScreen {
    private static final int CARD_W = 128, CARD_H = 44, GAP = 6;
    private TextFieldWidget search;
    private Mods.Category filter;
    private double scroll;
    private final Map<String, Float> knob = new HashMap<>();
    private final Map<String, Float> hoverAnim = new HashMap<>();
    private long last = -1;
    private int gridTop;

    public ModsScreen(Screen parent) {
        super("Mods", parent);
        Mods.init();
    }

    @Override
    protected void build() {
        String old = search != null ? search.getText() : "";
        search = new TextFieldWidget(textRenderer, px + pw - 126, py + 7, 118, 14, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search mods..."));
        search.setText(old);
        search.setChangedListener(s -> scroll = 0);
        addDrawableChild(search);
        int x = cx();
        int y = cy();
        x = chip(x, y, "All", null);
        for (Mods.Category c : Mods.Category.values()) x = chip(x, y, c.label, c);
        gridTop = y + 20;
    }

    private int chip(int x, int y, String label, Mods.Category c) {
        int w = textRenderer.getWidth(label) + 14;
        addDrawableChild(new JalyButton(x, y, w, 14, Text.literal(label), b -> {
            filter = c;
            scroll = 0;
            clearAndInit();
        }).selected(filter == c));
        return x + w + 4;
    }

    private List<Mods.Entry> visible() {
        String q = search == null ? "" : search.getText().toLowerCase(Locale.ROOT).trim();
        List<Mods.Entry> out = new ArrayList<>();
        for (Mods.Entry e : Mods.ALL) {
            if (filter != null && e.category != filter) continue;
            if (!q.isEmpty() && !(e.name.toLowerCase(Locale.ROOT).contains(q) || e.desc.toLowerCase(Locale.ROOT).contains(q))) continue;
            out.add(e);
        }
        return out;
    }

    private int cols() { return Math.max(1, (cw() + GAP) / (CARD_W + GAP)); }

    private int cardW() { return (cw() - (cols() - 1) * GAP) / cols(); }

    private int gridBottom() { return py + ph - 8; }

    private int maxScroll(int count) {
        int rows = (count + cols() - 1) / cols();
        return Math.max(0, rows * (CARD_H + GAP) - (gridBottom() - gridTop));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        List<Mods.Entry> list = visible();
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));
        int cw = cardW();
        ctx.enableScissor(cx() - 2, gridTop - 2, cx() + cw() + 2, gridBottom());
        for (int i = 0; i < list.size(); i++) {
            Mods.Entry e = list.get(i);
            int x = cx() + (i % cols()) * (cw + GAP);
            int y = gridTop + (i / cols()) * (CARD_H + GAP) - (int) scroll;
            if (y + CARD_H < gridTop - 4 || y > gridBottom()) continue;
            boolean hov = mouseX >= x && mouseX < x + cw && mouseY >= y && mouseY < y + CARD_H && mouseY >= gridTop && mouseY < gridBottom();
            float h = Gfx.approach(hoverAnim.getOrDefault(e.id, 0f), hov ? 1f : 0f, 14f, dt);
            hoverAnim.put(e.id, h);
            float k = Gfx.approach(knob.getOrDefault(e.id, e.on() ? 1f : 0f), e.on() ? 1f : 0f, 16f, dt);
            knob.put(e.id, k);
            int lift = Math.round(h * 2);
            Gfx.round(ctx, x + 1, y + 2, cw, CARD_H, 0x55000000);
            Gfx.round(ctx, x, y - lift, cw, CARD_H, Gfx.lerpColor(0xF0162A12, 0xF0203A18, h));
            Gfx.outline(ctx, x, y - lift, cw, CARD_H, Gfx.lerpColor(e.on() ? 0xAA3FB950 : 0x66C99E02, Gfx.GOLD_LIGHT, h));
            if (e.on()) ctx.fill(x + 1, y - lift + 1, x + 3, y - lift + CARD_H - 1, 0xFF3FB950);
            Gfx.text(ctx, Gfx.trim(e.name, cw - 12), x + 7, y - lift + 5, e.on() ? Gfx.SNOW : 0xFFB8C2B0, true);
            Gfx.text(ctx, Gfx.trim(e.desc, cw - 12), x + 7, y - lift + 16, 0xFF8FA088, false);
            String cat = e.category.label;
            Gfx.text(ctx, cat, x + 7, y - lift + CARD_H - 12, catColor(e.category), false);
            Gfx.toggle(ctx, x + cw - 24, y - lift + CARD_H - 14, k, 1f);
            if (!e.settings.isEmpty()) {
                boolean gh = hov && mouseX >= x + cw - 40 && mouseX < x + cw - 28 && mouseY >= y + CARD_H - 15;
                Gfx.sprite(ctx, Gfx.GEAR, new int[]{gh ? Gfx.GOLD_LIGHT : 0xFFA9B8A0}, "#", x + cw - 38, y - lift + CARD_H - 13, 1, 1f);
            }
        }
        ctx.disableScissor();
        if (list.isEmpty()) Gfx.centered(ctx, "No mods match your search", width / 2, gridTop + 30, 0xFF8FA088);
        int ms = maxScroll(list.size());
        if (ms > 0) {
            int trackH = gridBottom() - gridTop;
            int barH = Math.max(14, trackH * trackH / (trackH + ms));
            int barY = gridTop + (int) ((trackH - barH) * (scroll / ms));
            ctx.fill(px + pw - 5, gridTop, px + pw - 3, gridBottom(), 0x40FFFFFF);
            ctx.fill(px + pw - 5, barY, px + pw - 3, barY + barH, Gfx.GOLD);
        }
        String hint = "Click: toggle | Gear: Advanced";
        if (cx() + 250 + textRenderer.getWidth(hint) < px + pw - 10) Gfx.text(ctx, hint, px + pw - 10 - textRenderer.getWidth(hint), cy() + 3, 0xFF8FA088, false);
    }

    private static int catColor(Mods.Category c) {
        return switch (c) {
            case HUD -> 0xFF9BC3E7;
            case FESTIVE -> 0xFFE0384F;
            case PVP -> 0xFFFDE68A;
            case MUSIC -> 0xFF1DB954;
            case VISUAL -> 0xFFC3A6FF;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseY < gridTop || mouseY >= gridBottom()) return false;
        List<Mods.Entry> list = visible();
        int cw = cardW();
        for (int i = 0; i < list.size(); i++) {
            int x = cx() + (i % cols()) * (cw + GAP);
            int y = gridTop + (i / cols()) * (CARD_H + GAP) - (int) scroll;
            if (mouseX >= x && mouseX < x + cw && mouseY >= y && mouseY < y + CARD_H) {
                Mods.Entry e = list.get(i);
                boolean gear = !e.settings.isEmpty() && mouseX >= x + cw - 40 && mouseX < x + cw - 28 && mouseY >= y + CARD_H - 15;
                if (gear || button == 1) {
                    Sounds.play(Sounds.CLICK, 1.1f, 0.7f);
                    client.setScreen(new ModSettingsScreen(this, e));
                } else {
                    e.set(!e.on());
                    Sounds.play(e.on() ? Sounds.CHIME : Sounds.CLICK, e.on() ? 1.4f : 0.8f, 0.5f);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 24;
        return true;
    }

    /** Smoke test helper. */
    public void setFilter(Mods.Category c) {
        filter = c;
        clearAndInit();
    }
}
