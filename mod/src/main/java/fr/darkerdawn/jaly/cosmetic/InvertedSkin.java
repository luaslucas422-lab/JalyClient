package fr.darkerdawn.jaly.cosmetic;

//#if <1.21.5
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
//#endif
import fr.darkerdawn.jaly.JalyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/** Makes a colour-inverted copy of a skin texture (for the Jolly Shop's shopkeeper). */
public final class InvertedSkin {
    private InvertedSkin() {}

    private static final Identifier OUT = Identifier.of("jalyclient", "dynamic/shopkeeper");
    private static Identifier source;
    private static long retryAt;
    private static boolean broken;

    public static Identifier get(MinecraftClient mc, Identifier skin) {
        if (skin.equals(source)) return OUT;
        if (broken || System.currentTimeMillis() < retryAt) return skin;
        retryAt = System.currentTimeMillis() + 1000;
        try {
            AbstractTexture tex = mc.getTextureManager().getTexture(skin);
            //#if >=1.21.5
            NativeImage src;
            if (tex instanceof NativeImageBackedTexture nb && nb.getImage() != null) {
                src = nb.getImage();
            } else {
                var res = mc.getResourceManager().getResource(skin);
                if (res.isEmpty()) return skin; // not downloaded yet
                try (var in = res.get().getInputStream()) { src = NativeImage.read(in); }
            }
            int w = src.getWidth(), h = src.getHeight();
            if (w != 64 || (h != 64 && h != 32)) return skin;
            NativeImage img = new NativeImage(w, h, false);
            img.copyFrom(src);
            //#else
            RenderSystem.bindTexture(tex.getGlId());
            int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w != 64 || (h != 64 && h != 32)) return skin; // not downloaded yet
            NativeImage img = new NativeImage(w, h, false);
            img.loadFromTextureImage(0, false);
            //#endif
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = fr.darkerdawn.jaly.Compat.getPixel(img, x, y);
                    if ((c >>> 24) != 0) fr.darkerdawn.jaly.Compat.setPixel(img, x, y, c ^ 0x00FFFFFF);
                }
            }
            mc.getTextureManager().registerTexture(OUT, fr.darkerdawn.jaly.Compat.texture(img, "img"));
            source = skin;
            return OUT;
        } catch (Throwable t) {
            JalyClient.LOG.warn("Couldn't invert the shopkeeper skin", t);
            broken = true;
            return skin;
        }
    }
}
