package fr.darkerdawn.jaly.hud;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** A festive top-down minimap: north-up, with you, players and hostile mobs. Toggle it like any HUD mod. */
public final class Minimap extends HudModule {
    private static final int SIZE = 96;
    private static final Identifier TEX = Identifier.of("jalyclient", "minimap");
    private static NativeImage image;
    private static NativeImageBackedTexture texture;
    private static int rowCursor;
    private static int centerX, centerZ;

    public Minimap() {
        super("minimap", "Minimap", "A snowy top-down map of the area around you", false, 0.84f, 0.18f);
    }

    @Override public int width(MinecraftClient mc, boolean preview) { return SIZE + 6; }
    @Override public int height(MinecraftClient mc, boolean preview) { return SIZE + 16; }

    /** Refreshes a slice of the map every tick so it never costs a frame spike. */
    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        if (image == null) {
            image = new NativeImage(SIZE, SIZE, true);
            texture = fr.darkerdawn.jaly.Compat.texture(image, "img");
            mc.getTextureManager().registerTexture(TEX, texture);
        }
        if (rowCursor == 0) {
            centerX = mc.player.getBlockX();
            centerZ = mc.player.getBlockZ();
        }
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int rows = 24;
        for (int r = 0; r < rows && rowCursor < SIZE; r++, rowCursor++) {
            int z = centerZ - SIZE / 2 + rowCursor;
            for (int x = 0; x < SIZE; x++) {
                int bx = centerX - SIZE / 2 + x;
                int color = 0xFF2A1A0E;
                if (mc.world.getChunkManager().isChunkLoaded(bx >> 4, z >> 4)) {
                    int top = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, bx, z) - 1;
                    pos.set(bx, top, z);
                    BlockState st = mc.world.getBlockState(pos);
                    MapColor mcCol = st.getMapColor(mc.world, pos);
                    int north = mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, bx, z - 1) - 1;
                    MapColor.Brightness b = top > north ? MapColor.Brightness.HIGH : top < north ? MapColor.Brightness.LOW : MapColor.Brightness.NORMAL;
                    color = mcCol == MapColor.CLEAR ? 0xFF203018 : mcCol.getRenderColor(b);
                }
                fr.darkerdawn.jaly.Compat.setPixel(image, x, rowCursor, color);
            }
        }
        if (rowCursor >= SIZE) {
            rowCursor = 0;
            texture.upload();
        }
    }

    @Override
    public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
        int w = SIZE + 6;
        Gfx.round(ctx, 0, 0, w, SIZE + 16, 0xE0122312);
        Gfx.outline(ctx, 0, 0, w, SIZE + 16, Gfx.GOLD);
        Gfx.snowCap(ctx, 1, 0, w - 2, 0.3f, 1f);
        if (image != null && mc.player != null) {
            Gfx.tex(ctx, TEX, 3, 3, SIZE, SIZE, 0, 0, SIZE, SIZE, SIZE, SIZE, 1f);
            // other players and hostile mobs
            for (Entity e : mc.world.getEntities()) {
                if (e == mc.player) continue;
                int dx = (int) Math.floor(e.getX()) - centerX + SIZE / 2, dz = (int) Math.floor(e.getZ()) - centerZ + SIZE / 2;
                if (dx < 1 || dz < 1 || dx >= SIZE - 1 || dz >= SIZE - 1) continue;
                if (e instanceof PlayerEntity) ctx.fill(3 + dx - 1, 3 + dz - 1, 3 + dx + 2, 3 + dz + 2, 0xFFFFFFFF);
                else if (e instanceof HostileEntity) ctx.fill(3 + dx, 3 + dz, 3 + dx + 2, 3 + dz + 2, 0xFFE0384F);
            }
            int px = 3 + mc.player.getBlockX() - centerX + SIZE / 2, pz = 3 + mc.player.getBlockZ() - centerZ + SIZE / 2;
            double a = Math.toRadians(mc.player.getYaw());
            for (int i = 0; i < 6; i++) {
                int ax = px + (int) Math.round(-Math.sin(a) * i), az = pz + (int) Math.round(Math.cos(a) * i);
                ctx.fill(ax - (i < 2 ? 1 : 0), az - (i < 2 ? 1 : 0), ax + 1, az + 1, i < 2 ? 0xFFFFFFFF : 0xFFFDE68A);
            }
        } else {
            ctx.fillGradient(3, 3, 3 + SIZE, 3 + SIZE, 0xFF2E5E2A, 0xFFDCE6F2);
        }
        Gfx.text(ctx, "N", 3 + SIZE / 2 - 2, 4, 0xFFE0384F, true);
        String c = mc.player == null ? "12, -30" : mc.player.getBlockX() + ", " + mc.player.getBlockZ();
        Gfx.centered(ctx, c, w / 2, SIZE + 5, s.text);
    }
}
