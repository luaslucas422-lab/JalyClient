package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Festive;
import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import fr.darkerdawn.jaly.ui.Snow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.LocalDate;
import java.util.function.Supplier;

/** The J menu: a cozy cabin. Click the board, mirror, presents, fireplace, painting or clock. */
public class LodgeScreen extends Screen {
    private static final Identifier LODGE = Identifier.of("jalyclient", "textures/gui/lodge.png");
    private static final Identifier FIRE = Identifier.of("jalyclient", "textures/gui/fire.png");
    private static final int W = 360, H = 180;
    private static final int[][] BULBS = {{10, 12}, {30, 12}, {50, 12}, {70, 12}, {90, 12}, {110, 12}, {130, 12}, {150, 12}, {170, 12}, {190, 12}, {210, 12}, {230, 12}, {250, 12}, {270, 12}, {290, 12}, {310, 12}};
    private static final int[][] TREE = {{18, 122}, {36, 47}, {51, 95}, {40, 51}, {59, 130}, {50, 108}, {24, 108}, {12, 128}, {55, 73}, {30, 85}, {49, 52}, {43, 48}, {55, 96}, {41, 31}, {62, 117}, {65, 130}, {45, 60}, {63, 123}, {50, 40}, {34, 77}, {18, 105}, {24, 80}, {35, 50}};

    record Spot(String name, String hint, int x1, int y1, int x2, int y2, Supplier<Screen> open) {}

    private final Screen parent;
    private final Spot[] spots;
    private final Snow windowSnow = new Snow(40, 5);
    private final float[] glow;
    private float scale;
    private int ox, oy;
    private long openedAt = -1, last = -1;

    public LodgeScreen(Screen parent) {
        super(Text.literal("Jaly Lodge"));
        this.parent = parent;
        this.spots = new Spot[]{
            new Spot("Tools", "Your JalyClient tools: HUD, PvP, festive and music mods", 90, 28, 144, 80, () -> new ModsScreen(this)),
            new Spot("Mods", "Get mods, resource packs and shaders", 196, 84, 228, 152, () -> new ModrinthScreen(this, fr.darkerdawn.jaly.net.Modrinth.Kind.MOD)),
            new Spot("Edit HUD", "Drag your HUD where you want it", 96, 90, 138, 124, () -> new HudEditorScreen(this)),
            new Spot("Cosmetics", "Hats, capes, wings, pets and trails", 158, 92, 194, 162, () -> new CosmeticsScreen(this)),
            new Spot("Settings", "Menus, music, snow and more", 204, 16, 230, 68, () -> SettingsScreen.create(this)),
            new Spot("Packs", "Christmas resource packs", 228, 56, 316, 150, () -> new PacksScreen(this)),
            new Spot("Shop", "The Jolly Shop (the shopkeeper bites)", 322, 44, 358, 142, () -> new ShopScreen(this)),
            new Spot("Gifts", "Open today's present", 4, 8, 98, 162, () -> new GiftsScreen(this)),
        };
        this.glow = new float[spots.length];
    }

    @Override
    protected void init() {
        if (openedAt < 0) {
            openedAt = Gfx.now();
            Sounds.play(Sounds.SLEIGH, 1f, 0.5f);
        }
        scale = Math.min((width - 8) / (float) W, (height - 34) / (float) H);
        ox = (int) ((width - W * scale) / 2);
        oy = Math.max(4, (int) ((height - 30 - H * scale) / 2));
        int n = spots.length;
        int bw = Math.min(80, (width - 16) / n - 4);
        int total = n * (bw + 4) - 4;
        int bx = (width - total) / 2, by = height - 24;
        for (int i = 0; i < n; i++) {
            Spot s = spots[i];
            String label = s.name.equals("Gifts") && hasGift() ? "Gifts (!)" : s.name;
            addDrawableChild(new JalyButton(bx + i * (bw + 4), by, bw, 18, Text.literal(label), b -> client.setScreen(s.open.get())));
        }
    }

    private boolean hasGift() {
        JalyConfig c = JalyConfig.get();
        int avail = Festive.doorsAvailable(LocalDate.now(), c.firstDay);
        for (int d = 1; d <= avail; d++) if (!c.opened.contains(d)) return true;
        return false;
    }

    private int sx(float x) { return ox + Math.round(x * scale); }
    private int sy(float y) { return oy + Math.round(y * scale); }

    private int hovered(double mx, double my) {
        for (int i = 0; i < spots.length - 1; i++) {
            Spot s = spots[i];
            if (mx >= sx(s.x1) && mx < sx(s.x2) && my >= sy(s.y1) && my < sy(s.y2)) return i;
        }
        Spot g = spots[spots.length - 1];
        if (mx >= sx(g.x1) && mx < sx(g.x2) && my >= sy(g.y1) && my < sy(g.y2)) return spots.length - 1;
        return -1;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        float in = Math.min(1f, (now - openedAt) / 300f);
        ctx.fill(0, 0, width, height, Gfx.alpha(0xF0050A0E, Math.max(in, client.world == null ? 1f : 0f)));
        int sw = Math.round(W * scale), sh = Math.round(H * scale);
        Gfx.push(ctx);
        float pop = 0.94f + 0.06f * Gfx.easeOutBack(in);
        Gfx.translate(ctx, width / 2f, oy + sh / 2f);
        Gfx.scale(ctx, pop, pop);
        Gfx.translate(ctx, -width / 2f, -(oy + sh / 2f));
        // snow behind the window first, then the room on top (window glass is opaque art, so draw snow over it)
        Gfx.tex(ctx, LODGE, ox, oy, sw, sh, 0, 0, W, H, W, H, in);
        ctx.enableScissor(sx(152), sy(24), sx(198), sy(72));
        windowSnow.render(ctx, sx(152), sy(24), sx(198) - sx(152), sy(72) - sy(24), 1f, in);
        ctx.disableScissor();
        // fire
        int frame = (int) (now / 110 % 4);
        Gfx.tex(ctx, FIRE, sx(250), sy(110), Math.round(44 * scale), Math.round(40 * scale), 0, frame * 40, 44, 40, 44, 160, in);
        float flicker = 0.10f + 0.05f * (float) Math.sin(now / 90.0) * (float) Math.sin(now / 37.0);
        ctx.fillGradient(sx(240), sy(96), sx(304), sy(170), Gfx.alpha(0x00FF9A3C, in), Gfx.alpha(0xFFFF9A3C, flicker * in));
        // warm firelight across the room and a cool moonbeam from the window
        float fl = 0.10f + 0.04f * (float) Math.sin(now / 120.0);
        ctx.fillGradient(sx(150), sy(40), sx(320), sy(180), Gfx.alpha(0x00FFB060, in), Gfx.alpha(0xFFFFB060, fl * in));
        ctx.fillGradient(sx(152), sy(84), sx(198), sy(178), Gfx.alpha(0x309BC3E7, in), Gfx.alpha(0x009BC3E7, in));
        drawCharacter(ctx, in);
        // twinkling garland + tree lights
        int b = Math.max(1, Math.round(scale * 1.5f));
        for (int i = 0; i < BULBS.length; i++) {
            float tw = 0.5f + 0.5f * (float) Math.sin(now / 350.0 + i * 1.3);
            int col = Gfx.BULBS[i % Gfx.BULBS.length];
            ctx.fill(sx(BULBS[i][0]) - b, sy(BULBS[i][1]) - b, sx(BULBS[i][0]) + 2 * b, sy(BULBS[i][1]) + 2 * b, Gfx.alpha(col, 0.25f * tw * in));
            ctx.fill(sx(BULBS[i][0]), sy(BULBS[i][1]), sx(BULBS[i][0]) + b, sy(BULBS[i][1]) + b + 1, Gfx.alpha(col, (0.5f + 0.5f * tw) * in));
        }
        for (int i = 0; i < TREE.length; i++) {
            float tw = (float) Math.sin(now / 420.0 + i * 2.3);
            if (tw < -0.2f) continue;
            int col = Gfx.BULBS[(i * 3) % Gfx.BULBS.length];
            ctx.fill(sx(TREE[i][0]), sy(TREE[i][1]), sx(TREE[i][0]) + b, sy(TREE[i][1]) + b, Gfx.alpha(col, Math.min(1, 0.4f + tw) * in));
        }
        // light spilling under the shop door + a bobbing coin sign
        float spill = 0.35f + 0.15f * (float) Math.sin(now / 300.0);
        ctx.fill(sx(326), sy(139), sx(354), sy(141), Gfx.alpha(0xFFFFD27A, spill * in));
        int coinBob = (int) Math.round(Math.sin(now / 260.0) * 2 * scale);
        Gfx.tex(ctx, Identifier.of("jalyclient", "textures/gui/coin.png"), sx(334), sy(27) + coinBob, Math.round(12 * scale), Math.round(12 * scale), 0, 0, 16, 16, 16, 16, in);
        // star pulse
        float star = 0.25f + 0.2f * (float) Math.sin(now / 500.0);
        ctx.fill(sx(36), sy(8), sx(53), sy(30), Gfx.alpha(0xFFFDE68A, star * 0.35f * in));
        // hover glow + labels
        int hov = hovered(mouseX, mouseY);
        for (int i = 0; i < spots.length; i++) {
            glow[i] = Gfx.approach(glow[i], i == hov ? 1f : 0f, 14f, dt);
            if (glow[i] < 0.02f) continue;
            Spot s = spots[i];
            if (s.name.equals("Mods")) {
                int bounce = (int) Math.round(Math.abs(Math.sin(now / 180.0)) * 3);
                Gfx.text(ctx, "!", sx(212) - 2, sy(96) - bounce - 10, Gfx.alpha(Gfx.GOLD_LIGHT, glow[i] * in), true);
                continue;
            }
            float pulse = 0.7f + 0.3f * (float) Math.sin(now / 160.0);
            Gfx.outline(ctx, sx(s.x1) - 1, sy(s.y1) - 1, sx(s.x2) - sx(s.x1) + 2, sy(s.y2) - sy(s.y1) + 2, Gfx.alpha(Gfx.GOLD_LIGHT, glow[i] * pulse * in));
            ctx.fill(sx(s.x1), sy(s.y1), sx(s.x2), sy(s.y2), Gfx.alpha(0x30FDE68A, glow[i] * in));
            String label = s.name;
            int lw = textRenderer.getWidth(label) + 10;
            int lx = (sx(s.x1) + sx(s.x2)) / 2 - lw / 2;
            int ly = sy(s.y1) - 14 - Math.round(glow[i] * 3);
            lx = Math.max(2, Math.min(width - lw - 2, lx));
            ly = Math.max(2, ly);
            Gfx.round(ctx, lx, ly, lw, 12, Gfx.alpha(0xF0192C0C, glow[i]));
            Gfx.outline(ctx, lx, ly, lw, 12, Gfx.alpha(Gfx.GOLD, glow[i]));
            Gfx.text(ctx, label, lx + 5, ly + 2, Gfx.alpha(Gfx.GOLD_LIGHT, glow[i]), true);
        }
        Gfx.pop(ctx);
        // soft vignette
        ctx.fillGradient(0, 0, width, height / 5, Gfx.alpha(0x88000000, in), 0x00000000);
        // header strip
        String hint = hov >= 0 ? spots[hov].hint : (JalyConfig.get().seenLodgeHint ? "Press J to close" : "Click anything in the lodge!");
        Gfx.centered(ctx, hint, width / 2, height - 36, Gfx.alpha(0xFFDCE6F2, in));
        Gfx.text(ctx, "JalyClient " + JalyClient.VERSION, 4, 4, Gfx.alpha(0x88FDE68A, in), true);
    }

    @Override
//#if >=1.21.9
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
       double mouseX = click.x(), mouseY = click.y(); int button = click.button();
//#else
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//#endif
        if (choosing && button == 0) {
            if (inBox(optMods, mouseX, mouseY)) choose("mods");
            else if (inBox(optFriends, mouseX, mouseY)) choose("friends");
            else if (!inBox(choiceBox, mouseX, mouseY)) {
                choosing = false;
                pokes = 0;
                speech = "Whatever.";
                speechAt = Gfx.now();
            }
            return true;
        }
//#if >=1.21.9
        if (super.mouseClicked(click, doubled)) return true;
//#else
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
//#endif
        int h = hovered(mouseX, mouseY);
        if (h >= 0 && button == 0 && spots[h].name.equals("Mods")) {
            pokeCharacter();
            return true;
        }
        if (h >= 0 && button == 0) {
            Sounds.play(Sounds.CLICK, 1f, 0.7f);
            if (!JalyConfig.get().seenLodgeHint) {
                JalyConfig.get().seenLodgeHint = true;
                JalyConfig.save();
            }
            client.setScreen(spots[h].open.get());
            return true;
        }
        return false;
    }

    @Override
//#if >=1.21.9
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
       int keyCode = input.key(), scanCode = input.scancode(), modifiers = input.modifiers();
//#else
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
//#endif
//#if >=1.21.9
        if (JalyClient.MENU_KEY != null && JalyClient.MENU_KEY.matchesKey(input)) {
//#else
        if (JalyClient.MENU_KEY != null && JalyClient.MENU_KEY.matchesKey(keyCode, scanCode)) {
//#endif
            close();
            return true;
        }
//#if >=1.21.9
        return super.keyPressed(input);
//#else
        return super.keyPressed(keyCode, scanCode, modifiers);
//#endif
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private static final String[] GRUMPY1 = {"Leave me alone.", "Leave me alone.", "I'm on my break!", "Not again...", "Shh, I'm napping.", "Can't you see I'm leaning?",
        "Go poke the tree instead.", "Do I look like a button?", "Five more minutes...", "I was here first.", "The fire's warm. Go away.", "Who let you in?"};
    private static final String[] GRUMPY2 = {"I SAID leave me alone.", "You're really doing this, huh?", "Poke me ONE more time...", "*grumbles in pixel*",
        "I'm counting to three.", "Ugh. You again.", "Has anyone told you you're annoying?", "I'm not your elf.", "My cocoa is getting cold!"};
    private int pokes;
    private String speech = "";
    private long speechAt, openAt = -1;
    private boolean choosing, toFriends;
    private int[] optMods, optFriends, choiceBox;

    /** Click your skin 3 times: it grumbles twice, then asks what you want (Mods or Friends). Public for the smoke test. */
    public void pokeCharacter() {
        if (openAt > 0 || choosing) return;
        if (pokes >= 1 && Gfx.now() - speechAt < 350) return; // ignore double-click spam
        pokes++;
        speechAt = Gfx.now();
        if (pokes == 1) {
            speech = GRUMPY1[(int) (Math.random() * GRUMPY1.length)];
            Sounds.play(Sounds.CLICK, 0.6f, 0.7f);
        } else if (pokes == 2) {
            speech = GRUMPY2[(int) (Math.random() * GRUMPY2.length)];
            Sounds.play(Sounds.CLICK, 0.45f, 0.8f);
        } else {
            speech = "*sigh* ...What do you want?";
            choosing = true;
            Sounds.play(Sounds.JINGLE, 0.8f, 0.7f);
        }
    }

    /** Picks an option from the chat box ("mods" or "friends"). Public for the smoke test. */
    public void choose(String which) {
        if (!choosing) return;
        choosing = false;
        speechAt = Gfx.now();
        toFriends = which.equals("friends");
        speech = toFriends ? "Friends? YOU? ...Fine, here." : "Fine, here's the mods and resource packs.";
        Sounds.play(Sounds.CHIME, 1f, 0.7f);
        openAt = Gfx.now() + 1100;
    }

    @Override
    public void tick() {
        super.tick();
        if (openAt > 0 && Gfx.now() >= openAt) {
            openAt = -1;
            pokes = 0;
            speech = "";
            client.setScreen(toFriends ? new FriendsScreen(this) : new ModrinthScreen(this, fr.darkerdawn.jaly.net.Modrinth.Kind.MOD));
        }
    }

    private void drawCharacter(DrawContext ctx, float in) {
        net.minecraft.util.Identifier skin;
        if (client.player != null) skin = fr.darkerdawn.jaly.Compat.playerSkin(client.player);
        else skin = fr.darkerdawn.jaly.Compat.profileSkin(client.getGameProfile());
        float px = scale * 1.5f;
        long now = Gfx.now();
        float breathe = (float) Math.sin(now / 600.0) * 0.3f;
        Gfx.push(ctx);
        Gfx.translate(ctx, sx(212), sy(152));
        Gfx.rotate(ctx, -7f);
        Gfx.scale(ctx, px, px);
        // legs (feet at 0), body, head, arms crossed over the chest
        Gfx.tex(ctx, skin, -4, -12, 4, 12, 4, 20, 4, 12, 64, 64, in);
        Gfx.tex(ctx, skin, 0, -12, 4, 12, 20, 52, 4, 12, 64, 64, in);
        Gfx.tex(ctx, skin, -4, -24, 8, 12, 20, 20, 8, 12, 64, 64, in);
        // outer skin layer, slightly bigger so it pops out like 3D Skin Layers
        Gfx.tex(ctx, skin, -5, -13, 5, 13, 4, 36, 4, 12, 64, 64, in);
        Gfx.tex(ctx, skin, 0, -13, 5, 13, 4, 52, 4, 12, 64, 64, in);
        Gfx.tex(ctx, skin, -5, -25, 10, 13, 20, 36, 8, 12, 64, 64, in);
        float hy = -32 + breathe;
        Gfx.push(ctx);
        Gfx.translate(ctx, 0, hy + 8);
        Gfx.rotate(ctx, pokes > 0 && now - speechAt < 2500 ? 12f : 4f);
        Gfx.tex(ctx, skin, -4, -8, 8, 8, 8, 8, 8, 8, 64, 64, in);
        Gfx.tex(ctx, skin, -4, -8, 8, 8, 40, 8, 8, 8, 64, 64, in);
        Gfx.pop(ctx);
        for (int side = 0; side < 2; side++) {
            Gfx.push(ctx);
            Gfx.translate(ctx, side == 0 ? -6 : 6, -20 + side * 3);
            Gfx.rotate(ctx, side == 0 ? -90f : 90f);
            if (side == 0) {
                Gfx.tex(ctx, skin, -2, -2, 4, 12, 44, 20, 4, 12, 64, 64, in);
                Gfx.tex(ctx, skin, -3, -2, 5, 13, 44, 36, 4, 12, 64, 64, in);
            } else {
                Gfx.tex(ctx, skin, -2, -2, 4, 12, 36, 52, 4, 12, 64, 64, in);
                Gfx.tex(ctx, skin, -3, -2, 5, 13, 52, 52, 4, 12, 64, 64, in);
            }
            Gfx.pop(ctx);
        }
        Gfx.pop(ctx);
        if (!speech.isEmpty() && (choosing || now - speechAt < 3500)) {
            float a = choosing ? 1f : Math.min(1f, (now - speechAt) / 150f) * (now - speechAt > 3000 ? (3500 - (now - speechAt)) / 500f : 1f);
            int tw = textRenderer.getWidth(speech) + 10;
            int bx = Math.max(4, Math.min(width - tw - 4, sx(212) - tw / 2)), by = sy(150) - Math.round(46 * scale * 1.5f) - 14;
            Gfx.round(ctx, bx, by, tw, 14, Gfx.alpha(0xFFFFFFFF, a));
            Gfx.outline(ctx, bx, by, tw, 14, Gfx.alpha(0xFF1A2414, a));
            ctx.fill(sx(212) - 2, by + 14, sx(212) + 2, by + 17, Gfx.alpha(0xFFFFFFFF, a));
            Gfx.text(ctx, speech, bx + 5, by + 3, Gfx.alpha(0xFF1A2414, a), false);
        }
        if (choosing) drawChoice(ctx, now);
    }

    private static boolean inBox(int[] r, double x, double y) { return r != null && x >= r[0] && x < r[0] + r[2] && y >= r[1] && y < r[1] + r[3]; }

    /** The little chat box with the two answers. */
    private void drawChoice(DrawContext ctx, long now) {
        int w = 168, h = 62;
        int x = Math.min(width - w - 6, sx(232) + 6), y = Math.max(20, Math.min(height - h - 44, sy(84)));
        float pop = Math.min(1f, (now - speechAt) / 180f);
        choiceBox = new int[]{x, y, w, h};
        Gfx.round(ctx, x, y, w, h, Gfx.alpha(0xF0101C0E, pop));
        Gfx.outline(ctx, x, y, w, h, Gfx.alpha(Gfx.GOLD, pop));
        Gfx.candy(ctx, x + 4, y + 2, w - 8, 2, (int) (now / 90 % 8), pop);
        Gfx.text(ctx, "Pick one. Quickly.", x + 7, y + 7, Gfx.alpha(Gfx.MUTED, pop), false);
        double mx = client.mouse.getX() * width / client.getWindow().getWidth(), my = client.mouse.getY() * height / client.getWindow().getHeight();
        optMods = new int[]{x + 5, y + 19, w - 10, 18};
        optFriends = new int[]{x + 5, y + 39, w - 10, 18};
        String[][] opts = {{"Mods & resource packs", "\u2726"}, {"Friends", "\u2665"}};
        int[][] rects = {optMods, optFriends};
        for (int i = 0; i < 2; i++) {
            int[] rr = rects[i];
            boolean hov = inBox(rr, mx, my);
            Gfx.round(ctx, rr[0], rr[1], rr[2], rr[3], Gfx.alpha(hov ? 0xFF2A4A1E : 0xFF1A3016, pop));
            if (hov) Gfx.outline(ctx, rr[0], rr[1], rr[2], rr[3], Gfx.alpha(Gfx.GOLD_LIGHT, pop));
            Gfx.text(ctx, (hov ? "> " : "  ") + opts[i][0], rr[0] + 5, rr[1] + 5, Gfx.alpha(hov ? Gfx.GOLD_LIGHT : Gfx.SNOW, pop), false);
            Gfx.text(ctx, opts[i][1], rr[0] + rr[2] - 12, rr[1] + 5, Gfx.alpha(i == 0 ? 0xFF4CC3FF : 0xFFE0384F, pop), false);
        }
    }

    /** Smoke test helper: open a section by name. */
    public Screen openSpot(String name) {
        for (Spot s : spots) if (s.name.equals(name)) return s.open.get();
        return null;
    }
}
