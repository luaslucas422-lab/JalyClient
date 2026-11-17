package fr.darkerdawn.jaly.hud;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** One draggable HUD element. Sizes are in unscaled GUI pixels. */
public abstract class HudModule {
    public final String id, name, desc;
    public final boolean defaultOn;
    public final float defX, defY;

    protected HudModule(String id, String name, String desc, boolean defaultOn, float defX, float defY) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.defaultOn = defaultOn;
        this.defX = defX;
        this.defY = defY;
    }

    public abstract int width(MinecraftClient mc, boolean preview);

    public abstract int height(MinecraftClient mc, boolean preview);

    /** Draw at 0,0 (the caller has already translated and scaled). */
    public abstract void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview);

    public float[] pos() {
        float[] p = JalyConfig.get().hudPos.get(id);
        return p != null && p.length == 2 ? p : new float[]{defX, defY};
    }

    /** Background box in the chosen style. */
    public static void box(DrawContext ctx, int w, int h, JalyConfig.HudStyle s) {
        long t = Gfx.now();
        switch (s.bg) {
            case "none" -> { }
            case "candy" -> {
                Gfx.round(ctx, 0, 0, w, h, 0xC0141A10);
                Gfx.candy(ctx, 1, 0, w - 2, 2, (int) (t / 90 % 8), 1f);
                Gfx.candy(ctx, 1, h - 2, w - 2, 2, (int) (t / 90 % 8), 1f);
            }
            case "frost" -> {
                Gfx.round(ctx, 0, 0, w, h, 0xB0153050);
                Gfx.outline(ctx, 0, 0, w, h, 0xFF9BC3E7);
                Gfx.snowCap(ctx, 1, 0, w - 2, 0f, 1f);
                for (int i = 3; i < w - 2; i += 6) ctx.fill(i, h, i + 1, h + 1 + (i * 3 % 3), 0xCC9BC3E7);
            }
            case "ginger" -> {
                Gfx.round(ctx, 0, 0, w, h, 0xD024110B);
                Gfx.outline(ctx, 0, 0, w, h, 0xFF73301D);
                for (int i = 1; i < w - 1; i += 2) ctx.fill(i, 1 + (i / 2 % 2), i + 2, 2 + (i / 2 % 2), 0xFFF6F4F7);
            }
            default -> {
                Gfx.round(ctx, 0, 0, w, h, 0xB8192C0C);
                Gfx.outline(ctx, 0, 0, w, h, 0x88C99E02);
                Gfx.snowCap(ctx, 1, 0, w - 2, 0f, 0.9f);
            }
        }
    }
}
