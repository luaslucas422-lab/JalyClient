package fr.darkerdawn.jaly.visual;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Interactive Blocks (singleplayer): grass you keep walking over wears down into a dirt path,
 * and plants you keep stomping through get trampled flat.
 */
public final class InteractiveBlocks {
    private InteractiveBlocks() {}

    private static final Map<Long, Integer> STEPS = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Integer> e) { return size() > 6000; }
    };
    private static BlockPos lastPos;

    public static int stepsToWear() {
        return switch (JalyConfig.get().wearSpeed) { case "fast" -> 6; case "slow" -> 40; default -> 16; };
    }

    public static void tick(MinecraftClient mc) {
        if (!Mods.on("interactive_blocks") || mc.player == null || mc.getServer() == null || !mc.player.isOnGround() || mc.player.isSpectator()) return;
        BlockPos feet = mc.player.getBlockPos();
        if (feet.equals(lastPos)) return;
        lastPos = feet;
        BlockPos below = feet.down();
        var server = mc.getServer();
        var key = mc.world.getRegistryKey();
        boolean trample = JalyConfig.get().tramplePlants;
        int need = stepsToWear();
        server.execute(() -> {
            ServerWorld w = server.getWorld(key);
            if (w == null) return;
            // grass under your feet -> path
            BlockState ground = w.getBlockState(below);
            if (ground.isOf(Blocks.GRASS_BLOCK) && w.getBlockState(feet).isAir() || ground.isOf(Blocks.GRASS_BLOCK) && w.getBlockState(feet).isReplaceable()) {
                int n = STEPS.merge(below.asLong(), 1, Integer::sum);
                if (n >= need) {
                    STEPS.remove(below.asLong());
                    if (!w.getBlockState(feet).isAir()) w.breakBlock(feet, false);
                    w.setBlockState(below, Blocks.DIRT_PATH.getDefaultState());
                    w.playSound(null, below, BlockSoundGroup.GRAVEL.getStepSound(), SoundCategory.BLOCKS, 0.7f, 0.8f);
                } else if (n == need / 2) {
                    w.spawnParticles(new net.minecraft.particle.BlockStateParticleEffect(net.minecraft.particle.ParticleTypes.BLOCK, Blocks.DIRT.getDefaultState()),
                        below.getX() + 0.5, below.getY() + 1.02, below.getZ() + 0.5, 8, 0.25, 0.02, 0.25, 0.02);
                }
            }
            // plants you walk through -> trampled
            if (trample) {
                BlockState plant = w.getBlockState(feet);
                if (plant.isOf(Blocks.SHORT_GRASS) || plant.isOf(Blocks.FERN)) {
                    int n = STEPS.merge(feet.asLong() ^ 0x5DEECE66DL, 1, Integer::sum);
                    if (n >= Math.max(3, need / 3)) {
                        STEPS.remove(feet.asLong() ^ 0x5DEECE66DL);
                        w.breakBlock(feet, false);
                    }
                }
            }
        });
    }

    /** For the smoke test. */
    public static int tracked() { return STEPS.size(); }
}
