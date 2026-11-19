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
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import fr.darkerdawn.jaly.cosmetic.Cosmetics;
import fr.darkerdawn.jaly.hud.Minimap;
import org.joml.Vector3f;

import java.util.Random;

/** Gameplay-side features: zoom, sprint, fullbright, snowfall, hit jingles, snow bursts, crosshair. */
public final class Features {
    private Features() {}

    private static final Random RNG = new Random();
    public static boolean zoomHeld;
    public static boolean zoomForced;
    public static int combo;
    public static double reach;
    private static long lastHitTime;
    private static int lastHurtTime;
    public static long joinTime = System.currentTimeMillis();
    private static int trailTick;
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
        if (id.equals("tall_grass") || id.equals("better_rain") || id.equals("connected_textures")) fr.darkerdawn.jaly.visual.VisualPacks.sync();
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
        boolean physical = key.getCategory() == InputUtil.Type.KEYSYM && key.getCode() > 0 && fr.darkerdawn.jaly.Compat.keyDown(key.getCode());
        mc.options.sprintKey.setPressed(physical);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) { lastPos = null; joinTime = System.currentTimeMillis(); return; }
        if (mc.player.hurtTime > 0 && lastHurtTime == 0) combo = 0;
        lastHurtTime = mc.player.hurtTime;
        if (System.currentTimeMillis() - lastHitTime > 2500) combo = 0;
        if (Mods.on("minimap")) Minimap.tick(mc);
        trail(mc);
        snowSteps(mc);
        petTick(mc);
        Vec3d p = fr.darkerdawn.jaly.Compat.pos(mc.player);
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
                if (Mods.on("kill_effect")) snowBurst(mc, fr.darkerdawn.jaly.Compat.pos(e).add(0, e.getHeight() / 2, 0));
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
        Vec3d p = fr.darkerdawn.jaly.Compat.pos(mc.player);
        BlockPos.Mutable bp = new BlockPos.Mutable();
        for (int i = 0; i < n; i++) {
            double x = p.x + (RNG.nextDouble() - 0.5) * 28;
            double z = p.z + (RNG.nextDouble() - 0.5) * 28;
            double y = p.y + 6 + RNG.nextDouble() * 10;
            bp.set(x, y, z);
            if (!mc.world.isSkyVisible(bp)) continue;
            fr.darkerdawn.jaly.Compat.particle(mc.world, RNG.nextInt(4) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.SNOWFLAKE, x, y, z, (RNG.nextDouble() - 0.5) * 0.02, -0.04, (RNG.nextDouble() - 0.5) * 0.02);
        }
    }

    public static void snowBurst(MinecraftClient mc, Vec3d at) {
        for (int i = 0; i < 40; i++) {
            fr.darkerdawn.jaly.Compat.particle(mc.world, ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, (RNG.nextDouble() - 0.5) * 0.4, RNG.nextDouble() * 0.3, (RNG.nextDouble() - 0.5) * 0.4);
        }
        for (int i = 0; i < 12; i++) {
            fr.darkerdawn.jaly.Compat.particle(mc.world, ParticleTypes.HAPPY_VILLAGER, at.x + (RNG.nextDouble() - 0.5), at.y + (RNG.nextDouble() - 0.5), at.z + (RNG.nextDouble() - 0.5), 0, 0, 0);
        }
        Sounds.play(Sounds.KILL, 1f, 0.8f);
    }

    /** Reads the zoom key straight from the keyboard: C is also vanilla's "save hotbar" key and Minecraft gives a key to only one binding. */
    public static boolean zoomKeyDown(MinecraftClient mc) {
        if (zoomForced) return true;
        if (mc.currentScreen != null || JalyClient.ZOOM_KEY == null) return false;
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(JalyClient.ZOOM_KEY);
        long win = mc.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) return org.lwjgl.glfw.GLFW.glfwGetMouseButton(win, key.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        return key.getCode() > 0 && fr.darkerdawn.jaly.Compat.keyDown(key.getCode());
    }

    private static void trail(MinecraftClient mc) {
        if ("sparkles".equals(Cosmetics.selected(Cosmetics.Type.NAME_FX)) && !mc.options.getPerspective().isFirstPerson() && (trailTick++ % 5 == 0)) {
            Vec3d h = fr.darkerdawn.jaly.Compat.pos(mc.player).add(0, mc.player.getHeight() + 0.6, 0);
            fr.darkerdawn.jaly.Compat.particle(mc.world, ParticleTypes.SNOWFLAKE, h.x + (RNG.nextDouble() - 0.5) * 0.9, h.y + RNG.nextDouble() * 0.3, h.z + (RNG.nextDouble() - 0.5) * 0.9, 0, -0.01, 0);
        }
        String t = Cosmetics.selected(Cosmetics.Type.TRAIL);
        if (t.equals("none") || mc.player.isInvisible() || mc.player.isSpectator()) return;
        Vec3d v = mc.player.getVelocity();
        if (v.horizontalLengthSquared() < 0.0009 && mc.player.isOnGround()) return;
        trailTick++;
        Vec3d p = fr.darkerdawn.jaly.Compat.pos(mc.player).subtract(v.x * 2, 0, v.z * 2);
        ParticleEffect fx = switch (t) {
            case "candy" -> (trailTick % 2 == 0 ? Compat.dust(0.88f, 0.22f, 0.31f, 1.1f) : Compat.dust(1f, 1f, 1f, 1.1f));
            case "hearts" -> trailTick % 5 == 0 ? ParticleTypes.HEART : null;
            case "notes" -> trailTick % 3 == 0 ? ParticleTypes.NOTE : null;
            case "gold" -> Compat.dust(1f, 0.85f, 0.29f, 0.9f);
            case "stars" -> trailTick % 2 == 0 ? ParticleTypes.END_ROD : null;
            case "aurora" -> Compat.dustFade(0.24f, 1f, 0.69f, 0.7f, 0.36f, 1f, 1.2f);
            case "cherry" -> ParticleTypes.CHERRY_LEAVES;
            case "embers" -> trailTick % 2 == 0 ? ParticleTypes.SMALL_FLAME : null;
            case "totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "fireworks" -> trailTick % 2 == 0 ? ParticleTypes.FIREWORK : Compat.dust(1f, 0.3f, 0.6f, 1.2f);
            case "galaxy" -> trailTick % 3 == 0 ? ParticleTypes.END_ROD : Compat.dustFade(0.6f, 0.4f, 1f, 0.3f, 0.75f, 1f, 1.3f);
            case "og" -> (trailTick % 2 == 0 ? Compat.dust(1f, 0.85f, 0.29f, 1.3f) : Compat.dust(0.88f, 0.22f, 0.31f, 1.3f));
            default -> ParticleTypes.SNOWFLAKE;
        };
        if (fx == null) return;
        double vx = t.equals("notes") ? RNG.nextInt(24) / 24.0 : (RNG.nextDouble() - 0.5) * 0.02;
        fr.darkerdawn.jaly.Compat.particle(mc.world, fx, p.x + (RNG.nextDouble() - 0.5) * 0.4, p.y + 0.1 + RNG.nextDouble() * 0.3, p.z + (RNG.nextDouble() - 0.5) * 0.4, vx, 0.01, (RNG.nextDouble() - 0.5) * 0.02);
    }

    /** How high you are above where you last stood: walking pets stay on the floor when you jump. */
    public static float petAir;
    private static double groundY = Double.NaN;
    private static int petTick;

    private static void petTick(MinecraftClient mc) {
        if (mc.player.isOnGround() || Double.isNaN(groundY) || mc.player.getY() < groundY) groundY = mc.player.getY();
        petAir = (float) net.minecraft.util.math.MathHelper.clamp(mc.player.getY() - groundY, 0, 2.5);
        String pet = fr.darkerdawn.jaly.cosmetic.Cosmetics.selected(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET);
        if (!fr.darkerdawn.jaly.cosmetic.PlayerExtrasFeature.FLYERS.contains(pet) || petTick++ % 3 != 0 || mc.options.getPerspective().isFirstPerson()) return;
        // a little magic under flying pets: that's what keeps them up
        double a = Math.toRadians(180 - mc.player.bodyYaw);
        double lx = 0.95 * 0.9375, lz = 0.2 * 0.9375;
        double wx = lx * Math.cos(a) + lz * Math.sin(a), wz = -lx * Math.sin(a) + lz * Math.cos(a);
        double y = mc.player.getY() + 1.1;
        var fx = pet.equals("frost_dragon") ? ParticleTypes.SNOWFLAKE : pet.equals("sleigh") ? Compat.dust(1f, 0.85f, 0.3f, 0.7f) : ParticleTypes.WHITE_ASH;
        fr.darkerdawn.jaly.Compat.particle(mc.world, fx, mc.player.getX() + wx + (RNG.nextDouble() - 0.5) * 0.3, y, mc.player.getZ() + wz + (RNG.nextDouble() - 0.5) * 0.3, 0, -0.03, 0);
    }

    private static boolean wasOnGround = true;
    private static int stepTick;
    private static boolean lowWarned;

    /** Snow Steps: little snow puffs where you walk, and a burst when you jump. */
    private static void snowSteps(MinecraftClient mc) {
        boolean ground = mc.player.isOnGround();
        if (Mods.on("snow_steps") && !mc.player.isSpectator()) {
            Vec3d v = mc.player.getVelocity();
            double sp = Math.sqrt(v.x * v.x + v.z * v.z);
            if (ground && sp > 0.03 && stepTick++ % 4 == 0) {
                fr.darkerdawn.jaly.Compat.particle(mc.world, ParticleTypes.SNOWFLAKE, mc.player.getX() + (RNG.nextDouble() - 0.5) * 0.4, mc.player.getY() + 0.05, mc.player.getZ() + (RNG.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
            }
            if (wasOnGround && !ground && v.y > 0.2) {
                for (int i = 0; i < 8; i++) {
                    fr.darkerdawn.jaly.Compat.particle(mc.world, ParticleTypes.SNOWFLAKE, mc.player.getX(), mc.player.getY() + 0.1, mc.player.getZ(), (RNG.nextDouble() - 0.5) * 0.25, 0.08, (RNG.nextDouble() - 0.5) * 0.25);
                }
            }
        }
        wasOnGround = ground;
        // Low Health Warning: a bell when you drop under the limit
        boolean low = mc.player.getHealth() <= JalyConfig.get().lowHealthAt && mc.player.getHealth() > 0 && !mc.player.isCreative();
        if (Mods.on("low_health") && low && !lowWarned) Sounds.play(Sounds.CHIME, 0.6f, 0.9f);
        lowWarned = low;
    }

    /** Target HUD: what you last hit and how much health it has left. */
    public static String targetText(MinecraftClient mc) {
        if (mc.world == null) return "Zombie 14.0/20";
        if (System.currentTimeMillis() - lastAttackTime > 8000) return null;
        Entity e = mc.world.getEntityById(lastAttackedId);
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return null;
        return Gfx.trim(le.getName().getString(), 120) + " " + String.format("%.1f/%.0f", le.getHealth(), le.getMaxHealth());
    }

    public static void onAttack(Entity target) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        combo = now - lastHitTime < 2500 ? combo + 1 : 1;
        lastHitTime = now;
        if (mc.player != null) {
            Vec3d eye = mc.player.getEyePos();
            var b = target.getBoundingBox();
            double cx = Math.max(b.minX, Math.min(eye.x, b.maxX)), cy = Math.max(b.minY, Math.min(eye.y, b.maxY)), cz = Math.max(b.minZ, Math.min(eye.z, b.maxZ));
            reach = eye.distanceTo(new Vec3d(cx, cy, cz));
        }
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
        boolean active = (zoomHeld || zoomForced) && Mods.on("zoom") && (mc.currentScreen == null || zoomForced);
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
