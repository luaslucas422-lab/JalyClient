package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Festive;
import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import fr.darkerdawn.jaly.ui.Snow;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The JalyClient main menu: an animated, blocky Christmas valley at night with parallax hills,
 * an aurora, chimney smoke, a sleigh crossing the moon, falling snow and festive buttons.
 */
public class JalyTitleScreen extends Screen {
    private static final int TW = 480, TH = 270;
    private static final Identifier SKY = id("sky"), AURORA = id("aurora"), FAR = id("far"), MID = id("mid"), NEAR = id("near"), SLEIGH = id("sleigh");
    private static final Identifier LOGO = Identifier.of("jalyclient", "textures/gui/logo.png");
    private static final int[][] CHIMNEYS = {{121, 134}, {265, 150}, {409, 150}};
    private static final int[][] WINDOWS = {{100, 158}, {116, 158}, {244, 174}, {260, 174}, {388, 174}, {404, 174}};
    private static final Snow FRONT = new Snow(90, 11), BACK = new Snow(140, 12);
    private static long firstShown = -1;
    private final List<JalyButton> menuButtons = new ArrayList<>();
    private long last = -1;
    private float sceneTime;

    private static Identifier id(String n) { return Identifier.of("jalyclient", "textures/gui/title/" + n + ".png"); }

    public JalyTitleScreen() {
        super(Text.literal("JalyClient"));
    }

    @Override
    protected void init() {
        if (firstShown < 0) firstShown = Gfx.now();
        fr.darkerdawn.jaly.net.Updates.check();
        menuButtons.clear();
        int bw = 200, bh = 20, x = width / 2 - bw / 2;
        float ls = logoScale();
        int pillBottom = minecraftLogo() ? 30 + 44 + 14 + 26 : (int) (height * 0.10f) + Math.round(37 * ls) + 4 + 13 + 13;
        int y = Math.max((int) (height * 0.44f), pillBottom + 16);
        add(new JalyButton(x, y, bw, bh, Text.literal("Singleplayer"), b -> client.setScreen(new SelectWorldScreen(this))));
        add(new JalyButton(x, y + 24, bw, bh, Text.literal("Multiplayer"), b -> {
            Screen s = client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this);
            client.setScreen(s);
        }));
        add(new JalyButton(x, y + 48, bw, bh, Text.literal("Jaly Lodge"), b -> client.setScreen(new LodgeScreen(this))));
        add(new JalyButton(x, y + 76, 64, bh, Text.literal("Options"), b -> client.setScreen(new OptionsScreen(this, client.options))));
        add(new JalyButton(x + 68, y + 76, 64, bh, Text.literal("Credits"), b -> client.setScreen(new CreditsScreen(this))));
        add(new JalyButton(x + 136, y + 76, 64, bh, Text.literal("Quit"), b -> client.scheduleStop()));
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            addDrawableChild(new JalyButton(width - 58, height - 46, 54, 14, Text.literal("Mods"), b -> ModMenuCompat.openModList(this)));
        }
        addDrawableChild(new JalyButton(width - 58, height - 30, 54, 14, Text.literal("Packs"), b -> client.setScreen(new PacksScreen(this))));
    }

    private static final String[] SPLASHES = {"Merry Christmas!", "Ho ho ho!", "Now with 100% more snow!", "Jingle all the way!", "Free forever!",
        "Candy canes included!", "Let it snow!", "Made by DarkerDawnFR!", "Press J!", "Don't poke the grumpy guy!", "Cozy by the fire!", "Santa approved!"};
    private static String splash;
    private float gearHover;
    private final Snow gearSnow = new Snow(18, 77);
    private net.minecraft.client.gui.LogoDrawer logoDrawer;

    private boolean minecraftLogo() { return "minecraft".equals(JalyConfig.get().logoStyle); }

    private boolean overGear(double mx, double my) { return mx >= 4 && mx < 26 && my >= height - 48 && my < height - 26; }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (overGear(mouseX, mouseY)) {
            fr.darkerdawn.jaly.Sounds.play(fr.darkerdawn.jaly.Sounds.CLICK, 1.2f, 0.7f);
            client.setScreen(new CustomizerScreen(this));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawGear(DrawContext ctx, int mouseX, int mouseY, long now) {
        boolean hov = overGear(mouseX, mouseY);
        gearHover = Gfx.approach(gearHover, hov ? 1f : 0f, 10f, 0.016f);
        int gx = 5, gy = height - 47;
        Gfx.round(ctx, gx - 1, gy - 1, 22, 22, Gfx.alpha(0xC0122312, 0.6f + 0.4f * gearHover));
        if (gearHover > 0.01f) Gfx.outline(ctx, gx - 1, gy - 1, 22, 22, Gfx.alpha(Gfx.GOLD_LIGHT, gearHover));
        ctx.getMatrices().push();
        ctx.getMatrices().translate(gx + 10, gy + 10, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees((now % 6000) / 6000f * 360f * gearHover));
        Gfx.tex(ctx, Identifier.of("jalyclient", "textures/gui/gear.png"), -10, -10, 20, 20, 0, 0, 20, 20, 20, 20, 1f);
        ctx.getMatrices().pop();
        if (gearHover > 0.05f) {
            ctx.enableScissor(gx - 1, gy - 1, gx + 21, gy + 21);
            gearSnow.render(ctx, gx - 1, gy - 1, 22, 22, 1f, gearHover);
            ctx.disableScissor();
            Gfx.text(ctx, "Customize menu", gx + 26, gy + 6, Gfx.alpha(Gfx.GOLD_LIGHT, gearHover), true);
        }
    }

    private void drawSplash(DrawContext ctx, int x, int y, long now) {
        JalyConfig c = JalyConfig.get();
        if (!c.splashOn) return;
        if (splash == null) splash = SPLASHES[(int) (Math.random() * SPLASHES.length)];
        String text = c.splashText != null && !c.splashText.isBlank() ? c.splashText : splash;
        float pulse = 1.8f - Math.abs((float) Math.sin((now % 1000L) / 1000f * Math.PI * 2) * 0.1f);
        pulse = pulse * 100f / (textRenderer.getWidth(text) + 32);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(-20f));
        ctx.getMatrices().scale(pulse, pulse, 1);
        int tw = textRenderer.getWidth(text);
        if ("candy".equals(c.splashStyle)) {
            int cx = -tw / 2;
            for (int i = 0; i < text.length(); i++) {
                String ch = String.valueOf(text.charAt(i));
                int col = ((i + (int) (now / 200)) % 2 == 0) ? 0xFFE0384F : 0xFFFFFFFF;
                ctx.drawText(textRenderer, ch, cx, -4, col, true);
                cx += textRenderer.getWidth(ch);
            }
        } else {
            ctx.drawText(textRenderer, text, -tw / 2, -4, 0xFFFFFF00, true);
        }
        ctx.getMatrices().pop();
    }

    private void drawPlayerCard(DrawContext ctx) {
        String st = JalyConfig.get().playerCard;
        if ("off".equals(st)) return;
        String name = client.getSession().getUsername();
        var skin = client.getSkinProvider().getSkinTextures(client.getGameProfile()).texture();
        net.minecraft.text.MutableText styled = "gold".equals(st)
            ? fr.darkerdawn.jaly.cosmetic.NameStyle.style(name, "gold", "shine", true)
            : fr.darkerdawn.jaly.cosmetic.NameStyle.style(name, "white", "candy", true);
        int w = textRenderer.getWidth(styled) + 30, x = width - w - 4, y = 14;
        Gfx.round(ctx, x, y, w, 20, 0xC0122312);
        Gfx.outline(ctx, x, y, w, 20, "gold".equals(st) ? 0xFFFFD94A : 0xFFE0384F);
        Gfx.tex(ctx, skin, x + 3, y + 2, 16, 16, 8, 8, 8, 8, 64, 64, 1f);
        Gfx.tex(ctx, skin, x + 3, y + 2, 16, 16, 40, 8, 8, 8, 64, 64, 1f);
        ctx.drawText(textRenderer, styled, x + 23, y + 6, 0xFFFFFFFF, true);
    }

    private void add(JalyButton b) {
        b.mainMenu();
        menuButtons.add(b);
        addDrawableChild(b);
    }

    @Override
    public void tick() {
        JalyClient.tickMenuMusicSafe();
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    private void layer(DrawContext ctx, Identifier tex, float scrollPx, float s, float alpha) {
        int w = Math.round(TW * s), h = Math.round(TH * s);
        int oy = height - h;
        float off = scrollPx % TW;
        int x0 = Math.round(-off * s);
        for (int x = x0; x < width; x += w) Gfx.tex(ctx, tex, x, oy, w, h, 0, 0, TW, TH, TW, TH, alpha);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        boolean motion = JalyConfig.get().snowAmount() > 0;
        sceneTime += motion ? dt : 0;
        float s = Math.max(width / (float) TW, height / (float) TH);
        float t = sceneTime;
        String pano = JalyConfig.get().panorama;
        if (!pano.equals("valley") && !pano.equals("valley_still")) {
            drawOtherPanorama(ctx, pano, s, now, delta);
            Gfx.lights(ctx, 0, width, 0, 22, 1f, now, 1f);
            drawLogo(ctx, now);
            return;
        }
        if (pano.equals("valley_still")) { t = 0; mouseX = width / 2; }
        // parallax: mouse adds a tiny drift for depth
        float mx = (mouseX - width / 2f) / Math.max(1, width);
        Gfx.tex(ctx, SKY, 0, height - Math.round(TH * s), Math.round(TW * s), Math.round(TH * s), 0, 0, TW, TH, TW, TH, 1f);
        // twinkling stars
        for (int i = 0; i < 26; i++) {
            float tw = (float) Math.sin(now / 500.0 + i * 2.1);
            if (tw < 0.3f) continue;
            int sx = (int) (((i * 97) % TW) * s), sy = (int) (((i * 37) % 120 + 6) * s) + (height - Math.round(TH * s));
            int c = Gfx.alpha(0xFFFFFFFF, tw);
            ctx.fill(sx, sy - 1, sx + 1, sy + 2, c);
            ctx.fill(sx - 1, sy, sx + 2, sy + 1, c);
        }
        layer(ctx, AURORA, t * 3f, s, 0.55f + 0.35f * (float) Math.sin(t * 0.4));
        sleigh(ctx, s, now);
        BACK.render(ctx, 0, 0, width, height, 0.6f * JalyConfig.get().snowAmount(), 0.55f);
        layer(ctx, FAR, t * 3f + mx * 6, s, 1f);
        float midScroll = t * 7f + mx * 12;
        layer(ctx, MID, midScroll, s, 1f);
        smokeAndWindows(ctx, midScroll, s, now);
        layer(ctx, NEAR, t * 14f + mx * 24, s, 1f);
        if (!pano.equals("valley_still")) FRONT.render(ctx, 0, 0, width, height, JalyConfig.get().snowAmount(), 1f);
        // vignette so the buttons pop
        ctx.fillGradient(0, 0, width, height / 3, 0x66000000, 0x00000000);
        ctx.fillGradient(0, (int) (height * 0.35f), width, (int) (height * 0.8f), 0x00000000, 0x33000000);
        Gfx.lights(ctx, 0, width, 0, 22, 1f, now, 1f);
        drawLogo(ctx, now);
    }

    private void drawOtherPanorama(DrawContext ctx, String pano, float s, long now, float delta) {
        if (fr.darkerdawn.jaly.ui.MenuScenes.has(pano)) {
            fr.darkerdawn.jaly.ui.MenuScenes.draw(ctx, pano, width, height, sceneTime, now);
            ctx.fillGradient(0, 0, width, height / 3, 0x66000000, 0x00000000);
            return;
        }
        switch (pano) {
            case "aurora" -> {
                Gfx.tex(ctx, SKY, 0, height - Math.round(TH * s), Math.round(TW * s), Math.round(TH * s), 0, 0, TW, TH, TW, TH, 1f);
                layer(ctx, AURORA, sceneTime * 6f, s, 0.9f);
                layer(ctx, AURORA, sceneTime * -4f + 120, s, 0.6f + 0.3f * (float) Math.sin(sceneTime * 0.7));
                sleigh(ctx, s, now);
                layer(ctx, FAR, sceneTime * 2f, s, 1f);
                BACK.render(ctx, 0, 0, width, height, JalyConfig.get().snowAmount(), 0.8f);
            }
            case "vanilla" -> {
                this.renderPanoramaBackground(ctx, delta);
                ctx.fillGradient(0, 0, width, height, 0x60081428, 0x90102040);
                FRONT.render(ctx, 0, 0, width, height, JalyConfig.get().snowAmount(), 1f);
            }
            case "lodge" -> {
                float k = Math.max(width / 320f, height / 180f);
                Gfx.tex(ctx, Identifier.of("jalyclient", "textures/gui/lodge.png"), (int) (width - 320 * k) / 2, (int) (height - 180 * k) / 2, (int) (320 * k), (int) (180 * k), 0, 0, 320, 180, 320, 180, 1f);
                ctx.fill(0, 0, width, height, 0x88060A0E);
            }
            case "candy" -> {
                ctx.fill(0, 0, width, height, 0xFF7A0E1E);
                for (int x = -height; x < width; x += 24) {
                    for (int y = 0; y < height; y += 2) ctx.fill(x + y, y, x + y + 12, y + 2, 0x33FFFFFF);
                }
                ctx.fillGradient(0, 0, width, height, 0x22000000, 0xAA000000);
            }
            default -> {
                ctx.fillGradient(0, 0, width, height, 0xFF050A1C, 0xFF14254A);
                for (int i = 0; i < 90; i++) ctx.fill((i * 137) % width, (i * 71) % (height * 2 / 3), (i * 137) % width + 1, (i * 71) % (height * 2 / 3) + 1, 0xCCFFFFFF);
                ctx.fill(0, height - 26, width, height, 0xFFEEF3F8);
            }
        }
        ctx.fillGradient(0, 0, width, height / 3, 0x66000000, 0x00000000);
    }

    private void sleigh(DrawContext ctx, float s, long now) {
        long period = 36000;
        float p = (now % period) / (float) period;
        if (p > 0.35f) return;
        float k = p / 0.35f;
        float x = width + 20 - k * (width + 120 * s);
        float y = (float) (height - TH * s + (34 + Math.sin(k * Math.PI * 2) * 6) * s);
        Gfx.tex(ctx, SLEIGH, (int) x, (int) y, Math.round(64 * s * 0.7f), Math.round(16 * s * 0.7f), 0, 0, 64, 16, 64, 16, 0.95f);
    }

    private void smokeAndWindows(DrawContext ctx, float scroll, float s, long now) {
        int oy = height - Math.round(TH * s);
        float w = TW * s;
        float off = (scroll % TW) * s;
        for (int rep = 0; rep < 3; rep++) {
            float base = rep * w - off;
            for (int[] c : CHIMNEYS) {
                for (int i = 0; i < 5; i++) {
                    float ph = ((now / 1000f) * 0.35f + i / 5f + c[0] * 0.01f) % 1f;
                    float px = base + (c[0] + (float) Math.sin(ph * 5 + i) * 3 + ph * 8) * s;
                    float py = oy + (c[1] - ph * 26) * s;
                    int size = Math.max(1, Math.round((1 + ph * 3) * s * 0.6f));
                    if (px < -10 || px > width + 10) continue;
                    ctx.fill((int) px, (int) py, (int) px + size, (int) py + size, Gfx.alpha(0xFFB8C4D6, 0.55f * (1 - ph)));
                }
            }
            for (int i = 0; i < WINDOWS.length; i++) {
                int[] wv = WINDOWS[i];
                float flick = 0.12f + 0.12f * (float) Math.sin(now / 230.0 + i * 1.3) * (float) Math.sin(now / 97.0 + i);
                float px = base + wv[0] * s;
                if (px < -20 || px > width + 20) continue;
                ctx.fill((int) (px - s), (int) (oy + (wv[1] - 1) * s), (int) (px + 9 * s), (int) (oy + (wv[1] + 9) * s), Gfx.alpha(0xFFFFD27A, Math.max(0, flick)));
            }
        }
    }

    /** Logo size that always leaves room for the five menu buttons below it. */
    private float logoScale() {
        float byHeight = (height - 30 - 100 - 26 - 16 - height * 0.10f - 8) / 37f;
        return Math.max(0.8f, Math.min(2.4f, Math.min((width * 0.62f) / 188f, byHeight)));
    }

    private void drawLogo(DrawContext ctx, long now) {
        float intro = Math.min(1f, (now - firstShown) / 900f);
        if (minecraftLogo()) {
            if (logoDrawer == null) logoDrawer = new net.minecraft.client.gui.LogoDrawer(false);
            logoDrawer.draw(ctx, width, intro);
            drawSplash(ctx, width / 2 + 90, 70, now);
            String cdm = Festive.countdownText(LocalDate.now());
            Gfx.centered(ctx, cdm, width / 2, 92, Gfx.alpha(Gfx.GOLD_LIGHT, intro));
            return;
        }
        float drop = Gfx.easeOutBack(intro);
        float scale = logoScale();
        int lw = Math.round(188 * scale), lh = Math.round(37 * scale);
        int lx = (width - lw) / 2;
        int ly = (int) (height * 0.10f + (1 - drop) * -60 + Math.sin(now / 700.0) * 2);
        String ls = JalyConfig.get().logoStyle;
        Identifier logo = "frost".equals(ls) ? Identifier.of("jalyclient", "textures/gui/logo_frost.png") : "ginger".equals(ls) ? Identifier.of("jalyclient", "textures/gui/logo_ginger.png") : LOGO;
        Gfx.tex(ctx, logo, lx, ly, lw, lh, 0, 0, 188, 37, 188, 37, intro);
        int sy = ly + lh + 4;
        String sub = "The jolliest free Minecraft client";
        Gfx.centered(ctx, sub, width / 2, sy, Gfx.alpha(Gfx.GOLD_LIGHT, intro));
        String cd = Festive.countdownText(LocalDate.now());
        int cw = textRenderer.getWidth(cd) + 22;
        int cx = (width - cw) / 2, cy = sy + 13;
        Gfx.round(ctx, cx, cy, cw, 13, Gfx.alpha(0xC0122312, intro));
        Gfx.outline(ctx, cx, cy, cw, 13, Gfx.alpha(0x99C99E02, intro));
        Gfx.sprite(ctx, Gfx.TREE, new int[]{0xFF3FB950}, "#", cx + 5, cy + 3, 1, intro);
        ctx.fill(cx + 8, cy + 2, cx + 9, cy + 3, Gfx.alpha(0xFFFDE68A, intro));
        Gfx.text(ctx, cd, cx + 16, cy + 3, Gfx.alpha(Gfx.SNOW, intro), true);
        drawSplash(ctx, lx + lw - 10, ly + lh - 6, now);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        for (int i = 0; i < menuButtons.size(); i++) {
            float k = Math.min(1f, Math.max(0f, (now - firstShown - 300 - i * 90) / 350f));
            menuButtons.get(i).setAlpha(k);
        }
        super.render(ctx, mouseX, mouseY, delta);
        drawGear(ctx, mouseX, mouseY, now);
        drawPlayerCard(ctx);
        Gfx.text(ctx, "JalyClient " + JalyClient.VERSION + " | Minecraft 1.21.1", 4, height - 11, 0xFF2B3A55, false);
        if (fr.darkerdawn.jaly.net.Updates.available()) {
            String u = "JalyClient " + fr.darkerdawn.jaly.net.Updates.latest + " is here! Close Minecraft and open JalyClient to update.";
            int uw = textRenderer.getWidth(u) + 16, ux = (width - uw) / 2, uy = height - 30;
            float pulse = 0.75f + 0.25f * (float) Math.sin(Gfx.now() / 300.0);
            Gfx.round(ctx, ux, uy, uw, 14, 0xF0B11A33);
            Gfx.outline(ctx, ux, uy, uw, 14, Gfx.alpha(Gfx.GOLD_LIGHT, pulse));
            Gfx.text(ctx, u, ux + 8, uy + 3, Gfx.SNOW, true);
        }
        String r = "by DarkerDawnFR | free forever";
        Gfx.text(ctx, r, width - textRenderer.getWidth(r) - 4, height - 11, 0xFF2B3A55, false);
    }

    /** Small helper so the Mod Menu class is only touched when Mod Menu is installed. */
    static final class ModMenuCompat {
        static void openModList(Screen parent) {
            try {
                Class<?> c = Class.forName("com.terraformersmc.modmenu.gui.ModsScreen");
                Screen s = (Screen) c.getConstructor(Screen.class).newInstance(parent);
                net.minecraft.client.MinecraftClient.getInstance().setScreen(s);
            } catch (Throwable e) {
                JalyClient.LOG.warn("Mod Menu screen unavailable", e);
            }
        }
    }

}
