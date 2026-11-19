package fr.darkerdawn.jaly.net;

import fr.darkerdawn.jaly.JalyClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Downloads small web images (Modrinth icons) into textures. PNG/JPEG/GIF directly; WebP goes through an image proxy. */
public final class RemoteImages {
    private RemoteImages() {}

    private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
    private static final Set<String> STARTED = ConcurrentHashMap.newKeySet();
    private static final Map<String, String> MODRINTH_ICONS = new ConcurrentHashMap<>();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "Jaly icons");
        t.setDaemon(true);
        return t;
    });

    static void rememberIcon(String projectId, String url) {
        if (projectId != null && url != null && !url.isBlank()) MODRINTH_ICONS.put(projectId, url);
    }

    /** The icon of a Modrinth project, or null while it's loading (or if icons are turned off). */
    public static Identifier modrinthIcon(String projectId) {
        if (!fr.darkerdawn.jaly.JalyConfig.get().modrinthIcons || projectId == null || projectId.isEmpty()) return null;
        Identifier id = READY.get(projectId);
        if (id != null || !STARTED.add(projectId)) return id;
        POOL.execute(() -> {
            try {
                String url = MODRINTH_ICONS.get(projectId);
                if (url == null) { // installed earlier: ask Modrinth for the icon
                    var o = Modrinth.get("https://api.modrinth.com/v2/project/" + URLEncoder.encode(projectId, StandardCharsets.UTF_8)).get().getAsJsonObject();
                    if (o.has("icon_url") && !o.get("icon_url").isJsonNull()) url = o.get("icon_url").getAsString();
                }
                if (url == null || url.isBlank()) return;
                NativeImage img = load(url);
                if (img != null) upload(projectId, img);
            } catch (Exception e) {
                JalyClient.LOG.debug("No icon for {}", projectId, e);
            }
        });
        return null;
    }

    private static byte[] fetch(String url) throws Exception {
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("User-Agent", "DarkerDawnFR/JalyClient").GET().build();
        HttpResponse<byte[]> res = HTTP.send(r, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200 || res.body().length > 4_000_000) throw new IllegalStateException("HTTP " + res.statusCode());
        return res.body();
    }

    static NativeImage load(String url) throws Exception {
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(fetch(url)));
        if (bi == null) { // WebP and friends: ask a converting proxy for a small PNG
            String proxy = "https://wsrv.nl/?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8) + "&w=64&h=64&fit=contain&output=png";
            bi = ImageIO.read(new ByteArrayInputStream(fetch(proxy)));
        }
        if (bi == null) return null;
        int size = 64;
        float k = Math.min(1f, size / (float) Math.max(bi.getWidth(), bi.getHeight()));
        int w = Math.max(1, Math.round(bi.getWidth() * k)), h = Math.max(1, Math.round(bi.getHeight() * k));
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(bi, (size - w) / 2, (size - h) / 2, w, h, null);
        g.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(out, "png", png);
        return NativeImage.read(new ByteArrayInputStream(png.toByteArray()));
    }

    private static void upload(String key, NativeImage img) {
        MinecraftClient.getInstance().execute(() -> {
            Identifier id = Identifier.of("jalyclient", "remote/" + Integer.toHexString(key.hashCode()) + "_" + key.toLowerCase().replaceAll("[^a-z0-9_]", ""));
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(img));
            READY.put(key, id);
        });
    }

    /** True once at least one icon has loaded (smoke test). */
    public static int loaded() { return READY.size(); }
}
