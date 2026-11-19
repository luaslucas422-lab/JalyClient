package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/** Better Sounds: wind, rain on the roof, birds, crickets, cave drips and owls, all following where you are. */
public final class BetterSounds {
    private BetterSounds() {}

    private static final Random RNG = Random.create();

    private static SoundEvent ev(String id) { return SoundEvent.of(Identifier.of("jalyclient", id)); }

    /** A looping ambience track that fades its volume toward a target. */
    static final class Loop extends MovingSoundInstance {
        float target;

        Loop(String id) {
            super(ev(id), SoundCategory.AMBIENT, SoundInstance.createRandom());
            this.repeat = true;
            this.repeatDelay = 0;
            this.volume = 0.001f;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }

        @Override
        public void tick() {
            volume += (target - volume) * 0.03f;
            if (target <= 0 && volume < 0.005f) setDone();
        }

        @Override
        public boolean shouldAlwaysPlay() { return true; }
    }

    private static Loop wind, roof, crickets, leaves;
    private static int cooldownBird = 100, cooldownDrip = 60, cooldownOwl = 400;

    private static Loop keep(MinecraftClient mc, Loop l, String id, float target) {
        if (target > 0.01f && (l == null || l.isDone() || !mc.getSoundManager().isPlaying(l))) {
            l = new Loop(id);
            mc.getSoundManager().play(l);
        }
        if (l != null) l.target = target;
        return l;
    }

    public static void tick(MinecraftClient mc) {
        boolean on = Mods.on("better_sounds") && mc.world != null && mc.player != null && !mc.isPaused();
        if (!on) {
            if (wind != null) wind.target = 0;
            if (roof != null) roof.target = 0;
            if (crickets != null) crickets.target = 0;
            if (leaves != null) leaves.target = 0;
            return;
        }
        var w = mc.world;
        BlockPos head = BlockPos.ofFloored(mc.player.getEyePos());
        boolean overworld = w.getDimension().hasSkyLight() && !w.getDimension().hasCeiling();
        boolean sky = w.isSkyVisible(head);
        int skyLight = w.getLightLevel(net.minecraft.world.LightType.SKY, head);
        float rain = w.getRainGradient(1f), thunder = w.getThunderGradient(1f);
        float night = Ambience.night(w);
        double y = mc.player.getY();
        var biome = w.getBiome(head);

        // wind: stronger up high and in storms, muffled indoors
        float windT = overworld ? (0.12f + (float) Math.max(0, Math.min(0.35, (y - 70) / 120.0)) + rain * 0.15f + thunder * 0.2f) * (sky ? 1f : skyLight > 6 ? 0.35f : 0f) : 0f;
        wind = keep(mc, wind, "amb_wind", windT);
        // rain drumming on the roof when you're sheltered
        float roofT = overworld && rain > 0.05f && !sky && skyLight > 3 && w.getBiome(head).value().getPrecipitation(head) == net.minecraft.world.biome.Biome.Precipitation.RAIN ? 0.45f * rain : 0f;
        roof = keep(mc, roof, "amb_rain_roof", roofT);
        // crickets on warm nights
        float temp = biome.value().getTemperature();
        float cricketT = overworld && night > 0.5f && rain < 0.2f && temp > 0.4f && skyLight > 8 ? 0.22f * night : 0f;
        crickets = keep(mc, crickets, "amb_crickets", cricketT);
        // leaves rustling in forests
        float leavesT = overworld && biome.isIn(BiomeTags.IS_FOREST) && skyLight > 8 ? 0.15f + rain * 0.1f : 0f;
        leaves = keep(mc, leaves, "amb_leaves", leavesT);

        // one-shots: birds by day in green places, drips in caves, owls on cold nights
        if (--cooldownBird <= 0) {
            cooldownBird = 120 + RNG.nextInt(260);
            if (overworld && night < 0.2f && rain < 0.2f && skyLight > 10 && temp > 0.3f && (biome.isIn(BiomeTags.IS_FOREST) || biome.isIn(BiomeTags.IS_HILL) || RNG.nextInt(3) == 0)) {
                oneShot(mc, "amb_bird" + (1 + RNG.nextInt(3)), 0.35f, 0.85f + RNG.nextFloat() * 0.3f, 12);
            }
        }
        if (--cooldownDrip <= 0) {
            cooldownDrip = 50 + RNG.nextInt(140);
            if (skyLight == 0 && y < 56) oneShot(mc, "amb_drip" + (1 + RNG.nextInt(2)), 0.45f, 0.8f + RNG.nextFloat() * 0.5f, 8);
        }
        if (--cooldownOwl <= 0) {
            cooldownOwl = 500 + RNG.nextInt(900);
            if (overworld && night > 0.8f && skyLight > 8 && (temp < 0.4f || biome.isIn(BiomeTags.IS_FOREST))) oneShot(mc, "amb_owl", 0.3f, 0.95f + RNG.nextFloat() * 0.1f, 20);
        }
    }

    private static void oneShot(MinecraftClient mc, String id, float vol, float pitch, int spread) {
        double x = mc.player.getX() + (RNG.nextDouble() - 0.5) * spread * 2, z = mc.player.getZ() + (RNG.nextDouble() - 0.5) * spread * 2;
        double y = mc.player.getEyeY() + RNG.nextDouble() * 6 - 1;
        mc.getSoundManager().play(new PositionedSoundInstance(ev(id), SoundCategory.AMBIENT, vol, pitch, SoundInstance.createRandom(), x, y, z));
    }

    /** True when an ambience loop is running (smoke test). */
    public static boolean anyPlaying() {
        return wind != null && !wind.isDone() || leaves != null && !leaves.isDone() || crickets != null && !crickets.isDone();
    }
}
