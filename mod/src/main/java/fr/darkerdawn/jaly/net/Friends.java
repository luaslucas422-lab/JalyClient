package fr.darkerdawn.jaly.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Sounds;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/** The Jaly friends list: JalyClient players, their status and the server they're on. */
public final class Friends {
    private Friends() {}

    public record Friend(String uuid, String name, String status, String server, long lastSeen) {
        public boolean online() { return !status.equals("offline"); }
    }

    public record Person(String uuid, String name) {}

    public static final String[] STATUSES = {"online", "dnd", "sleeping", "invisible"};
    public static final List<Friend> FRIENDS = new CopyOnWriteArrayList<>();
    public static final List<Person> INCOMING = new CopyOnWriteArrayList<>();
    public static final List<Person> OUTGOING = new CopyOnWriteArrayList<>();
    public static volatile String message = "";
    public static volatile boolean loaded, busy;
    private static long nextSync;
    private static final Map<String, String> lastStatus = new HashMap<>();
    private static int lastIncoming = -1;

    public static String label(String status) {
        return switch (status) {
            case "online" -> "Online";
            case "dnd" -> "Do Not Disturb";
            case "sleeping" -> "Sleeping";
            case "invisible" -> "Appear Offline";
            default -> "Offline";
        };
    }

    public static int color(String status) {
        return switch (status) {
            case "online" -> 0xFF3FB950;
            case "dnd" -> 0xFFE0384F;
            case "sleeping" -> 0xFF9B8CFF;
            default -> 0xFF6E7681;
        };
    }

    /** What we tell friends we're doing. Called on the client thread. */
    private static String whereAmI(MinecraftClient mc) {
        if (!JalyConfig.get().friendShareServer) return "";
        if (mc.world == null) return "In menus";
        if (mc.isInSingleplayer()) return "Singleplayer";
        var entry = mc.getCurrentServerEntry();
        if (entry == null) return "Multiplayer";
        return entry.isRealm() ? "Realms" : entry.address;
    }

    /** Every minute while the friends list is in use: send our presence and get theirs. */
    public static void tick(MinecraftClient mc) {
        if (!JalyConfig.get().friendsUsed || System.currentTimeMillis() < nextSync) return;
        nextSync = System.currentTimeMillis() + 60_000;
        if (!ShopApi.signedIn) {
            if (!ShopApi.busy) ShopApi.connect().thenRun(() -> nextSync = 0);
            return;
        }
        sync(mc);
    }

    public static CompletableFuture<Void> sync(MinecraftClient mc) {
        String where = whereAmI(mc);
        String status = JalyConfig.get().friendStatus;
        return CompletableFuture.runAsync(() -> {
            try {
                JsonObject b = new JsonObject();
                b.addProperty("status", status);
                b.addProperty("server", where);
                ShopApi.call("POST", "/presence", b.toString());
                apply(ShopApi.call("GET", "/friends", null), mc);
                message = "";
            } catch (Exception e) {
                message = e.getMessage() == null ? "Couldn't reach the friends server" : e.getMessage();
            }
        });
    }

    /** Best effort "I left" so friends see you go offline straight away. */
    public static void leaving() {
        if (!ShopApi.signedIn) return;
        try {
            JsonObject b = new JsonObject();
            b.addProperty("leaving", true);
            ShopApi.call("POST", "/presence", b.toString());
        } catch (Exception ignored) {
        }
    }

    private static void apply(JsonObject o, MinecraftClient mc) {
        List<Friend> f = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("friends")) {
            JsonObject x = e.getAsJsonObject();
            f.add(new Friend(x.get("uuid").getAsString(), x.get("name").getAsString(), x.get("status").getAsString(),
                x.has("server") ? x.get("server").getAsString() : "", x.has("lastSeen") ? x.get("lastSeen").getAsLong() : 0));
        }
        f.sort((a, b) -> a.online() != b.online() ? (a.online() ? -1 : 1) : a.name().compareToIgnoreCase(b.name()));
        List<Person> in = people(o, "incoming"), out = people(o, "outgoing");
        // pop-ups: friends coming online, new requests
        if (loaded && JalyConfig.get().friendToasts && !JalyConfig.get().friendStatus.equals("dnd")) {
            for (Friend fr : f) {
                String before = lastStatus.get(fr.uuid());
                if (fr.online() && before != null && before.equals("offline")) toast(mc, fr.name() + " is online", fr.server().isEmpty() ? "Say hi!" : fr.server());
            }
            if (in.size() > lastIncoming && lastIncoming >= 0) toast(mc, "Friend request", in.get(in.size() - 1).name() + " wants to be friends");
        }
        lastStatus.clear();
        for (Friend fr : f) lastStatus.put(fr.uuid(), fr.status());
        lastIncoming = in.size();
        FRIENDS.clear(); FRIENDS.addAll(f);
        INCOMING.clear(); INCOMING.addAll(in);
        OUTGOING.clear(); OUTGOING.addAll(out);
        loaded = true;
        if (!f.isEmpty() && !JalyConfig.get().friendBuddy) {
            JalyConfig.get().friendBuddy = true;
            JalyConfig.save();
            toast(mc, "Your first friend!", "You unlocked the Ginger Buddy pet");
        }
    }

    private static List<Person> people(JsonObject o, String key) {
        List<Person> out = new ArrayList<>();
        if (o.has(key)) for (JsonElement e : o.getAsJsonArray(key)) out.add(new Person(e.getAsJsonObject().get("uuid").getAsString(), e.getAsJsonObject().get("name").getAsString()));
        return out;
    }

    public static void toast(MinecraftClient mc, String title, String body) {
        mc.execute(() -> {
            SystemToast.add(mc.getToastManager(), SystemToast.Type.PERIODIC_NOTIFICATION, Text.literal("❄ " + title), Text.literal(body));
            Sounds.play(Sounds.CHIME, 1.2f, 0.6f);
        });
    }

    private static CompletableFuture<Void> post(String path, String key, String value, String doing) {
        busy = true;
        message = doing;
        MinecraftClient mc = MinecraftClient.getInstance();
        return CompletableFuture.runAsync(() -> {
            try {
                JsonObject b = new JsonObject();
                b.addProperty(key, value);
                JsonObject r = ShopApi.call("POST", path, b.toString());
                String res = r.has("result") ? r.get("result").getAsString() : "";
                message = switch (res) {
                    case "sent" -> "Request sent to " + r.get("name").getAsString() + "!";
                    case "friends" -> "You and " + r.get("name").getAsString() + " are now friends!";
                    case "already" -> "You're already friends with " + r.get("name").getAsString();
                    default -> "";
                };
                apply(ShopApi.call("GET", "/friends", null), mc);
            } catch (Exception e) {
                message = e.getMessage() == null ? "Something went wrong" : e.getMessage();
            } finally {
                busy = false;
            }
        });
    }

    public static CompletableFuture<Void> add(String name) { return post("/friends/add", "name", name.trim(), "Looking for " + name.trim() + "..."); }
    public static CompletableFuture<Void> accept(String uuid) { return post("/friends/accept", "uuid", uuid, "Accepting..."); }
    public static CompletableFuture<Void> decline(String uuid) { return post("/friends/decline", "uuid", uuid, ""); }
    public static CompletableFuture<Void> remove(String uuid) { return post("/friends/remove", "uuid", uuid, ""); }

    public static void setStatus(String status) {
        JalyConfig.get().friendStatus = status;
        JalyConfig.save();
        nextSync = 0;
    }

    /** "3 minutes ago" style text. */
    public static String ago(long epochSeconds) {
        if (epochSeconds <= 0) return "a while ago";
        long s = System.currentTimeMillis() / 1000 - epochSeconds;
        if (s < 120) return "just now";
        if (s < 3600) return (s / 60) + " minutes ago";
        if (s < 86400) return (s / 3600) + (s < 7200 ? " hour ago" : " hours ago");
        return (s / 86400) + (s < 172800 ? " day ago" : " days ago");
    }

    /** Smoke test: fake friends so the screen can be checked without a server. */
    public static void fakeForTest() {
        FRIENDS.clear();
        FRIENDS.add(new Friend("069a79f4-44e9-4726-a5be-fca90e38aaf5", "Notch", "online", "play.hypixel.net", 0));
        FRIENDS.add(new Friend("853c80ef-3c37-49fd-aa49-938b674adae6", "jeb_", "dnd", "Singleplayer", 0));
        FRIENDS.add(new Friend("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6", "Dinnerbone", "sleeping", "In menus", 0));
        FRIENDS.add(new Friend("c06f8906-4c8a-4911-9c29-ea1dbd1aab82", "Grumm", "offline", "", System.currentTimeMillis() / 1000 - 7200));
        INCOMING.clear();
        INCOMING.add(new Person("af74a02d-19cb-445b-b07f-6866a861f783", "Santa"));
        loaded = true;
    }
}
