package fr.darkerdawn.jaly.net;

import fr.darkerdawn.jaly.JalyClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;

/** Talks to Iris by reflection so JalyClient works with or without it. */
public final class Shaders {
    private Shaders() {}

    public static boolean irisLoaded() { return FabricLoader.getInstance().isModLoaded("iris"); }

    /** Picks a shader pack and turns shaders on. */
    public static void select(String packFile) {
        if (!irisLoaded()) return;
        try {
            Class<?> iris = Class.forName("net.irisshaders.iris.Iris");
            Object cfg = iris.getMethod("getIrisConfig").invoke(null);
            cfg.getClass().getMethod("setShaderPackName", String.class).invoke(cfg, packFile);
            cfg.getClass().getMethod("setShadersEnabled", boolean.class).invoke(cfg, true);
            cfg.getClass().getMethod("save").invoke(cfg);
            iris.getMethod("reload").invoke(null);
        } catch (Throwable t) {
            JalyClient.LOG.warn("Could not switch shader pack automatically (open Iris settings to pick it)", t);
        }
    }

    public static Screen settingsScreen(Screen parent) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object inst = api.getMethod("getInstance").invoke(null);
            return (Screen) api.getMethod("openMainIrisScreenObj", Object.class).invoke(inst, parent);
        } catch (Throwable t) {
            return null;
        }
    }
}
