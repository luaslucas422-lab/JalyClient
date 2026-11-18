package fr.darkerdawn.jaly;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShopCatalogTest {
    private JsonObject load() throws Exception {
        try (var in = getClass().getResourceAsStream("/assets/jalyclient/shop.json")) {
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void packsMatchTheAgreedPrices() throws Exception {
        int[][] want = {{100, 100}, {550, 500}, {1200, 1000}, {2600, 2000}};
        var packs = load().getAsJsonArray("packs");
        assertEquals(4, packs.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(want[i][0], packs.get(i).getAsJsonObject().get("coins").getAsInt());
            assertEquals(want[i][1], packs.get(i).getAsJsonObject().get("cents").getAsInt());
        }
    }

    @Test
    void itemsArePricedBetween200And1200AndUnique() throws Exception {
        Set<String> seen = new HashSet<>();
        for (JsonElement e : load().getAsJsonArray("items")) {
            JsonObject o = e.getAsJsonObject();
            int p = o.get("price").getAsInt();
            assertTrue(p >= 200 && p <= 1200, o.get("id") + " price " + p);
            assertTrue(seen.add(o.get("id").getAsString()), "duplicate " + o.get("id"));
            assertTrue(o.get("id").getAsString().matches("(hat|cape|wings|pet|trail|namefx):[a-z_]+"));
        }
        assertTrue(seen.size() >= 15);
    }
}
