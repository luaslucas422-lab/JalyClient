package fr.darkerdawn.jaly.screen;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Sounds;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Util;

import java.util.List;

/** General JalyClient settings, built on the same rows as the Advanced pages. */
public final class SettingsScreen {
    private SettingsScreen() {}

    public static Screen create(Screen parent) {
        JalyConfig c = JalyConfig.get();
        List<Mods.Setting> rows = List.of(
            new Mods.Cycle("Festive level", new String[]{"full", "cozy", "calm"}, new String[]{"Full Jolly", "Cozy", "Calm (no snow)"}, () -> c.intensity, v -> { c.intensity = v; JalyConfig.save(); }),
            new Mods.Toggle("Jaly main menu", () -> c.jalyTitle, v -> { c.jalyTitle = v; JalyConfig.save(); }),
            new Mods.Toggle("Jaly loading screen", () -> c.loadingScreen, v -> { c.loadingScreen = v; JalyConfig.save(); }),
            new Mods.Toggle("Snow in menus", () -> c.menuSnow, v -> { c.menuSnow = v; JalyConfig.save(); }),
            new Mods.Toggle("Menu music (Jingle Bells)", () -> c.menuMusic, v -> {
                c.menuMusic = v;
                JalyConfig.save();
                if (!v) Sounds.stopMenuMusic(MinecraftClient.getInstance());
            }),
            new Mods.Toggle("Button hover sounds", () -> c.hoverSounds, v -> { c.hoverSounds = v; JalyConfig.save(); }),
            new Mods.Slider("Effects volume", 0, 1, 0.05, () -> c.sfxVolume, v -> { c.sfxVolume = v.floatValue(); JalyConfig.save(); }, "%"),
            new Mods.Slider("HUD size", 0.5, 2, 0.05, () -> c.hudScale, v -> { c.hudScale = v.floatValue(); JalyConfig.save(); }, "x"),
            new Mods.Action("HUD positions", "Reset", () -> { c.hudPos.clear(); JalyConfig.save(); }),
            new Mods.Action("Config folder", "Open", () -> Util.getOperatingSystem().open(FabricLoader.getInstance().getConfigDir().toFile()))
        );
        return new ModSettingsScreen(parent, "Settings", "Everything in JalyClient is free. Make it as jolly as you like.", rows, null);
    }
}
