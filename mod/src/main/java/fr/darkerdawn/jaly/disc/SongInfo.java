package fr.darkerdawn.jaly.disc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Reads the title and artist from an mp3 (ID3v2) or ogg (Vorbis comments). Returns {title, artist}, either may be null. */
final class SongInfo {
    private SongInfo() {}

    static String[] read(Path file, String ext) {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] b = in.readNBytes(256 * 1024);
            return ext.equals("mp3") ? id3(b) : ext.equals("ogg") ? vorbis(b) : new String[2];
        } catch (Exception e) {
            return new String[2];
        }
    }

    static String[] id3(byte[] b) {
        String[] out = new String[2];
        if (b.length < 10 || b[0] != 'I' || b[1] != 'D' || b[2] != '3') return out;
        int ver = b[3];
        int size = syncsafe(b, 6);
        int p = 10, end = Math.min(b.length, 10 + size);
        while (p + 10 <= end) {
            String id = new String(b, p, 4, StandardCharsets.ISO_8859_1);
            if (id.charAt(0) == 0) break;
            int len = ver >= 4 ? syncsafe(b, p + 4) : ((b[p + 4] & 0xFF) << 24) | ((b[p + 5] & 0xFF) << 16) | ((b[p + 6] & 0xFF) << 8) | (b[p + 7] & 0xFF);
            if (len <= 0 || p + 10 + len > end) break;
            if (id.equals("TIT2") || id.equals("TPE1")) {
                String text = text(b, p + 10, len);
                if (id.equals("TIT2")) out[0] = text;
                else out[1] = text;
            }
            p += 10 + len;
        }
        return out;
    }

    private static int syncsafe(byte[] b, int o) {
        return ((b[o] & 0x7F) << 21) | ((b[o + 1] & 0x7F) << 14) | ((b[o + 2] & 0x7F) << 7) | (b[o + 3] & 0x7F);
    }

    private static String text(byte[] b, int o, int len) {
        int enc = b[o];
        byte[] data = java.util.Arrays.copyOfRange(b, o + 1, o + len);
        String s = switch (enc) {
            case 1 -> new String(data, StandardCharsets.UTF_16);
            case 2 -> new String(data, StandardCharsets.UTF_16BE);
            case 3 -> new String(data, StandardCharsets.UTF_8);
            default -> new String(data, StandardCharsets.ISO_8859_1);
        };
        return s.replace("\u0000", "").trim();
    }

    static String[] vorbis(byte[] b) {
        String[] out = new String[2];
        // find the comment header: 0x03 'vorbis'
        for (int i = 0; i + 7 < b.length; i++) {
            if (b[i] == 3 && b[i + 1] == 'v' && b[i + 2] == 'o' && b[i + 3] == 'r' && b[i + 4] == 'b' && b[i + 5] == 'i' && b[i + 6] == 's') {
                int p = i + 7;
                int vendor = le32(b, p);
                p += 4 + vendor;
                int count = le32(b, p);
                p += 4;
                for (int c = 0; c < count && p + 4 <= b.length; c++) {
                    int len = le32(b, p);
                    p += 4;
                    if (len < 0 || p + len > b.length) break;
                    String kv = new String(b, p, len, StandardCharsets.UTF_8);
                    p += len;
                    int eq = kv.indexOf('=');
                    if (eq <= 0) continue;
                    String k = kv.substring(0, eq).toUpperCase(Locale.ROOT), v = kv.substring(eq + 1).trim();
                    if (k.equals("TITLE")) out[0] = v;
                    if (k.equals("ARTIST")) out[1] = v;
                }
                break;
            }
        }
        return out;
    }

    private static int le32(byte[] b, int o) {
        if (o + 4 > b.length) return -1;
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
    }
}
