package fr.darkerdawn.jaly;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
//#if <1.21.2
import org.joml.Vector3f;
//#endif

/** Small helpers whose Minecraft API changed between versions (see the //#if blocks). */
public final class Compat {
    private Compat() {}

    private static int rgb(float r, float g, float b) {
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static DustParticleEffect dust(float r, float g, float b, float size) {
        //#if >=1.21.2
        return new DustParticleEffect(rgb(r, g, b), size);
        //#else
        return new DustParticleEffect(new Vector3f(r, g, b), size);
        //#endif
    }

    public static DustColorTransitionParticleEffect dustFade(float r1, float g1, float b1, float r2, float g2, float b2, float size) {
        //#if >=1.21.2
        return new DustColorTransitionParticleEffect(rgb(r1, g1, b1), rgb(r2, g2, b2), size);
        //#else
        return new DustColorTransitionParticleEffect(new Vector3f(r1, g1, b1), new Vector3f(r2, g2, b2), size);
        //#endif
    }

    /** Pixel get/set in the image's own colour order (whatever this version uses). */
    public static int getPixel(NativeImage img, int x, int y) {
        //#if >=1.21.2
        return img.getColorArgb(x, y);
        //#else
        return img.getColor(x, y);
        //#endif
    }

    public static void setPixel(NativeImage img, int x, int y, int c) {
        //#if >=1.21.2
        img.setColorArgb(x, y, c);
        //#else
        img.setColor(x, y, c);
        //#endif
    }

    public static float cooldown(PlayerEntity p, Item item) {
        //#if >=1.21.2
        return p.getItemCooldownManager().getCooldownProgress(new net.minecraft.item.ItemStack(item), 0f);
        //#else
        return p.getItemCooldownManager().getCooldownProgress(item, 0f);
        //#endif
    }

    public static Biome.Precipitation precipitation(World w, BlockPos pos) {
        //#if >=1.21.2
        return w.getBiome(pos).value().getPrecipitation(pos, w.getSeaLevel());
        //#else
        return w.getBiome(pos).value().getPrecipitation(pos);
        //#endif
    }
}
