package fr.darkerdawn.jaly.screen;

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
    private Cosmetics.Type tab = Cosmetics.Type.HAT;
    private int gridTop;
    private double scroll;
    private float spin = 200f;
    private boolean dragging;
    private long last = -1;

    public CosmeticsScreen(Screen parent) {
        super("Cosmetics", parent);
    }

    @Override
    protected void build() {
        int x = gridX(), y = cy();
        for (Cosmetics.Type t : Cosmetics.Type.values()) {
            String label = t == Cosmetics.Type.NAME_COLOR ? "Name Colours" : t == Cosmetics.Type.NAME_FX ? "Name Effects" : t.label;
            int bw = textRenderer.getWidth(label) + 12;
            if (x + bw > gridX() + gridW()) { x = gridX(); y += 17; }
            addDrawableChild(new JalyButton(x, y, bw, 14, Text.literal(label), b -> { tab = t; clearAndInit(); }).selected(tab == t));
            x += bw + 4;
        }
        gridTop = y + 20;
    }

    private int previewW() { return Math.min(150, cw() / 3); }

    private int gridX() { return cx() + previewW() + 10; }

    private int gridW() { return cx() + cw() - gridX(); }

    private int cols() { return Math.max(1, (gridW() + GAP) / (TILE_W + GAP)); }

    private List<Map.Entry<String, String>> items() { return new ArrayList<>(Cosmetics.ALL.get(tab).entrySet()); }

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
            drawSpinning(ctx, pcx, pby - 6, Math.min(56, bh / 3), p);
            ctx.disableScissor();
        } else {
            String cape = Cosmetics.selected(Cosmetics.Type.CAPE);
            if (!cape.equals("none")) Gfx.tex(ctx, Cosmetics.capeTexture(cape), pcx - 20, pby - 70, 40, 64, 4, 4, 40, 64, 256, 128, 1f);
            String hat = Cosmetics.selected(Cosmetics.Type.HAT);
            if (!hat.equals("none")) Gfx.tex(ctx, Cosmetics.hatIcon(hat), pcx - 16, pby - 108, 32, 32, 0, 0, 32, 32, 32, 32, 1f);
            Gfx.centered(ctx, "Join a world to see", pcx, by + 6, 0xFF8FA088);
            Gfx.centered(ctx, "yourself spin!", pcx, by + 16, 0xFF8FA088);
        }
        Gfx.centered(ctx, "Drag to spin", pcx, by + bh - 10, 0xFF6F8068);
        // grid
        List<Map.Entry<String, String>> list = items();
        String selected = Cosmetics.selected(tab);
        int gy = gridTop - (int) scroll;
        int maxS = Math.max(0, ((list.size() + cols() - 1) / cols()) * (TILE_H + GAP) - (py + ph - 8 - gridTop));
        scroll = Math.max(0, Math.min(scroll, maxS));
        gy = gridTop - (int) scroll;
        ctx.enableScissor(gridX() - 2, gridTop - 3, gridX() + gridW() + 2, py + ph - 6);
        for (int i = 0; i < list.size(); i++) {
            int x = gridX() + (i % cols()) * (TILE_W + GAP), y = gy + (i / cols()) * (TILE_H + GAP);
            String id = list.get(i).getKey();
            boolean sel = id.equals(selected);
            boolean locked = !Cosmetics.unlocked(tab, id);
            boolean hov = mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H;
            int lift = hov ? 2 : 0;
            Gfx.round(ctx, x, y - lift, TILE_W, TILE_H, sel ? 0xF0244A1C : hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, x, y - lift, TILE_W, TILE_H, sel ? Gfx.GOLD_LIGHT : hov ? 0xFFC99E02 : 0x55C99E02);
            if (tab == Cosmetics.Type.NAME_COLOR || tab == Cosmetics.Type.NAME_FX) {
                String nm = client.getSession().getUsername();
                var styled = tab == Cosmetics.Type.NAME_COLOR ? fr.darkerdawn.jaly.cosmetic.NameStyle.style(nm, id, "none", true)
                    : fr.darkerdawn.jaly.cosmetic.NameStyle.style(nm, Cosmetics.selected(Cosmetics.Type.NAME_COLOR), id, true);
                ctx.getMatrices().push();
                ctx.getMatrices().translate(x + TILE_W / 2f, y - lift + 18, 0);
                float sc = Math.min(1f, (TILE_W - 6) / (float) Math.max(1, textRenderer.getWidth(styled)));
                ctx.getMatrices().scale(sc, sc, 1);
                ctx.drawText(textRenderer, styled, -textRenderer.getWidth(styled) / 2, 0, 0xFFFFFFFF, true);
                ctx.getMatrices().pop();
            } else if (id.equals("none")) {
                Gfx.centered(ctx, "-", x + TILE_W / 2, y - lift + 16, 0xFF8FA088);
            } else if (tab == Cosmetics.Type.CAPE) {
                Gfx.tex(ctx, Cosmetics.capeTexture(id), x + TILE_W / 2 - 10, y - lift + 4, 20, 32, 4, 4, 40, 64, 256, 128, locked ? 0.35f : 1f);
            } else {
                Gfx.tex(ctx, Cosmetics.icon(tab, id), x + TILE_W / 2 - 16, y - lift + 4, 32, 32, 0, 0, 32, 32, 32, 32, locked ? 0.35f : 1f);
            }
            if (locked) {
                int door = fr.darkerdawn.jaly.Festive.doorFor(tab.key + ":" + id);
                boolean nameTile = tab == Cosmetics.Type.NAME_COLOR || tab == Cosmetics.Type.NAME_FX;
                int ty0 = nameTile ? y - lift + 30 : y - lift + 14;
                Gfx.round(ctx, x + 4, ty0, TILE_W - 8, 12, 0xE0B11A33);
                Gfx.centered(ctx, "Gift " + door, x + TILE_W / 2, ty0 + 2, Gfx.SNOW);
            }
            Gfx.centered(ctx, Gfx.trim(list.get(i).getValue(), TILE_W - 4), x + TILE_W / 2, y - lift + TILE_H - 13, locked ? 0xFF8FA088 : sel ? Gfx.GOLD_LIGHT : Gfx.SNOW);
            if (sel) Gfx.sprite(ctx, Gfx.STAR, new int[]{Gfx.GOLD_LIGHT}, "#", x + TILE_W - 8, y - lift + 3, 1, 1f);
        }
        ctx.disableScissor();
        String tag = "All free! Red tags unlock from Gifts";
        Gfx.text(ctx, tag, px + pw - 8 - textRenderer.getWidth(tag), py + 12, Gfx.GOLD_LIGHT, true);
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
        int gy = gridTop - (int) scroll;
        for (int i = 0; i < list.size(); i++) {
            int x = gridX() + (i % cols()) * (TILE_W + GAP), y = gy + (i / cols()) * (TILE_H + GAP);
            if (mouseY < gridTop || mouseY > py + ph - 6) continue;
            if (mouseX >= x && mouseX < x + TILE_W && mouseY >= y && mouseY < y + TILE_H) {
                select(list.get(i).getKey());
                return true;
            }
        }
        return false;
    }

    public void select(String id) {
        if (!Cosmetics.unlocked(tab, id)) {
            Sounds.play(Sounds.CLICK, 0.6f, 0.5f);
            return;
        }
        Cosmetics.select(tab, id);
        Sounds.play(Sounds.CHIME, 1.3f, 0.5f);
    }

    public void showTab(Cosmetics.Type t) {
        tab = t;
        clearAndInit();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= verticalAmount * 24;
        return true;
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
