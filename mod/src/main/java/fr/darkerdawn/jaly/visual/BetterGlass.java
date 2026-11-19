package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/** Better Glass: rain drops gather and run down windows that face the weather; frost creeps in when it's freezing. */
public final class BetterGlass {
    private BetterGlass() {}

    private static final Identifier DROPS = Identifier.of("jalyclient", "textures/visual/glass_drops.png");
    private static final Identifier STREAKS = Identifier.of("jalyclient", "textures/visual/glass_streaks.png");
    private static final Identifier FROST = Identifier.of("jalyclient", "textures/visual/glass_frost.png");

    /** A window face: block + side that looks out at the sky, and whether it's cold there. */
    private record Face(BlockPos pos, Direction side, boolean pane, boolean cold) {}

    private static List<Face> faces = new ArrayList<>();
    private static int scanTick;
    private static float wet, frost;

    static boolean isGlass(BlockState s) {
        return s.isIn(BlockTags.IMPERMEABLE) || (s.getBlock() instanceof PaneBlock && !s.isOf(Blocks.IRON_BARS));
    }

    public static void tick(MinecraftClient mc) {
        if (!Mods.on("better_glass") || mc.world == null || mc.player == null) {
            faces = new ArrayList<>();
            return;
        }
        JalyConfig c = JalyConfig.get();
        float rain = mc.world.getRainGradient(1f);
        wet += ((c.glassDrops ? rain : 0f) - wet) * 0.01f; // windows get wet slowly and dry slowly
        boolean freezing = mc.world.getBiome(mc.player.getBlockPos()).value().getTemperature() < 0.15f;
        frost += ((c.glassFrost && freezing ? Math.max(0.35f, Ambience.night(mc.world)) : 0f) - frost) * 0.005f;
        if (scanTick++ % 20 != 0) return;
        List<Face> found = new ArrayList<>();
        int r = (int) c.glassRange;
        BlockPos center = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        outer:
        for (int dy = -r / 2; dy <= r / 2; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = mc.world.getBlockState(m);
                    if (s.isAir() || !isGlass(s)) continue;
                    boolean pane = s.getBlock() instanceof PaneBlock;
                    BlockPos pos = m.toImmutable();
                    boolean cold = mc.world.getBiome(pos).value().getTemperature() < 0.15f;
                    for (Direction d : Direction.Type.HORIZONTAL) {
                        BlockPos n = pos.offset(d);
                        BlockState ns = mc.world.getBlockState(n);
                        if (!ns.isAir() && !ns.isReplaceable()) continue;
                        if (!mc.world.isSkyVisible(n) && !mc.world.isSkyVisible(n.offset(d))) continue; // indoor side: stays dry
                        found.add(new Face(pos, d, pane, cold));
                        if (found.size() > 600) break outer;
                    }
                }
            }
        }
        faces = found;
    }

    public static void render(WorldRenderContext ctx) {
        if (!Mods.on("better_glass") || faces.isEmpty() || (wet < 0.02f && frost < 0.02f)) return;
        Vec3d cam = ctx.camera().getPos();
        float t = (System.currentTimeMillis() % 1_000_000L) / 1000f;
        List<Face> list = faces;
        if (wet > 0.02f) {
            WorldDraw.textured(DROPS, true, b -> list.forEach(f -> quad(b, f, cam, 0.0f, 0f, 0f, WorldDraw.argb(wet * 0.9f, 1f, 1f, 1f))));
            WorldDraw.textured(STREAKS, true, b -> list.forEach(f -> quad(b, f, cam, 0.003f, (f.pos.getX() * 0.37f + f.pos.getZ() * 0.19f) % 1f, -t * 0.12f * (0.8f + (f.pos.hashCode() & 7) * 0.05f), WorldDraw.argb(wet * 0.8f, 1f, 1f, 1f))));
        }
        if (frost > 0.02f) {
            WorldDraw.textured(FROST, true, b -> list.forEach(f -> { if (f.cold) quad(b, f, cam, 0.006f, 0f, 0f, WorldDraw.argb(frost * 0.85f, 1f, 1f, 1f)); }));
        }
    }

    /** One textured quad just outside the glass face. du/dv scroll the texture (running drops). */
    private static void quad(net.minecraft.client.render.BufferBuilder b, Face f, Vec3d cam, float extra, float du, float dv, int col) {
        Direction d = f.side;
        double cx = f.pos.getX() + 0.5 - cam.x, cy = f.pos.getY() - cam.y, cz = f.pos.getZ() + 0.5 - cam.z;
        double out = (f.pane ? 0.0625 + 0.004 : 0.5 + 0.004) + extra;
        double nx = d.getOffsetX() * out, nz = d.getOffsetZ() * out;
        double ax = d.getOffsetZ() * 0.5, az = -d.getOffsetX() * 0.5; // along the face
        float x0 = (float) (cx + nx - ax), z0 = (float) (cz + nz - az), x1 = (float) (cx + nx + ax), z1 = (float) (cz + nz + az);
        float y0 = (float) cy, y1 = (float) (cy + 1);
        float u0 = du, u1 = du + 1f, v0 = dv, v1 = dv + 1f;
        WorldDraw.vt(b, x0, y1, z0, u0, v0, col);
        WorldDraw.vt(b, x1, y1, z1, u1, v0, col);
        WorldDraw.vt(b, x1, y0, z1, u1, v1, col);
        WorldDraw.vt(b, x0, y0, z0, u0, v1, col);
    }

    public static int faces() { return faces.size(); }

    /** Smoke test: soak the windows straight away. */
    public static void forceWet() { wet = 1f; scanTick = 0; }
}
