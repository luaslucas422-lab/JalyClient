package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** The people (and rats) who made JalyClient. */
public class CreditsScreen extends JalyScreen {
    static final String[] RAT = {
        "..##........",
        ".#pp#.......",
        ".#ggg##.....",
        "#gegggg#..t.",
        "#gggggggg#t.",
        ".#nggggg#.t.",
        "..#g#.#g#tt."};
    static final String[] CHEESE_BLOCK = {"....#", "..##y", "#yyoy", "#yyyy", "#oyyy", "#####"};
    static final String[] CROWN = {"#.#.#", "#####", "#r#b#", "#####"};

    public CreditsScreen(Screen parent) {
        super("Credits", parent);
    }

    @Override
    protected int maxPanelHeight() { return 230; }

    @Override
    protected void build() {
        addDrawableChild(new JalyButton(px + pw / 2 - 60, py + ph - 28, 120, 20, Text.literal("MORE CREDITS"), b -> {
            Sounds.play(Sounds.JINGLE, 1.2f, 0.8f);
            client.setScreen(new MoreCreditsScreen(this));
        }));
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        int mid = px + pw / 2;
        int y = py + 38;
        // the maker
        float glow = 0.85f + 0.15f * (float) Math.sin(now / 300.0);
        Gfx.scaledText(ctx, "Lucas Ciugulea / DarkerDawnFR", mid - textRenderer.getWidth("Lucas Ciugulea / DarkerDawnFR") * 1.3f / 2f, y, 1.3f, Gfx.alpha(Gfx.GOLD_LIGHT, open * glow), true);
        Gfx.centered(ctx, "MAKER OF CLIENT", mid, y + 16, Gfx.alpha(Gfx.RED_LIGHT, open));
        Gfx.candy(ctx, mid - 70, y + 30, 140, 2, (int) (now / 80 % 8), open);
        // Jim Bob
        y += 44;
        Gfx.centered(ctx, "Jim Bob", mid, y, Gfx.alpha(Gfx.SNOW, open));
        Gfx.centered(ctx, "Did Nothing, js wanted to add him", mid, y + 11, Gfx.alpha(Gfx.MUTED, open));
        // CHEESE the rat
        y += 32;
        int bob = (int) Math.round(Math.sin(now / 150.0));
        Gfx.sprite(ctx, RAT, new int[]{0xFF3A3A44, 0xFF8E8E9A, 0xFFFF9AB0, 0xFF101010, 0xFFFF9AB0, 0xFFFF9AB0}, "#gpent", mid - 64, y - 2 + bob, 2, open);
        Gfx.sprite(ctx, CHEESE_BLOCK, new int[]{0xFFB8860B, 0xFFFFD94A, 0xFFE8B400}, "#yo", mid + 44, y, 2, open);
        Gfx.centered(ctx, "CHEESE", mid, y, Gfx.alpha(0xFFFFD94A, open));
        Gfx.centered(ctx, "For Being a Rat", mid, y + 11, Gfx.alpha(Gfx.MUTED, open));
    }

    /** "MORE CREDITS": King Bob the 3, a googolplex times. It never ends. */
    public static class MoreCreditsScreen extends JalyScreen {
        private double scroll = 170; // start with the screen already full of King Bobs
        private long last = -1;
        private double speed = 1;

        public MoreCreditsScreen(Screen parent) {
            super("MORE CREDITS", parent);
        }

        @Override
        protected void build() {
            addDrawableChild(new JalyButton(px + pw - 76, py + 6, 70, 14, Text.literal("Faster!"), b -> speed = Math.min(64, speed * 2)));
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
            long now = Gfx.now();
            if (last > 0) scroll += (now - last) / 1000.0 * 26 * speed;
            last = now;
            int top = cy() + 4, bottom = py + ph - 24, mid = px + pw / 2;
            int row = 16;
            ctx.enableScissor(px + 4, top, px + pw - 4, bottom);
            long first = (long) (scroll / row);
            for (long i = Math.max(0, first - (bottom - top) / row - 2); i <= first + 1; i++) {
                int y = (int) Math.round(top + (i * row - scroll) + (bottom - top));
                if (y < top - row || y > bottom) continue;
                String line = "KING BOB THE 3";
                int w = textRenderer.getWidth(line);
                int hue = Gfx.BULBS[(int) (i % Gfx.BULBS.length)];
                Gfx.sprite(ctx, CROWN, new int[]{0xFFFFD94A, 0xFFE0384F, 0xFF4CC3FF}, "#rb", mid - w / 2 - 14, y + 1, 2, open);
                Gfx.text(ctx, line, mid - w / 2, y + 2, Gfx.alpha(hue, open), true);
                Gfx.text(ctx, "#" + String.format("%,d", i + 1), mid + w / 2 + 8, y + 2, Gfx.alpha(Gfx.MUTED, open), false);
            }
            ctx.disableScissor();
            long shown = Math.max(0, (long) ((scroll + (bottom - top)) / row));
            String progress = String.format("%,d of a googolplex (10^10^100). Almost done!%s", shown, speed > 1 ? " (" + (int) speed + "x)" : "");
            Gfx.centered(ctx, Gfx.trim(progress, pw - 16), mid, py + ph - 16, Gfx.alpha(Gfx.GOLD_LIGHT, open));
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double h, double v) {
            scroll = Math.max(0, scroll - v * 48);
            return true;
        }
    }
}
