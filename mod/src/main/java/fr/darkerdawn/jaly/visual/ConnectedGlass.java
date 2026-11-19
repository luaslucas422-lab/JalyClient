package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
//#if >=1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//#endif
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Connected Textures (for when Continuity isn't installed): glass blocks lose their inner borders
 * (a built-in borderless glass pack) and this draws a clean frame only around the outside of each window.
 */
public final class ConnectedGlass {
    private ConnectedGlass() {}

    /** True when this should run: switched on and the player doesn't have Continuity doing it already. */
    public static boolean active() {
        return Mods.on("connected_textures") && !FabricLoader.getInstance().isModLoaded("continuity");
    }

    /** One frame line: a face of a glass block and the edge of that face that needs a border. */
    private record Line(BlockPos pos, Direction face, Direction edge, int color) {}

    private static List<Line> lines = new ArrayList<>();
    private static int scanTick;

    static boolean isGlass(Block b) {
        String p = Registries.BLOCK.getId(b).getPath();
        return p.equals("glass") || p.equals("tinted_glass") || p.endsWith("_stained_glass");
    }

    public static void tick(MinecraftClient mc) {
        if (!active() || mc.world == null || mc.player == null) {
            if (!lines.isEmpty()) lines = new ArrayList<>();
            return;
        }
        if (scanTick++ % 10 != 0) return;
        ClientWorld w = mc.world;
        int r = (int) JalyConfig.get().ctmRange;
        BlockPos c = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        List<Line> found = new ArrayList<>();
        for (int dy = -r / 2; dy <= r / 2; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = w.getBlockState(m);
                    if (!isGlass(s.getBlock())) continue;
                    Block b = s.getBlock();
                    for (Direction face : Direction.values()) {
                        BlockPos out = m.offset(face);
                        BlockState o = w.getBlockState(out);
                        if (o.getBlock() == b || fr.darkerdawn.jaly.Compat.opaqueCube(o, w, out)) continue; // face hidden
                        for (Direction edge : Direction.values()) {
                            if (edge.getAxis() == face.getAxis()) continue;
                            BlockPos side = m.offset(edge);
                            boolean same = w.getBlockState(side).getBlock() == b;
                            // border where the window ends, or where it turns a corner
                            if (!same || w.getBlockState(side.offset(face)).getBlock() == b)
                                found.add(new Line(m.toImmutable(), face, edge, frameColor(s, w, m)));
                        }
                    }
                }
            }
        }
        lines = found;
    }

    private static int frameColor(BlockState s, ClientWorld w, BlockPos p) {
        String id = Registries.BLOCK.getId(s.getBlock()).getPath();
        if (id.equals("glass")) return 0xFFE4EEF2;
        if (id.equals("tinted_glass")) return 0xFF2A2230;
        int map = s.getMapColor(w, p).color;
        int r = (int) (((map >> 16) & 255) * 0.75), g = (int) (((map >> 8) & 255) * 0.75), b = (int) ((map & 255) * 0.75);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static int count() { return lines.size(); }

    public static void render(WorldRenderContext ctx) {
        List<Line> list = lines;
        if (!active() || list.isEmpty()) return;
        Vec3d cam = fr.darkerdawn.jaly.Compat.camPos();
        float t = JalyConfig.get().ctmFrame / 16f;
        WorldDraw.colored(false, true, b -> {
            for (Line l : list) line(b, l, cam, t);
        });
    }

    /** A thin quad on `face` hugging `edge`, nudged just outside the block so it never flickers. */
    private static void line(net.minecraft.client.render.VertexConsumer b, Line l, Vec3d cam, float t) {
        float ox = (float) (l.pos.getX() - cam.x), oy = (float) (l.pos.getY() - cam.y), oz = (float) (l.pos.getZ() - cam.z);
        float e = 0.0025f;
        // the face plane
        float[] lo = {0, 0, 0}, hi = {1, 1, 1};
        int fa = l.face.getAxis().ordinal();
        float plane = l.face.getDirection() == Direction.AxisDirection.POSITIVE ? 1 + e : -e;
        lo[fa] = plane; hi[fa] = plane;
        // squeeze to a strip along the edge
        int ea = l.edge.getAxis().ordinal();
        if (l.edge.getDirection() == Direction.AxisDirection.POSITIVE) lo[ea] = 1 - t; else hi[ea] = t;
        // the four corners in order around the strip
        int u = 3 - fa - ea; // the remaining axis
        float[][] pts = new float[4][3];
        float[][] uv = {{lo[ea], lo[u]}, {hi[ea], lo[u]}, {hi[ea], hi[u]}, {lo[ea], hi[u]}};
        for (int i = 0; i < 4; i++) {
            pts[i][fa] = plane;
            pts[i][ea] = uv[i][0];
            pts[i][u] = uv[i][1];
        }
        for (float[] p : pts) WorldDraw.v(b, ox + p[0], oy + p[1], oz + p[2], l.color);
    }
}
