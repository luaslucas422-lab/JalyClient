package fr.darkerdawn.jaly.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.darkerdawn.jaly.JalyClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Tiny Modrinth API client (search, versions, downloads). Everything runs off the render thread. */
public final class Modrinth {
    private Modrinth() {}

    public static final String MC = "1.21.1";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();

    public enum Kind {
        MOD("mod", "fabric", "mods"), RESOURCEPACK("resourcepack", "minecraft", "resourcepacks"), SHADER("shader", "iris", "shaderpacks");
        public final String type, loader, folder;
        Kind(String t, String l, String f) { type = t; loader = l; folder = f; }
    }

    public record Project(String id, String slug, String title, String description, String author, long downloads) {}

    public record FileRef(String projectId, String url, String filename, List<String> requiredDeps) {}

    private static String ua() { return "DarkerDawnFR/JalyClient/" + JalyClient.VERSION + " (github.com/luaslucas422-lab/JalyClient)"; }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    static CompletableFuture<JsonElement> get(String url) {
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20)).header("User-Agent", ua()).GET().build();
        return HTTP.sendAsync(r, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
            if (res.statusCode() != 200) throw new RuntimeException("Modrinth returned " + res.statusCode());
            return JsonParser.parseString(res.body());
        });
    }

    private static Project project(JsonObject o) {
        String id = o.has("project_id") ? o.get("project_id").getAsString() : o.get("id").getAsString();
        return new Project(id, str(o, "slug"), str(o, "title"), str(o, "description"), o.has("author") ? str(o, "author") : "",
            o.has("downloads") ? o.get("downloads").getAsLong() : 0);
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    /** Search Modrinth. Empty query sorts by downloads (popular). */
    public static CompletableFuture<List<Project>> search(Kind kind, String query, int offset) {
        String facets = switch (kind) {
            case MOD -> "[[\"project_type:mod\"],[\"versions:" + MC + "\"],[\"categories:fabric\"]]";
            case RESOURCEPACK -> "[[\"project_type:resourcepack\"],[\"versions:" + MC + "\"]]";
            case SHADER -> "[[\"project_type:shader\"],[\"categories:iris\"]]";
        };
        String url = "https://api.modrinth.com/v2/search?limit=20&offset=" + offset + "&index=" + (query.isBlank() ? "downloads" : "relevance")
            + "&query=" + enc(query) + "&facets=" + enc(facets);
        return get(url).thenApply(j -> {
            List<Project> out = new ArrayList<>();
            for (JsonElement e : j.getAsJsonObject().getAsJsonArray("hits")) out.add(project(e.getAsJsonObject()));
            return out;
        });
    }

    /** Several projects by slug or id (for the Featured row). */
    public static CompletableFuture<List<Project>> projects(List<String> ids) {
        JsonArray a = new JsonArray();
        ids.forEach(a::add);
        return get("https://api.modrinth.com/v2/projects?ids=" + enc(a.toString())).thenApply(j -> {
            List<Project> out = new ArrayList<>();
            for (JsonElement e : j.getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                out.add(new Project(str(o, "id"), str(o, "slug"), str(o, "title"), str(o, "description"), "", o.get("downloads").getAsLong()));
            }
            out.sort((x, y) -> Integer.compare(ids.indexOf(x.slug()), ids.indexOf(y.slug())));
            return out;
        });
    }

    /** Newest file for Minecraft 1.21.1 (shaders fall back to any version). */
    public static CompletableFuture<FileRef> latestFile(Kind kind, String idOrSlug) {
        String base = "https://api.modrinth.com/v2/project/" + enc(idOrSlug) + "/version?loaders=" + enc("[\"" + kind.loader + "\"]");
        return get(base + "&game_versions=" + enc("[\"" + MC + "\"]")).thenCompose(j -> {
            if (j.getAsJsonArray().isEmpty() && kind == Kind.SHADER) return get(base);
            return CompletableFuture.completedFuture(j);
        }).thenApply(j -> {
            JsonArray arr = j.getAsJsonArray();
            if (arr.isEmpty()) throw new RuntimeException("No version for Minecraft " + MC);
            JsonObject v = arr.get(0).getAsJsonObject();
            JsonObject file = null;
            for (JsonElement f : v.getAsJsonArray("files")) if (f.getAsJsonObject().get("primary").getAsBoolean()) file = f.getAsJsonObject();
            if (file == null) file = v.getAsJsonArray("files").get(0).getAsJsonObject();
            List<String> deps = new ArrayList<>();
            for (JsonElement d : v.getAsJsonArray("dependencies")) {
                JsonObject dd = d.getAsJsonObject();
                if ("required".equals(str(dd, "dependency_type")) && !str(dd, "project_id").isEmpty()) deps.add(str(dd, "project_id"));
            }
            return new FileRef(str(v, "project_id"), str(file, "url"), str(file, "filename"), deps);
        });
    }

    public static void download(String url, Path dest) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(3)).header("User-Agent", ua()).GET().build();
        HttpResponse<InputStream> res = HTTP.send(r, HttpResponse.BodyHandlers.ofInputStream());
        if (res.statusCode() != 200) throw new RuntimeException("Download failed (" + res.statusCode() + ")");
        Files.createDirectories(dest.getParent());
        Path tmp = dest.resolveSibling(dest.getFileName() + ".part");
        try (InputStream in = res.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING);
    }
}
