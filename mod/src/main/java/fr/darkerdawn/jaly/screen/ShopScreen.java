package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.cosmetic.Cosmetics;
import fr.darkerdawn.jaly.cosmetic.InvertedSkin;
import fr.darkerdawn.jaly.cosmetic.NameStyle;
import fr.darkerdawn.jaly.net.ShopApi;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Behind the door at the end of the Lodge: the Jolly Shop and its very grumpy shopkeeper (your skin, inverted). */
public class ShopScreen extends Screen {
    private static final Identifier ROOM = Identifier.of("jalyclient", "textures/gui/shop.png");
    private static final Identifier COIN = Identifier.of("jalyclient", "textures/gui/coin.png");
    private static final int W = 360, H = 180, KX = 180, KFEET = 146;
    private static final float KS = 2.9f;

    static final String[] HELLO = {"What. Do. You. Want.", "Oh great, a customer.", "We're closed. We're always closed. Except now.", "Wipe your feet. Actually, don't come in.",
        "Welcome to the Jolly Shop. I am NOT jolly.", "You again?!", "Don't touch the snow globe."};
    static final String[] POKE = {"Stop poking me.", "I said STOP.", "Do I look like a button to you?", "Poke me again. I dare you.", "My sunglasses cost more than you.",
        "I'm not your skin. You're MY skin.", "Every poke is +1 grumpy.", "Buy something or go back to the Lodge.", "Ho. Ho. No.", "The hat is crooked on PURPOSE.",
        "I've been inverted since 1843.", "Santa owes me money.", "This is my resting angry face.", "You're blocking my light.", "I work for coins, not hugs.",
        "*sigh* ...what?", "The elves unionized. Now it's just me.", "Is that a coin in your pocket? No? Then shoo."};
    static final String[] RAGE = {"THAT'S IT. Buy something or LEAVE!", "MY BLOOD PRESSURE IS INVERTED TOO!", "ENOUGH POKING! I'M CALLING THE KRAMPUS!"};
    static final String[] BROWSE = {"Look, don't lick.", "Those wings won't fly you out of here.", "Everything's handmade. By me. Grumpily.", "No refunds on vibes.",
        "Premium stuff. Premium attitude.", "Try it on. Don't stretch it."};
    static final String[] BOUGHT = {"Finally, someone with taste.", "Pleasure doing business. Ugh.", "Fine. It suits you. Barely.", "Don't tell anyone I smiled.", "Wear it with pride. Or whatever."};
    static final String[] BROKE = {"You can't afford that. Shocking.", "Coins first. Dreams later.", "Nice try. Come back richer.", "Your wallet is emptier than my heart."};
    static final String[] COINS = {"Ah. Money. My favourite subject.", "Bigger bag, bigger bonus. I'm not a monster.", "Shiny, shiny coins..."};

    enum Panel { NONE, ITEMS, COINS }

    private final Screen parent;
    private Panel panel = Panel.NONE;
    private float scale, shift;
    private int ox, oy, baseOx;
    private long lastFrame = -1;
    private long openedAt = -1, speechAt, rageAt = -1, lastPoke;
    private float anger = 1f, spin;
    private String speech = "";
    private boolean musicOn;
    private ShopApi.Item selected;
    private double scroll;

    public ShopScreen(Screen parent) {
        super(Text.literal("Jolly Shop"));
        this.parent = parent;
    }

    public ShopScreen(Screen parent, String item) {
        this(parent);
        this.selected = ShopApi.item(item);
        if (selected != null) panel = Panel.ITEMS;
    }

    // ---------- layout ----------
    private int sx(float x) { return ox + Math.round(x * scale); }
    private int sy(float y) { return oy + Math.round(y * scale); }
    private int panelW() { return Math.min(300, width / 2 + 20); }
    private int panelX() { return width - panelW() - 6; }
    private int panelY() { return 30; }
    private int panelH() { return height - 30 - 44; }

    @Override
    protected void init() {
        if (openedAt < 0) {
            openedAt = Gfx.now();
            Sounds.play(Sounds.JINGLE, 0.6f, 0.6f);
            say(pick(HELLO));
            if (!ShopApi.signedIn && !ShopApi.busy) ShopApi.connect();
            else ShopApi.refresh();
        }
        scale = Math.min((width - 8) / (float) W, (height - 34) / (float) H);
        baseOx = (int) ((width - W * scale) / 2);
        ox = baseOx + Math.round(shift);
        oy = Math.max(4, (int) ((height - 30 - H * scale) / 2));
        int by = height - 24;
        String[] labels = {"Browse", "Jolly Coins", "Back to Lodge"};
        int bw = Math.min(110, (width - 24) / 3);
        int bx = (width - (bw * 3 + 8)) / 2;
        addDrawableChild(new JalyButton(bx, by, bw, 18, Text.literal(labels[0]), b -> open(panel == Panel.ITEMS ? Panel.NONE : Panel.ITEMS)));
        addDrawableChild(new JalyButton(bx + bw + 4, by, bw, 18, Text.literal(labels[1]), b -> open(panel == Panel.COINS ? Panel.NONE : Panel.COINS)));
        addDrawableChild(new JalyButton(bx + 2 * (bw + 4), by, bw, 18, Text.literal(labels[2]), b -> close()));
        if (!ShopApi.signedIn) {
            addDrawableChild(new JalyButton(6, 28, 96, 18, Text.literal("Sign in"), b -> ShopApi.connect()));
        }
        if (panel == Panel.ITEMS && selected != null) {
            int px = panelX(), pw = panelW(), py = panelY(), ph = panelH();
            int lw = prevW() - 4;
            boolean owned = ShopApi.owns(selected.id());
            String txt = owned ? (isEquipped(selected) ? "Unequip" : "Equip") : "Buy (" + selected.price() + ")";
            addDrawableChild(new JalyButton(px + 6, py + ph - 24, lw, 18, Text.literal(txt), b -> buyOrEquip()));
        }
    }

    private void open(Panel p) {
        panel = p;
        scroll = 0;
        if (p == Panel.ITEMS) say(pick(BROWSE));
        if (p == Panel.COINS) say(pick(COINS));
        Cosmetics.PREVIEW.clear();
        if (p == Panel.ITEMS && selected != null) preview(selected);
        clearAndInit();
    }

    private static String pick(String[] a) { return a[(int) (Math.random() * a.length)]; }

    private void say(String s) {
        speech = s;
        speechAt = Gfx.now();
    }

    private static Cosmetics.Type typeOf(ShopApi.Item it) {
        String k = it.id().split(":")[0];
        for (Cosmetics.Type t : Cosmetics.Type.values()) if (t.key.equals(k)) return t;
        return Cosmetics.Type.HAT;
    }

    private static String idOf(ShopApi.Item it) { return it.id().split(":")[1]; }

    private static boolean isEquipped(ShopApi.Item it) {
        Cosmetics.Type t = typeOf(it);
        Cosmetics.PREVIEW.remove(t);
        boolean eq = Cosmetics.selected(t).equals(idOf(it));
        return eq;
    }

    private void preview(ShopApi.Item it) {
        Cosmetics.PREVIEW.clear();
        Cosmetics.PREVIEW.put(typeOf(it), idOf(it));
    }

    private void buyOrEquip() {
        ShopApi.Item it = selected;
        if (it == null) return;
        if (ShopApi.owns(it.id())) {
            boolean eq = isEquipped(it);
            Cosmetics.select(typeOf(it), eq ? (typeOf(it) == Cosmetics.Type.NAME_COLOR ? "white" : "none") : idOf(it));
            Sounds.play(Sounds.CLICK, 1f, 0.7f);
            preview(it);
            clearAndInit();
            return;
        }
        if (!ShopApi.signedIn) { say("Sign in first. I don't do IOUs."); return; }
        if (ShopApi.coins < it.price()) {
            say(pick(BROKE));
            anger = Math.min(5, anger + 0.6f);
            open(Panel.COINS);
            return;
        }
        ShopApi.buy(it).thenRun(() -> client.execute(() -> {
            if (ShopApi.owns(it.id())) {
                Cosmetics.select(typeOf(it), idOf(it));
                Sounds.play(Sounds.UNWRAP, 1f, 0.8f);
                say(pick(BOUGHT));
                anger = Math.max(0, anger - 1.5f);
            }
            if (client.currentScreen == this) clearAndInit();
        }));
    }

    /** Click the shopkeeper: he grumbles, gets angrier, and the shop music starts. Public for the smoke test. */
    public void pokeShopkeeper() {
        if (Gfx.now() - lastPoke < 250) return;
        lastPoke = Gfx.now();
        if (!musicOn) {
            musicOn = true;
            Sounds.startShopMusic(client);
        }
        anger = Math.min(5f, anger + 1f);
        if (anger >= 5f) {
            say(pick(RAGE));
            rageAt = Gfx.now();
            anger = 3f;
            Sounds.play(Sounds.KILL, 0.6f, 0.8f);
        } else {
            say(pick(POKE));
            Sounds.play(Sounds.CLICK, 0.55f + (float) Math.random() * 0.2f, 0.7f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        anger = Math.max(0.5f, anger - 0.01f);
        if (musicOn) Sounds.startShopMusic(client); // loops the tune
    }

    // ---------- drawing ----------
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = lastFrame < 0 ? 0 : Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float target = panel == Panel.NONE ? 0f : panelX() / 2f - (baseOx + KX * scale);
        shift = Gfx.approach(shift, target, 9f, dt);
        ox = baseOx + Math.round(shift);
        float in = Math.min(1f, (now - openedAt) / 300f);
        ctx.fill(0, 0, width, height, Gfx.alpha(0xF0050A0E, Math.max(in, client.world == null ? 1f : 0f)));
        ctx.getMatrices().push();
        if (rageAt > 0 && now - rageAt < 600) {
            float k = 1f - (now - rageAt) / 600f;
            ctx.getMatrices().translate((float) Math.sin(now / 18.0) * 4 * k, (float) Math.cos(now / 23.0) * 3 * k, 0);
        }
        int sw = Math.round(W * scale), sh = Math.round(H * scale);
        Gfx.tex(ctx, ROOM, ox, oy, sw, sh, 0, 0, W, H, W, H, in);
        // lamp glow over the counter
        float fl = 0.12f + 0.03f * (float) Math.sin(now / 140.0);
        ctx.fillGradient(sx(90), sy(30), sx(270), sy(150), Gfx.alpha(0x00FFC870, in), Gfx.alpha(0xFFFFC870, fl * in));
        drawShopkeeper(ctx, in, now, mouseX, mouseY);
        // the counter goes back in front of him
        redraw(ctx, 92, 106, 268, 150, in);
        redraw(ctx, 102, 88, 124, 106, in);
        redraw(ctx, 130, 88, 148, 106, in);
        redraw(ctx, 226, 84, 256, 106, in);
        // twinkling coins on the counter
        for (int i = 0; i < 3; i++) {
            float tw = (float) Math.sin(now / 260.0 + i * 2.1);
            if (tw > 0.6f) ctx.fill(sx(134 + i * 5), sy(92 - i * 2), sx(134 + i * 5) + Math.max(1, Math.round(scale)), sy(92 - i * 2) + Math.max(1, Math.round(scale)), Gfx.alpha(0xFFFFFFFF, in));
        }
        ctx.getMatrices().pop();
        ctx.fillGradient(0, 0, width, height / 5, Gfx.alpha(0x88000000, in), 0x00000000);
        drawSpeech(ctx, in, now);
        drawBalance(ctx, in);
        if (panel == Panel.ITEMS) drawItems(ctx, mouseX, mouseY, delta);
        if (panel == Panel.COINS) drawCoins(ctx, mouseX, mouseY);
        String st = ShopApi.status;
        if (!st.isEmpty()) Gfx.centered(ctx, st, width / 2, height - 36, ShopApi.busy ? Gfx.GOLD_LIGHT : 0xFFFF9A9A);
        else if (panel == Panel.NONE) Gfx.centered(ctx, overKeeper(mouseX, mouseY) ? "Poke the shopkeeper (if you dare)" : "Browse premium cosmetics or get Jolly Coins", width / 2, height - 36, 0xFFDCE6F2);
    }

    private void redraw(DrawContext ctx, int x1, int y1, int x2, int y2, float in) {
        Gfx.tex(ctx, ROOM, sx(x1), sy(y1), sx(x2) - sx(x1), sy(y2) - sy(y1), x1, y1, x2 - x1, y2 - y1, W, H, in);
    }

    private boolean overKeeper(double mx, double my) {
        if (panel != Panel.NONE && mx >= panelX()) return false;
        return mx >= sx(KX - 22) && mx < sx(KX + 22) && my >= sy(KFEET - 36 * KS) && my < sy(108);
    }

    private void drawShopkeeper(DrawContext ctx, float in, long now, int mx, int my) {
        Identifier skin;
        if (client.player != null) skin = client.player.getSkinTextures().texture();
        else skin = client.getSkinProvider().getSkinTextures(client.getGameProfile()).texture();
        skin = InvertedSkin.get(client, skin);
        float px = scale * KS;
        float breathe = (float) Math.sin(now / 520.0) * 0.4f;
        float shake = anger >= 3 ? (float) Math.sin(now / 40.0) * (anger - 2.5f) * 0.25f : 0f;
        boolean hover = overKeeper(mx, my);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(sx(KX) + shake * px, sy(KFEET), 0);
        ctx.getMatrices().scale(px, px, 1);
        // legs + body
        Gfx.tex(ctx, skin, -4, -12, 4, 12, 4, 20, 4, 12, 64, 64, in);
        Gfx.tex(ctx, skin, 0, -12, 4, 12, 20, 52, 4, 12, 64, 64, in);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, breathe, 0);
        Gfx.tex(ctx, skin, -4, -24, 8, 12, 20, 20, 8, 12, 64, 64, in);
        Gfx.tex(ctx, skin, -4, -24, 8, 12, 20, 36, 8, 12, 64, 64, in);
        // big gold chain with a J coin
        int gold = Gfx.alpha(0xFFFFD94A, in), dark = Gfx.alpha(0xFFB8860B, in);
        int cy = -24;
        for (int i = 0; i < 4; i++) {
            ctx.fill(-4 + i, cy + i, -3 + i, cy + i + 1, (i % 2 == 0) ? gold : dark);
            ctx.fill(3 - i, cy + i, 4 - i, cy + i + 1, (i % 2 == 0) ? gold : dark);
        }
        ctx.fill(-1, cy + 4, 1, cy + 6, gold);
        ctx.fill(0, cy + 4, 1, cy + 5, Gfx.alpha(0xFFB11A33, in));
        // arms resting on the counter, slightly flared
        for (int side = 0; side < 2; side++) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(side == 0 ? -6 : 6, -23, 0);
            ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side == 0 ? 14f : -14f));
            if (side == 0) {
                Gfx.tex(ctx, skin, -2, 0, 4, 12, 44, 20, 4, 12, 64, 64, in);
                Gfx.tex(ctx, skin, -2, 0, 4, 12, 44, 36, 4, 12, 64, 64, in);
            } else {
                Gfx.tex(ctx, skin, -2, 0, 4, 12, 36, 52, 4, 12, 64, 64, in);
                Gfx.tex(ctx, skin, -2, 0, 4, 12, 52, 52, 4, 12, 64, 64, in);
            }
            ctx.getMatrices().pop();
        }
        // head (shakes when angry, tilts when hovered)
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, -24, 0);
        float tilt = (hover ? -6f : 3f) + (anger >= 4 ? (float) Math.sin(now / 30.0) * 4f : 0f);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
        Gfx.tex(ctx, skin, -4, -8, 8, 8, 8, 8, 8, 8, 64, 64, in);
        Gfx.tex(ctx, skin, -4, -8, 8, 8, 40, 8, 8, 8, 64, 64, in);
        // angry eyebrows
        int brow = Gfx.alpha(0xFF2A1A10, in);
        ctx.fill(-4, -6, -2, -5, brow);
        ctx.fill(-2, -5, -1, -4, brow);
        ctx.fill(2, -6, 4, -5, brow);
        ctx.fill(1, -5, 2, -4, brow);
        // cool shades (slide down when he's furious so you see the red glare)
        int glassY = anger >= 4 ? -3 : -4;
        int black = Gfx.alpha(0xFF0E0E12, in);
        if (anger >= 4) {
            ctx.fill(-3, -4, -2, -3, Gfx.alpha(0xFFFF2020, in));
            ctx.fill(2, -4, 3, -3, Gfx.alpha(0xFFFF2020, in));
        }
        ctx.fill(-4, glassY, -1, glassY + 2, black);
        ctx.fill(1, glassY, 4, glassY + 2, black);
        ctx.fill(-1, glassY, 1, glassY + 1, black);
        ctx.fill(-5, glassY, -4, glassY + 1, black);
        ctx.fill(4, glassY, 5, glassY + 1, black);
        float shine = (float) ((now / 1800.0) % 1.0);
        if (shine < 0.15f) {
            int sx0 = -4 + Math.round(shine / 0.15f * 7);
            ctx.fill(sx0, glassY, sx0 + 1, glassY + 1, Gfx.alpha(0xCCFFFFFF, in));
        } else {
            ctx.fill(-4, glassY, -3, glassY + 1, Gfx.alpha(0x66FFFFFF, in));
            ctx.fill(1, glassY, 2, glassY + 1, Gfx.alpha(0x66FFFFFF, in));
        }
        // grumpy moustache
        int stache = Gfx.alpha(0xFF3B2A1E, in);
        ctx.fill(-3, -2, 3, -1, stache);
        ctx.fill(-4, -1, -2, 0, stache);
        ctx.fill(2, -1, 4, 0, stache);
        // crooked Santa hat
        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, -8, 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-16f));
        int red = Gfx.alpha(0xFFC8102E, in), redD = Gfx.alpha(0xFF8E0B20, in), white = Gfx.alpha(0xFFF6F4F7, in);
        ctx.fill(-4, -4, 4, -1, red);
        ctx.fill(-3, -6, 3, -4, red);
        ctx.fill(-1, -8, 3, -6, redD);
        ctx.fill(2, -9, 6, -7, redD);
        ctx.fill(5, -10, 7, -8, white);
        ctx.fill(-5, -1, 5, 1, white);
        ctx.getMatrices().pop();
        // anger marks
        int marks = Math.min(3, (int) anger);
        for (int m = 0; m < marks; m++) {
            float pulse = 1f + 0.25f * (float) Math.sin(now / 90.0 + m * 2);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(m == 0 ? 6 : m == 1 ? -7 : 7, m == 1 ? -9 : m == 0 ? -10 : -3, 0);
            ctx.getMatrices().scale(pulse * 0.5f, pulse * 0.5f, 1);
            angerMark(ctx, Gfx.alpha(0xFFE8202A, in));
            ctx.getMatrices().pop();
        }
        ctx.getMatrices().pop();
        // steam puffs when he's cross
        if (anger >= 2.5f) {
            for (int i = 0; i < 3; i++) {
                float t = ((now + i * 400) % 1200) / 1200f;
                int side = i % 2 == 0 ? -1 : 1;
                int a = Math.round((1 - t) * 160 * in);
                ctx.fill(Math.round(side * (5 + t * 4)), Math.round(-34 - t * 8), Math.round(side * (5 + t * 4)) + 2, Math.round(-32 - t * 8), (a << 24) | 0xDDDDDD);
            }
        }
        ctx.getMatrices().pop();
        ctx.getMatrices().pop();
    }

    /** The classic cartoon "vein" anger symbol: four little brackets around a centre. */
    private static void angerMark(DrawContext ctx, int c) {
        ctx.fill(-3, -3, -1, -2, c); ctx.fill(-2, -3, -1, -1, c);
        ctx.fill(1, -3, 3, -2, c); ctx.fill(1, -3, 2, -1, c);
        ctx.fill(-3, 2, -1, 3, c); ctx.fill(-2, 1, -1, 3, c);
        ctx.fill(1, 2, 3, 3, c); ctx.fill(1, 1, 2, 3, c);
    }

    private void drawSpeech(DrawContext ctx, float in, long now) {
        if (speech.isEmpty() || now - speechAt > 4200) return;
        float a = Math.min(1f, (now - speechAt) / 150f) * (now - speechAt > 3700 ? (4200 - (now - speechAt)) / 500f : 1f);
        boolean rage = rageAt > 0 && now - rageAt < 1500;
        int limit = panel == Panel.NONE ? width : panelX();
        int tw = Math.min(Math.max(60, limit - 20), textRenderer.getWidth(speech) + 12);
        int headR = sx(KX + 5 * KS), headL = sx(KX - 5 * KS), headY = sy(KFEET - 30 * KS);
        int bx, by = Math.max(28, headY - 8);
        boolean right = headR + 8 + tw <= limit - 4;
        if (right) bx = headR + 8;
        else if (headL - 8 - tw >= 4) bx = headL - 8 - tw;
        else { bx = Math.max(4, Math.min(limit - tw - 4, sx(KX) - tw / 2)); by = Math.max(ShopApi.signedIn ? 30 : 50, sy(KFEET - 40 * KS) - 18); }
        Gfx.round(ctx, bx, by, tw, 16, Gfx.alpha(rage ? 0xFFFFE0E0 : 0xFFFFFFFF, a * in));
        Gfx.outline(ctx, bx, by, tw, 16, Gfx.alpha(rage ? 0xFFC8102E : 0xFF1A2414, a * in));
        int tailX = bx == headR + 8 ? bx - 3 : bx + tw;
        if (bx == headR + 8 || bx == headL - 8 - tw) ctx.fill(tailX, by + 6, tailX + 3, by + 10, Gfx.alpha(0xFFFFFFFF, a * in));
        Gfx.text(ctx, Gfx.trim(speech, tw - 10), bx + 6, by + 4, Gfx.alpha(rage ? 0xFFC8102E : 0xFF1A2414, a * in), false);
    }

    private void drawBalance(DrawContext ctx, float in) {
        int x = 6, y = 6;
        String txt = ShopApi.signedIn ? String.format("%,d", ShopApi.coins) + " Jolly Coins" : "Not signed in";
        int w = textRenderer.getWidth(txt) + 30;
        Gfx.round(ctx, x, y, w, 18, Gfx.alpha(0xF0192C0C, in));
        Gfx.outline(ctx, x, y, w, 18, Gfx.alpha(Gfx.GOLD, in));
        float bob = (float) Math.sin(Gfx.now() / 300.0);
        Gfx.tex(ctx, COIN, x + 4, y + 1 + Math.round(bob), 16, 16, 0, 0, 16, 16, 16, 16, in);
        Gfx.text(ctx, txt, x + 24, y + 5, Gfx.alpha(Gfx.GOLD_LIGHT, in), true);
        if (ShopApi.testMode) Gfx.text(ctx, "TEST MODE - no real money", x, y + 22, 0xFFFF9A9A, true);
        Gfx.text(ctx, "Jolly Shop", width - textRenderer.getWidth("Jolly Shop") - 6, 10, Gfx.alpha(Gfx.GOLD_LIGHT, in), true);
    }

    private void panelBox(DrawContext ctx, String title, String sub) {
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        Gfx.round(ctx, px, py, pw, ph, 0xF00E1A0B);
        Gfx.outline(ctx, px, py, pw, ph, Gfx.GOLD);
        Gfx.text(ctx, title, px + 8, py + 7, Gfx.GOLD_LIGHT, true);
        if (sub != null) Gfx.text(ctx, sub, px + pw - 8 - textRenderer.getWidth(sub), py + 7, Gfx.MUTED, true);
        ctx.fill(px + 6, py + 19, px + pw - 6, py + 20, 0x55C99E02);
    }

    // items: left half = try-on preview + buy button, right half = scrolling list
    private static final int ROW = 30;

    private int prevW() { return panelW() * 2 / 5; }
    private int listX() { return panelX() + 6 + prevW() + 4; }
    private int listW() { return panelW() - prevW() - 16; }

    private void drawItems(DrawContext ctx, int mouseX, int mouseY, float delta) {
        panelBox(ctx, "Premium cosmetics", null);
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        // preview
        int lw = prevW();
        Gfx.round(ctx, px + 6, py + 24, lw, ph - 52, 0xC0091408);
        spin += delta * 2.2f;
        if (selected != null) {
            Cosmetics.Type t = typeOf(selected);
            String id = idOf(selected);
            ClientPlayerEntity p = client.player;
            if (p != null && t != Cosmetics.Type.NAME_FX) {
                drawSpinning(ctx, px + 6 + lw / 2, py + ph - 60, Math.min(52, (ph - 100) / 2), p);
            } else if (t == Cosmetics.Type.NAME_FX) {
                var styled = NameStyle.style(client.getSession().getUsername(), Cosmetics.selected(Cosmetics.Type.NAME_COLOR), id, true);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(px + 6 + lw / 2f, py + 24 + (ph - 52) / 2f - 8, 0);
                ctx.getMatrices().scale(1.6f, 1.6f, 1);
                ctx.drawText(textRenderer, styled, -textRenderer.getWidth(styled) / 2, 0, 0xFFFFFFFF, true);
                ctx.getMatrices().pop();
            } else {
                drawIcon(ctx, selected, px + 6 + lw / 2 - 32, py + 40, 64);
            }
            float ts = Math.max(0.7f, Math.min(1f, (lw - 6) / (float) Math.max(1, textRenderer.getWidth(selected.name()))));
            ctx.getMatrices().push();
            ctx.getMatrices().translate(px + 6 + lw / 2f, py + 28, 0);
            ctx.getMatrices().scale(ts, ts, 1);
            String nm = Gfx.trim(selected.name(), Math.round((lw - 6) / ts));
            Gfx.text(ctx, nm, -textRenderer.getWidth(nm) / 2, 0, Gfx.GOLD_LIGHT, true);
            ctx.getMatrices().pop();
            if (p != null && t == Cosmetics.Type.TRAIL) Gfx.centered(ctx, "(trails show in the world)", px + 6 + lw / 2, py + 40, Gfx.MUTED);
        } else {
            Gfx.centered(ctx, "Pick something to", px + 6 + lw / 2, py + ph / 2 - 10, Gfx.MUTED);
            Gfx.centered(ctx, "try it on!", px + 6 + lw / 2, py + ph / 2, Gfx.MUTED);
        }
        // list
        int lx = listX(), lwid = listW(), top = py + 24, bottom = py + ph - 6;
        int total = ShopApi.ITEMS.size() * ROW;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, total - (bottom - top))));
        ctx.enableScissor(lx, top, lx + lwid, bottom);
        for (int i = 0; i < ShopApi.ITEMS.size(); i++) {
            ShopApi.Item it = ShopApi.ITEMS.get(i);
            int y = top + i * ROW - (int) scroll;
            if (y + ROW < top || y > bottom) continue;
            boolean hov = mouseX >= lx && mouseX < lx + lwid && mouseY >= y && mouseY < y + ROW - 2 && mouseY >= top && mouseY < bottom;
            boolean sel = it == selected;
            boolean owned = ShopApi.owns(it.id());
            Gfx.round(ctx, lx, y, lwid, ROW - 2, sel ? 0xF0244A1C : hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, lx, y, lwid, ROW - 2, sel ? Gfx.GOLD_LIGHT : hov ? Gfx.GOLD : 0x55C99E02);
            drawIcon(ctx, it, lx + 3, y + 2, 24);
            float ns = Math.max(0.75f, Math.min(1f, (lwid - 34) / (float) Math.max(1, textRenderer.getWidth(it.name()))));
            ctx.getMatrices().push();
            ctx.getMatrices().translate(lx + 30, y + 4, 0);
            ctx.getMatrices().scale(ns, ns, 1);
            Gfx.text(ctx, Gfx.trim(it.name(), Math.round((lwid - 34) / ns)), 0, 0, owned ? Gfx.GREEN_ON : Gfx.SNOW, true);
            ctx.getMatrices().pop();
            if (owned) Gfx.text(ctx, "Owned", lx + 30, y + 15, Gfx.GREEN_ON, false);
            else {
                Gfx.tex(ctx, COIN, lx + 30, y + 14, 9, 9, 0, 0, 16, 16, 16, 16, 1f);
                Gfx.text(ctx, String.valueOf(it.price()), lx + 42, y + 15, Gfx.GOLD_LIGHT, false);
            }
        }
        ctx.disableScissor();
    }

    private void drawIcon(DrawContext ctx, ShopApi.Item it, int x, int y, int size) {
        Cosmetics.Type t = typeOf(it);
        String id = idOf(it);
        if (t == Cosmetics.Type.CAPE) {
            String tex = Cosmetics.ANIMATED_CAPES.contains(id) ? id + "_f" + (Gfx.now() / 110 % 8) : id;
            int w = size * 10 / 16;
            Gfx.tex(ctx, Cosmetics.capeTexture(tex), x + (size - w) / 2, y, w, size, 4, 4, 40, 64, 256, 128, 1f);
        } else if (t == Cosmetics.Type.NAME_FX) {
            var styled = NameStyle.style("Aa", "white", id, false);
            Gfx.round(ctx, x, y, size, size, 0x80000000);
            ctx.drawText(textRenderer, styled, x + size / 2 - textRenderer.getWidth(styled) / 2, y + size / 2 - 4, 0xFFFFFFFF, true);
        } else {
            Gfx.tex(ctx, Cosmetics.icon(t, id), x, y, size, size, 0, 0, 32, 32, 32, 32, 1f);
        }
    }

    private void drawSpinning(DrawContext ctx, int x, int feetY, int size, ClientPlayerEntity e) {
        float bodyYaw = e.bodyYaw, yaw = e.getYaw(), pitch = e.getPitch(), prevHead = fr.darkerdawn.jaly.Compat.prevHeadYaw(e), head = e.headYaw, prevBody = fr.darkerdawn.jaly.Compat.prevBodyYaw(e);
        float angle = 180f + spin;
        e.bodyYaw = angle;
        fr.darkerdawn.jaly.Compat.setPrevYaws(e, angle, fr.darkerdawn.jaly.Compat.prevHeadYaw(e));
        e.setYaw(angle);
        e.setPitch(0);
        e.headYaw = angle;
        fr.darkerdawn.jaly.Compat.setPrevYaws(e, fr.darkerdawn.jaly.Compat.prevBodyYaw(e), angle);
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        float sc = size / e.getScale();
        InventoryScreen.drawEntity(ctx, x, feetY - size * e.getHeight() / 2f + 4, sc, new Vector3f(0, e.getHeight() / 2f, 0), rot, new Quaternionf(), e);
        e.bodyYaw = bodyYaw;
        fr.darkerdawn.jaly.Compat.setPrevYaws(e, prevBody, fr.darkerdawn.jaly.Compat.prevHeadYaw(e));
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.headYaw = head;
        fr.darkerdawn.jaly.Compat.setPrevYaws(e, fr.darkerdawn.jaly.Compat.prevBodyYaw(e), prevHead);
    }

    // coins: 4 packs as cards with growing coin piles
    private int[] packRect(int i) {
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        int cw = (pw - 18) / 2, chh = (ph - 60) / 2;
        return new int[]{px + 6 + (i % 2) * (cw + 6), py + 26 + (i / 2) * (chh + 6), cw, chh};
    }

    private void drawCoins(DrawContext ctx, int mouseX, int mouseY) {
        panelBox(ctx, "Get Jolly Coins", "100 coins = " + ShopApi.euros(100));
        long now = Gfx.now();
        for (int i = 0; i < ShopApi.PACKS.size() && i < 4; i++) {
            ShopApi.Pack p = ShopApi.PACKS.get(i);
            int[] r = packRect(i);
            boolean hov = mouseX >= r[0] && mouseX < r[0] + r[2] && mouseY >= r[1] && mouseY < r[1] + r[3];
            int lift = hov ? 2 : 0;
            Gfx.round(ctx, r[0], r[1] - lift, r[2], r[3], hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, r[0], r[1] - lift, r[2], r[3], hov ? Gfx.GOLD_LIGHT : 0x88C99E02);
            int pile = 3 + i * 4;
            int base = r[1] - lift + r[3] - 34;
            for (int c = 0; c < pile; c++) {
                int col = c % (3 + i), row = c / (3 + i);
                int cx = r[0] + r[2] / 2 - (3 + i) * 5 + col * 10 + (row % 2) * 5;
                int cyy = base - row * 6 + (hov ? (int) Math.round(Math.sin(now / 120.0 + c) * 1) : 0);
                Gfx.tex(ctx, COIN, cx, cyy, 11, 11, 0, 0, 16, 16, 16, 16, 1f);
            }
            int bonus = p.coins() - p.cents();
            if (bonus > 0) {
                String b = "+" + bonus + " bonus";
                int bw = textRenderer.getWidth(b) + 8;
                Gfx.round(ctx, r[0] + r[2] - bw - 3, r[1] - lift + 3, bw, 11, 0xFFB11A33);
                Gfx.text(ctx, b, r[0] + r[2] - bw + 1, r[1] - lift + 5, Gfx.SNOW, false);
            }
            Gfx.centered(ctx, String.format("%,d coins", p.coins()), r[0] + r[2] / 2, r[1] - lift + r[3] - 22, Gfx.GOLD_LIGHT);
            Gfx.centered(ctx, ShopApi.euros(p.cents()), r[0] + r[2] / 2, r[1] - lift + r[3] - 11, Gfx.SNOW);
        }
        int py = panelY(), ph = panelH(), px = panelX();
        Gfx.text(ctx, Gfx.trim("Paid securely with Stripe, in your browser.", panelW() - 12), px + 6, py + ph - 28, Gfx.MUTED, false);
        Gfx.text(ctx, Gfx.trim("Cosmetics only. JalyClient stays free!", panelW() - 12), px + 6, py + ph - 16, Gfx.MUTED, false);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button != 0) return false;
        if (panel == Panel.ITEMS) {
            int top = panelY() + 24, bottom = panelY() + panelH() - 6;
            if (mx >= listX() && mx < listX() + listW() && my >= top && my < bottom) {
                int i = (int) ((my - top + scroll) / ROW);
                if (i >= 0 && i < ShopApi.ITEMS.size()) {
                    selected = ShopApi.ITEMS.get(i);
                    preview(selected);
                    Sounds.play(Sounds.CLICK, 1.2f, 0.6f);
                    if (Math.random() < 0.35) say(pick(BROWSE));
                    clearAndInit();
                }
                return true;
            }
        }
        if (panel == Panel.COINS) {
            for (int i = 0; i < ShopApi.PACKS.size() && i < 4; i++) {
                int[] r = packRect(i);
                if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                    Sounds.play(Sounds.CHIME, 1f, 0.7f);
                    if (!ShopApi.signedIn) { say("Sign in first. I don't do IOUs."); return true; }
                    say("Don't spend it all at once. Actually, do.");
                    ShopApi.checkout(ShopApi.PACKS.get(i));
                    return true;
                }
            }
        }
        if (panel != Panel.NONE && mx >= panelX()) return true;
        if (overKeeper(mx, my)) {
            pokeShopkeeper();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (panel == Panel.ITEMS) { scroll -= v * 20; return true; }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (JalyClient.MENU_KEY != null && JalyClient.MENU_KEY.matchesKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        Cosmetics.PREVIEW.clear();
        Sounds.stopShopMusic(client);
        super.removed();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /** Smoke test helpers. */
    public void openItems(String id) { selected = ShopApi.item(id); open(Panel.ITEMS); }
    public void openCoins() { open(Panel.COINS); }
}
