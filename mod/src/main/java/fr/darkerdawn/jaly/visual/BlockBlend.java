package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
//#if >=1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
//#else
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
//#endif
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Realistic Blending: where two different ground blocks meet (sand and red sand, grass and dirt, stone and gravel...)
 * each one fades softly into the other instead of a hard square edge.
 */
public final class BlockBlend {
    private BlockBlend() {}

    static final Identifier ATLAS = net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;

    /** Ground-type blocks that blend in "Ground only" mode. */
    private static final Set<String> GROUND = Set.of(
        "sand", "red_sand", "gravel", "dirt", "coarse_dirt", "rooted_dirt", "grass_block", "podzol", "mycelium", "mud", "packed_mud",
        "clay", "snow_block", "stone", "granite", "diorite", "andesite", "tuff", "deepslate", "calcite", "moss_block", "dirt_path",
        "soul_sand", "soul_soil", "netherrack", "end_stone", "sandstone", "red_sandstone", "terracotta", "white_terracotta",
        "orange_terracotta", "yellow_terracotta", "brown_terracotta", "red_terracotta", "light_gray_terracotta", "cobblestone",
        "mossy_cobblestone", "dripstone_block", "pale_moss_block", "suspicious_sand", "suspicious_gravel", "blackstone", "basalt",
        "smooth_basalt", "crimson_nylium", "warped_nylium", "ice", "packed_ice", "blue_ice", "powder_snow");

    /** Blocks whose top texture isn't called block/<name>. */
    private static final Map<String, String> TOP = Map.of(
        "grass_block", "grass_block_top", "podzol", "podzol_top", "mycelium", "mycelium_top", "dirt_path", "dirt_path_top",
        "snow_block", "snow", "sandstone", "sandstone_top", "red_sandstone", "red_sandstone_top", "basalt", "basalt_top",
        "crimson_nylium", "crimson_nylium", "warped_nylium", "warped_nylium");

    /** One soft edge: drawn on top of `pos`, fading in the texture of the block next to it. */
    private record Edge(BlockPos pos, Direction toward, Sprite sprite, int tint, int light) {}

    private static List<Edge> edges = new ArrayList<>();
    private static int scanTick;
    private static final Map<Block, Sprite> SPRITES = new HashMap<>();

    public static void tick(MinecraftClient mc) {
        if (!Mods.on("block_blend") || mc.world == null || mc.player == null) {
            if (!edges.isEmpty()) edges = new ArrayList<>();
            SPRITES.clear();
            return;
        }
        if (scanTick++ % 15 != 0) return;
        JalyConfig c = JalyConfig.get();
        ClientWorld w = mc.world;
        int r = (int) c.blendRange;
        BlockPos center = mc.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable(), n = new BlockPos.Mutable(), up = new BlockPos.Mutable();
        List<Edge> found = new ArrayList<>();
        for (int dy = -10; dy <= 6; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz > r * r) continue;
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState a = w.getBlockState(m);
                    if (!blends(a, w, m, c)) continue;
                    up.set(m.getX(), m.getY() + 1, m.getZ());
                    if (fr.darkerdawn.jaly.Compat.opaqueCube(w.getBlockState(up), w, up)) continue; // top hidden
                    for (Direction d : Direction.Type.HORIZONTAL) {
                        n.set(m.getX() + d.getOffsetX(), m.getY(), m.getZ() + d.getOffsetZ());
                        BlockState b = w.getBlockState(n);
                        if (b.getBlock() == a.getBlock() || !blends(b, w, n, c)) continue;
                        Sprite sp = sprite(mc, b.getBlock());
                        if (sp == null) continue;
                        int tint = b.isOf(Blocks.GRASS_BLOCK) ? BiomeColors.getGrassColor(w, n) : 0xFFFFFF;
                        int light = LightmapTextureManager.pack(w.getLightLevel(LightType.BLOCK, up), w.getLightLevel(LightType.SKY, up));
                        found.add(new Edge(m.toImmutable(), d, sp, tint, light));
                    }
                }
            }
        }
        edges = found;
    }

    private static boolean blends(BlockState s, ClientWorld w, BlockPos p, JalyConfig c) {
        if (!fr.darkerdawn.jaly.Compat.opaqueCube(s, w, p)) return false;
        if (!c.blendGrass && s.isOf(Blocks.GRASS_BLOCK)) return false;
        return "all".equals(c.blendWhich) || GROUND.contains(Registries.BLOCK.getId(s.getBlock()).getPath());
    }

    private static Sprite sprite(MinecraftClient mc, Block b) {
        return SPRITES.computeIfAbsent(b, k -> {
            String path = Registries.BLOCK.getId(k).getPath();
            Identifier id = Identifier.ofVanilla("block/" + TOP.getOrDefault(path, path));
            Sprite s = fr.darkerdawn.jaly.Compat.blockSprite(id);
            return s == null || s.getContents().getId().getPath().contains("missingno") ? null : s;
        });
    }

    public static int count() { return edges.size(); }

    public static void render(WorldRenderContext ctx) {
        List<Edge> list = edges;
        if (!Mods.on("block_blend") || list.isEmpty()) return;
        JalyConfig c = JalyConfig.get();
        Vec3d cam = fr.darkerdawn.jaly.Compat.camPos();
        float width = c.blendWidth, strength = c.blendStrength;
        WorldDraw.textured(ATLAS, true, b -> {
            for (Edge e : list) strip(b, e, cam, width, strength);
        });
    }

    /** A strip on the top of e.pos along the side facing e.toward: full strength at the edge, fading to nothing `width` in. */
    private static void strip(net.minecraft.client.render.VertexConsumer b, Edge e, Vec3d cam, float width, float strength) {
        float bx = (float) (e.pos.getX() - cam.x), by = (float) (e.pos.getY() + 1.0015 - cam.y), bz = (float) (e.pos.getZ() - cam.z);
        Sprite s = e.sprite;
        float u0 = s.getMinU(), u1 = s.getMaxU(), v0 = s.getMinV(), v1 = s.getMaxV();
        float r = ((e.tint >> 16) & 255) / 255f, g = ((e.tint >> 8) & 255) / 255f, bl = (e.tint & 255) / 255f;
        int edge = WorldDraw.argb(strength, r, g, bl), inner = WorldDraw.argb(0f, r, g, bl);
        // the strip in block-local coords: (x0,z0)-(x1,z1) with the "edge" side flagged
        float x0 = 0, x1 = 1, z0 = 0, z1 = 1;
        switch (e.toward) {
            case NORTH -> z1 = width;
            case SOUTH -> z0 = 1 - width;
            case WEST -> x1 = width;
            default -> x0 = 1 - width; // EAST
        }
        float[][] corners = {{x0, z0}, {x0, z1}, {x1, z1}, {x1, z0}};
        for (float[] k : corners) {
            boolean atEdge = switch (e.toward) {
                case NORTH -> k[1] == 0;
                case SOUTH -> k[1] == 1;
                case WEST -> k[0] == 0;
                default -> k[0] == 1;
            };
            WorldDraw.vtl(b, bx + k[0], by, bz + k[1], u0 + (u1 - u0) * k[0], v0 + (v1 - v0) * k[1], atEdge ? edge : inner, e.light);
        }
    }
}
