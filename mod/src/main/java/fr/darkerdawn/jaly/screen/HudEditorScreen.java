package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.hud.Hud;
import fr.darkerdawn.jaly.hud.HudModule;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Drag HUD modules around. Snaps to edges and the centre. Right-click hides a module. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule dragging;
    private double offX, offY;
    private boolean snapX, snapY;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Edit HUD"));
        this.parent = parent;
        Mods.init();
    }

    @Override
    protected void init() {
        addDrawableChild(new JalyButton(width / 2 - 104, height - 26, 100, 18, Text.literal("Reset"), b -> { JalyConfig.get().hudPos.clear(); JalyConfig.save(); }));
        addDrawableChild(new JalyButton(width / 2 + 4, height - 26, 100, 18, Text.literal("Done"), b -> close()));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world == null) ctx.fillGradient(0, 0, width, height, 0xFF0B1530, 0xFF2B4A80);
        else ctx.fill(0, 0, width, height, 0x55000000);
        for (int x = 0; x < width; x += 16) ctx.fill(x, 0, x + 1, height, 0x10FFFFFF);
        for (int y = 0; y < height; y += 16) ctx.fill(0, y, width, y + 1, 0x10FFFFFF);
        if (dragging != null && snapX) ctx.fill(width / 2, 0, width / 2 + 1, height, 0xAAFDE68A);
        if (dragging != null && snapY) ctx.fill(0, height / 2, width, height / 2 + 1, 0xAAFDE68A);
        HudModule hov = at(mouseX, mouseY);
        for (HudModule m : Hud.MODULES) {
            if (!Mods.on(m.id)) continue;
            Hud.drawModule(ctx, client, m, width, height, true);
            int[] b = Hud.bounds(client, m, width, height, true);
            boolean active = m == dragging || m == hov;
            Gfx.outline(ctx, b[0] - 1, b[1] - 1, b[2] + 2, b[3] + 2, active ? Gfx.GOLD_LIGHT : 0x66FFFFFF);
            if (active) {
                int ty = b[1] > 12 ? b[1] - 11 : b[1] + b[3] + 2;
                Gfx.text(ctx, m.name, b[0], ty, Gfx.GOLD_LIGHT, true);
            }
        }
        Gfx.centered(ctx, "Drag modules to move them  |  Right-click to hide  |  Esc to save", width / 2, height / 2 - 4, 0x88FFFFFF);
    }

    private HudModule at(double mx, double my) {
        for (int i = Hud.MODULES.size() - 1; i >= 0; i--) {
            HudModule m = Hud.MODULES.get(i);
            if (!Mods.on(m.id)) continue;
            int[] b = Hud.bounds(client, m, width, height, true);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) return m;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        HudModule m = at(mouseX, mouseY);
        if (m == null) return false;
        if (button == 1) {
            Mods.byId(m.id).set(false);
            Sounds.play(Sounds.CLICK, 0.8f, 0.6f);
            return true;
        }
        int[] b = Hud.bounds(client, m, width, height, true);
        dragging = m;
        offX = mouseX - b[0];
        offY = mouseY - b[1];
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging == null) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        int[] b = Hud.bounds(client, dragging, width, height, true);
        double x = mouseX - offX, y = mouseY - offY;
        if (Math.abs(x) < 4) x = 0;
        if (Math.abs(y) < 4) y = 0;
        if (Math.abs(x + b[2] - width) < 4) x = width - b[2];
        if (Math.abs(y + b[3] - height) < 4) y = height - b[3];
        snapX = Math.abs(x + b[2] / 2.0 - width / 2.0) < 4;
        snapY = Math.abs(y + b[3] / 2.0 - height / 2.0) < 4;
        if (snapX) x = width / 2.0 - b[2] / 2.0;
        if (snapY) y = height / 2.0 - b[3] / 2.0;
        JalyConfig.get().hudPos.put(dragging.id, new float[]{(float) (x / width), (float) (y / height)});
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging != null) JalyConfig.save();
        dragging = null;
        snapX = snapY = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        JalyConfig.save();
        client.setScreen(parent);
    }
}
