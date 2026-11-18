package fr.darkerdawn.jaly.cosmetic;

import fr.darkerdawn.jaly.Festive;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Free festive cosmetics: hats, capes, wings, pets and trails. Some are gift exclusives from the daily presents. */
public final class Cosmetics {
    private Cosmetics() {}

    public enum Type {
        HAT("hat", "Hats"), CAPE("cape", "Capes"), WINGS("wings", "Wings"), PET("pet", "Pets"), TRAIL("trail", "Trails");
        public final String key, label;
        Type(String k, String l) { key = k; label = l; }
    }

    public static final Map<Type, Map<String, String>> ALL = new LinkedHashMap<>();
    private static final Map<String, Supplier<TexturedModelData>> MODELS = new HashMap<>();
    private static final Map<String, ModelPart> BUILT = new HashMap<>();

    private static void put(Type t, String id, String name) { ALL.computeIfAbsent(t, k -> new LinkedHashMap<>()).put(id, name); }

    static {
        for (Type t : Type.values()) put(t, "none", "None");
        put(Type.HAT, "santa", "Santa Hat"); put(Type.HAT, "antlers", "Reindeer Antlers"); put(Type.HAT, "elf", "Elf Hat");
        put(Type.HAT, "tophat", "Snowman Top Hat"); put(Type.HAT, "halo", "Star Halo"); put(Type.HAT, "beanie", "Candy Beanie");
        put(Type.HAT, "holly", "Holly Crown"); put(Type.HAT, "gift", "Gift Hat");
        put(Type.CAPE, "candy_cane", "Candy Cane"); put(Type.CAPE, "tree", "Christmas Tree"); put(Type.CAPE, "snowflake", "Snowflake");
        put(Type.CAPE, "gingerbread", "Gingerbread"); put(Type.CAPE, "aurora", "Northern Lights"); put(Type.CAPE, "sweater", "Ugly Sweater");
        put(Type.CAPE, "mint", "Mint Candy"); put(Type.CAPE, "snowy_night", "Snowy Night"); put(Type.CAPE, "present", "Present Wrap");
        put(Type.CAPE, "golden", "Golden Jaly");
        put(Type.WINGS, "angel", "Angel Wings"); put(Type.WINGS, "candy", "Candy Wings"); put(Type.WINGS, "ice", "Ice Wings");
        put(Type.WINGS, "fairy", "Fairy Light Wings"); put(Type.WINGS, "gold", "Golden Wings");
        put(Type.PET, "snowman", "Mini Snowman"); put(Type.PET, "penguin", "Santa Penguin"); put(Type.PET, "present", "Living Present");
        put(Type.PET, "tree", "Tiny Tree"); put(Type.PET, "reindeer", "Baby Reindeer");
        put(Type.TRAIL, "snow", "Snowflakes"); put(Type.TRAIL, "candy", "Candy Sparkles"); put(Type.TRAIL, "hearts", "Hearts");
        put(Type.TRAIL, "notes", "Carol Notes"); put(Type.TRAIL, "gold", "Gold Dust"); put(Type.TRAIL, "stars", "Starlight");
        put(Type.TRAIL, "aurora", "Aurora"); put(Type.TRAIL, "cherry", "Cherry Blossom"); put(Type.TRAIL, "embers", "Fireplace Embers");
        put(Type.TRAIL, "totem", "Totem Glow");
        MODELS.put("santa", HatModels::santa); MODELS.put("antlers", HatModels::antlers); MODELS.put("elf", HatModels::elf);
        MODELS.put("tophat", HatModels::tophat); MODELS.put("halo", HatModels::halo); MODELS.put("beanie", HatModels::beanie);
        MODELS.put("holly", HatModels::holly); MODELS.put("gift", HatModels::gift);
        MODELS.put("pet_snowman", HatModels::pet_snowman); MODELS.put("pet_penguin", HatModels::pet_penguin);
        MODELS.put("pet_present", HatModels::pet_present); MODELS.put("pet_tree", HatModels::pet_tree); MODELS.put("pet_reindeer", HatModels::pet_reindeer);
    }

    public static String name(Type t, String id) { return ALL.get(t).getOrDefault(id, id); }

    /** Display name for a reward string like "wings:ice". */
    public static String rewardName(String reward) {
        String[] p = reward.split(":");
        for (Type t : Type.values()) if (t.key.equals(p[0])) return name(t, p[1]) + " (" + t.label.substring(0, t.label.length() - 1) + ")";
        return reward;
    }

    public static boolean unlocked(Type t, String id) {
        if (id.equals("none")) return true;
        int door = Festive.doorFor(t.key + ":" + id);
        return door == 0 || JalyConfig.get().opened.contains(door);
    }

    public static String selected(Type t) {
        JalyConfig c = JalyConfig.get();
        String s = switch (t) { case HAT -> c.hat; case CAPE -> c.cape; case WINGS -> c.wings; case PET -> c.pet; case TRAIL -> c.trail; };
        return s == null || !ALL.get(t).containsKey(s) || !unlocked(t, s) ? "none" : s;
    }

    public static void select(Type t, String id) {
        JalyConfig c = JalyConfig.get();
        switch (t) { case HAT -> c.hat = id; case CAPE -> c.cape = id; case WINGS -> c.wings = id; case PET -> c.pet = id; case TRAIL -> c.trail = id; }
        JalyConfig.save();
    }

    public static Identifier icon(Type t, String id) {
        return switch (t) {
            case HAT -> Identifier.of("jalyclient", "textures/gui/hat_" + id + ".png");
            case PET -> Identifier.of("jalyclient", "textures/gui/hat_pet_" + id + ".png");
            case WINGS -> Identifier.of("jalyclient", "textures/gui/wings_" + id + ".png");
            case TRAIL -> Identifier.of("jalyclient", "textures/gui/trail_" + id + ".png");
            case CAPE -> capeTexture(id);
        };
    }

    public static Identifier hatTexture(String id) { return Identifier.of("jalyclient", "textures/hat/" + id + ".png"); }

    public static Identifier hatIcon(String id) { return icon(Type.HAT, id); }

    public static Identifier capeTexture(String id) { return Identifier.of("jalyclient", "textures/cape/" + id + ".png"); }

    public static Identifier wingTexture(String id) { return Identifier.of("jalyclient", "textures/wings/" + id + ".png"); }

    public static ModelPart model(String id) {
        Supplier<TexturedModelData> s = MODELS.get(id);
        if (s == null) return null;
        return BUILT.computeIfAbsent(id, k -> s.get().createModel());
    }

    public static String hatFor(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.player) {
            String h = selected(Type.HAT);
            return h.equals("none") ? null : h;
        }
        if (e instanceof PlayerEntity) return Mods.on("player_hats") ? "santa" : null;
        return Mods.on("mob_hats") ? "santa" : null;
    }

    public static Identifier capeFor(Entity e) {
        if (e != MinecraftClient.getInstance().player) return null;
        String c = selected(Type.CAPE);
        return c.equals("none") ? null : capeTexture(c);
    }
}
