package fr.darkerdawn.jaly;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/** JalyClient sounds. Not put in the registry so the client can join any server safely. */
public final class Sounds {
    private Sounds() {}

    public static final SoundEvent JINGLE = of("jingle");
    public static final SoundEvent SLEIGH = of("sleigh");
    public static final SoundEvent CHIME = of("chime");
    public static final SoundEvent CLICK = of("click");
    public static final SoundEvent HOVER = of("hover");
    public static final SoundEvent KILL = of("kill");
    public static final SoundEvent UNWRAP = of("unwrap");
    public static final SoundEvent MENU_MUSIC = of("menu_music");

    private static SoundInstance music;

    private static SoundEvent of(String name) {
        return SoundEvent.of(Identifier.of("jalyclient", name));
    }

    public static void play(SoundEvent e, float pitch, float volume) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;
        float v = volume * JalyConfig.get().sfxVolume;
        if (v <= 0.01f) return;
        mc.getSoundManager().play(PositionedSoundInstance.master(e, pitch, v));
    }

    public static SoundEvent hitSound() {
        return switch (JalyConfig.get().hitSound) {
            case "sleigh" -> SLEIGH;
            case "chime" -> CHIME;
            default -> JINGLE;
        };
    }

    /** Keeps the Jingle Bells music box playing on JalyClient menus (replaces vanilla menu music). */
    public static void tickMenuMusic(MinecraftClient mc) {
        String track = JalyConfig.get().menuTrack;
        if (!JalyConfig.get().menuMusic || "none".equals(track)) { stopMenuMusic(mc); return; }
        mc.getMusicTracker().stop();
        if (music == null || !mc.getSoundManager().isPlaying(music)) {
            music = PositionedSoundInstance.music(SoundEvent.of(Identifier.of("jalyclient", track == null ? "menu_music" : track)));
            mc.getSoundManager().play(music);
        }
    }

    public static void stopMenuMusic(MinecraftClient mc) {
        if (music != null) {
            mc.getSoundManager().stop(music);
            music = null;
        }
    }
}
