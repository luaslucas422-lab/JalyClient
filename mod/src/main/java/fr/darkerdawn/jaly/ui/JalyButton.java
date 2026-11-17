package fr.darkerdawn.jaly.ui;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

/**
 * The Christmas button. On hover it lifts, the gold rim brightens, candy stripes sweep in,
 * a row of tiny lights switches on one by one, the snow cap puffs up and a snowflake slides in.
 */
public class JalyButton extends ButtonWidget {
    private float hover;
    private boolean wasHovered;
    private long last = -1;
    private boolean selected;
    private final boolean small;

    public JalyButton(int x, int y, int w, int h, Text text, PressAction action) {
        super(x, y, w, h, text, action, DEFAULT_NARRATION_SUPPLIER);
        this.small = h < 18;
    }

    public JalyButton selected(boolean s) { this.selected = s; return this; }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        boolean hov = this.active && (this.isHovered() || this.isFocused());
        if (hov && !wasHovered && this.isHovered() && JalyConfig.get().hoverSounds) Sounds.play(Sounds.HOVER, 1.0f + (float) Math.random() * 0.2f, 0.35f);
        wasHovered = hov;
        hover = Gfx.approach(hover, hov ? 1f : 0f, 12f, dt);
        float a = this.alpha;
        int x = getX(), w = getWidth(), h = getHeight();
        int y = getY() - Math.round(hover * 1.5f);

        Gfx.round(ctx, x + 1, getY() + 2, w, h, Gfx.alpha(0x70000000, a));
        int body = !this.active ? 0xD0303030 : selected ? 0xF0244A1C : Gfx.lerpColor(0xE6183018, 0xF0213E1A, hover);
        Gfx.round(ctx, x, y, w, h, Gfx.alpha(body, a));
        if (this.active && hover > 0.02f) {
            int sw = (int) ((w - 2) * hover);
            ctx.enableScissor(x + 1, y + 1, x + 1 + sw, y + h - 1);
            Gfx.candy(ctx, x + 1, y + h - 4, w - 2, 3, (int) (now / 60 % 8), 0.85f * a);
            ctx.disableScissor();
        }
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, Gfx.alpha(0x30FFFFFF, a));
        int rim = !this.active ? 0xFF555555 : selected ? Gfx.GOLD_LIGHT : Gfx.lerpColor(0xFF8A6A00, Gfx.GOLD_LIGHT, hover);
        Gfx.outline(ctx, x, y, w, h, Gfx.alpha(rim, a));
        if (!small) {
            Gfx.snowCap(ctx, x + 2, y, w - 4, hover, a);
            if (w >= 60) Gfx.lights(ctx, x + 8, x + w - 8, y + 2, Math.max(10, (w - 16) / 6), this.active ? hover : 0f, now, a * (0.4f + 0.6f * hover));
        }
        int tc = !this.active ? 0xFF8A8A8A : Gfx.lerpColor(Gfx.SNOW, Gfx.GOLD_LIGHT, Math.max(hover, selected ? 1f : 0f));
        String label = getMessage().getString();
        int tw = Gfx.font().getWidth(label);
        int tx = x + (w - tw) / 2 + (int) (hover * 4);
        int ty = y + (h - 8) / 2 + (small ? 0 : 1);
        if (hover > 0.05f && !small) {
            int fx = tx - 9 + (int) ((1 - hover) * -6);
            Gfx.sprite(ctx, Gfx.SNOWFLAKE, new int[]{0xFFFFFFFF}, "#", fx, ty + 1, 1, hover * a);
        }
        Gfx.text(ctx, Gfx.trim(label, w - 6), tx, ty, Gfx.alpha(tc, a), true);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        Sounds.play(Sounds.CLICK, 1.0f, 0.7f);
    }
}
