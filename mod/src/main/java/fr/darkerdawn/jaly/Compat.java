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

    /** Entity in a GUI, centred on (x, y), `size` pixels per block. */
    public static void drawEntity(net.minecraft.client.gui.DrawContext ctx, float x, float y, float size, org.joml.Vector3f off, org.joml.Quaternionf rot, org.joml.Quaternionf cam, net.minecraft.entity.LivingEntity e) {
        //#if >=1.21.6
        int hw = Math.round(size * 1.2f), hh = Math.round(size * Math.max(1.2f, e.getHeight() * 0.75f));
        //#if >=1.21.11
        // 1.21.11 only has the "looks at the mouse" version: aim it straight ahead
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, Math.round(x) - hw, Math.round(y) - hh, Math.round(x) + hw, Math.round(y) + hh, Math.round(size), 0.0625f, x, y - size * 1.4f, e);
        //#else
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, Math.round(x) - hw, Math.round(y) - hh, Math.round(x) + hw, Math.round(y) + hh, size, off, rot, cam, e);
        //#endif
        //#else
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, x, y, size, off, rot, cam, e);
        //#endif
    }

    public static net.minecraft.server.world.ServerWorld serverWorld(net.minecraft.server.network.ServerPlayerEntity p) {
        //#if >=1.21.9
        return p.getEntityWorld();
        //#elif >=1.21.6
        return p.getWorld();
        //#else
        return p.getServerWorld();
        //#endif
    }

    public static net.minecraft.client.sound.PositionedSoundInstance music(net.minecraft.sound.SoundEvent ev) {
        //#if >=1.21.11
        return net.minecraft.client.sound.PositionedSoundInstance.music(ev);
        //#elif >=1.21.6
        return net.minecraft.client.sound.PositionedSoundInstance.music(ev, 1f);
        //#else
        return net.minecraft.client.sound.PositionedSoundInstance.music(ev);
        //#endif
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

    //#if >=1.21.9
    public static final net.minecraft.client.option.KeyBinding.Category KEY_CATEGORY = net.minecraft.client.option.KeyBinding.Category.create(net.minecraft.util.Identifier.of("jalyclient", "main"));
    //#else
    public static final String KEY_CATEGORY = "key.categories.jalyclient";
    //#endif

    public static net.minecraft.util.math.Vec3d pos(net.minecraft.entity.Entity e) {
        //#if >=1.21.9
        return e.getEntityPos();
        //#else
        return e.getPos();
        //#endif
    }

    public static net.minecraft.world.World world(net.minecraft.entity.Entity e) {
        //#if >=1.21.9
        return e.getEntityWorld();
        //#else
        return e.getWorld();
        //#endif
    }

    public static boolean isClient(net.minecraft.world.World w) {
        //#if >=1.21.9
        return w.isClient();
        //#else
        return w.isClient;
        //#endif
    }

    public static boolean keyDown(int code) {
        //#if >=1.21.9
        return net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow(), code);
        //#else
        return net.minecraft.client.util.InputUtil.isKeyPressed(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), code);
        //#endif
    }

    public static java.util.UUID profileId(com.mojang.authlib.GameProfile p) {
        //#if >=1.21.9
        return p.id();
        //#else
        return p.getId();
        //#endif
    }

    public static String profileName(com.mojang.authlib.GameProfile p) {
        //#if >=1.21.9
        return p.name();
        //#else
        return p.getName();
        //#endif
    }

    public static com.mojang.authlib.minecraft.MinecraftSessionService sessionService(net.minecraft.client.MinecraftClient mc) {
        //#if >=1.21.9
        return mc.getApiServices().sessionService();
        //#else
        return mc.getSessionService();
        //#endif
    }

    public static net.minecraft.util.math.Vec3d camPos() {
        //#if >=1.21.9
        return net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getCameraPos();
        //#else
        return net.minecraft.client.MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        //#endif
    }

    //#if >=1.21.9
    private static net.minecraft.entity.player.SkinTextures skinOf(com.mojang.authlib.GameProfile p) {
        return net.minecraft.client.MinecraftClient.getInstance().getSkinProvider().supplySkinTextures(p, true).get();
    }
    //#else
    private static net.minecraft.client.util.SkinTextures skinOf(com.mojang.authlib.GameProfile p) {
        return net.minecraft.client.MinecraftClient.getInstance().getSkinProvider().getSkinTextures(p);
    }
    //#endif

    public static net.minecraft.util.Identifier profileSkin(com.mojang.authlib.GameProfile p) {
        //#if >=1.21.9
        return skinOf(p).body().texturePath();
        //#else
        return skinOf(p).texture();
        //#endif
    }

    public static boolean profileSlim(com.mojang.authlib.GameProfile p) {
        //#if >=1.21.9
        return skinOf(p).model() == net.minecraft.entity.player.PlayerSkinType.SLIM;
        //#else
        return skinOf(p).model() == net.minecraft.client.util.SkinTextures.Model.SLIM;
        //#endif
    }

    public static net.minecraft.util.Identifier playerSkin(net.minecraft.client.network.AbstractClientPlayerEntity p) {
        //#if >=1.21.9
        return p.getSkin().body().texturePath();
        //#else
        return p.getSkinTextures().texture();
        //#endif
    }

    public static net.minecraft.util.Identifier defaultSkin(java.util.UUID id) {
        //#if >=1.21.9
        return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(id).body().texturePath();
        //#else
        return net.minecraft.client.util.DefaultSkinHelper.getSkinTextures(id).texture();
        //#endif
    }

    // ---- render layers (1.21.11 moved the factories to RenderLayers)
    public static net.minecraft.client.render.RenderLayer cutoutNoCull(net.minecraft.util.Identifier id) {
        //#if >=1.21.11
        return net.minecraft.client.render.RenderLayers.entityCutoutNoCull(id);
        //#else
        return net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(id);
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer translucent(net.minecraft.util.Identifier id) {
        //#if >=1.21.11
        return net.minecraft.client.render.RenderLayers.entityTranslucent(id);
        //#else
        return net.minecraft.client.render.RenderLayer.getEntityTranslucent(id);
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer solid(net.minecraft.util.Identifier id) {
        //#if >=1.21.11
        return net.minecraft.client.render.RenderLayers.entitySolid(id);
        //#else
        return net.minecraft.client.render.RenderLayer.getEntitySolid(id);
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer lightning() {
        //#if >=1.21.11
        return net.minecraft.client.render.RenderLayers.lightning();
        //#else
        return net.minecraft.client.render.RenderLayer.getLightning();
        //#endif
    }

    public static net.minecraft.client.render.RenderLayer debugQuads() {
        //#if >=1.21.11
        return net.minecraft.client.render.RenderLayers.debugQuads();
        //#else
        return net.minecraft.client.render.RenderLayer.getDebugQuads();
        //#endif
    }

    public static net.minecraft.client.sound.PositionedSoundInstance ui(net.minecraft.sound.SoundEvent e, float pitch, float volume) {
        //#if >=1.21.11
        return net.minecraft.client.sound.PositionedSoundInstance.ui(e, pitch, volume);
        //#else
        return net.minecraft.client.sound.PositionedSoundInstance.master(e, pitch, volume);
        //#endif
    }

    public static boolean opaqueCube(net.minecraft.block.BlockState s, net.minecraft.world.BlockView w, net.minecraft.util.math.BlockPos p) {
        //#if >=1.21.2
        return s.isOpaqueFullCube();
        //#else
        return s.isOpaqueFullCube(w, p);
        //#endif
    }

    /** A sprite from the block atlas, e.g. minecraft:block/sand. */
    public static net.minecraft.client.texture.Sprite blockSprite(net.minecraft.util.Identifier id) {
        //#if >=1.21.9
        return net.minecraft.client.MinecraftClient.getInstance().getAtlasManager().getSprite(new net.minecraft.client.util.SpriteIdentifier(net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, id));
        //#else
        return net.minecraft.client.MinecraftClient.getInstance().getSpriteAtlas(net.minecraft.client.texture.SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE).apply(id);
        //#endif
    }

}
