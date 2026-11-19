package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
//#if >=1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//#endif
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Better Ambience: aurora borealis on some nights, shooting stars and a warm glow at sunrise/sunset. */
public final class Ambience {
    private Ambience() {}

    private static final Random RNG = new Random();
    private static float auroraFade, glowFade;
    private static long starStart = -1, nextStar;
    private static float starYaw, starPitch, starDir;
    private static long lastDay = Long.MIN_VALUE;
    private static boolean auroraTonight;

    /** 0..1 how much it's night (fades over dusk and dawn). */
    static float night(ClientWorld w) {
        long t = w.getTimeOfDay() % 24000L;
        if (t >= 13500 && t <= 22500) return 1f;
        if (t > 12500 && t < 13500) return (t - 12500) / 1000f;
        if (t > 22500 && t < 23500) return 1f - (t - 22500) / 1000f;
        return 0f;
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || !Mods.on("better_ambience")) return;
        JalyConfig c = JalyConfig.get();
        long day = (mc.world.getTimeOfDay() + 6000) / 24000L;
        if (day != lastDay) {
            lastDay = day;
            float chance = switch (c.auroraChance) { case "never" -> 0f; case "often" -> 0.6f; case "always" -> 1f; default -> 0.25f; };
            Random r = new Random(day * 31 + 7);
            float biome = mc.player != null && mc.world.getBiome(mc.player.getBlockPos()).value().getTemperature() < 0.3f ? 0.3f : 0f;
            auroraTonight = r.nextFloat() < Math.min(1f, chance + (chance > 0 ? biome : 0));
        }
        float clear = 1f - mc.world.getRainGradient(1f);
        boolean overworld = mc.world.getDimension().hasSkyLight() && !mc.world.getDimension().hasCeiling();
        float wantAurora = overworld && auroraTonight ? night(mc.world) * clear : 0f;
        auroraFade += (wantAurora - auroraFade) * 0.02f;
        long t = mc.world.getTimeOfDay() % 24000L;
        float sunset = Math.max(0f, 1f - Math.abs(t - 12450) / 1150f);
        float dawn = Math.max(0f, 1f - Math.abs(((t + 1000) % 24000L) - 500) / 1150f);
        float dusk = Math.max(sunset, dawn);
        glowFade += ((overworld && c.sunsetGlow ? Math.max(0, dusk) * clear : 0f) - glowFade) * 0.05f;
        long now = System.currentTimeMillis();
        if (c.shootingStars && overworld && night(mc.world) > 0.8f && clear > 0.8f && now > nextStar && starStart < 0) {
            starStart = now;
            starYaw = RNG.nextFloat() * 360f;
            starPitch = 25f + RNG.nextFloat() * 35f;
            starDir = RNG.nextBoolean() ? 1 : -1;
            nextStar = now + 6000 + RNG.nextInt(16000);
        }
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Mods.on("better_ambience") || mc.world == null) return;
        JalyConfig c = JalyConfig.get();
        float time = (System.currentTimeMillis() % 3_600_000L) / 1000f;
        float dist = Math.min(170f, mc.gameRenderer.getFarPlaneDistance() * 0.85f);
        if (auroraFade > 0.01f) {
            float k = auroraFade * c.auroraBrightness;
            WorldDraw.colored(true, true, b -> {
                for (int layer = 0; layer < 3; layer++) {
                    float ph = layer * 1.7f, z0 = -dist * (0.8f + layer * 0.08f);
                    float h0 = dist * (0.5f + layer * 0.06f), h1 = h0 + dist * (0.45f + 0.08f * MathHelper.sin(time * 0.1f + layer));
                    int seg = 48;
                    for (int i = 0; i < seg; i++) {
                        float a0 = (i / (float) seg - 0.5f) * 2.4f, a1 = ((i + 1) / (float) seg - 0.5f) * 2.4f;
                        float x0 = a0 * dist, x1 = a1 * dist;
                        float w0 = MathHelper.sin(a0 * 3f + time * 0.25f + ph) * dist * 0.12f + MathHelper.sin(a0 * 7f - time * 0.4f) * dist * 0.03f;
                        float w1 = MathHelper.sin(a1 * 3f + time * 0.25f + ph) * dist * 0.12f + MathHelper.sin(a1 * 7f - time * 0.4f) * dist * 0.03f;
                        float edge0 = 1f - Math.abs(a0) / 1.2f, edge1 = 1f - Math.abs(a1) / 1.2f;
                        float shimmer0 = 0.6f + 0.4f * MathHelper.sin(a0 * 19f + time * 1.3f + ph), shimmer1 = 0.6f + 0.4f * MathHelper.sin(a1 * 19f + time * 1.3f + ph);
                        float g = layer == 1 ? 0.75f : 1f;
                        int bot0 = WorldDraw.argb(0.6f * k * edge0 * shimmer0, 0.2f, g, 0.55f), bot1 = WorldDraw.argb(0.6f * k * edge1 * shimmer1, 0.2f, g, 0.55f);
                        int top0 = WorldDraw.argb(0f, 0.65f, 0.3f, 0.95f), top1 = WorldDraw.argb(0f, 0.65f, 0.3f, 0.95f);
                        int mid0 = WorldDraw.argb(0.32f * k * edge0 * shimmer0, 0.45f, 0.55f, 0.95f), mid1 = WorldDraw.argb(0.32f * k * edge1 * shimmer1, 0.45f, 0.55f, 0.95f);
                        float hm = (h0 + h1) / 2f;
                        WorldDraw.v(b, x0, h0, z0 + w0, bot0); WorldDraw.v(b, x1, h0, z0 + w1, bot1); WorldDraw.v(b, x1, hm, z0 + w1, mid1); WorldDraw.v(b, x0, hm, z0 + w0, mid0);
                        WorldDraw.v(b, x0, hm, z0 + w0, mid0); WorldDraw.v(b, x1, hm, z0 + w1, mid1); WorldDraw.v(b, x1, h1, z0 + w1, top1); WorldDraw.v(b, x0, h1, z0 + w0, top0);
                    }
                }
            });
        }
        if (glowFade > 0.01f) {
            long t = mc.world.getTimeOfDay() % 24000L;
            float side = (t > 6000 && t < 18000) ? -1f : 1f; // sets in the west (-x), rises in the east (+x)
            float g = glowFade * 0.32f;
            WorldDraw.colored(true, true, b -> {
                float x = side * dist;
                int low = WorldDraw.argb(g, 1f, 0.45f, 0.15f), high = WorldDraw.argb(0f, 1f, 0.6f, 0.3f);
                WorldDraw.v(b, x, -dist * 0.05f, -dist, low); WorldDraw.v(b, x, -dist * 0.05f, dist, low);
                WorldDraw.v(b, x, dist * 0.35f, dist, high); WorldDraw.v(b, x, dist * 0.35f, -dist, high);
            });
        }
        if (starStart > 0) {
            float age = (System.currentTimeMillis() - starStart) / 900f;
            if (age >= 1f) {
                starStart = -1;
            } else {
                double yaw = Math.toRadians(starYaw), pitch = Math.toRadians(starPitch);
                Vec3d dir = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).multiply(dist * 0.9);
                Vec3d right = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
                Vec3d head = dir.add(right.multiply(starDir * (age - 0.5) * dist * 0.5)).add(0, -age * dist * 0.15, 0);
                Vec3d tail = head.subtract(right.multiply(starDir * dist * 0.12)).add(0, dist * 0.035, 0);
                float fade = MathHelper.sin(age * MathHelper.PI);
                WorldDraw.colored(true, true, b -> {
                    float w = dist * 0.004f;
                    WorldDraw.v(b, (float) head.x, (float) head.y - w, (float) head.z, WorldDraw.argb(fade, 1f, 1f, 0.95f));
                    WorldDraw.v(b, (float) head.x, (float) head.y + w, (float) head.z, WorldDraw.argb(fade, 1f, 1f, 0.95f));
                    WorldDraw.v(b, (float) tail.x, (float) tail.y + w * 0.3f, (float) tail.z, WorldDraw.argb(0f, 0.6f, 0.8f, 1f));
                    WorldDraw.v(b, (float) tail.x, (float) tail.y - w * 0.3f, (float) tail.z, WorldDraw.argb(0f, 0.6f, 0.8f, 1f));
                });
            }
        }
    }

    /** For the smoke test. */
    public static void forceAurora() { auroraTonight = true; auroraFade = 1f; }
}
