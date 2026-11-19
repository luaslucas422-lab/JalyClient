package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import fr.darkerdawn.jaly.ui.Snow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

/** Base for every JalyClient page: night backdrop, snow, a pine-glass panel that pops in, a header and a back button. */
public abstract class JalyScreen extends Screen {
    protected final Screen parent;
    protected int px, py, pw, ph;
    protected float open;
    private long openedAt = -1;
    private static final Snow SNOW = new Snow(110, 3);

    protected JalyScreen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    protected int maxPanelWidth() { return 470; }

    protected int maxPanelHeight() { return 280; }

    @Override
    protected void init() {
        if (openedAt < 0) openedAt = Gfx.now();
        pw = Math.min(width - 16, maxPanelWidth());
        ph = Math.min(height - 16, maxPanelHeight());
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        addDrawableChild(new JalyButton(px + 6, py + 6, 44, 14, Text.literal("< Back"), b -> close()));
        build();
    }

    protected abstract void build();

    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    protected void drawOverlay(DrawContext ctx, int mouseX, int mouseY, float delta) {}

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float tOpen = Math.min(1f, (Gfx.now() - openedAt) / 260f);
        open = tOpen;
        if (client.world == null) {
            ctx.fillGradient(0, 0, width, height, 0xFF070D22, 0xFF1E3366);
        } else {
            ctx.fillGradient(0, 0, width, height, Gfx.alpha(0xB0040A14, tOpen), Gfx.alpha(0xD0081A10, tOpen));
        }
        SNOW.renderFull(ctx, width, height, 0.9f);
        float s = 0.92f + 0.08f * Gfx.easeOutBack(tOpen);
        Gfx.push(ctx);
        Gfx.translate(ctx, width / 2f, height / 2f);
        Gfx.scale(ctx, s, s);
        Gfx.translate(ctx, -width / 2f, -height / 2f);
        Gfx.panel(ctx, px, py, pw, ph, tOpen);
        Gfx.lights(ctx, px + 6, px + pw - 6, py + 1, 18, 1f, Gfx.now(), tOpen);
        String t = getTitle().getString();
        int tw = textRenderer.getWidth(t);
        Gfx.scaledText(ctx, t, px + (pw - tw * 1.5f) / 2f, py + 9, 1.5f, Gfx.alpha(Gfx.GOLD_LIGHT, tOpen), true);
        Gfx.candy(ctx, px + (pw - (int) (tw * 1.5f)) / 2, py + 23, (int) (tw * 1.5f), 2, (int) (Gfx.now() / 80 % 8), tOpen);
        Gfx.pop(ctx);
        for (Element e : children()) if (e instanceof ClickableWidget w) w.setAlpha(tOpen);
        drawContent(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawOverlay(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (JalyClient.MENU_KEY != null && JalyClient.MENU_KEY.matchesKey(keyCode, scanCode) && !(getFocused() instanceof net.minecraft.client.gui.widget.TextFieldWidget)) {
            client.setScreen(client.world != null ? null : parent);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    /** Content area below the header. */
    protected int cx() { return px + 10; }
    protected int cy() { return py + 32; }
    protected int cw() { return pw - 20; }
    protected int ch() { return ph - 42; }
}
