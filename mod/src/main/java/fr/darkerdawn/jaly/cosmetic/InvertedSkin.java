package fr.darkerdawn.jaly.cosmetic;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.darkerdawn.jaly.JalyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

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
            RenderSystem.bindTexture(tex.getGlId());
            int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            if (w != 64 || (h != 64 && h != 32)) return skin; // not downloaded yet
            NativeImage img = new NativeImage(w, h, false);
            img.loadFromTextureImage(0, false);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = fr.darkerdawn.jaly.Compat.getPixel(img, x, y);
                    if ((c >>> 24) != 0) fr.darkerdawn.jaly.Compat.setPixel(img, x, y, c ^ 0x00FFFFFF);
                }
            }
            mc.getTextureManager().registerTexture(OUT, new NativeImageBackedTexture(img));
            source = skin;
            return OUT;
        } catch (Throwable t) {
            JalyClient.LOG.warn("Couldn't invert the shopkeeper skin", t);
            broken = true;
            return skin;
        }
    }
}
