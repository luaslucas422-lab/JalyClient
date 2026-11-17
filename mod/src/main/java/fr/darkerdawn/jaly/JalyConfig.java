package fr.darkerdawn.jaly;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Everything JalyClient remembers, saved to config/jalyclient.json. */
public class JalyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file;
    private static JalyConfig instance = new JalyConfig();

    public static JalyConfig get() { return instance; }

    public static class HudStyle {
        public float scale = 1f;
        public int text = 0xFFF6F4F7;
        public int accent = 0xFFFDE68A;
        public String bg = "pine"; // pine, candy, frost, ginger, none
        public boolean brackets = false;
        public boolean shadow = true;
    }

    public int version = 1;
    public Map<String, Boolean> mods = new HashMap<>();
    public Map<String, float[]> hudPos = new HashMap<>();
    public Map<String, HudStyle> hudStyle = new HashMap<>();

    // features
    public String crosshair = "snowflake";
    public int crosshairColor = 0xFFFFFFFF;
    public String snowfallLevel = "light";
    public String hitSound = "jingle";
    public float sfxVolume = 0.8f;
    public double zoomLevel = 4.0;
    public boolean zoomSmooth = true;
    public boolean zoomCinematic = true;
    public double prevGamma = 0.5;
    public float skyLyricsSize = 1.5f;
    public float skyLyricsHeight = 28f;
    public int skyLyricsColor = 0xFFFDE68A;
    public boolean skyLyricsNext = true;

    // cosmetics (all free)
    public String hat = "santa";
    public String cape = "candy_cane";

    // general
    public String intensity = "full"; // full, cozy, calm
    public boolean jalyTitle = true;
    public boolean loadingScreen = true;
    public boolean menuSnow = true;
    public boolean menuMusic = true;
    public float musicVolume = 0.7f;
    public boolean hoverSounds = true;
    public float hudScale = 1f;

    // gifts
    public long firstDay = 0;
    public List<Integer> opened = new ArrayList<>();
    public boolean seenLodgeHint = false;

    public boolean on(String id, boolean def) {
        Boolean b = mods.get(id);
        return b == null ? def : b;
    }

    public void set(String id, boolean value) {
        mods.put(id, value);
        save();
    }

    public HudStyle style(String id) {
        return hudStyle.computeIfAbsent(id, k -> new HudStyle());
    }

    public float snowAmount() {
        return switch (intensity) {
            case "calm" -> 0f;
            case "cozy" -> 0.45f;
            default -> 1f;
        };
    }

    public static void load(Path configDir) {
        file = configDir.resolve("jalyclient.json");
        try {
            if (Files.exists(file)) {
                JalyConfig c = GSON.fromJson(Files.readString(file), JalyConfig.class);
                if (c != null) instance = c;
            }
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not read JalyClient config, using defaults", e);
            instance = new JalyConfig();
        }
        if (instance.mods == null) instance.mods = new HashMap<>();
        if (instance.hudPos == null) instance.hudPos = new HashMap<>();
        if (instance.hudStyle == null) instance.hudStyle = new HashMap<>();
        if (instance.opened == null) instance.opened = new ArrayList<>();
        if (instance.firstDay == 0) instance.firstDay = LocalDate.now().toEpochDay();
        save();
    }

    public static void save() {
        if (file == null) return;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling("jalyclient.json.tmp");
            Files.writeString(tmp, GSON.toJson(instance));
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not save JalyClient config", e);
        }
    }

    public static String toJson() { return GSON.toJson(instance); }
}
