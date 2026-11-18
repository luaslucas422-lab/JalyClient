package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** Jaly Radio: music-box Christmas carols while you play (all public-domain songs, arranged for JalyClient). */
public final class Radio {
    private Radio() {}

    public static final String[][] TRACKS = {
        {"menu_music", "Jingle Bells"}, {"carol_deck", "Deck the Halls"}, {"carol_wish", "We Wish You a Merry Christmas"}, {"carol_silent", "Silent Night"}
    };
    private static SoundInstance playing;
    private static long startedAt, toastAt = -100000;
    private static String toast = "";
    public static String current = "";

    public static void tick(MinecraftClient mc) {
        boolean on = Mods.on("radio") && mc.world != null;
        if (!on) {
            stop(mc);
            return;
        }
        mc.getMusicTracker().stop();
        boolean ended = playing == null || (System.currentTimeMillis() - startedAt > 3000 && !mc.getSoundManager().isPlaying(playing));
        if (ended) next(mc, playing != null);
    }

    public static void next(MinecraftClient mc, boolean advance) {
        if (playing != null) mc.getSoundManager().stop(playing);
        JalyConfig c = JalyConfig.get();
        if (advance) c.radioTrack = (c.radioTrack + 1) % TRACKS.length;
        String[] t = TRACKS[Math.floorMod(c.radioTrack, TRACKS.length)];
        playing = PositionedSoundInstance.music(SoundEvent.of(Identifier.of("jalyclient", t[0])));
        mc.getSoundManager().play(playing);
        startedAt = System.currentTimeMillis();
        current = t[1];
        toast = "Jaly Radio: " + t[1];
        toastAt = System.currentTimeMillis();
    }

    public static void stop(MinecraftClient mc) {
        if (playing != null) {
            mc.getSoundManager().stop(playing);
            playing = null;
            current = "";
        }
    }

    public static void drawToast(DrawContext ctx, int sw) {
        long age = System.currentTimeMillis() - toastAt;
        if (age > 4500 || toast.isEmpty()) return;
        float a = age < 300 ? age / 300f : age > 4000 ? (4500 - age) / 500f : 1f;
        int w = Gfx.font().getWidth(toast) + 24, x = (sw - w) / 2, y = 8 + (int) ((1 - a) * -10);
        Gfx.round(ctx, x, y, w, 15, Gfx.alpha(0xE0122312, a));
        Gfx.outline(ctx, x, y, w, 15, Gfx.alpha(Gfx.GOLD, a));
        Gfx.candy(ctx, x + 2, y + 13, w - 4, 1, (int) (Gfx.now() / 80 % 8), a);
        Gfx.sprite(ctx, new String[]{"..##", "..#.", "..#.", "###.", "###."}, new int[]{Gfx.GOLD_LIGHT}, "#", x + 6, y + 5, 1, a);
        Gfx.text(ctx, toast, x + 14, y + 4, Gfx.alpha(Gfx.SNOW, a), true);
    }
}
