package fr.darkerdawn.jaly.net;

import com.google.gson.JsonObject;
import fr.darkerdawn.jaly.JalyClient;

/** Checks GitHub for a newer JalyClient so the menu can show a festive "update ready" toast. */
public final class Updates {
    private Updates() {}

    public static volatile String latest = "";
    private static boolean started;

    public static void check() {
        if (started) return;
        started = true;
        Modrinth.get("https://api.github.com/repos/luaslucas422-lab/JalyClient/releases/latest").thenAccept(j -> {
            JsonObject o = j.getAsJsonObject();
            String tag = o.get("tag_name").getAsString().replaceFirst("^v", "");
            if (newer(tag, JalyClient.VERSION)) latest = tag;
        }).exceptionally(e -> null);
    }

    public static boolean available() { return !latest.isEmpty(); }

    /** True when a is a higher dotted version than b. */
    public static boolean newer(String a, String b) {
        String[] x = a.split("[.+-]"), y = b.split("[.+-]");
        for (int i = 0; i < 3; i++) {
            int xi = i < x.length ? parse(x[i]) : 0, yi = i < y.length ? parse(y[i]) : 0;
            if (xi != yi) return xi > yi;
        }
        return false;
    }

    private static int parse(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
