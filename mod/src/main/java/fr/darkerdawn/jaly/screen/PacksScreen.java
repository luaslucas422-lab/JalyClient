package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.Sounds;
import fr.darkerdawn.jaly.ui.Gfx;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** The two built-in Christmas resource packs, one click each. */
public class PacksScreen extends JalyScreen {
    private static final Identifier ICON = Identifier.of("jalyclient", "icon.png");
    record Pack(String key, String name, String desc, int accent) {}

    private static final Pack[] PACKS = {
        new Pack("jaly_christmas", "Jaly Christmas", "Candy cane swords, festive hotbar, ornament apples, gingerbread totems.", 0xFFB11A33),
        new Pack("jaly_winter", "Winter Wonderland", "Snow on every grass block and frosted leaves on every tree.", 0xFF9BC3E7),
        new Pack("jaly_frost", "Frost PvP", "Icy swords, ice-crystal hearts, frozen hotbar and XP bar.", 0xFF5AA0D8),
        new Pack("jaly_candy", "Candy Land", "Lollipop swords, candy hearts, caramel apples, pastel hotbar.", 0xFFFF6FAE),
    };

    public PacksScreen(Screen parent) {
        super("Christmas Packs", parent);
    }

    @Override
    protected void build() {
        addDrawableChild(new JalyButton(px + pw / 2 - 70, py + ph - 26, 140, 18, Text.literal("All resource packs..."), b ->
            client.setScreen(new PackScreen(client.getResourcePackManager(), m -> {
                client.options.refreshResourcePacks(m);
                client.setScreen(this);
            }, client.getResourcePackDir(), Text.translatable("resourcePack.title")))));
    }

    public static String profileId(ResourcePackManager m, String key) {
        for (String id : m.getIds()) if (id.endsWith(key)) return id;
        return null;
    }

    public static boolean enabled(ResourcePackManager m, String key) {
        String id = profileId(m, key);
        return id != null && m.getEnabledIds().contains(id);
    }

    public static void setEnabled(net.minecraft.client.MinecraftClient client, String key, boolean on) {
        ResourcePackManager m = client.getResourcePackManager();
        String id = profileId(m, key);
        if (id == null) return;
        List<String> ids = new ArrayList<>(m.getEnabledIds());
        if (on && !ids.contains(id)) ids.add(id);
        if (!on) ids.remove(id);
        m.setEnabledProfiles(ids);
        client.options.refreshResourcePacks(m);
    }

    private int cardW() { return (cw() - 26) / 2; }

    private int cardX(int i) { return cx() + 8 + (i % 2) * (cardW() + 10); }

    private int cardY(int i) { return cy() + 2 + (i / 2) * 82; }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ResourcePackManager m = client.getResourcePackManager();
        for (int i = 0; i < PACKS.length; i++) {
            Pack p = PACKS[i];
            int x = cardX(i), y = cardY(i), w = cardW(), h = 76;
            boolean on = enabled(m, p.key);
            boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            Gfx.round(ctx, x, y - (hov ? 1 : 0), w, h, hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, x, y - (hov ? 1 : 0), w, h, on ? Gfx.GOLD_LIGHT : 0x66C99E02);
            ctx.fill(x + 1, y + 1, x + 4, y + h - 1, p.accent);
            Gfx.tex(ctx, Identifier.of("jalyclient", "textures/gui/pack_" + p.key + ".png"), x + 8, y + 8, 32, 32, 0, 0, 64, 64, 64, 64, 1f);
            Gfx.text(ctx, Gfx.trim(p.name, w - 80), x + 46, y + 9, Gfx.GOLD_LIGHT, true);
            int ty = y + 24;
            for (var line : textRenderer.wrapLines(Text.literal(p.desc), w - 54)) { if (ty > y + h - 12) break; ctx.drawText(textRenderer, line, x + 46, ty, 0xFFB8C2B0, false); ty += 10; }
            Gfx.toggle(ctx, x + w - 26, y + 8, on ? 1f : 0f, 1f);
        }
        Gfx.centered(ctx, "Packs reload when you switch them (takes a few seconds)", width / 2, cardY(4) + 2, 0xFF8FA088);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        for (int i = 0; i < PACKS.length; i++) {
            int x = cardX(i), y = cardY(i), w = cardW();
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 76) {
                boolean on = !enabled(client.getResourcePackManager(), PACKS[i].key);
                Sounds.play(on ? Sounds.CHIME : Sounds.CLICK, 1.2f, 0.6f);
                setEnabled(client, PACKS[i].key, on);
                return true;
            }
        }
        return false;
    }
}
