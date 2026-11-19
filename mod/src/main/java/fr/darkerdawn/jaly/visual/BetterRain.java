package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.ui.Gfx;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Better Rain: thick rain falling right in front of you (realistic or blocky), splashes, and drops on your "camera". */
public final class BetterRain {
    private BetterRain() {}

    private static final Random RNG = new Random();
    private static final List<float[]> LENS = new ArrayList<>(); // x, y, size, speed, age
    private static float strength;

    /** How hard it is raining where you stand (0 = not at all). */
    public static float rainHere(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return 0f;
        float g = mc.world.getRainGradient(1f);
        if (g <= 0f) return 0f;
        BlockPos p = mc.player.getBlockPos();
        if (fr.darkerdawn.jaly.Compat.precipitation(mc.world, p) != Biome.Precipitation.RAIN) return 0f;
        return g * (mc.world.isThundering() ? 1.4f : 1f);
    }

    public static void tick(MinecraftClient mc) {
        if (!Mods.on("better_rain") || mc.world == null || mc.player == null) {
            LENS.clear();
            return;
        }
        strength = rainHere(mc);
        JalyConfig c = JalyConfig.get();
        if (strength <= 0) return;
        // splashes on the ground around you
        if (c.rainSplashes) {
            int n = (int) (6 * strength * c.rainDensity);
            for (int i = 0; i < n; i++) {
                int x = MathHelper.floor(mc.player.getX() + RNG.nextGaussian() * 6), z = MathHelper.floor(mc.player.getZ() + RNG.nextGaussian() * 6);
                int y = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
                if (Math.abs(y - mc.player.getY()) > 12) continue;
                fr.darkerdawn.jaly.Compat.particle(mc.world, RNG.nextInt(3) == 0 ? ParticleTypes.SPLASH : ParticleTypes.RAIN, x + RNG.nextDouble(), y + 0.05, z + RNG.nextDouble(), 0, 0, 0);
            }
        }
        // drops on the "lens" when you're out in it and looking up
        boolean exposed = mc.world.isSkyVisible(mc.player.getBlockPos().up());
        if (c.rainLens && exposed && mc.options.getPerspective().isFirstPerson()) {
            float up = MathHelper.clamp((-mc.player.getPitch() + 20f) / 70f, 0f, 1f);
            if (RNG.nextFloat() < 0.25f * strength * (0.25f + up) && LENS.size() < 40) {
                LENS.add(new float[]{RNG.nextFloat(), RNG.nextFloat() * 0.8f, 2 + RNG.nextInt(4), 0, 0});
            }
        }
        for (int i = LENS.size() - 1; i >= 0; i--) {
            float[] d = LENS.get(i);
            d[4]++;
            if (d[4] > 30 && RNG.nextFloat() < 0.04f) d[3] = 0.004f + RNG.nextFloat() * 0.01f; // starts running down
            d[1] += d[3];
            if (d[1] > 1.1f || d[4] > 240) LENS.remove(i);
        }
    }

    /** Streaks of rain right around you, so it really looks like rain in front of your face. */
    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Mods.on("better_rain") || mc.world == null || mc.player == null || strength <= 0 || !JalyConfig.get().rainNear) return;
        JalyConfig c = JalyConfig.get();
        boolean blocky = "blocky".equals(c.rainStyle);
        Vec3d cam = ctx.camera().getPos();
        float yaw = ctx.camera().getYaw();
        double ry = Math.toRadians(yaw);
        float rx = (float) Math.cos(ry), rz = (float) Math.sin(ry); // camera right vector (horizontal)
        int count = (int) (160 * c.rainDensity * Math.min(1.4f, strength));
        long ms = System.currentTimeMillis();
        WorldDraw.colored(false, true, b -> {
            Random r = new Random(1234);
            for (int i = 0; i < count; i++) {
                float ox = (r.nextFloat() - 0.5f) * 16f, oz = (r.nextFloat() - 0.5f) * 16f;
                float speed = blocky ? 9f + r.nextFloat() * 3 : 13f + r.nextFloat() * 5;
                float len = blocky ? 0.22f : 0.55f + r.nextFloat() * 0.5f;
                float wdt = blocky ? 0.09f : 0.012f + r.nextFloat() * 0.01f;
                double wx = Math.floor(cam.x) + ox, wz = Math.floor(cam.z) + oz;
                float cycle = 14f;
                float fall = ((ms / 1000f) * speed + r.nextFloat() * cycle) % cycle;
                double wy = Math.floor(cam.y) + 8 - fall;
                int top = mc.world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(wx), MathHelper.floor(wz));
                if (wy < top) continue; // under a roof or below ground: no rain here
                float x = (float) (wx - cam.x), y = (float) (wy - cam.y), z = (float) (wz - cam.z);
                float d2 = x * x + z * z;
                if (d2 < 0.6f) continue;
                float a = (blocky ? 0.75f : 0.32f) * Math.min(1f, (1f - d2 / 70f) * 1.3f);
                if (a <= 0.02f) continue;
                int top0 = blocky ? WorldDraw.argb(a, 0.42f, 0.6f, 0.9f) : WorldDraw.argb(a * 0.2f, 0.82f, 0.88f, 0.96f);
                int bot0 = blocky ? WorldDraw.argb(a, 0.55f, 0.72f, 1f) : WorldDraw.argb(a, 0.82f, 0.88f, 0.96f);
                WorldDraw.v(b, x - rx * wdt, y + len, z - rz * wdt, top0);
                WorldDraw.v(b, x + rx * wdt, y + len, z + rz * wdt, top0);
                WorldDraw.v(b, x + rx * wdt, y, z + rz * wdt, bot0);
                WorldDraw.v(b, x - rx * wdt, y, z - rz * wdt, bot0);
            }
        });
    }

    /** Water drops on the screen when you look up into the rain. */
    public static void renderHud(DrawContext ctx) {
        if (LENS.isEmpty() || !Mods.on("better_rain")) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        boolean blocky = "blocky".equals(JalyConfig.get().rainStyle);
        for (float[] d : LENS) {
            int x = (int) (d[0] * w), y = (int) (d[1] * h), s = (int) d[2];
            float fade = Math.min(1f, d[4] / 6f) * (d[4] > 200 ? (240 - d[4]) / 40f : 1f);
            if (blocky) {
                ctx.fill(x, y, x + s, y + s, Gfx.alpha(0x8899C8FF, fade));
                ctx.fill(x, y, x + 1, y + 1, Gfx.alpha(0xCCFFFFFF, fade));
            } else {
                ctx.fill(x - s / 2, y - s, x + s / 2 + 1, y + s, Gfx.alpha(0x30DDEEFF, fade));
                ctx.fill(x - s, y - s / 2, x + s + 1, y + s / 2 + 1, Gfx.alpha(0x30DDEEFF, fade));
                ctx.fill(x - s / 2, y - s / 2, x, y, Gfx.alpha(0x88FFFFFF, fade));
                if (d[3] > 0) ctx.fill(x, (int) (y - d[4] * 0.4f), x + 1, y - s, Gfx.alpha(0x22DDEEFF, fade));
            }
        }
    }
}
