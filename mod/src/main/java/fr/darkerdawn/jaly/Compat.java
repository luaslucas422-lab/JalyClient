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

    /** ModelTransform.pivot became origin in 1.21.5. */
    public static net.minecraft.client.model.ModelTransform at(float x, float y, float z) {
        //#if >=1.21.5
        return net.minecraft.client.model.ModelTransform.origin(x, y, z);
        //#else
        return net.minecraft.client.model.ModelTransform.pivot(x, y, z);
        //#endif
    }

    public static void applyTransform(net.minecraft.client.model.ModelPart part, net.minecraft.client.util.math.MatrixStack ms) {
        //#if >=1.21.5
        part.applyTransform(ms);
        //#else
        part.rotate(ms);
        //#endif
    }

    public static void particle(World w, net.minecraft.particle.ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
        //#if >=1.21.5
        w.addParticleClient(fx, x, y, z, vx, vy, vz);
        //#else
        w.addParticle(fx, x, y, z, vx, vy, vz);
        //#endif
    }

    public static net.minecraft.client.texture.NativeImageBackedTexture texture(NativeImage img, String name) {
        //#if >=1.21.5
        return new net.minecraft.client.texture.NativeImageBackedTexture(() -> "jalyclient:" + name, img);
        //#else
        return new net.minecraft.client.texture.NativeImageBackedTexture(img);
        //#endif
    }

    public static float prevBodyYaw(net.minecraft.entity.LivingEntity e) {
        //#if >=1.21.5
        return e.lastBodyYaw;
        //#else
        return e.prevBodyYaw;
        //#endif
    }

    public static float prevHeadYaw(net.minecraft.entity.LivingEntity e) {
        //#if >=1.21.5
        return e.lastHeadYaw;
        //#else
        return e.prevHeadYaw;
        //#endif
    }

    public static void setPrevYaws(net.minecraft.entity.LivingEntity e, float body, float head) {
        //#if >=1.21.5
        e.lastBodyYaw = body;
        e.lastHeadYaw = head;
        //#else
        e.prevBodyYaw = body;
        e.prevHeadYaw = head;
        //#endif
    }

    public static String nbtString(net.minecraft.nbt.NbtCompound n, String key) {
        //#if >=1.21.5
        return n.getString(key, null);
        //#else
        return n.contains(key) ? n.getString(key) : null;
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
