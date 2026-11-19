package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;

import java.util.Set;

/** Tall Smooth Grass: the grass sway animation speeds up with wind, rain and storms (and can be frozen for less lag). */
public final class WindGrass {
    private WindGrass() {}

    private static final Set<String> GRASS = Set.of("block/short_grass", "block/fern", "block/tall_grass_bottom", "block/tall_grass_top");

    public static boolean isGrass(Identifier id) {
        return id.getNamespace().equals("minecraft") && GRASS.contains(id.getPath());
    }

    static float speed() {
        JalyConfig c = JalyConfig.get();
        if (!Mods.on("tall_grass") || !c.grassAnimate) return 0f;
        float s = switch (c.grassWind) { case "calm" -> 0.45f; case "windy" -> 1.8f; default -> 1f; };
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null) s *= 1f + mc.world.getRainGradient(1f) * 0.9f + mc.world.getThunderGradient(1f) * 1.2f;
        return Math.min(4f, s);
    }

    /** Wraps the vanilla animator and ticks it faster or slower. */
    public static final class Animator implements net.minecraft.client.texture.Animator {
        private final net.minecraft.client.texture.Animator inner;
        private float acc;

        public Animator(net.minecraft.client.texture.Animator inner) { this.inner = inner; }

        //#if >=1.21.5
        @Override
        public void tick(int x, int y, com.mojang.blaze3d.textures.GpuTexture tex) {
            acc += speed();
            int n = 0;
            while (acc >= 1f && n++ < 5) {
                inner.tick(x, y, tex);
                acc -= 1f;
            }
        }
        //#else
        @Override
        public void tick(int x, int y) {
            acc += speed();
            int n = 0;
            while (acc >= 1f && n++ < 5) {
                inner.tick(x, y);
                acc -= 1f;
            }
        }
        //#endif

        @Override
        public void close() { inner.close(); }
    }
}
