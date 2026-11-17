package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.hud.Hud;
import fr.darkerdawn.jaly.hud.HudModule;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Every built-in mod shown on the Mods page, with its simple toggle and its Advanced settings. */
public final class Mods {
    private Mods() {}

    public enum Category {
        HUD("HUD"), FESTIVE("Festive"), MUSIC("Music"), PVP("PvP"), VISUAL("Visual");
        public final String label;
        Category(String l) { label = l; }
    }

    public sealed interface Setting permits Toggle, Cycle, Slider, Swatch, Action {
        String name();
    }

    public record Toggle(String name, Supplier<Boolean> get, Consumer<Boolean> set) implements Setting {}

    public record Cycle(String name, String[] values, String[] labels, Supplier<String> get, Consumer<String> set) implements Setting {}

    public record Slider(String name, double min, double max, double step, DoubleSupplier get, Consumer<Double> set, String unit) implements Setting {}

    public record Action(String name, String button, Runnable run) implements Setting {}

    public record Swatch(String name, int[] colors, IntSupplier get, Consumer<Integer> set) implements Setting {}

    public static final class Entry {
        public final String id, name, desc;
        public final Category category;
        public final boolean defaultOn;
        public final List<Setting> settings = new ArrayList<>();
        public final HudModule hud;

        Entry(String id, String name, String desc, Category category, boolean defaultOn, HudModule hud) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.category = category;
            this.defaultOn = defaultOn;
            this.hud = hud;
        }

        public boolean on() { return JalyConfig.get().on(id, defaultOn); }

        public void set(boolean v) {
            JalyConfig.get().set(id, v);
            Features.onToggled(id, v);
        }

        Entry with(Setting s) { settings.add(s); return this; }
    }

    public static final int[] TEXT_COLORS = {0xFFF6F4F7, 0xFFFDE68A, 0xFFE0384F, 0xFF5CFF7A, 0xFF9BC3E7, 0xFFFFB86B, 0xFFE0C3FF, 0xFF000000};
    public static final List<Entry> ALL = new ArrayList<>();

    public static Entry byId(String id) {
        for (Entry e : ALL) if (e.id.equals(id)) return e;
        return null;
    }

    public static boolean on(String id) {
        Entry e = byId(id);
        return e != null && e.on();
    }

    static Entry add(Entry e) { ALL.add(e); return e; }

    private static void hudSettings(Entry e) {
        String id = e.id;
        e.with(new Slider("Size", 0.5, 2.0, 0.05, () -> JalyConfig.get().style(id).scale, v -> { JalyConfig.get().style(id).scale = v.floatValue(); JalyConfig.save(); }, "x"));
        e.with(new Swatch("Text colour", TEXT_COLORS, () -> JalyConfig.get().style(id).text, v -> { JalyConfig.get().style(id).text = v; JalyConfig.save(); }));
        e.with(new Swatch("Label colour", TEXT_COLORS, () -> JalyConfig.get().style(id).accent, v -> { JalyConfig.get().style(id).accent = v; JalyConfig.save(); }));
        e.with(new Cycle("Background", new String[]{"pine", "candy", "frost", "ginger", "none"}, new String[]{"Pine glass", "Candy cane", "Frost", "Gingerbread", "None"},
            () -> JalyConfig.get().style(id).bg, v -> { JalyConfig.get().style(id).bg = v; JalyConfig.save(); }));
        e.with(new Toggle("[Brackets]", () -> JalyConfig.get().style(id).brackets, v -> { JalyConfig.get().style(id).brackets = v; JalyConfig.save(); }));
        e.with(new Toggle("Text shadow", () -> JalyConfig.get().style(id).shadow, v -> { JalyConfig.get().style(id).shadow = v; JalyConfig.save(); }));
    }

    public static void init() {
        if (!ALL.isEmpty()) return;
        for (HudModule m : Hud.MODULES) {
            Entry e = add(new Entry(m.id, m.name, m.desc, m.id.equals("now_playing") ? Category.MUSIC : Category.HUD, m.defaultOn, m));
            if (m.id.equals("now_playing")) e.with(new Toggle("Show lyrics line", () -> JalyConfig.get().on("now_playing_lyrics", true), v -> JalyConfig.get().set("now_playing_lyrics", v)));
            hudSettings(e);
        }
        JalyConfig c = JalyConfig.get();
        add(new Entry("snowfall", "Snowfall", "Gentle snow falls around you in every biome", Category.FESTIVE, true, null))
            .with(new Cycle("Amount", new String[]{"light", "medium", "blizzard"}, new String[]{"Light", "Medium", "Blizzard"}, () -> c.snowfallLevel, v -> { c.snowfallLevel = v; JalyConfig.save(); }));
        add(new Entry("lights", "Twinkle Lights", "A string of fairy lights across the top of your screen", Category.FESTIVE, true, null));
        add(new Entry("frost", "Frost Vignette", "Icy frost creeps in from the screen edges", Category.FESTIVE, false, null));
        add(new Entry("chests", "Christmas Chests", "Chests are wrapped presents all year", Category.FESTIVE, true, null));
        add(new Entry("hit_sound", "Jingle Hits", "A jingle every time you hit something", Category.FESTIVE, true, null))
            .with(new Cycle("Sound", new String[]{"jingle", "sleigh", "chime"}, new String[]{"Jingle bell", "Sleigh bells", "Chime"}, () -> c.hitSound, v -> { c.hitSound = v; JalyConfig.save(); Sounds.play(Sounds.hitSound(), 1f, 1f); }));
        add(new Entry("kill_effect", "Snow Burst", "Snowflakes and sparkles when something you hit is defeated", Category.FESTIVE, true, null));
        add(new Entry("mob_hats", "Mob Hats", "Zombies, skeletons and friends wear Santa hats", Category.FESTIVE, false, null));
        add(new Entry("player_hats", "Hats on Players", "Other players get a Santa hat too (only you see it)", Category.FESTIVE, false, null));
        add(new Entry("sky_lyrics", "Sky Lyrics", "The lyrics of your Spotify song float in the sky", Category.MUSIC, false, null))
            .with(new Slider("Text size", 0.5, 3, 0.1, () -> c.skyLyricsSize, v -> { c.skyLyricsSize = v.floatValue(); JalyConfig.save(); }, "x"))
            .with(new Slider("Height in the sky", 10, 60, 1, () -> c.skyLyricsHeight, v -> { c.skyLyricsHeight = v.floatValue(); JalyConfig.save(); }, ""))
            .with(new Swatch("Colour", TEXT_COLORS, () -> c.skyLyricsColor, v -> { c.skyLyricsColor = v; JalyConfig.save(); }))
            .with(new Toggle("Show next line", () -> c.skyLyricsNext, v -> { c.skyLyricsNext = v; JalyConfig.save(); }));
        add(new Entry("zoom", "Zoom", "Hold C to zoom. Scroll to zoom further", Category.PVP, true, null))
            .with(new Slider("Zoom amount", 2, 10, 0.5, () -> c.zoomLevel, v -> { c.zoomLevel = v; JalyConfig.save(); }, "x"))
            .with(new Toggle("Smooth zoom", () -> c.zoomSmooth, v -> { c.zoomSmooth = v; JalyConfig.save(); }))
            .with(new Toggle("Cinematic camera", () -> c.zoomCinematic, v -> { c.zoomCinematic = v; JalyConfig.save(); }));
        add(new Entry("toggle_sprint", "Toggle Sprint", "Always sprint without holding the key", Category.PVP, false, null));
        add(new Entry("crosshair", "Festive Crosshair", "Snowflake, star, candy or tree crosshairs", Category.VISUAL, true, null))
            .with(new Cycle("Style", new String[]{"snowflake", "star", "candy", "tree", "dot", "plus"}, new String[]{"Snowflake", "Star", "Candy plus", "Tiny tree", "Dot", "Plus"}, () -> c.crosshair, v -> { c.crosshair = v; JalyConfig.save(); }))
            .with(new Swatch("Colour", TEXT_COLORS, () -> c.crosshairColor, v -> { c.crosshairColor = v; JalyConfig.save(); }));
        add(new Entry("fullbright", "Fullbright", "See clearly in the dark", Category.VISUAL, false, null));
        add(new Entry("low_fire", "Low Fire", "Lowers the fire overlay so you can see", Category.VISUAL, true, null));
    }
}
