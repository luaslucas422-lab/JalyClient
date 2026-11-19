package fr.darkerdawn.jaly.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.darkerdawn.jaly.JalyClient;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Talks to the Jolly Shop server: Mojang-verified sign-in, Jolly Coins, purchases and Stripe checkout. */
public final class ShopApi {
    private ShopApi() {}

    public record Item(String id, String name, int price) {}
    public record Pack(String id, String name, int coins, int cents) {}

    public static final List<Item> ITEMS = new ArrayList<>();
    public static final List<Pack> PACKS = new ArrayList<>();
    public static volatile int coins;
    public static final Set<String> OWNED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static volatile String status = "";
    public static volatile boolean signedIn, busy, testMode;
    private static volatile String endpoint, token;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("jalyclient-shop.json");

    static {
        try (var in = ShopApi.class.getResourceAsStream("/assets/jalyclient/shop.json")) {
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : o.getAsJsonArray("items")) { JsonObject i = e.getAsJsonObject(); ITEMS.add(new Item(i.get("id").getAsString(), i.get("name").getAsString(), i.get("price").getAsInt())); }
            for (JsonElement e : o.getAsJsonArray("packs")) { JsonObject p = e.getAsJsonObject(); PACKS.add(new Pack(p.get("id").getAsString(), p.get("name").getAsString(), p.get("coins").getAsInt(), p.get("cents").getAsInt())); }
        } catch (Exception e) {
            JalyClient.LOG.warn("Shop catalog missing", e);
        }
        try {
            if (Files.exists(FILE)) {
                JsonObject o = JsonParser.parseString(Files.readString(FILE)).getAsJsonObject();
                token = o.has("token") ? o.get("token").getAsString() : null;
                if (o.has("owned")) for (JsonElement e : o.getAsJsonArray("owned")) OWNED.add(e.getAsString());
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isPremium(String typeAndId) {
        for (Item i : ITEMS) if (i.id().equals(typeAndId)) return true;
        return false;
    }

    public static Item item(String typeAndId) {
        for (Item i : ITEMS) if (i.id().equals(typeAndId)) return i;
        return null;
    }

    public static boolean owns(String typeAndId) { return OWNED.contains(typeAndId); }

    private static void save() {
        try {
            JsonObject o = new JsonObject();
            if (token != null) o.addProperty("token", token);
            JsonArray a = new JsonArray();
            OWNED.forEach(a::add);
            o.add("owned", a);
            Files.writeString(FILE, o.toString());
        } catch (Exception ignored) {
        }
    }

    private static String endpoint() throws Exception {
        if (endpoint != null) return endpoint;
        String override = System.getProperty("jaly.shop");
        if (override != null) return endpoint = override;
        HttpRequest r = HttpRequest.newBuilder(URI.create("https://raw.githubusercontent.com/luaslucas422-lab/JalyClient/main/server/endpoint.txt")).timeout(Duration.ofSeconds(10)).build();
        HttpResponse<String> res = HTTP.send(r, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200 || !res.body().trim().startsWith("https://")) throw new IllegalStateException("JalyClient's online features open soon!");
        return endpoint = res.body().trim();
    }

    static JsonObject call(String method, String path, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(endpoint() + path)).timeout(Duration.ofSeconds(20)).header("content-type", "application/json");
        if (token != null) b.header("authorization", "Bearer " + token);
        b.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        HttpResponse<String> res = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        JsonObject o = res.body().isBlank() ? new JsonObject() : JsonParser.parseString(res.body()).getAsJsonObject();
        if (res.statusCode() == 401 && path.equals("/me")) { token = null; signedIn = false; }
        if (res.statusCode() >= 400) throw new IllegalStateException(o.has("error") ? o.get("error").getAsString() : "Shop error " + res.statusCode());
        return o;
    }

    private static void apply(JsonObject o) {
        if (o.has("token")) token = o.get("token").getAsString();
        coins = o.has("coins") ? o.get("coins").getAsInt() : coins;
        testMode = o.has("testMode") && o.get("testMode").getAsBoolean();
        if (o.has("owned")) { OWNED.clear(); for (JsonElement e : o.getAsJsonArray("owned")) OWNED.add(e.getAsString()); }
        signedIn = true;
        save();
    }

    private static CompletableFuture<Void> run(String doing, ThrowingRunnable r) {
        busy = true;
        status = doing;
        return CompletableFuture.runAsync(() -> {
            try {
                r.run();
                status = "";
            } catch (Exception e) {
                status = e.getMessage() == null ? "Something went wrong" : e.getMessage();
            } finally {
                busy = false;
            }
        });
    }

    interface ThrowingRunnable { void run() throws Exception; }

    /** Signs in the Minecraft account (Mojang session check, no password ever leaves the game). */
    public static CompletableFuture<Void> connect() {
        return run("Opening the shop...", () -> {
            if (token != null) {
                try { apply(call("GET", "/me", null)); return; } catch (Exception ignored) { }
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            var session = mc.getSession();
            if (session.getUuidOrNull() == null || session.getAccessToken() == null || session.getAccessToken().length() < 20) {
                throw new IllegalStateException("Sign in with a real Minecraft account to shop");
            }
            String serverId = call("POST", "/auth/start", "{}").get("serverId").getAsString();
            try {
                mc.getSessionService().joinServer(session.getUuidOrNull(), session.getAccessToken(), serverId);
            } catch (Exception e) {
                throw new IllegalStateException("Minecraft couldn't confirm your account");
            }
            JsonObject body = new JsonObject();
            body.addProperty("name", session.getUsername());
            body.addProperty("serverId", serverId);
            apply(call("POST", "/auth/finish", body.toString()));
        });
    }

    public static CompletableFuture<Void> refresh() {
        if (token == null || busy) return CompletableFuture.completedFuture(null);
        return CompletableFuture.runAsync(() -> { try { apply(call("GET", "/me", null)); } catch (Exception ignored) { } });
    }

    public static CompletableFuture<Void> buy(Item it) {
        return run("Buying " + it.name() + "...", () -> {
            JsonObject b = new JsonObject();
            b.addProperty("item", it.id());
            apply(call("POST", "/buy", b.toString()));
        });
    }

    public static CompletableFuture<Void> checkout(Pack p) {
        return run("Opening secure checkout...", () -> {
            JsonObject b = new JsonObject();
            b.addProperty("pack", p.id());
            String url = call("POST", "/checkout", b.toString()).get("url").getAsString();
            Util.getOperatingSystem().open(url);
        });
    }

    public static String euros(int cents) { return String.format("%s%d.%02d", "€", cents / 100, cents % 100); }

    public static Set<String> ownedCopy() { return new HashSet<>(OWNED); }
}
