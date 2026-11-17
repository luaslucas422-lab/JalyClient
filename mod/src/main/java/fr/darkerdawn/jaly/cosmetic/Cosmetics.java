package fr.darkerdawn.jaly.cosmetic;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Free festive cosmetics. They render on your own client only. */
public final class Cosmetics {
    private Cosmetics() {}

    public static final Map<String, String> HATS = new LinkedHashMap<>();
    public static final Map<String, String> CAPES = new LinkedHashMap<>();
    private static final Map<String, Supplier<net.minecraft.client.model.TexturedModelData>> MODELS = new HashMap<>();
    private static final Map<String, ModelPart> BUILT = new HashMap<>();

    static {
        HATS.put("none", "No hat");
        HATS.put("santa", "Santa Hat");
        HATS.put("antlers", "Reindeer Antlers");
        HATS.put("elf", "Elf Hat");
        HATS.put("tophat", "Snowman Top Hat");
        HATS.put("halo", "Star Halo");
        CAPES.put("none", "No cape");
        CAPES.put("candy_cane", "Candy Cane");
        CAPES.put("tree", "Christmas Tree");
        CAPES.put("snowflake", "Snowflake");
        CAPES.put("gingerbread", "Gingerbread");
        CAPES.put("aurora", "Northern Lights");
        CAPES.put("golden", "Golden Jaly");
        MODELS.put("santa", HatModels::santa);
        MODELS.put("antlers", HatModels::antlers);
        MODELS.put("elf", HatModels::elf);
        MODELS.put("tophat", HatModels::tophat);
        MODELS.put("halo", HatModels::halo);
    }

    public static Identifier hatTexture(String id) { return Identifier.of("jalyclient", "textures/hat/" + id + ".png"); }

    public static Identifier hatIcon(String id) { return Identifier.of("jalyclient", "textures/gui/hat_" + id + ".png"); }

    public static Identifier capeTexture(String id) { return Identifier.of("jalyclient", "textures/cape/" + id + ".png"); }

    public static ModelPart model(String id) {
        Supplier<net.minecraft.client.model.TexturedModelData> s = MODELS.get(id);
        if (s == null) return null;
        return BUILT.computeIfAbsent(id, k -> s.get().createModel());
    }

    /** Which hat (if any) this entity should wear. */
    public static String hatFor(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.player) {
            String h = JalyConfig.get().hat;
            return h == null || h.equals("none") ? null : h;
        }
        if (e instanceof PlayerEntity) return Mods.on("player_hats") ? "santa" : null;
        return Mods.on("mob_hats") ? "santa" : null;
    }

    /** Cape texture for this player, or null for the normal one. */
    public static Identifier capeFor(Entity e) {
        if (e != MinecraftClient.getInstance().player) return null;
        String c = JalyConfig.get().cape;
        if (c == null || c.equals("none") || !CAPES.containsKey(c)) return null;
        return capeTexture(c);
    }
}
