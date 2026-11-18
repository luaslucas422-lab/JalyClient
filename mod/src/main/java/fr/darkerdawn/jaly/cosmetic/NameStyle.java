package fr.darkerdawn.jaly.cosmetic;

import fr.darkerdawn.jaly.JalyConfig;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;

/** Turns a plain name into a festive one: JalyClient logo, colour and an (animated) effect. */
public final class NameStyle {
    private NameStyle() {}

    private static final Identifier LOGO_FONT = Identifier.of("jalyclient", "logo");

    public static int baseColor(String id) {
        return switch (id) {
            case "red" -> 0xE0384F;
            case "gold" -> 0xFFD94A;
            case "ice" -> 0x9BE1FF;
            case "holly" -> 0x3FB950;
            case "ginger" -> 0xE69F47;
            case "pink" -> 0xFF8FC7;
            case "custom" -> JalyConfig.get().nameCustom & 0xFFFFFF;
            case "og" -> 0xFFC93A;
            default -> 0xF6F4F7;
        };
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = (int) (((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = (int) ((a & 255) + ((b & 255) - (a & 255)) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int hsv(float h) {
        return java.awt.Color.HSBtoRGB(h, 0.65f, 1f) & 0xFFFFFF;
    }

    /** Colour of character i of n at time t for the chosen effect. */
    public static int charColor(String color, String fx, int i, int n, long t) {
        float pos = n <= 1 ? 0 : i / (float) (n - 1);
        float phase = (t % 3000) / 3000f;
        int base = baseColor(color);
        if (color.equals("og") && fx.equals("none")) fx = "og";
        return switch (fx) {
            case "candy" -> ((i + (int) (t / 250)) % 2 == 0) ? 0xE0384F : 0xFFFFFF;
            case "rainbow" -> hsv(pos * 0.8f + phase);
            case "ice" -> lerp(0xE8F6FF, 0x5AA0D8, (float) (0.5 + 0.5 * Math.sin(pos * 3 + phase * 6.283)));
            case "gold" -> lerp(0xFFF2B3, 0xC99E02, (float) (0.5 + 0.5 * Math.sin(pos * 3 + phase * 6.283)));
            case "og" -> lerp(0xFFD94A, 0xE0384F, (float) (0.5 + 0.5 * Math.sin(pos * 4 - phase * 6.283)));
            case "shine" -> {
                float d = Math.abs(pos - ((phase * 1.6f) - 0.3f));
                yield d < 0.18f ? lerp(base, 0xFFFFFF, 1 - d / 0.18f) : base;
            }
            case "wave" -> lerp(base, 0xFFFFFF, (float) (0.5 + 0.5 * Math.sin(pos * 6 - phase * 12.566)) * 0.6f);
            default -> base;
        };
    }

    /** The festive version of a name (logo + colours). */
    public static MutableText style(String name, String color, String fx, boolean logo) {
        MutableText out = Text.empty();
        if (logo) out.append(Text.literal("").setStyle(Style.EMPTY.withFont(LOGO_FONT))).append(Text.literal(" "));
        long t = System.currentTimeMillis();
        int n = name.length();
        for (int i = 0; i < n; i++) {
            out.append(Text.literal(String.valueOf(name.charAt(i))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(charColor(color, fx, i, n, t))).withBold(color.equals("og"))));
        }
        return out;
    }

    public static MutableText mine(String name) {
        return style(name, Cosmetics.selected(Cosmetics.Type.NAME_COLOR), Cosmetics.selected(Cosmetics.Type.NAME_FX), true);
    }
}
