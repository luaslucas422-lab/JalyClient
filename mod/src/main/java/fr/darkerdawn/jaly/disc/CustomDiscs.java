package fr.darkerdawn.jaly.disc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fr.darkerdawn.jaly.JalyClient;
import fr.darkerdawn.jaly.Mods;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.block.JukeboxBlock;
import net.minecraft.block.entity.JukeboxBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.AudioStream;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.JukeboxPlayableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Custom music discs: your own songs (mp3/ogg/wav file or direct link) as discs that play in jukeboxes. */
public final class CustomDiscs {
    private CustomDiscs() {}

    public static final String[] ICONS = {"red", "candy", "gold", "frost", "holly", "midnight", "ginger", "rainbow"};
    public static final String[] ICON_NAMES = {"Classic Red", "Candy Cane", "Gold Star", "Frost", "Holly", "Midnight", "Gingerbread", "Rainbow"};
    public static final int MODEL_BASE = 9101;

    public static final class Disc {
        public String id, name, file;
        public int icon;
    }

    public record Imported(Path file, String title, String artist) {
        public String suggestedName() {
            if (title == null || title.isBlank()) return "My Song";
            return artist == null || artist.isBlank() ? title : title + " - " + artist;
        }
    }

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("jalyclient/discs");
    private static final Path LIST = DIR.resolve("discs.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static List<Disc> discs;

    public static synchronized List<Disc> all() {
        if (discs == null) {
            try {
                discs = Files.exists(LIST) ? GSON.fromJson(Files.readString(LIST), new TypeToken<List<Disc>>() {}.getType()) : new ArrayList<>();
            } catch (Exception e) {
                discs = new ArrayList<>();
            }
            if (discs == null) discs = new ArrayList<>();
        }
        return discs;
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(LIST, GSON.toJson(all()));
        } catch (Exception e) {
            JalyClient.LOG.warn("Couldn't save discs", e);
        }
    }

    public static Disc byId(String id) {
        for (Disc d : all()) if (d.id.equals(id)) return d;
        return null;
    }

    public static boolean isYouTube(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("youtube.com/") || l.contains("youtu.be/") || l.contains("music.youtube");
    }

    /** Copies a song file, or downloads a direct link, into the discs folder and reads its title/artist. */
    public static CompletableFuture<Imported> importSong(String source) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String src = source.trim().replace("\"", "");
                if (src.isEmpty()) throw new IllegalArgumentException("Paste a link or pick a song file first");
                if (isYouTube(src)) throw new IllegalArgumentException("YouTube links can't be used: YouTube doesn't allow downloading its videos. Use a song file or a direct .mp3/.ogg link.");
                Files.createDirectories(DIR);
                String id = UUID.randomUUID().toString().substring(0, 8);
                Path tmp = DIR.resolve(id + ".part");
                String nameHint;
                if (src.startsWith("http://") || src.startsWith("https://")) {
                    HttpRequest r = HttpRequest.newBuilder(URI.create(src)).timeout(Duration.ofMinutes(3)).header("User-Agent", "DarkerDawnFR/JalyClient").GET().build();
                    HttpResponse<InputStream> res = HTTP.send(r, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() != 200) throw new IllegalStateException("That link didn't work (error " + res.statusCode() + ")");
                    try (InputStream in = res.body()) {
                        byte[] data = in.readNBytes(80 * 1024 * 1024 + 1);
                        if (data.length > 80 * 1024 * 1024) throw new IllegalStateException("That song is too big (max 80 MB)");
                        Files.write(tmp, data);
                    }
                    String path = URI.create(src).getPath();
                    nameHint = URLDecoder.decode(path.substring(path.lastIndexOf('/') + 1), StandardCharsets.UTF_8);
                } else {
                    Path p = Path.of(src);
                    if (!Files.isRegularFile(p)) throw new IllegalArgumentException("Couldn't find that file");
                    Files.copy(p, tmp, StandardCopyOption.REPLACE_EXISTING);
                    nameHint = p.getFileName().toString();
                }
                String ext = kind(tmp);
                if (ext == null) {
                    Files.deleteIfExists(tmp);
                    throw new IllegalArgumentException("That isn't an mp3, ogg or wav song");
                }
                Path file = DIR.resolve(id + "." + ext);
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
                String[] meta = SongInfo.read(file, ext);
                String title = meta[0], artist = meta[1];
                if (title == null || title.isBlank()) {
                    String base = nameHint.replaceAll("\\.(mp3|ogg|wav|oga)$", "").replace('_', ' ').trim();
                    int dash = base.indexOf(" - ");
                    if (dash > 0) {
                        artist = base.substring(0, dash).trim();
                        title = base.substring(dash + 3).trim();
                    } else {
                        title = base;
                    }
                }
                return new Imported(file, title, artist);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e.getMessage() == null ? "Couldn't get that song" : e.getMessage(), e);
            }
        }, Util.getIoWorkerExecutor());
    }

    private static String kind(Path f) throws Exception {
        byte[] h = new byte[12];
        try (InputStream in = Files.newInputStream(f)) {
            if (in.readNBytes(h, 0, 12) < 4) return null;
        }
        if (h[0] == 'O' && h[1] == 'g' && h[2] == 'g' && h[3] == 'S') return "ogg";
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F') return "wav";
        if (h[0] == 'I' && h[1] == 'D' && h[2] == '3') return "mp3";
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0) return "mp3";
        return null;
    }

    /** Saves the disc and puts it in your inventory. Returns a message for the player. */
    public static String create(Imported song, String name, int icon) {
        Disc d = new Disc();
        d.id = song.file().getFileName().toString().replaceAll("\\..*$", "");
        d.name = name.isBlank() ? song.suggestedName() : name.trim();
        if (d.name.length() > 48) d.name = d.name.substring(0, 48);
        d.file = song.file().getFileName().toString();
        d.icon = Math.max(0, Math.min(ICONS.length - 1, icon));
        all().removeIf(x -> x.id.equals(d.id));
        all().add(0, d);
        save();
        return give(d);
    }

    public static ItemStack stack(Disc d) {
        ItemStack st = new ItemStack(Items.MUSIC_DISC_PRECIPICE);
        st.set(DataComponentTypes.CUSTOM_NAME, Text.literal(d.name).setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.AQUA)));
        st.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("JalyClient custom disc").formatted(Formatting.GRAY), Text.literal(ICON_NAMES[d.icon]).formatted(Formatting.DARK_GRAY))));
        //#if >=1.21.4
        st.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(List.of((float) (MODEL_BASE + d.icon)), List.of(), List.of(), List.of()));
        //#else
        st.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(MODEL_BASE + d.icon));
        //#endif
        JukeboxPlayableComponent jp = st.get(DataComponentTypes.JUKEBOX_PLAYABLE);
        if (jp != null) st.set(DataComponentTypes.JUKEBOX_PLAYABLE, jp.withShowInTooltip(false));
        NbtCompound n = new NbtCompound();
        n.putString("jaly_disc", d.id);
        st.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(n));
        return st;
    }

    /** Singleplayer: straight into your inventory. On servers: only in creative (the server decides what you own). */
    public static String give(Disc d) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return "Join a world to get the disc. It's saved in your discs list.";
        ItemStack st = stack(d);
        if (mc.getServer() != null) {
            UUID me = mc.player.getUuid();
            var server = mc.getServer();
            server.execute(() -> {
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(me);
                if (sp == null) return;
                ItemStack copy = st.copy();
                if (!sp.getInventory().insertStack(copy)) sp.dropItem(copy, false);
                sp.playerScreenHandler.sendContentUpdates();
            });
            return "\"" + d.name + "\" is in your inventory!";
        }
        if (mc.player.getAbilities().creativeMode && mc.interactionManager != null) {
            var inv = mc.player.getInventory();
            for (int i = 0; i < 36; i++) {
                if (inv.getStack(i).isEmpty()) {
                    inv.setStack(i, st);
                    mc.interactionManager.clickCreativeStack(st, i < 9 ? 36 + i : i);
                    return "\"" + d.name + "\" is in your inventory!";
                }
            }
            return "Your inventory is full";
        }
        return "On servers you can only get discs in creative mode. In singleplayer it always works!";
    }

    public static String discId(ItemStack st) {
        NbtComponent c = st.get(DataComponentTypes.CUSTOM_DATA);
        if (c == null) return null;
        NbtCompound n = c.copyNbt();
        return n.contains("jaly_disc") ? n.getString("jaly_disc") : null;
    }

    // ------------------------------------------------------------------ playback

    private static final Map<BlockPos, String> INSERTED = new HashMap<>();
    private static final Map<BlockPos, DiscSound> PLAYING = new HashMap<>();
    private static final Map<BlockPos, Integer> STOP_CHECK = new HashMap<>();

    /** You clicked a jukebox with a custom disc (used on servers, where the client can't look inside jukeboxes). */
    public static void rememberInsert(BlockPos pos, ItemStack st) {
        String id = discId(st);
        if (id != null) INSERTED.put(pos.toImmutable(), id);
    }

    /** A jukebox started playing. Returns true if it's one of our discs (then the vanilla song is skipped). */
    public static int events;
    public static String lastWhy = "";

    public static boolean onJukeboxStart(BlockPos pos) {
        events++;
        MinecraftClient mc = MinecraftClient.getInstance();
        String id = null;
        if (mc.getServer() != null && mc.world != null) {
            var sw = mc.getServer().getWorld(mc.world.getRegistryKey());
            // World.getBlockEntity refuses other threads, so read the loaded chunk directly
            var chunk = sw == null ? null : sw.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && chunk.getBlockEntity(pos) instanceof JukeboxBlockEntity be) id = discId(be.getStack());
            if (id == null) lastWhy = "server lookup: chunk=" + (chunk != null) + " be=" + (chunk == null ? null : chunk.getBlockEntity(pos));
        }
        if (id == null) id = INSERTED.get(pos);
        if (id == null) { lastWhy += " | no disc id at " + pos; return false; }
        Disc d = byId(id);
        if (d == null) { lastWhy = "unknown disc " + id; return false; }
        Path file = DIR.resolve(d.file);
        if (!Files.exists(file)) { lastWhy = "missing file " + file; return false; }
        stop(pos);
        DiscSound s = new DiscSound(pos, file);
        PLAYING.put(pos.toImmutable(), s);
        mc.getSoundManager().play(s);
        mc.inGameHud.setRecordPlayingOverlay(Text.literal(d.name));
        return true;
    }

    /** The jukebox stopped: if the disc was taken out, stop the song (otherwise let it finish). */
    public static void onJukeboxStop(BlockPos pos) {
        if (PLAYING.containsKey(pos)) STOP_CHECK.put(pos.toImmutable(), 3);
    }

    public static void stop(BlockPos pos) {
        DiscSound s = PLAYING.remove(pos);
        if (s != null) MinecraftClient.getInstance().getSoundManager().stop(s);
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            if (!PLAYING.isEmpty()) {
                PLAYING.values().forEach(s -> mc.getSoundManager().stop(s));
                PLAYING.clear();
            }
            INSERTED.clear();
            return;
        }
        STOP_CHECK.entrySet().removeIf(e -> {
            if (e.setValue(e.getValue() - 1) > 1) return false;
            var st = mc.world.getBlockState(e.getKey());
            if (!st.isOf(Blocks.JUKEBOX) || !st.get(JukeboxBlock.HAS_RECORD)) {
                stop(e.getKey());
                INSERTED.remove(e.getKey());
            }
            return true;
        });
        PLAYING.entrySet().removeIf(e -> !mc.getSoundManager().isPlaying(e.getValue()) && e.getValue().started && e.getValue().age > 40);
        PLAYING.values().forEach(s -> s.age++);
    }

    public static int playing() { return PLAYING.size(); }

    public static boolean enabled() { return Mods.on("custom_discs"); }

    /** The song coming out of a jukebox: positional, like a real record. */
    static final class DiscSound extends AbstractSoundInstance {
        private final Path file;
        boolean started;
        int age;

        DiscSound(BlockPos pos, Path file) {
            super(SoundEvent.of(Identifier.of("jalyclient", "custom_disc")), SoundCategory.RECORDS, SoundInstance.createRandom());
            this.file = file;
            this.x = pos.getX() + 0.5;
            this.y = pos.getY() + 0.5;
            this.z = pos.getZ() + 0.5;
            this.volume = 4f;
            this.attenuationType = AttenuationType.LINEAR;
        }

        @Override
        public CompletableFuture<AudioStream> getAudioStream(SoundLoader loader, Identifier id, boolean repeatInstantly) {
            started = true;
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return DiscAudio.open(file);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }, Util.getIoWorkerExecutor());
        }
    }
}
