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
//#if >=1.21.11
public class JalyButton extends ButtonWidget.Text {
//#else
public class JalyButton extends ButtonWidget {
//#endif
    public static final String[] STYLES = {"jaly", "frost", "ginger", "aurora", "lights", "present", "candy", "ice", "ornament", "neon", "cocoa", "starry", "vanilla"};
    public static final String[] STYLE_NAMES = {"Jaly Classic", "Frost Shimmer", "Gingerbread", "Aurora Flow", "Fairy Lights", "Present Unwrap",
        "Candy Stripe", "Ice Crystal", "Ornament", "Neon Glow", "Hot Cocoa", "Starry Night", "Vanilla"};
    private static final Map<ClickableWidget, float[]> STATE = new WeakHashMap<>();
    private boolean selected;
    private boolean mainMenu;
    private String forcedStyle;
    private final boolean small;

    public JalyButton(int x, int y, int w, int h, net.minecraft.text.Text text, PressAction action) {
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
    //#if >=1.21.11
    protected void drawIcon(DrawContext ctx, int mouseX, int mouseY, float delta) {
        String st = styleNow();
        if ("vanilla".equals(st)) {
            super.drawIcon(ctx, mouseX, mouseY, delta);
            return;
        }
    //#else
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        String st = styleNow();
        if ("vanilla".equals(st)) {
            super.renderWidget(ctx, mouseX, mouseY, delta);
            return;
        }
    //#endif
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
            case "candy" -> 0xF0F6F4F7;
            case "ice" -> Gfx.lerpColor(0xD09BD3F0, 0xE0BFE8FF, hover);
            case "ornament" -> Gfx.lerpColor(0xF0A0122A, 0xF0C8102E, hover);
            case "neon" -> 0xF00A0A14;
            case "cocoa" -> Gfx.lerpColor(0xF04A2A18, 0xF05C3620, hover);
            case "starry" -> Gfx.lerpColor(0xF00A1030, 0xF0101848, hover);
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
            case "candy" -> {
                // red stripes that slide along on hover
                int shift = (int) (now / 40 % 12 * hover);
                ctx.enableScissor(x + 1, y + 1, x + wd - 1, y + h - 1);
                for (int i = -h; i < wd + h; i += 12) {
                    for (int k = 0; k < h; k++) ctx.fill(x + i + k + shift, y + k, x + i + k + shift + 5, y + k + 1, Gfx.alpha(0xFFD7263D, a));
                }
                ctx.disableScissor();
                ctx.fill(x + 2, y + (h - 10) / 2, x + wd - 2, y + (h + 10) / 2, Gfx.alpha(0x88000000, a * (0.6f + 0.2f * hover)));
                rim = Gfx.lerpColor(0xFFB11A33, Gfx.GOLD_LIGHT, hover);
            }
            case "ice" -> {
                // cracks + glints that sparkle when you hover
                int seed = wd * 31 + h;
                for (int i = 0; i < 4; i++) {
                    int cx0 = x + 6 + Math.floorMod(seed * (i + 3), Math.max(1, wd - 12)), cy0 = y + 2 + Math.floorMod(seed * (i + 7), Math.max(1, h - 4));
                    for (int k = 0; k < 6; k++) ctx.fill(cx0 + k, cy0 + (k * (i % 2 == 0 ? 1 : -1)) / 2, cx0 + k + 1, cy0 + (k * (i % 2 == 0 ? 1 : -1)) / 2 + 1, Gfx.alpha(0x88FFFFFF, a));
                }
                for (int i = 0; i < 5; i++) {
                    float tw = (float) Math.max(0, Math.sin(now / 180.0 + i * 1.9));
                    int gx = x + 4 + (i * 37 + wd) % Math.max(1, wd - 8), gy = y + 2 + (i * 11) % Math.max(1, h - 4);
                    int c = Gfx.alpha(0xFFFFFFFF, tw * (0.3f + 0.7f * hover) * a);
                    ctx.fill(gx - 1, gy, gx + 2, gy + 1, c);
                    ctx.fill(gx, gy - 1, gx + 1, gy + 2, c);
                }
                rim = Gfx.lerpColor(0xFF7FC4F0, 0xFFFFFFFF, hover);
            }
            case "ornament" -> {
                // glossy bauble: shine band + gold cap that swings on hover
                ctx.fill(x + 3, y + 2, x + wd - 3, y + 4, Gfx.alpha(0x55FFFFFF, a));
                ctx.fill(x + 6, y + 4, x + wd / 3, y + 5, Gfx.alpha(0x33FFFFFF, a));
                float swing = (float) Math.sin(now / 160.0) * 2 * hover;
                int capX = x + 4 + Math.round(swing);
                ctx.fill(capX, y + h / 2 - 3, capX + 4, y + h / 2 + 3, Gfx.alpha(0xFFFFD94A, a));
                ctx.fill(capX + 1, y + h / 2 - 5, capX + 3, y + h / 2 - 3, Gfx.alpha(0xFFB8860B, a));
                for (int i = 10; i < wd - 4; i += 9) ctx.fill(x + i, y + h - 3, x + i + 4, y + h - 2, Gfx.alpha(0x99FFD94A, a));
                rim = Gfx.lerpColor(0xFFB8860B, 0xFFFFE9A0, hover);
            }
            case "neon" -> {
                // pulsing neon tube in two colours
                float pulse = 0.55f + 0.45f * (float) Math.sin(now / 260.0);
                int c1 = Gfx.lerpColor(0xFFFF4FA0, 0xFF4CF0FF, (float) (0.5 + 0.5 * Math.sin(now / 900.0)));
                float glow = (0.35f + 0.65f * hover) * pulse;
                Gfx.outline(ctx, x - 1, y - 1, wd + 2, h + 2, Gfx.alpha(c1, glow * 0.45f * a));
                Gfx.outline(ctx, x + 2, y + 2, wd - 4, h - 4, Gfx.alpha(c1, glow * 0.7f * a));
                rim = Gfx.alpha(c1, 0.6f + 0.4f * pulse);
            }
            case "cocoa" -> {
                // marshmallows bobbing on top, steam when hovered
                for (int i = 4; i < wd - 6; i += 11) {
                    int by2 = y + 2 + (int) Math.round(Math.sin(now / 300.0 + i) * hover);
                    ctx.fill(x + i, by2, x + i + 5, by2 + 3, Gfx.alpha(0xFFFFF4E6, a));
                }
                if (hover > 0.05f) {
                    for (int i = 0; i < 4; i++) {
                        float ph = ((now / 1000f) * 0.8f + i / 4f) % 1f;
                        int sx = x + wd / 5 * (i + 1) + (int) (Math.sin(ph * 6 + i) * 2), sy = y - (int) (ph * 8);
                        ctx.fill(sx, sy, sx + 1, sy + 2, Gfx.alpha(0xFFFFFFFF, (1 - ph) * hover * 0.7f * a));
                    }
                }
                rim = Gfx.lerpColor(0xFF2E1A0E, 0xFFE8C9A0, hover);
            }
            case "starry" -> {
                for (int i = 0; i < Math.max(4, wd / 8); i++) {
                    float tw = (float) (0.4 + 0.6 * Math.sin(now / 350.0 + i * 2.3));
                    int sx = x + 3 + (i * 53) % Math.max(1, wd - 6), sy = y + 2 + (i * 17) % Math.max(1, h - 4);
                    ctx.fill(sx, sy, sx + 1, sy + 1, Gfx.alpha(0xFFFFFFFF, tw * a));
                }
                if (hover > 0.05f) {
                    float p = (now % 900) / 900f;
                    int sx = x + (int) (p * wd), sy = y + 2 + (int) (p * (h - 6));
                    for (int k = 0; k < 6; k++) ctx.fill(sx - k, sy - k / 2, sx - k + 1, sy - k / 2 + 1, Gfx.alpha(0xFFFFF4C0, (1 - k / 6f) * hover * a));
                }
                rim = Gfx.lerpColor(0xFF2A3A80, 0xFFFFE9A0, hover);
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
            if (!"lights".equals(style) && !"neon".equals(style) && !"starry".equals(style)) Gfx.snowCap(ctx, x + 2, y, wd - 4, hover, a);
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
