package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.Snow;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** The JalyClient loading screen, painted over the vanilla splash while the game loads. */
public final class LoadingScreen {
    private LoadingScreen() {}

    public static final Identifier LOGO = Identifier.of("jalyclient", "loading_logo");
    private static boolean logoTried, logoOk;
    private static int logoW = 188, logoH = 37;
    private static final Snow SNOW = new Snow(120, 9);
    private static float shown;
    private static final String[] TIPS = {
        "Wrapping presents...", "Untangling the fairy lights...", "Feeding the reindeer...",
        "Polishing candy canes...", "Building snowmen...", "Warming up the cocoa...", "Hanging the stockings..."
    };

    /** Our own textures are not loaded yet while the game boots, so read the logo straight from the jar. */
    private static void ensureLogo() {
        if (logoTried) return;
        logoTried = true;
        try {
            Optional<Path> p = FabricLoader.getInstance().getModContainer("jalyclient").flatMap(c -> c.findPath("assets/jalyclient/textures/gui/logo.png"));
            if (p.isEmpty()) return;
            try (InputStream in = Files.newInputStream(p.get())) {
                NativeImage img = NativeImage.read(in);
                logoW = img.getWidth();
                logoH = img.getHeight();
                MinecraftClient.getInstance().getTextureManager().registerTexture(LOGO, fr.darkerdawn.jaly.Compat.texture(img, "img"));
                logoOk = true;
            }
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not load the loading-screen logo", e);
        }
    }

    public static void render(DrawContext ctx, int w, int h, float alpha, float progress, boolean fontReady) {
        ensureLogo();
        long t = Gfx.now();
        Gfx.push(ctx);
        Gfx.translate(ctx, 0, 0);
        ctx.fillGradient(0, 0, w, h, Gfx.alpha(0xFF060B1F, alpha), Gfx.alpha(0xFF1E3366, alpha));
        for (int i = 0; i < 40; i++) {
            int sx = (int) ((i * 137.5f) % w), sy = (int) ((i * 71.3f) % (h * 0.6f));
            float tw = 0.4f + 0.6f * (float) Math.abs(Math.sin(t / 700.0 + i));
            ctx.fill(sx, sy, sx + 1, sy + 1, Gfx.alpha(0xFFFFFFFF, alpha * tw));
        }
        SNOW.render(ctx, 0, 0, w, h, 1f, alpha);
        // snowy ground
        int gy = h - 18;
        ctx.fill(0, gy, w, h, Gfx.alpha(0xFFEEF3F8, alpha));
        for (int x = 0; x < w; x += 6) ctx.fill(x, gy - 1 - (x / 6 % 3 == 0 ? 1 : 0), x + 4, gy, Gfx.alpha(0xFFEEF3F8, alpha));
        // logo
        float scale = Math.min(2.5f, (w * 0.6f) / logoW);
        int lw = (int) (logoW * scale), lh = (int) (logoH * scale);
        int lx = (w - lw) / 2, ly = (int) (h * 0.30f) - lh / 2 + (int) (Math.sin(t / 600.0) * 2);
        if (logoOk) Gfx.tex(ctx, LOGO, lx, ly, lw, lh, 0, 0, logoW, logoH, logoW, logoH, alpha);
        else if (fontReady) Gfx.scaledText(ctx, "JalyClient", w / 2f - 60, ly, 3f, Gfx.alpha(Gfx.GOLD_LIGHT, alpha), true);
        // progress bar
        shown = shown + (progress - shown) * 0.2f;
        int bw = Math.min(240, w - 40), bh = 8;
        int bx = (w - bw) / 2, by = (int) (h * 0.62f);
        Gfx.round(ctx, bx - 2, by - 2, bw + 4, bh + 4, Gfx.alpha(0xCC0B1A0C, alpha));
        int fill = (int) (bw * Math.max(0, Math.min(1, shown)));
        if (fill > 0) Gfx.candy(ctx, bx, by, fill, bh, (int) (t / 50 % 8), alpha);
        Gfx.outline(ctx, bx - 2, by - 2, bw + 4, bh + 4, Gfx.alpha(Gfx.GOLD, alpha));
        Gfx.snowCap(ctx, bx, by - 2, bw, 0.5f, alpha);
        // a little present riding the bar
        Gfx.sprite(ctx, new String[]{"..YY..", "RRYYRR", "RRYYRR", "YYYYYY", "RRYYRR", "RRYYRR"}, new int[]{0xFFB11A33, 0xFFFDE68A}, "RY", bx + fill - 3, by - 9, 1, alpha);
        if (!fontReady) { Gfx.pop(ctx); return; } // the game font isn't loaded yet on first boot
        String tip = TIPS[(int) (t / 1600 % TIPS.length)];
        Gfx.centered(ctx, tip, w / 2, by + 16, Gfx.alpha(0xFFDCE6F2, alpha));
        Gfx.centered(ctx, (int) (Math.max(0, Math.min(1, shown)) * 100) + "%", w / 2, by + 28, Gfx.alpha(Gfx.GOLD_LIGHT, alpha));
        Gfx.text(ctx, "JalyClient " + JalyClient.VERSION, 4, h - 12, Gfx.alpha(0xFF2B4A80, alpha), false);
        Gfx.pop(ctx);
    }
}
