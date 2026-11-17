package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.cosmetic.Cosmetics;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The wardrobe: a spinning preview of you plus every hat and cape. All free. */
public class CosmeticsScreen extends JalyScreen {
    private static final int TILE_W = 62, TILE_H = 54, GAP = 5;
    private boolean capes;
    private float spin = 200f;
    private boolean dragging;
    private long last = -1;

    public CosmeticsScreen(Screen parent) {
        super("Cosmetics", parent);
    }

    @Override
    protected void build() {
        int x = gridX();
        addDrawableChild(new JalyButton(x, cy(), 50, 14, Text.literal("Hats"), b -> { capes = false; clearAndInit(); }).selected(!capes));
        addDrawableChild(new JalyButton(x + 54, cy(), 50, 14, Text.literal("Capes"), b -> { capes = true; clearAndInit(); }).selected(capes));
    }

    private int previewW() { return Math.min(150, cw() / 3); }

    private int gridX() { return cx() + previewW() + 10; }

    private int gridW() { return cx() + cw() - gridX(); }

    private int cols() { return Math.max(1, (gridW() + GAP) / (TILE_W + GAP)); }

    private List<Map.Entry<String, String>> items() { return new ArrayList<>((capes ? Cosmetics.CAPES : Cosmetics.HATS).entrySet()); }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Gfx.now();
        float dt = last < 0 ? 0 : Math.min(0.1f, (now - last) / 1000f);
        last = now;
        if (!dragging) spin += dt * 40f;
        int bx = cx(), by = cy(), bw = previewW(), bh = ch();
        Gfx.round(ctx, bx, by, bw, bh, 0xA0060E06);
        Gfx.outline(ctx, bx, by, bw, bh, 0x66C99E02);
        ctx.fillGradient(bx + 1, by + 1, bx + bw - 1, by + bh - 1, 0x30123060, 0x60000000);
        // snowy pedestal
        int pcx = bx + bw / 2, pby = by + bh - 22;
        Gfx.round(ctx, pcx - 34, pby, 68, 8, 0xFFEEF3F8);
        Gfx.round(ctx, pcx - 30, pby + 8, 60, 6, 0xFFC3D2E6);
        ClientPlayerEntity p = client.player;
        if (p != null) {
            ctx.enableScissor(bx + 1, by + 1, bx + bw - 1, by + bh - 1);
            drawSpinning(ctx, pcx, pby, Math.min(60, bh / 3), p);
            ctx.disableScissor();
        } else {
            String cape = JalyConfig.get().cape;
            if (!cape.equals("none")) Gfx.tex(ctx, Cosmetics.capeTexture(cape), pcx - 20, pby - 70, 40, 64, 4, 4, 40, 64, 256, 128, 1f);
            String hat = JalyConfig.get().hat;
            if (!hat.equals("none")) Gfx.tex(ctx, Cosmetics.hatIcon(hat), pcx - 16, pby - 108, 32, 32, 0, 0, 32, 32, 32, 32, 1f);
            Gfx.centered(ctx, "Join a world to see", pcx, by + 6, 0xFF8FA088);
            Gfx.centered(ctx, "yourself spin!", pcx, by + 16, 0xFF8FA088);
        }
        Gfx.centered(ctx, "Drag to spin", pcx, by + bh - 10, 0xFF6F8068);
        // grid
        List<Map.Entry<String, String>> list = items();
        String selected = capes ? JalyConfig.get().cape : JalyConfig.get().hat;
        int gy = cy() + 20;
        for (int i = 0; i < list.size(); i++) {
            int x = gridX() + (i % cols()) * (TILE_W + GAP), y = gy + (i / cols()) * (TILE_H + GAP);
            String id = list.get(i).getKey();
            boolean sel = id.equals(selected);
            boolean hov = mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H;
            int lift = hov ? 2 : 0;
            Gfx.round(ctx, x, y - lift, TILE_W, TILE_H, sel ? 0xF0244A1C : hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, x, y - lift, TILE_W, TILE_H, sel ? Gfx.GOLD_LIGHT : hov ? 0xFFC99E02 : 0x55C99E02);
            if (id.equals("none")) {
                Gfx.centered(ctx, "-", x + TILE_W / 2, y - lift + 16, 0xFF8FA088);
            } else if (capes) {
                Gfx.tex(ctx, Cosmetics.capeTexture(id), x + TILE_W / 2 - 10, y - lift + 4, 20, 32, 4, 4, 40, 64, 256, 128, 1f);
            } else {
                Gfx.tex(ctx, Cosmetics.hatIcon(id), x + TILE_W / 2 - 16, y - lift + 4, 32, 32, 0, 0, 32, 32, 32, 32, 1f);
            }
            Gfx.centered(ctx, Gfx.trim(list.get(i).getValue(), TILE_W - 4), x + TILE_W / 2, y - lift + TILE_H - 13, sel ? Gfx.GOLD_LIGHT : Gfx.SNOW);
            if (sel) Gfx.sprite(ctx, Gfx.STAR, new int[]{Gfx.GOLD_LIGHT}, "#", x + TILE_W - 8, y - lift + 3, 1, 1f);
        }
        Gfx.text(ctx, "Everything is free!", gridX() + 112, cy() + 3, Gfx.GOLD_LIGHT, true);
    }

    /** Like the inventory player preview, but spinning so you can see your cape. */
    private void drawSpinning(DrawContext ctx, int x, int feetY, int size, ClientPlayerEntity e) {
        float bodyYaw = e.bodyYaw, yaw = e.getYaw(), pitch = e.getPitch(), prevHead = e.prevHeadYaw, head = e.headYaw, prevBody = e.prevBodyYaw;
        float angle = 180f + spin;
        e.bodyYaw = angle;
        e.prevBodyYaw = angle;
        e.setYaw(angle);
        e.setPitch(0);
        e.headYaw = angle;
        e.prevHeadYaw = angle;
        Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf cam = new Quaternionf();
        float scale = size / e.getScale();
        InventoryScreen.drawEntity(ctx, x, feetY - size * e.getHeight() / 2f + 4, scale, new Vector3f(0, e.getHeight() / 2f, 0), rot, cam, e);
        e.bodyYaw = bodyYaw;
        e.prevBodyYaw = prevBody;
        e.setYaw(yaw);
        e.setPitch(pitch);
        e.prevHeadYaw = prevHead;
        e.headYaw = head;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseX >= cx() && mouseX < cx() + previewW() && mouseY >= cy() && mouseY < cy() + ch()) {
            dragging = true;
            return true;
        }
        List<Map.Entry<String, String>> list = items();
        int gy = cy() + 20;
        for (int i = 0; i < list.size(); i++) {
            int x = gridX() + (i % cols()) * (TILE_W + GAP), y = gy + (i / cols()) * (TILE_H + GAP);
            if (mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H) {
                select(list.get(i).getKey());
                return true;
            }
        }
        return false;
    }

    public void select(String id) {
        if (capes) JalyConfig.get().cape = id; else JalyConfig.get().hat = id;
        JalyConfig.save();
        Sounds.play(Sounds.CHIME, 1.3f, 0.5f);
    }

    public void showCapes(boolean c) {
        capes = c;
        clearAndInit();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            spin += (float) deltaX * 2.5f;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
