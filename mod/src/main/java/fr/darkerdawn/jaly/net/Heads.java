package fr.darkerdawn.jaly.net;

import com.mojang.authlib.GameProfile;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Minecraft heads for the friends list (real skins via Mojang, Steve/Alex while loading). */
public final class Heads {
    private Heads() {}

    private static final Map<UUID, GameProfile> PROFILES = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ASKED = new ConcurrentHashMap<>();

    public static Identifier skin(String uuidStr) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (Exception e) {
            return DefaultSkinHelper.getTexture();
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        GameProfile prof = PROFILES.get(uuid);
        if (prof == null && ASKED.putIfAbsent(uuid, true) == null) {
            CompletableFuture.runAsync(() -> {
                try {
                    var res = mc.getSessionService().fetchProfile(uuid, false);
                    if (res != null) PROFILES.put(uuid, res.profile());
                } catch (Exception ignored) {
                }
            });
        }
        if (prof != null) return mc.getSkinProvider().getSkinTextures(prof).texture();
        return DefaultSkinHelper.getSkinTextures(uuid).texture();
    }

    /** Draws a head (face + hat layer) at x,y with the given size. */
    public static void draw(DrawContext ctx, String uuid, int x, int y, int size, float alpha) {
        Identifier skin = skin(uuid);
        Gfx.tex(ctx, skin, x, y, size, size, 8, 8, 8, 8, 64, 64, alpha);
        Gfx.tex(ctx, skin, x, y, size, size, 40, 8, 8, 8, 64, 64, alpha);
    }
}
