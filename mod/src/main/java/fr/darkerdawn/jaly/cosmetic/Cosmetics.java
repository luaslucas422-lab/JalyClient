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
        HAT("hat", "Hats"), CAPE("cape", "Capes"), WINGS("wings", "Wings"), PET("pet", "Pets"), TRAIL("trail", "Trails"),
        NAME_COLOR("namecolor", "Name Colours"), NAME_FX("namefx", "Name Effects");
        public final String key, label;
        Type(String k, String l) { key = k; label = l; }
    }

    public static final Map<Type, Map<String, String>> ALL = new LinkedHashMap<>();
    private static final Map<String, Supplier<TexturedModelData>> MODELS = new HashMap<>();
    private static final Map<String, ModelPart> BUILT = new HashMap<>();

    private static void put(Type t, String id, String name) { ALL.computeIfAbsent(t, k -> new LinkedHashMap<>()).put(id, name); }

    static {
        for (Type t : Type.values()) if (t != Type.NAME_COLOR) put(t, "none", "None");
        put(Type.HAT, "santa", "Santa Hat"); put(Type.HAT, "antlers", "Reindeer Antlers"); put(Type.HAT, "elf", "Elf Hat");
        put(Type.HAT, "tophat", "Snowman Top Hat"); put(Type.HAT, "halo", "Star Halo"); put(Type.HAT, "beanie", "Candy Beanie");
        put(Type.HAT, "holly", "Holly Crown"); put(Type.HAT, "gift", "Gift Hat");
        put(Type.CAPE, "candy_cane", "Candy Cane"); put(Type.CAPE, "tree", "Christmas Tree"); put(Type.CAPE, "snowflake", "Snowflake");
        put(Type.CAPE, "gingerbread", "Gingerbread"); put(Type.CAPE, "aurora", "Northern Lights"); put(Type.CAPE, "sweater", "Ugly Sweater");
        put(Type.CAPE, "mint", "Mint Candy"); put(Type.CAPE, "snowy_night", "Snowy Night"); put(Type.CAPE, "present", "Present Wrap");
        put(Type.CAPE, "golden", "Golden Jaly"); put(Type.CAPE, "candy_flow", "Candy Flow (animated)"); put(Type.CAPE, "aurora_flow", "Aurora Flow (animated)");
        put(Type.CAPE, "snowfall", "Snowfall (animated)"); put(Type.CAPE, "holly", "Holly"); put(Type.CAPE, "fireplace", "Fireplace"); put(Type.CAPE, "og", "OG Cape");
        put(Type.HAT, "og", "OG Crown"); put(Type.PET, "og", "OG Snowman"); put(Type.TRAIL, "og", "OG Sparkle");
        put(Type.WINGS, "dragon", "Dragon Wings"); put(Type.WINGS, "butterfly", "Butterfly Wings"); put(Type.WINGS, "crystal", "Snowflake Crystal Wings");
        put(Type.WINGS, "krampus", "Krampus Wings"); put(Type.WINGS, "og", "OG Wings");
        put(Type.NAME_COLOR, "white", "Snow White"); put(Type.NAME_COLOR, "red", "Candy Red"); put(Type.NAME_COLOR, "gold", "Gold");
        put(Type.NAME_COLOR, "ice", "Ice Blue"); put(Type.NAME_COLOR, "holly", "Holly Green"); put(Type.NAME_COLOR, "ginger", "Gingerbread");
        put(Type.NAME_COLOR, "pink", "Sugar Plum"); put(Type.NAME_COLOR, "custom", "Custom (Tools)"); put(Type.NAME_COLOR, "og", "OG");
        put(Type.NAME_FX, "candy", "Candy Cane Gradient"); put(Type.NAME_FX, "rainbow", "Rainbow Gradient"); put(Type.NAME_FX, "ice", "Ice Gradient");
        put(Type.NAME_FX, "gold", "Gold Gradient"); put(Type.NAME_FX, "shine", "Shine Sweep"); put(Type.NAME_FX, "sparkles", "Snow Sparkles");
        put(Type.NAME_FX, "wave", "Colour Wave");
        // premium (Jolly Shop)
        put(Type.WINGS, "frost_dragon", "Frost Dragon Wings"); put(Type.WINGS, "aurora_phoenix", "Aurora Phoenix Wings"); put(Type.WINGS, "royal_gold", "Royal Gold Wings");
        put(Type.CAPE, "starlight", "Starlight (animated)"); put(Type.CAPE, "ice_king", "Ice King (animated)"); put(Type.CAPE, "royal_velvet", "Royal Velvet");
        put(Type.HAT, "crystal_crown", "Crystal Crown"); put(Type.HAT, "santa_mech", "Santa Mech Helmet"); put(Type.HAT, "snow_globe", "Snow Globe Head");
        put(Type.PET, "buddy", "Ginger Buddy (first friend)");
        put(Type.PET, "owl", "Snowy Owl"); put(Type.PET, "frost_dragon", "Baby Frost Dragon");
        put(Type.PET, "sleigh", "Mini Sleigh"); put(Type.PET, "ice_golem", "Ice Golem");
        put(Type.TRAIL, "fireworks", "Fireworks"); put(Type.TRAIL, "galaxy", "Galaxy");
        put(Type.NAME_FX, "prism", "Prism"); put(Type.NAME_FX, "inferno", "Inferno");
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
        MODELS.put("crystal_crown", HatModels::crystal_crown); MODELS.put("santa_mech", HatModels::santa_mech); MODELS.put("snow_globe", HatModels::snow_globe);
        MODELS.put("pet_sleigh", HatModels::pet_sleigh); MODELS.put("pet_buddy", HatModels::pet_buddy);
        MODELS.put("pet_owl", HatModels::pet_owl); MODELS.put("pet_frost_dragon", HatModels::pet_frost_dragon); MODELS.put("pet_ice_golem", HatModels::pet_ice_golem);
        MODELS.put("og", HatModels::og); MODELS.put("pet_og", HatModels::pet_og);
        MODELS.put("pet_snowman", HatModels::pet_snowman); MODELS.put("pet_penguin", HatModels::pet_penguin);
        MODELS.put("pet_present", HatModels::pet_present); MODELS.put("pet_tree", HatModels::pet_tree); MODELS.put("pet_reindeer", HatModels::pet_reindeer);
    }

    public static String name(Type t, String id) { return ALL.get(t).getOrDefault(id, id); }

    /** Display name for a reward string like "wings:ice". */
    public static final java.util.Set<String> ANIMATED_CAPES = java.util.Set.of("candy_flow", "aurora_flow", "snowfall", "starlight", "ice_king");

    public static String rewardName(String reward) {
        if (reward.equals("bundle:og")) return "The OG bundle (one of everything!)";
        String[] p = reward.split(":");
        for (Type t : Type.values()) if (t.key.equals(p[0])) return name(t, p[1]) + " (" + t.label.substring(0, t.label.length() - 1) + ")";
        return reward;
    }

    public static boolean unlocked(Type t, String id) {
        if (id.equals("none") || (t == Type.NAME_COLOR && !id.equals("og"))) return true;
        if (t == Type.PET && id.equals("buddy")) return fr.darkerdawn.jaly.JalyConfig.get().friendBuddy;
        if (fr.darkerdawn.jaly.net.ShopApi.isPremium(t.key + ":" + id)) return fr.darkerdawn.jaly.net.ShopApi.owns(t.key + ":" + id);
        int door = Festive.doorFor(t.key + ":" + id);
        return door == 0 || JalyConfig.get().opened.contains(door);
    }

    /** Shop try-on: temporarily shows an item on you without owning it. */
    public static final Map<Type, String> PREVIEW = new java.util.EnumMap<>(Type.class);

    public static String selected(Type t) {
        String pv = PREVIEW.get(t);
        if (pv != null) return pv;
        JalyConfig c = JalyConfig.get();
        String s = switch (t) { case HAT -> c.hat; case CAPE -> c.cape; case WINGS -> c.wings; case PET -> c.pet; case TRAIL -> c.trail; case NAME_COLOR -> c.nameColor; case NAME_FX -> c.nameFx; };
        String fallback = t == Type.NAME_COLOR ? "white" : "none";
        return s == null || !ALL.get(t).containsKey(s) || !unlocked(t, s) ? fallback : s;
    }

    public static void select(Type t, String id) {
        JalyConfig c = JalyConfig.get();
        switch (t) { case HAT -> c.hat = id; case CAPE -> c.cape = id; case WINGS -> c.wings = id; case PET -> c.pet = id; case TRAIL -> c.trail = id; case NAME_COLOR -> c.nameColor = id; case NAME_FX -> c.nameFx = id; }
        JalyConfig.save();
    }

    public static Identifier icon(Type t, String id) {
        return switch (t) {
            case HAT -> Identifier.of("jalyclient", "textures/gui/hat_" + id + ".png");
            case PET -> Identifier.of("jalyclient", "textures/gui/hat_pet_" + id + ".png");
            case WINGS -> Identifier.of("jalyclient", "textures/gui/wings_" + id + ".png");
            case TRAIL -> Identifier.of("jalyclient", "textures/gui/trail_" + id + ".png");
            case CAPE -> capeTexture(id);
            default -> Identifier.of("jalyclient", "textures/gui/gear.png");
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
        if (c.equals("none")) return null;
        if (ANIMATED_CAPES.contains(c)) return capeTexture(c + "_f" + (System.currentTimeMillis() / 110 % 8));
        return capeTexture(c);
    }
}
