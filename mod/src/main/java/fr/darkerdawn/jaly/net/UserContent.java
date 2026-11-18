package fr.darkerdawn.jaly.net;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import fr.darkerdawn.jaly.JalyClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipFile;

/**
 * Mods, resource packs and shaders the player installs from inside the game. Kept apart from the launcher's
 * Pre Installs so the launcher never removes them. Mod changes finish on restart (Windows locks loaded jars).
 */
public final class UserContent {
    private UserContent() {}

    public record Entry(String id, String slug, String title, String file) {}

    public static final class Data {
        public Map<String, Entry> mods = new LinkedHashMap<>();
        public Map<String, Entry> resourcepacks = new LinkedHashMap<>();
        public Map<String, Entry> shaderpacks = new LinkedHashMap<>();
        public List<String> pendingRemovals = new ArrayList<>();
        public List<String> pendingInstalls = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data data;
    /** project id -> status text while working ("Installing...", errors). */
    public static final Map<String, String> busy = new ConcurrentHashMap<>();

    public static Path gameDir() { return FabricLoader.getInstance().getGameDir(); }

    private static Path file() { return gameDir().resolve("jaly-user-content.json"); }

    public static synchronized Data data() {
        if (data == null) {
            try {
                data = Files.exists(file()) ? GSON.fromJson(Files.readString(file()), Data.class) : new Data();
            } catch (Exception e) {
                data = new Data();
            }
            if (data == null) data = new Data();
            if (data.pendingRemovals == null) data.pendingRemovals = new ArrayList<>();
            if (data.pendingInstalls == null) data.pendingInstalls = new ArrayList<>();
            // anything pending from last session is done now that we restarted
            data.pendingInstalls.clear();
            data.pendingRemovals.removeIf(p -> !Files.exists(gameDir().resolve(p)));
            save();
        }
        return data;
    }

    public static synchronized void save() {
        try {
            Files.writeString(file(), GSON.toJson(data));
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not save user content list", e);
        }
    }

    public static Map<String, Entry> map(Modrinth.Kind k) {
        return switch (k) { case MOD -> data().mods; case RESOURCEPACK -> data().resourcepacks; case SHADER -> data().shaderpacks; };
    }

    /** Slugs the launcher installed as Pre Installs. */
    public static Set<String> preInstalled() {
        Set<String> out = new HashSet<>();
        try {
            Path p = gameDir().resolve("jaly-installed.json");
            if (Files.exists(p)) out.addAll(GSON.<Map<String, String>>fromJson(Files.readString(p), new TypeToken<Map<String, String>>() {}.getType()).keySet());
        } catch (Exception ignored) {
        }
        return out;
    }

    public enum State { NONE, INSTALLED, PRE_INSTALLED, PENDING_REMOVE, BUSY }

    public static State state(Modrinth.Kind k, Modrinth.Project p) {
        if (busy.containsKey(p.id())) return State.BUSY;
        Entry e = map(k).get(p.id());
        if (e != null) return data().pendingRemovals.contains(k.folder + "/" + e.file()) ? State.PENDING_REMOVE : State.INSTALLED;
        if (k == Modrinth.Kind.MOD && (preInstalled().contains(p.slug()) || FabricLoader.getInstance().isModLoaded(p.slug()))) return State.PRE_INSTALLED;
        return State.NONE;
    }

    public static boolean restartNeeded() {
        return !data().pendingRemovals.isEmpty() || !data().pendingInstalls.isEmpty();
    }

    public static int pendingCount() { return data().pendingRemovals.size() + data().pendingInstalls.size(); }

    private static String modId(Path jar) {
        try (ZipFile z = new ZipFile(jar.toFile())) {
            var e = z.getEntry("fabric.mod.json");
            if (e == null) return null;
            JsonObject o = JsonParser.parseReader(new InputStreamReader(z.getInputStream(e), StandardCharsets.UTF_8)).getAsJsonObject();
            return o.get("id").getAsString();
        } catch (Exception ex) {
            return null;
        }
    }

    private static Set<String> modIdsOnDisk() {
        Set<String> ids = new HashSet<>();
        FabricLoader.getInstance().getAllMods().forEach(m -> ids.add(m.getMetadata().getId()));
        try (var s = Files.list(gameDir().resolve("mods"))) {
            s.filter(p -> p.toString().endsWith(".jar")).forEach(p -> { String id = modId(p); if (id != null) ids.add(id); });
        } catch (Exception ignored) {
        }
        return ids;
    }

    /** Installs a project (and, for mods, its required dependencies). */
    public static CompletableFuture<Void> install(Modrinth.Kind k, Modrinth.Project p) {
        busy.put(p.id(), "Installing...");
        return CompletableFuture.runAsync(() -> {
            try {
                Set<String> seen = new HashSet<>();
                installOne(k, p.id(), p.slug(), p.title(), seen, true);
                busy.remove(p.id());
                save();
                if (k == Modrinth.Kind.RESOURCEPACK) MinecraftClient.getInstance().execute(() -> enablePack(map(k).get(p.id()).file(), true));
                if (k == Modrinth.Kind.SHADER) MinecraftClient.getInstance().execute(() -> Shaders.select(map(k).get(p.id()).file()));
            } catch (Exception e) {
                JalyClient.LOG.warn("Install failed for " + p.slug(), e);
                busy.put(p.id(), "Failed: " + (e.getMessage() == null ? "error" : e.getMessage()));
            }
        });
    }

    private static void installOne(Modrinth.Kind k, String id, String slug, String title, Set<String> seen, boolean top) throws Exception {
        if (!seen.add(id) || id.equals("P7dR8mSH")) return;
        Modrinth.FileRef f = Modrinth.latestFile(k, id).get();
        Path dest = gameDir().resolve(k.folder).resolve(f.filename());
        if (k == Modrinth.Kind.MOD) {
            if (Files.exists(dest)) return;
            Path tmp = gameDir().resolve(k.folder).resolve(f.filename() + ".check");
            Modrinth.download(f.url(), tmp);
            String modId = modId(tmp);
            if (modId != null && modIdsOnDisk().contains(modId)) {
                Files.deleteIfExists(tmp);
                return;
            }
            Files.move(tmp, dest);
            data().pendingInstalls.add(k.folder + "/" + f.filename());
            for (String dep : f.requiredDeps()) installOne(k, dep, dep, dep, seen, false);
        } else {
            Modrinth.download(f.url(), dest);
        }
        synchronized (UserContent.class) {
            map(k).put(id, new Entry(id, slug, title, f.filename()));
        }
    }

    /** Removes something. Packs/shaders go now; mods are marked and finish on restart. */
    public static void uninstall(Modrinth.Kind k, String projectId) {
        Entry e = map(k).get(projectId);
        if (e == null) return;
        String rel = k.folder + "/" + e.file();
        Path p = gameDir().resolve(rel);
        if (k == Modrinth.Kind.RESOURCEPACK) enablePack(e.file(), false);
        boolean deleted = false;
        if (k != Modrinth.Kind.MOD || data().pendingInstalls.contains(rel)) {
            try {
                deleted = Files.deleteIfExists(p) || !Files.exists(p);
            } catch (Exception ignored) {
            }
        }
        if (deleted) {
            map(k).remove(projectId);
            data().pendingInstalls.remove(rel);
        } else if (!data().pendingRemovals.contains(rel)) {
            data().pendingRemovals.add(rel);
        }
        save();
    }

    /** Cancels a pending removal. */
    public static void keep(Modrinth.Kind k, String projectId) {
        Entry e = map(k).get(projectId);
        if (e != null) data().pendingRemovals.remove(k.folder + "/" + e.file());
        save();
    }

    public static void enablePack(String fileName, boolean on) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        String id = "file/" + fileName;
        if (!m.getIds().contains(id)) return;
        List<String> ids = new ArrayList<>(m.getEnabledIds());
        if (on && !ids.contains(id)) ids.add(id);
        if (!on) ids.remove(id);
        m.setEnabledProfiles(ids);
        mc.options.refreshResourcePacks(m);
    }

    /** Closes the game, finishes removals, and reopens JalyClient ready to play. */
    public static void restart() {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<String> remove = new ArrayList<>(data().pendingRemovals);
        for (String r : remove) {
            String id = null;
            for (var en : data().mods.entrySet()) if (("mods/" + en.getValue().file()).equals(r)) id = en.getKey();
            if (id != null) data().mods.remove(id);
        }
        data().pendingInstalls.clear();
        save();
        try {
            if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
                long pid = ProcessHandle.current().pid();
                Path app = Path.of(System.getenv().getOrDefault("LOCALAPPDATA", ""), "Programs", "JalyClient", "JalyClient.exe");
                StringBuilder s = new StringBuilder("@echo off\r\n:wait\r\ntasklist /FI \"PID eq " + pid + "\" | find \"" + pid + "\" >nul && (timeout /t 1 /nobreak >nul & goto wait)\r\n");
                for (String r : remove) s.append("del /f /q \"").append(gameDir().resolve(r)).append("\"\r\n");
                if (Files.exists(app)) s.append("start \"\" \"").append(app).append("\" --play\r\n");
                Path script = Files.createTempFile("jaly-restart", ".cmd");
                Files.writeString(script, s.toString());
                new ProcessBuilder("cmd", "/c", "start", "\"\"", "/min", script.toString()).start();
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(() -> remove.forEach(r -> {
                    try { Files.deleteIfExists(gameDir().resolve(r)); } catch (Exception ignored) { }
                })));
            }
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not prepare the restart helper", e);
        }
        mc.scheduleStop();
    }
}
