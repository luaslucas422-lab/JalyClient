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
        new Pack("jaly_christmas", "Jaly Christmas", "Candy cane swords, festive hotbar and XP bar, ornament golden apples, gingerbread totems, snow globe pearls.", 0xFFB11A33),
        new Pack("jaly_winter", "Jaly Winter Wonderland", "Snow on every grass block and frosted leaves on every tree. The whole world turns white.", 0xFF9BC3E7),
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

    private int cardY(int i) { return cy() + 4 + i * 66; }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ResourcePackManager m = client.getResourcePackManager();
        for (int i = 0; i < PACKS.length; i++) {
            Pack p = PACKS[i];
            int x = cx() + 10, y = cardY(i), w = cw() - 20, h = 58;
            boolean on = enabled(m, p.key);
            boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
            Gfx.round(ctx, x, y - (hov ? 1 : 0), w, h, hov ? 0xF0203A18 : 0xE0162A12);
            Gfx.outline(ctx, x, y - (hov ? 1 : 0), w, h, on ? Gfx.GOLD_LIGHT : 0x66C99E02);
            ctx.fill(x + 1, y + 1, x + 4, y + h - 1, p.accent);
            Gfx.tex(ctx, ICON, x + 8, y + 9, 40, 40, 0, 0, 128, 128, 128, 128, 1f);
            Gfx.text(ctx, p.name, x + 56, y + 8, Gfx.GOLD_LIGHT, true);
            int ty = y + 21;
            for (var line : textRenderer.wrapLines(Text.literal(p.desc), w - 120)) { ctx.drawText(textRenderer, line, x + 56, ty, 0xFFB8C2B0, false); ty += 10; }
            Gfx.toggle(ctx, x + w - 28, y + 8, on ? 1f : 0f, 1f);
            Gfx.text(ctx, on ? "ON" : "OFF", x + w - 48, y + 9, on ? Gfx.GREEN_ON : 0xFF8A8A8A, false);
        }
        Gfx.centered(ctx, "Packs reload when you switch them (takes a few seconds)", width / 2, cardY(2) + 2, 0xFF8FA088);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        for (int i = 0; i < PACKS.length; i++) {
            int x = cx() + 10, y = cardY(i), w = cw() - 20;
            if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + 58) {
                boolean on = !enabled(client.getResourcePackManager(), PACKS[i].key);
                Sounds.play(on ? Sounds.CHIME : Sounds.CLICK, 1.2f, 0.6f);
                setEnabled(client, PACKS[i].key, on);
                return true;
            }
        }
        return false;
    }
}
