package fr.darkerdawn.jaly;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads what Spotify (or any media app) is playing through the Windows media controls - no login needed -
 * and fetches time-synced lyrics from LRCLIB (free, no key).
 */
public final class Music {
    private Music() {}

    public record Track(String title, String artist, String app, long posMs, long durMs, long updatedMs, boolean playing) {}

    public record Line(long timeMs, String text) {}

    public static volatile Track current;
    public static volatile List<Line> lyrics = List.of();
    public static volatile String lyricsStatus = "";
    private static volatile String lyricsKey = "";
    private static Thread reader;
    private static Process process;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)");

    public static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    public static boolean wanted() {
        return Mods.on("now_playing") || Mods.on("sky_lyrics");
    }

    /** Called every client tick: starts or stops the background reader as needed. */
    public static void tick() {
        boolean want = wanted() && supported();
        if (want && (reader == null || !reader.isAlive())) start();
        if (!want && process != null) stop();
    }

    private static final String SCRIPT = String.join("\n",
        "$ErrorActionPreference = 'SilentlyContinue'",
        "Add-Type -AssemblyName System.Runtime.WindowsRuntime",
        "$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | ? { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]",
        "Function Await($op, $type) { $t = $asTask.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(-1) | Out-Null; $t.Result }",
        "[Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager,Windows.Media.Control,ContentType=WindowsRuntime] | Out-Null",
        "$mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])",
        "while ($true) {",
        "  $s = $null",
        "  if ($mgr) { foreach ($x in $mgr.GetSessions()) { if ($x.SourceAppUserModelId -like '*Spotify*') { $s = $x } }; if ($s -eq $null) { $s = $mgr.GetCurrentSession() } }",
        "  if ($s) {",
        "    $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])",
        "    $tl = $s.GetTimelineProperties(); $pb = $s.GetPlaybackInfo()",
        "    $o = @{ app = $s.SourceAppUserModelId; title = $p.Title; artist = $p.Artist; pos = [long]$tl.Position.TotalMilliseconds; dur = [long]$tl.EndTime.TotalMilliseconds; updated = [long]$tl.LastUpdatedTime.ToUnixTimeMilliseconds(); playing = ($pb.PlaybackStatus -eq 'Playing') }",
        "    [Console]::Out.WriteLine(($o | ConvertTo-Json -Compress))",
        "  } else { [Console]::Out.WriteLine('{}') }",
        "  [Console]::Out.Flush(); Start-Sleep -Milliseconds 900",
        "}");

    private static synchronized void start() {
        try {
            Path script = Files.createTempFile("jaly-media", ".ps1");
            Files.writeString(script, SCRIPT);
            script.toFile().deleteOnExit();
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString())
                .redirectErrorStream(true).start();
            Process p = process;
            reader = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) accept(line.trim());
                } catch (Exception ignored) {
                }
            }, "JalyClient media reader");
            reader.setDaemon(true);
            reader.start();
        } catch (Exception e) {
            JalyClient.LOG.warn("Could not start the media reader", e);
            reader = new Thread(() -> {});
        }
    }

    public static synchronized void stop() {
        if (process != null) process.destroy();
        process = null;
        reader = null;
        current = null;
    }

    /** Parses one JSON line from the reader. Public so the smoke test can feed it. */
    public static void accept(String line) {
        if (!line.startsWith("{")) return;
        try {
            JsonObject o = JsonParser.parseString(line).getAsJsonObject();
            if (!o.has("title") || o.get("title").getAsString().isBlank()) { current = null; return; }
            Track t = new Track(o.get("title").getAsString(), o.has("artist") ? o.get("artist").getAsString() : "",
                o.has("app") ? o.get("app").getAsString() : "", o.has("pos") ? o.get("pos").getAsLong() : 0,
                o.has("dur") ? o.get("dur").getAsLong() : 0, o.has("updated") ? o.get("updated").getAsLong() : System.currentTimeMillis(),
                o.has("playing") && o.get("playing").getAsBoolean());
            current = t;
            String key = t.artist() + "|" + t.title();
            if (!key.equals(lyricsKey)) {
                lyricsKey = key;
                fetchLyrics(t);
            }
        } catch (Exception ignored) {
        }
    }

    /** Where the song is now, estimated between updates. */
    public static long positionMs() {
        Track t = current;
        if (t == null) return 0;
        long pos = t.posMs();
        if (t.playing()) pos += Math.max(0, System.currentTimeMillis() - t.updatedMs());
        return t.durMs() > 0 ? Math.min(pos, t.durMs()) : pos;
    }

    public static String currentLine() {
        List<Line> l = lyrics;
        if (l.isEmpty()) return "";
        long pos = positionMs() + 200;
        String text = "";
        for (Line line : l) {
            if (line.timeMs() <= pos) text = line.text();
            else break;
        }
        return text;
    }

    public static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        if (lrc == null) return out;
        for (String raw : lrc.split("\\r?\\n")) {
            Matcher m = LRC.matcher(raw.trim());
            if (!m.matches()) continue;
            long ms = Long.parseLong(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2)) * 1000);
            out.add(new Line(ms, m.group(3).trim()));
        }
        out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return out;
    }

    private static void fetchLyrics(Track t) {
        lyrics = List.of();
        lyricsStatus = "Finding lyrics...";
        String url = "https://lrclib.net/api/get?artist_name=" + enc(t.artist()) + "&track_name=" + enc(t.title())
            + (t.durMs() > 0 ? "&duration=" + (t.durMs() / 1000) : "");
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8))
            .header("User-Agent", "JalyClient/" + JalyClient.VERSION + " (https://github.com/luaslucas422-lab/JalyClient)").GET().build();
        String key = lyricsKey;
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
            if (!key.equals(lyricsKey)) return;
            if (r.statusCode() != 200) { lyricsStatus = "No lyrics found"; return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (o.has("instrumental") && o.get("instrumental").getAsBoolean()) { lyricsStatus = "Instrumental"; return; }
            String synced = o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull() ? o.get("syncedLyrics").getAsString() : null;
            List<Line> parsed = parseLrc(synced);
            lyrics = parsed;
            lyricsStatus = parsed.isEmpty() ? "No synced lyrics" : "";
        }).exceptionally(e -> {
            lyricsStatus = "Lyrics offline";
            return null;
        });
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }

    /** Smoke test helper: pretend a song is playing. */
    public static void fake(String title, String artist, String lrc, long posMs, long durMs) {
        lyricsKey = artist + "|" + title;
        current = new Track(title, artist, "Spotify.exe", posMs, durMs, System.currentTimeMillis(), true);
        lyrics = parseLrc(lrc);
        lyricsStatus = "";
    }
}
