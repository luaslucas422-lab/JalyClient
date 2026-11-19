package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.screen.PacksScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Turns the grass and rain resource packs on and off to match the Visuals settings (one reload at most). */
public final class VisualPacks {
    private VisualPacks() {}

    public static void sync() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        JalyConfig c = JalyConfig.get();
        boolean grass = Mods.on("tall_grass"), rain = Mods.on("better_rain");
        Map<String, Boolean> want = Map.of(
            "jaly_grass", grass && !"xl".equals(c.grassHeight),
            "jaly_grass_xl", grass && "xl".equals(c.grassHeight),
            "jaly_rain", rain && !"blocky".equals(c.rainStyle),
            "jaly_rain_blocky", rain && "blocky".equals(c.rainStyle));
        ResourcePackManager m = mc.getResourcePackManager();
        List<String> ids = new ArrayList<>(m.getEnabledIds());
        boolean changed = false;
        for (var e : want.entrySet()) {
            String id = PacksScreen.profileId(m, e.getKey());
            if (id == null) continue;
            if (e.getValue() && !ids.contains(id)) { ids.add(id); changed = true; }
            if (!e.getValue() && ids.remove(id)) changed = true;
        }
        if (!changed) return;
        m.setEnabledProfiles(ids);
        mc.options.refreshResourcePacks(m);
    }
}
