package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.hud.Hud;
import fr.darkerdawn.jaly.mixin.SimpleOptionAccessor;
import fr.darkerdawn.jaly.ui.Gfx;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Random;

/** Gameplay-side features: zoom, sprint, fullbright, snowfall, hit jingles, snow bursts, crosshair. */
public final class Features {
    private Features() {}

    private static final Random RNG = new Random();
    public static boolean zoomHeld;
    private static double zoomCurrent = 1.0;
    private static double zoomScroll = 0;
    private static boolean cinematicBefore;
    private static boolean cinematicOwned;
    private static long lastFovNanos = System.nanoTime();
    private static int lastAttackedId = -1;
    private static long lastAttackTime;
    private static Vec3d lastPos;

    public static void onToggled(String id, boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if (id.equals("fullbright")) applyFullbright(mc, on);
        if (id.equals("toggle_sprint") && !on) releaseSprint(mc);
    }

    public static void applyFullbright(MinecraftClient mc, boolean on) {
        SimpleOptionAccessor gamma = (SimpleOptionAccessor) (Object) mc.options.getGamma();
        double cur = mc.options.getGamma().getValue();
        if (on) {
            if (cur <= 1.0) JalyConfig.get().prevGamma = cur;
            gamma.jaly$setValue(16.0);
        } else {
            gamma.jaly$setValue(Math.min(1.0, JalyConfig.get().prevGamma));
        }
    }

    private static void releaseSprint(MinecraftClient mc) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(mc.options.sprintKey);
        boolean physical = key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() > 0 && InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getCode());
        mc.options.sprintKey.setPressed(physical);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { lastPos = null; return; }
        Vec3d p = mc.player.getPos();
        if (lastPos != null) {
            double dx = p.x - lastPos.x, dz = p.z - lastPos.z;
            Hud.speed = Hud.speed * 0.7 + Math.sqrt(dx * dx + dz * dz) * 20 * 0.3;
        }
        lastPos = p;
        if (Mods.on("toggle_sprint") && mc.currentScreen == null) mc.options.sprintKey.setPressed(true);
        if (Mods.on("fullbright") && mc.options.getGamma().getValue() < 2.0) applyFullbright(mc, true);
        if (Mods.on("snowfall")) snowfall(mc);
        if (lastAttackedId >= 0) {
            Entity e = mc.world.getEntityById(lastAttackedId);
            boolean dead = e == null ? System.currentTimeMillis() - lastAttackTime < 150 : e instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0);
            if (System.currentTimeMillis() - lastAttackTime > 2500) lastAttackedId = -1;
            else if (dead && e != null) {
                if (Mods.on("kill_effect")) snowBurst(mc, e.getPos().add(0, e.getHeight() / 2, 0));
                lastAttackedId = -1;
            }
        }
    }

    private static void snowfall(MinecraftClient mc) {
        int n = switch (JalyConfig.get().snowfallLevel) {
            case "blizzard" -> 22;
            case "medium" -> 9;
            default -> 4;
        };
        Vec3d p = mc.player.getPos();
        BlockPos.Mutable bp = new BlockPos.Mutable();
        for (int i = 0; i < n; i++) {
            double x = p.x + (RNG.nextDouble() - 0.5) * 28;
            double z = p.z + (RNG.nextDouble() - 0.5) * 28;
            double y = p.y + 6 + RNG.nextDouble() * 10;
            bp.set(x, y, z);
            if (!mc.world.isSkyVisible(bp)) continue;
            mc.world.addParticle(RNG.nextInt(4) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.SNOWFLAKE, x, y, z, (RNG.nextDouble() - 0.5) * 0.02, -0.04, (RNG.nextDouble() - 0.5) * 0.02);
        }
    }

    public static void snowBurst(MinecraftClient mc, Vec3d at) {
        for (int i = 0; i < 40; i++) {
            mc.world.addParticle(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, (RNG.nextDouble() - 0.5) * 0.4, RNG.nextDouble() * 0.3, (RNG.nextDouble() - 0.5) * 0.4);
        }
        for (int i = 0; i < 12; i++) {
            mc.world.addParticle(ParticleTypes.HAPPY_VILLAGER, at.x + (RNG.nextDouble() - 0.5), at.y + (RNG.nextDouble() - 0.5), at.z + (RNG.nextDouble() - 0.5), 0, 0, 0);
        }
        Sounds.play(Sounds.KILL, 1f, 0.8f);
    }

    public static void onAttack(Entity target) {
        if (Mods.on("hit_sound")) Sounds.play(Sounds.hitSound(), 0.9f + RNG.nextFloat() * 0.25f, 0.7f);
        lastAttackedId = target.getId();
        lastAttackTime = System.currentTimeMillis();
    }

    /** Zoom: divides the FOV. Smooth, frame-rate independent. */
    public static double applyZoom(double fov) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFovNanos) / 1e9f);
        lastFovNanos = now;
        boolean active = zoomHeld && Mods.on("zoom") && mc.currentScreen == null;
        if (!active) zoomScroll = 0;
        double target = active ? Math.max(1.0, Math.min(50.0, JalyConfig.get().zoomLevel * Math.pow(1.25, zoomScroll))) : 1.0;
        zoomCurrent = JalyConfig.get().zoomSmooth ? zoomCurrent + (target - zoomCurrent) * (1 - Math.exp(-14 * dt)) : target;
        if (Math.abs(zoomCurrent - target) < 0.001) zoomCurrent = target;
        if (JalyConfig.get().zoomCinematic) {
            if (active && !cinematicOwned) {
                cinematicBefore = mc.options.smoothCameraEnabled;
                mc.options.smoothCameraEnabled = true;
                cinematicOwned = true;
            } else if (!active && cinematicOwned) {
                mc.options.smoothCameraEnabled = cinematicBefore;
                cinematicOwned = false;
            }
        }
        return fov / zoomCurrent;
    }

    public static boolean onScroll(double amount) {
        if (zoomHeld && Mods.on("zoom") && MinecraftClient.getInstance().currentScreen == null) {
            zoomScroll = Math.max(-4, Math.min(8, zoomScroll + Math.signum(amount)));
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- crosshair
    private static final String[] CH_SNOWFLAKE = {"#...#...#", ".#..#..#.", "..#.#.#..", "...###...", "#########", "...###...", "..#.#.#..", ".#..#..#.", "#...#...#"};
    private static final String[] CH_STAR = {"....#....", "....#....", "...###...", "#########", ".#######.", "..#####..", "..##.##..", ".##...##.", ".#.....#."};
    private static final String[] CH_CANDY = {"....R....", "....W....", "....R....", "....W....", "RWRW.WRWR", "....W....", "....R....", "....W....", "....R...."};
    private static final String[] CH_TREE = {"....Y....", "...GGG...", "..GGGGG..", "...GGG...", "..GGGGG..", ".GGGGGGG.", "..GGGGG..", "GGGGGGGGG", "....B...."};
    private static final String[] CH_DOT = {"##", "##"};
    private static final String[] CH_PLUS = {"....#....", "....#....", "....#....", "....#....", "####.####", "....#....", "....#....", "....#....", "....#...."};

    public static void drawCrosshair(DrawContext ctx, MinecraftClient mc) {
        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        int col = JalyConfig.get().crosshairColor;
        String[] rows;
        int[] pal;
        String keys;
        switch (JalyConfig.get().crosshair) {
            case "star" -> { rows = CH_STAR; pal = new int[]{col}; keys = "#"; }
            case "candy" -> { rows = CH_CANDY; pal = new int[]{Gfx.RED_LIGHT, 0xFFFFFFFF}; keys = "RW"; }
            case "tree" -> { rows = CH_TREE; pal = new int[]{0xFF3FB950, 0xFFFDE68A, 0xFF8B5A2B}; keys = "GYB"; }
            case "dot" -> { rows = CH_DOT; pal = new int[]{col}; keys = "#"; }
            case "plus" -> { rows = CH_PLUS; pal = new int[]{col}; keys = "#"; }
            default -> { rows = CH_SNOWFLAKE; pal = new int[]{col}; keys = "#"; }
        }
        int w = rows[0].length(), h = rows.length;
        int x = cx - w / 2, y = cy - h / 2;
        for (int[] o : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
            Gfx.sprite(ctx, rows, new int[]{0x99000000, 0x99000000, 0x99000000}, keys, x + o[0], y + o[1], 1, 1f);
        }
        Gfx.sprite(ctx, rows, pal, keys, x, y, 1, 1f);
        if (mc.options.getAttackIndicator().getValue() == AttackIndicator.CROSSHAIR && mc.player != null) {
            float f = mc.player.getAttackCooldownProgress(0.0F);
            if (f < 1.0F) {
                int bw = 16;
                ctx.fill(cx - bw / 2, cy + h / 2 + 3, cx + bw / 2, cy + h / 2 + 5, 0x99000000);
                ctx.fill(cx - bw / 2, cy + h / 2 + 3, cx - bw / 2 + (int) (bw * f), cy + h / 2 + 5, 0xFFFDE68A);
            }
        }
    }
}
