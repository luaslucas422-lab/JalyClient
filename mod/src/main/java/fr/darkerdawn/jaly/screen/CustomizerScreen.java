package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/** The Christmas gear: customise the main menu background, logo, splash, buttons, music and name card. */
public class CustomizerScreen extends JalyScreen {
    static final String[][] PANORAMAS = {
        {"valley", "Snowy Valley", "Animated"}, {"aurora", "Aurora Night", "Animated"}, {"vanilla", "Minecraft Panorama + Snow", "Animated"},
        {"lake", "Frozen Lake", "Animated"}, {"blizzard", "Blizzard Pines", "Animated"}, {"fireworks", "Fireworks Night", "Animated"}, {"gingertown", "Gingerbread Town", "Animated"},
        {"valley_still", "Snowy Valley (still)", "Still"}, {"lodge", "Cozy Lodge", "Still"}, {"candy", "Candy Stripes", "Still"}, {"midnight", "Midnight Sky", "Still"}};
    static final String[] LOGOS = {"jaly", "frost", "ginger", "minecraft"};
    static final String[] LOGO_NAMES = {"JalyClient", "Frosty", "Gingerbread", "Minecraft"};
    private int tab;
    private TextFieldWidget splashField;

    public CustomizerScreen(Screen parent) {
        super("Customize Menu", parent);
    }

    @Override
    protected int maxPanelWidth() { return 480; }

    @Override
    protected void build() {
        String[] tabs = {"Panorama", "Buttons", "Music & Name"};
        int x = cx();
        for (int i = 0; i < tabs.length; i++) {
            int w = textRenderer.getWidth(tabs[i]) + 16, idx = i;
            addDrawableChild(new JalyButton(x, cy(), w, 14, Text.literal(tabs[i]), b -> { tab = idx; clearAndInit(); }).selected(tab == i));
            x += w + 4;
        }
        JalyConfig c = JalyConfig.get();
        if (tab == 0) {
            int by = py + ph - 46;
            int li = java.util.Arrays.asList(LOGOS).indexOf(c.logoStyle);
            addDrawableChild(new JalyButton(cx(), by, 120, 14, Text.literal("Logo: " + LOGO_NAMES[Math.max(0, li)]), b -> {
                c.logoStyle = LOGOS[(Math.max(0, java.util.Arrays.asList(LOGOS).indexOf(c.logoStyle)) + 1) % LOGOS.length]; JalyConfig.save(); clearAndInit(); }));
            addDrawableChild(new JalyButton(cx() + 124, by, 96, 14, Text.literal("Splash: " + (c.splashOn ? "On" : "Off")), b -> {
                c.splashOn = !c.splashOn; JalyConfig.save(); clearAndInit(); }));
            addDrawableChild(new JalyButton(cx() + 224, by, 120, 14, Text.literal("Style: " + ("candy".equals(c.splashStyle) ? "Candy cane" : "Classic")), b -> {
                c.splashStyle = "candy".equals(c.splashStyle) ? "classic" : "candy"; JalyConfig.save(); clearAndInit(); }));
            splashField = new TextFieldWidget(textRenderer, cx(), by + 18, cw() - 4, 14, Text.literal("Splash"));
            splashField.setPlaceholder(Text.literal("Your own splash text (empty = random festive ones)"));
            splashField.setMaxLength(60);
            splashField.setText(c.splashText == null ? "" : c.splashText);
            splashField.setChangedListener(t -> { c.splashText = t; JalyConfig.save(); });
            addDrawableChild(splashField);
        } else if (tab == 1) {
            int cols = 4, bw = (cw() - (cols - 1) * 6) / cols;
            for (int i = 0; i < JalyButton.STYLES.length; i++) {
                String st = JalyButton.STYLES[i];
                int bx = cx() + (i % cols) * (bw + 6), by = cy() + 34 + (i / cols) * 44;
                addDrawableChild(new JalyButton(bx, by, bw, 20, Text.literal(JalyButton.STYLE_NAMES[i]), b -> {
                    c.buttonStyle = st; JalyConfig.save(); Sounds.play(Sounds.CHIME, 1.2f, 0.5f); clearAndInit(); }).style(st).selected(st.equals(c.buttonStyle)));
            }
            addDrawableChild(new JalyButton(cx(), py + ph - 28, 220, 18, Text.literal("Use on: " + ("everywhere".equals(c.buttonScope) ? "Every button in Minecraft" : "Main menu only")), b -> {
                c.buttonScope = "everywhere".equals(c.buttonScope) ? "main" : "everywhere"; JalyConfig.save(); clearAndInit(); }));
        } else {
            addDrawableChild(new JalyButton(cx(), cy() + 30, 200, 18, Text.literal("Open music & name settings"), b -> client.setScreen(musicAndName(this))));
        }
    }

    static Screen musicAndName(Screen parent) {
        JalyConfig c = JalyConfig.get();
        List<Mods.Setting> rows = List.of(
            new Mods.Cycle("Menu music", new String[]{"menu_music", "carol_deck", "carol_wish", "carol_silent", "none"},
                new String[]{"Jingle Bells", "Deck the Halls", "We Wish You...", "Silent Night", "Off"}, () -> c.menuTrack,
                v -> { c.menuTrack = v; JalyConfig.save(); Sounds.stopMenuMusic(MinecraftClient.getInstance()); }),
            new Mods.Cycle("Name card", new String[]{"candy", "gold", "off"}, new String[]{"Candy cane", "Gold shine", "Hidden"}, () -> c.playerCard, v -> { c.playerCard = v; JalyConfig.save(); }),
            new Mods.Toggle("Snow on the menu", () -> c.menuSnow, v -> { c.menuSnow = v; JalyConfig.save(); }),
            new Mods.Toggle("Button hover sounds", () -> c.hoverSounds, v -> { c.hoverSounds = v; JalyConfig.save(); })
        );
        return new ModSettingsScreen(parent, "Music & Name", "Pick the menu music and how your name card looks.", rows, null);
    }

    private int cardW() { return (cw() - 3 * 6) / 4; }

    private int[] cardPos(int i) { return new int[]{cx() + (i % 4) * (cardW() + 6), cy() + 22 + (i / 4) * 52}; }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        JalyConfig c = JalyConfig.get();
        if (tab == 0) {
            for (int i = 0; i < PANORAMAS.length; i++) {
                int[] p = cardPos(i);
                int w = cardW();
                boolean sel = PANORAMAS[i][0].equals(c.panorama);
                boolean hov = mouseX >= p[0] && mouseX < p[0] + w && mouseY >= p[1] && mouseY < p[1] + 46;
                Gfx.round(ctx, p[0], p[1] - (hov ? 1 : 0), w, 46, sel ? 0xF0244A1C : hov ? 0xF0203A18 : 0xE0162A12);
                Gfx.outline(ctx, p[0], p[1] - (hov ? 1 : 0), w, 46, sel ? Gfx.GOLD_LIGHT : 0x55C99E02);
                preview(ctx, PANORAMAS[i][0], p[0] + 3, p[1] + 3, w - 6, 24);
                Gfx.text(ctx, Gfx.trim(PANORAMAS[i][1], w - 8), p[0] + 4, p[1] + 29, sel ? Gfx.GOLD_LIGHT : Gfx.SNOW, true);
                boolean anim = PANORAMAS[i][2].equals("Animated");
                Gfx.text(ctx, PANORAMAS[i][2], p[0] + 4, p[1] + 38, anim ? 0xFF5CFF7A : 0xFF8FA088, false);
            }
        } else if (tab == 1) {
            Gfx.text(ctx, "Hover the buttons to see their animations", cx(), cy() + 20, 0xFF8FA088, false);
        } else {
            Gfx.text(ctx, "Change the menu music, your name card and more.", cx(), cy() + 18, 0xFF8FA088, false);
        }
    }

    private void preview(DrawContext ctx, String id, int x, int y, int w, int h) {
        long t = Gfx.now();
        if (fr.darkerdawn.jaly.ui.MenuScenes.has(id)) {
            ctx.enableScissor(x, y, x + w, y + h);
            Gfx.push(ctx);
            Gfx.translate(ctx, x, y);
            Gfx.scale(ctx, w / 240f, h / 135f);
            fr.darkerdawn.jaly.ui.MenuScenes.draw(ctx, id, 240, 135, t / 1000f, t);
            Gfx.pop(ctx);
            ctx.disableScissor();
            return;
        }
        switch (id) {
            case "valley", "valley_still" -> {
                ctx.fillGradient(x, y, x + w, y + h, 0xFF0B1530, 0xFF2B4A80);
                ctx.fill(x, y + h - 7, x + w, y + h, 0xFFEEF3F8);
                for (int i = 4; i < w; i += 11) ctx.fill(x + i, y + h - 13, x + i + 4, y + h - 7, 0xFF163824);
                if (id.equals("valley")) ctx.fill(x + (int) (t / 40 % w), y + 3, x + (int) (t / 40 % w) + 1, y + 4, 0xFFFFFFFF);
            }
            case "aurora" -> {
                ctx.fill(x, y, x + w, y + h, 0xFF060B1F);
                for (int i = 0; i < w; i += 2) {
                    int yy = y + 6 + (int) (Math.sin(i * 0.2 + t / 400.0) * 3);
                    ctx.fill(x + i, yy, x + i + 2, yy + 5, 0xAA3DFFB0);
                }
            }
            case "vanilla" -> {
                ctx.fill(x, y, x + w, y + h, 0xFF5C8A3A);
                ctx.fill(x, y, x + w, y + h / 2, 0xFF7FB6E8);
                ctx.fill(x + (int) (t / 60 % w), y + 3, x + (int) (t / 60 % w) + 2, y + 5, 0xFFFFFFFF);
            }
            case "lodge" -> Gfx.tex(ctx, net.minecraft.util.Identifier.of("jalyclient", "textures/gui/lodge.png"), x, y, w, h, 0, 40, 320, 100, 320, 180, 1f);
            case "candy" -> Gfx.candy(ctx, x, y, w, h, 0, 1f);
            default -> {
                ctx.fill(x, y, x + w, y + h, 0xFF050A1C);
                for (int i = 0; i < 12; i++) ctx.fill(x + (i * 13) % w, y + (i * 7) % h, x + (i * 13) % w + 1, y + (i * 7) % h + 1, 0xFFFFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (tab == 0) {
            for (int i = 0; i < PANORAMAS.length; i++) {
                int[] p = cardPos(i);
                if (mouseX >= p[0] && mouseX < p[0] + cardW() && mouseY >= p[1] && mouseY < p[1] + 46) {
                    JalyConfig.get().panorama = PANORAMAS[i][0];
                    JalyConfig.save();
                    Sounds.play(Sounds.CHIME, 1.2f, 0.5f);
                    return true;
                }
            }
        }
        return false;
    }
}
