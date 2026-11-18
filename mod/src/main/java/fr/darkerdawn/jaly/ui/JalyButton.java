package fr.darkerdawn.jaly.ui;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Christmas buttons. Styles: jaly (lights + candy sweep), frost (ice shimmer), ginger (wiggly icing),
 * aurora (flowing colours), lights (chasing fairy lights), present (ribbon unwraps) and vanilla.
 */
public class JalyButton extends ButtonWidget {
    public static final String[] STYLES = {"jaly", "frost", "ginger", "aurora", "lights", "present", "vanilla"};
    public static final String[] STYLE_NAMES = {"Jaly Classic", "Frost Shimmer", "Gingerbread", "Aurora Flow", "Fairy Lights", "Present Unwrap", "Vanilla"};
    private static final Map<ClickableWidget, float[]> STATE = new WeakHashMap<>();
    private boolean selected;
    private boolean mainMenu;
    private String forcedStyle;
    private final boolean small;

    public JalyButton(int x, int y, int w, int h, Text text, PressAction action) {
        super(x, y, w, h, text, action, DEFAULT_NARRATION_SUPPLIER);
        this.small = h < 18;
    }

    public JalyButton selected(boolean s) { this.selected = s; return this; }

    public JalyButton mainMenu() { this.mainMenu = true; return this; }

    public JalyButton style(String s) { this.forcedStyle = s; return this; }

    private String styleNow() {
        if (forcedStyle != null) return forcedStyle;
        JalyConfig c = JalyConfig.get();
        return mainMenu || "everywhere".equals(c.buttonScope) ? c.buttonStyle : "jaly";
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        String st = styleNow();
        if ("vanilla".equals(st)) {
            super.renderWidget(ctx, mouseX, mouseY, delta);
            return;
        }
        renderStyled(ctx, this, st, selected, small);
    }

    /** Draws any button in a JalyClient style (also used for vanilla buttons when "everywhere" is on). */
    public static void renderStyled(DrawContext ctx, ClickableWidget w, String style, boolean selected, boolean small) {
        long now = Gfx.now();
        float[] s = STATE.computeIfAbsent(w, k -> new float[]{0f, -1f, 0f});
        float dt = s[1] < 0 ? 0 : Math.min(0.1f, (now - (long) s[1]) / 1000f);
        s[1] = now;
        boolean hov = w.active && (w.isHovered() || w.isFocused());
        if (hov && s[2] == 0f && w.isHovered() && JalyConfig.get().hoverSounds) Sounds.play(Sounds.HOVER, 1.0f + (float) Math.random() * 0.2f, 0.35f);
        s[2] = hov ? 1f : 0f;
        s[0] = Gfx.approach(s[0], hov ? 1f : 0f, 12f, dt);
        float hover = s[0];
        float a = w instanceof JalyButton jb ? jb.alpha : 1f;
        int x = w.getX(), wd = w.getWidth(), h = w.getHeight();
        small = small || h < 18;
        int y = w.getY() - Math.round(hover * 1.5f);
        Gfx.round(ctx, x + 1, w.getY() + 2, wd, h, Gfx.alpha(0x70000000, a));
        int body = !w.active ? 0xD0303030 : selected ? 0xF0244A1C : switch (style) {
            case "frost" -> Gfx.lerpColor(0xE61A3A5A, 0xF0285A86, hover);
            case "ginger" -> Gfx.lerpColor(0xF07A4422, 0xF0965A2E, hover);
            case "aurora" -> 0xF0101830;
            case "lights" -> Gfx.lerpColor(0xF0101A10, 0xF0182A18, hover);
            case "present" -> Gfx.lerpColor(0xF08E1B2C, 0xF0B11A33, hover);
            default -> Gfx.lerpColor(0xE6183018, 0xF0213E1A, hover);
        };
        Gfx.round(ctx, x, y, wd, h, Gfx.alpha(body, a));
        int rim;
        switch (style) {
            case "frost" -> {
                int sweep = (int) ((now / 12) % (wd + 40)) - 20;
                if (hover > 0.05f) {
                    ctx.enableScissor(x + 1, y + 1, x + wd - 1, y + h - 1);
                    for (int i = 0; i < 6; i++) ctx.fill(x + sweep + i, y, x + sweep + i + 2, y + h, Gfx.alpha(0x40FFFFFF, hover * a));
                    ctx.disableScissor();
                }
                for (int i = 3; i < wd - 3; i += 7) ctx.fill(x + i, y + h - 1, x + i + 1, y + h + 1 + (i % 3), Gfx.alpha(0xCC9BE1FF, a));
                rim = Gfx.lerpColor(0xFF5AA0D8, 0xFFE8F6FF, hover);
            }
            case "ginger" -> {
                float wig = hover * 1.5f;
                for (int i = 1; i < wd - 1; i += 2) {
                    int off = (int) Math.round(Math.sin(now / 150.0 + i * 0.6) * wig);
                    ctx.fill(x + i, y + 1 + off + (i / 2 % 2), x + i + 2, y + 2 + off + (i / 2 % 2), Gfx.alpha(0xFFF6F4F7, a));
                }
                rim = Gfx.lerpColor(0xFF4A2A14, 0xFFFFE0AE, hover);
            }
            case "aurora" -> {
                for (int i = 1; i < wd - 1; i += 2) {
                    float f = (float) (0.5 + 0.5 * Math.sin(i * 0.08 + now / 600.0));
                    int c = Gfx.lerpColor(0xFF3DFFB0, 0xFFB35CFF, f);
                    ctx.fill(x + i, y + 1, x + i + 2, y + h - 1, Gfx.alpha(c, (0.18f + 0.3f * hover) * a));
                }
                rim = Gfx.lerpColor(0xFF3DC8FF, 0xFFB35CFF, (float) (0.5 + 0.5 * Math.sin(now / 400.0)));
            }
            case "lights" -> {
                int n = Math.max(4, (wd + h) * 2 / 10);
                for (int i = 0; i < n; i++) {
                    float p = (i / (float) n);
                    int per = 2 * (wd + h), d = (int) (p * per);
                    int bx = d < wd ? x + d : d < wd + h ? x + wd - 2 : d < 2 * wd + h ? x + wd - (d - wd - h) : x;
                    int by = d < wd ? y - 1 : d < wd + h ? y + (d - wd) : d < 2 * wd + h ? y + h - 1 : y + h - (d - 2 * wd - h);
                    boolean on = hover < 0.05f ? (i % 2 == 0) : ((i + now / 120) % 3) == 0;
                    ctx.fill(bx, by, bx + 2, by + 2, Gfx.alpha(on ? Gfx.BULBS[i % Gfx.BULBS.length] : 0xFF3A3A3A, a));
                }
                rim = 0x00000000;
            }
            case "present" -> {
                int rw = (int) (wd * (1 - hover * 0.8f));
                ctx.fill(x + (wd - 4) / 2 - rw / 2 + rw / 2, y, x + (wd + 4) / 2, y + h, Gfx.alpha(0xFFFDE68A, (1 - hover) * a));
                ctx.fill(x + 1, y + h / 2 - 1, x + 1 + (int) ((wd - 2) * (1 - hover)), y + h / 2 + 1, Gfx.alpha(0xFFFDE68A, a));
                if (hover > 0.3f) Gfx.sprite(ctx, Gfx.STAR, new int[]{Gfx.GOLD_LIGHT}, "#", x + wd - 9, y + 2, 1, hover * a);
                rim = Gfx.lerpColor(0xFFC99E02, Gfx.GOLD_LIGHT, hover);
            }
            default -> {
                if (w.active && hover > 0.02f) {
                    int sw = (int) ((wd - 2) * hover);
                    ctx.enableScissor(x + 1, y + 1, x + 1 + sw, y + h - 1);
                    Gfx.candy(ctx, x + 1, y + h - 4, wd - 2, 3, (int) (now / 60 % 8), 0.85f * a);
                    ctx.disableScissor();
                }
                rim = Gfx.lerpColor(0xFF8A6A00, Gfx.GOLD_LIGHT, hover);
            }
        }
        ctx.fill(x + 1, y + 1, x + wd - 1, y + 2, Gfx.alpha(0x30FFFFFF, a));
        if (!w.active) rim = 0xFF555555;
        else if (selected) rim = Gfx.GOLD_LIGHT;
        if ((rim >>> 24) != 0) Gfx.outline(ctx, x, y, wd, h, Gfx.alpha(rim, a));
        if (!small) {
            if (!"lights".equals(style)) Gfx.snowCap(ctx, x + 2, y, wd - 4, hover, a);
            if ("jaly".equals(style) && wd >= 60) Gfx.lights(ctx, x + 8, x + wd - 8, y + 2, Math.max(10, (wd - 16) / 6), w.active ? hover : 0f, now, a * (0.4f + 0.6f * hover));
        }
        int tc = !w.active ? 0xFF8A8A8A : Gfx.lerpColor(Gfx.SNOW, "frost".equals(style) ? 0xFFE8F6FF : Gfx.GOLD_LIGHT, Math.max(hover, selected ? 1f : 0f));
        String label = w.getMessage().getString();
        int tw = Gfx.font().getWidth(label);
        int tx = x + (wd - tw) / 2 + (int) (hover * 4);
        int ty = y + (h - 8) / 2 + (small ? 0 : 1);
        if (hover > 0.05f && !small) {
            int fx = tx - 9 + (int) ((1 - hover) * -6);
            Gfx.sprite(ctx, Gfx.SNOWFLAKE, new int[]{0xFFFFFFFF}, "#", fx, ty + 1, 1, hover * a);
        }
        Gfx.text(ctx, Gfx.trim(label, wd - 6), tx, ty, Gfx.alpha(tc, a), true);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        Sounds.play(Sounds.CLICK, 1.0f, 0.7f);
    }
}
