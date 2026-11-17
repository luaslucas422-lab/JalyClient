package fr.darkerdawn.jaly.hud;

import fr.darkerdawn.jaly.Festive;
import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.Music;
import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** All HUD modules plus click tracking. */
public final class Hud {
    private Hud() {}

    private static final long[] LEFT = new long[64], RIGHT = new long[64];
    private static int li, ri;
    public static double speed;

    public static void click(int button) {
        long now = System.currentTimeMillis();
        if (button == 0) LEFT[li++ & 63] = now;
        else if (button == 1) RIGHT[ri++ & 63] = now;
    }

    public static int cps(int button) {
        long now = System.currentTimeMillis();
        long[] arr = button == 0 ? LEFT : RIGHT;
        int n = 0;
        for (long t : arr) if (now - t <= 1000) n++;
        return n;
    }

    /** A "[Label value]" text module. */
    static final class TextModule extends HudModule {
        private final String label;
        private final Function<MinecraftClient, String> value;
        private String cached = "";

        TextModule(String id, String name, String desc, boolean on, float x, float y, String label, Function<MinecraftClient, String> value) {
            super(id, name, desc, on, x, y);
            this.label = label;
            this.value = value;
        }

        private String full(MinecraftClient mc, boolean preview) {
            String v;
            try {
                v = value.apply(mc);
            } catch (Exception e) {
                v = "-";
            }
            if (v == null) v = preview ? "-" : "";
            return v;
        }

        @Override
        public int width(MinecraftClient mc, boolean preview) {
            cached = full(mc, preview);
            JalyConfig.HudStyle s = JalyConfig.get().style(id);
            String t = (s.brackets ? "[" : "") + (label.isEmpty() ? "" : label + " ") + cached + (s.brackets ? "]" : "");
            return mc.textRenderer.getWidth(t) + 8;
        }

        @Override
        public int height(MinecraftClient mc, boolean preview) { return 13; }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            int x = 4;
            String open = s.brackets ? "[" : "";
            if (!open.isEmpty()) x = ctx.drawText(mc.textRenderer, open, x, 3, s.text, s.shadow);
            if (!label.isEmpty()) x = ctx.drawText(mc.textRenderer, label + " ", x, 3, s.accent, s.shadow);
            x = ctx.drawText(mc.textRenderer, cached, x, 3, s.text, s.shadow);
            if (s.brackets) ctx.drawText(mc.textRenderer, "]", x, 3, s.text, s.shadow);
        }
    }

    static final class Keystrokes extends HudModule {
        Keystrokes() { super("keystrokes", "Keystrokes", "Shows WASD, mouse buttons and space", true, 0.006f, 0.25f); }

        @Override public int width(MinecraftClient mc, boolean preview) { return 58; }
        @Override public int height(MinecraftClient mc, boolean preview) { return 64; }

        private void key(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, int x, int y, int w, int h, String label, boolean down, String sub) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate(x, y, 0);
            if (down) {
                Gfx.round(ctx, 0, 0, w, h, 0xE0B11A33);
                Gfx.outline(ctx, 0, 0, w, h, 0xFFFDE68A);
            } else {
                box(ctx, w, h, s);
            }
            int tc = down ? 0xFFFFFFFF : s.text;
            int ty = sub == null ? (h - 8) / 2 : 2;
            ctx.drawText(mc.textRenderer, label, (w - mc.textRenderer.getWidth(label)) / 2, ty, tc, s.shadow);
            if (sub != null) {
                ctx.getMatrices().push();
                ctx.getMatrices().translate(w / 2f, 10.5f, 0);
                ctx.getMatrices().scale(0.5f, 0.5f, 1);
                ctx.drawText(mc.textRenderer, sub, -mc.textRenderer.getWidth(sub) / 2, 0, down ? 0xFFFFFFFF : s.accent, false);
                ctx.getMatrices().pop();
            }
            ctx.getMatrices().pop();
        }

        private boolean pressed(KeyBinding k, boolean preview, boolean demo) {
            return preview ? demo : k.isPressed();
        }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            var o = mc.options;
            key(ctx, mc, s, 20, 0, 18, 18, "W", pressed(o.forwardKey, preview, true), null);
            key(ctx, mc, s, 0, 20, 18, 18, "A", pressed(o.leftKey, preview, false), null);
            key(ctx, mc, s, 20, 20, 18, 18, "S", pressed(o.backKey, preview, false), null);
            key(ctx, mc, s, 40, 20, 18, 18, "D", pressed(o.rightKey, preview, false), null);
            key(ctx, mc, s, 0, 40, 28, 14, "LMB", pressed(o.attackKey, preview, false), cps(0) + " CPS");
            key(ctx, mc, s, 30, 40, 28, 14, "RMB", pressed(o.useKey, preview, false), cps(1) + " CPS");
            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 56, 0);
            boolean sp = pressed(o.jumpKey, preview, false);
            if (sp) Gfx.round(ctx, 0, 0, 58, 8, 0xE0B11A33); else box(ctx, 58, 8, s);
            ctx.fill(18, 3, 40, 4, sp ? 0xFFFFFFFF : s.text);
            ctx.getMatrices().pop();
        }
    }

    static final class Armor extends HudModule {
        private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        private static final ItemStack[] DEMO = {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS), new ItemStack(Items.DIAMOND_SWORD)};

        Armor() { super("armor", "Armor Status", "Your armor and held item with durability", true, 0.006f, 0.48f); }

        private ItemStack stack(MinecraftClient mc, int i, boolean preview) {
            if (mc.player == null) return preview ? DEMO[i] : ItemStack.EMPTY;
            ItemStack st = mc.player.getEquippedStack(SLOTS[i]);
            return st.isEmpty() && preview ? DEMO[i] : st;
        }

        @Override public int width(MinecraftClient mc, boolean preview) { return 48; }
        @Override public int height(MinecraftClient mc, boolean preview) { return 5 * 17 + 2; }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            box(ctx, 48, 5 * 17 + 2, s);
            for (int i = 0; i < 5; i++) {
                ItemStack st = stack(mc, i, preview);
                int y = 1 + i * 17;
                if (st.isEmpty()) continue;
                ctx.drawItem(st, 2, y);
                String t = st.isDamageable() ? String.valueOf(st.getMaxDamage() - st.getDamage()) : st.getCount() > 1 ? "x" + st.getCount() : "";
                int col = s.text;
                if (st.isDamageable() && st.getMaxDamage() > 0) {
                    float f = 1f - st.getDamage() / (float) st.getMaxDamage();
                    col = f < 0.25f ? 0xFFE0384F : f < 0.5f ? 0xFFFDE68A : s.text;
                }
                ctx.drawText(mc.textRenderer, t, 20, y + 4, col, s.shadow);
            }
        }
    }

    static final class Potions extends HudModule {
        Potions() { super("potions", "Potion Effects", "Active effects with time left", true, 0.80f, 0.30f); }

        private List<StatusEffectInstance> list(MinecraftClient mc, boolean preview) {
            List<StatusEffectInstance> l = new ArrayList<>();
            if (mc.player != null) l.addAll(mc.player.getStatusEffects());
            if (l.isEmpty() && preview) {
                l.add(new StatusEffectInstance(StatusEffects.SPEED, 2400, 1));
                l.add(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 900, 0));
            }
            return l;
        }

        @Override public int width(MinecraftClient mc, boolean preview) { return 112; }
        @Override public int height(MinecraftClient mc, boolean preview) { return Math.max(1, list(mc, preview).size()) * 20 + 2; }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            List<StatusEffectInstance> l = list(mc, preview);
            if (l.isEmpty()) return;
            box(ctx, 112, l.size() * 20 + 2, s);
            int y = 2;
            for (StatusEffectInstance e : l) {
                ctx.drawSprite(2, y, 0, 18, 18, mc.getStatusEffectSpriteManager().getSprite(e.getEffectType()));
                String name = e.getEffectType().value().getName().getString();
                if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
                ctx.drawText(mc.textRenderer, Gfx.trim(name, 86), 23, y + 1, s.text, s.shadow);
                String time;
                if (e.isInfinite()) time = "**:**";
                else {
                    int sec = e.getDuration() / 20;
                    time = String.format("%d:%02d", sec / 60, sec % 60);
                }
                ctx.drawText(mc.textRenderer, time, 23, y + 10, s.accent, s.shadow);
                y += 20;
            }
        }

        private static String roman(int n) {
            return switch (n) { case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V"; default -> String.valueOf(n); };
        }
    }

    static final class Countdown extends HudModule {
        private String text = "";

        Countdown() { super("countdown", "Christmas Countdown", "Days until Christmas, with a little tree", true, 0.006f, 0.19f); }

        @Override
        public int width(MinecraftClient mc, boolean preview) {
            text = Festive.countdownText(LocalDate.now());
            return mc.textRenderer.getWidth(text) + 19;
        }

        @Override public int height(MinecraftClient mc, boolean preview) { return 13; }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            Gfx.sprite(ctx, Gfx.TREE, new int[]{0xFF2E8B3A}, "#", 4, 3, 1, 1f);
            ctx.fill(7, 2, 8, 3, 0xFFFDE68A);
            long t = Gfx.now();
            ctx.fill(5, 6, 6, 7, (t / 400) % 2 == 0 ? 0xFFE0384F : 0xFF4CC3FF);
            ctx.fill(8, 8, 9, 9, (t / 400) % 2 == 1 ? 0xFFFDE68A : 0xFFE0384F);
            ctx.drawText(mc.textRenderer, text, 15, 3, s.text, s.shadow);
        }
    }

    static final class NowPlaying extends HudModule {
        NowPlaying() { super("now_playing", "Now Playing (Spotify)", "The song playing in Spotify, with live lyrics", false, 0.70f, 0.82f); }

        private boolean lyricsOn() { return JalyConfig.get().on("now_playing_lyrics", true); }

        @Override public int width(MinecraftClient mc, boolean preview) { return 176; }
        @Override public int height(MinecraftClient mc, boolean preview) { return lyricsOn() ? 58 : 46; }

        @Override
        public void draw(DrawContext ctx, MinecraftClient mc, JalyConfig.HudStyle s, boolean preview) {
            Music.Track t = Music.current;
            if (t == null && !preview) {
                if (!Music.supported()) return;
                t = new Music.Track("Nothing playing", "Open Spotify and play a song", "", 0, 0, 0, false);
            } else if (t == null) {
                t = new Music.Track("Jingle Bell Rock", "Your Favourite Artist", "Spotify", 42000, 130000, System.currentTimeMillis(), true);
            }
            int w = 176, h = height(mc, preview);
            if (!s.bg.equals("none")) {
                Gfx.round(ctx, 0, 0, w, h, 0xE6121212);
                Gfx.outline(ctx, 0, 0, w, h, 0x661DB954);
                Gfx.snowCap(ctx, 1, 0, w - 2, 0f, 0.95f);
            }
            ctx.fillGradient(5, 6, 39, 40, 0xFF1DB954, 0xFF0B5E2A);
            Gfx.sprite(ctx, new String[]{"...####", "...#..#", "...#..#", "...#..#", ".###.##", "####.##", ".##...."}, new int[]{0xFFFFFFFF}, "#", 15, 15, 2, 1f);
            ctx.drawText(mc.textRenderer, Gfx.trim(t.title(), w - 50), 44, 7, s.text, s.shadow);
            ctx.drawText(mc.textRenderer, Gfx.trim(t.artist(), w - 50), 44, 18, 0xFFB3B3B3, s.shadow);
            long pos = preview && Music.current == null ? 42000 : Music.positionMs();
            long dur = t.durMs();
            int bx = 44, bw = w - 50, by = 32;
            ctx.fill(bx, by, bx + bw, by + 2, 0xFF4D4D4D);
            if (dur > 0) ctx.fill(bx, by, bx + (int) (bw * Math.min(1, pos / (double) dur)), by + 2, 0xFF1DB954);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(bx, by + 4, 0);
            ctx.getMatrices().scale(0.5f, 0.5f, 1);
            ctx.drawText(mc.textRenderer, fmt(pos), 0, 0, 0xFFB3B3B3, false);
            String d = dur > 0 ? fmt(dur) : "";
            ctx.drawText(mc.textRenderer, d, bw * 2 - mc.textRenderer.getWidth(d), 0, 0xFFB3B3B3, false);
            ctx.getMatrices().pop();
            if (lyricsOn()) {
                String line = preview && Music.current == null ? "Jingle bell, jingle bell, jingle bell rock" : Music.currentLine();
                if (line.isEmpty()) line = Music.lyricsStatus.isEmpty() ? "..." : Music.lyricsStatus;
                ctx.drawText(mc.textRenderer, Gfx.trim(line, w - 10), 5, 46, s.accent, s.shadow);
            }
        }

        private static String fmt(long ms) {
            long sec = ms / 1000;
            return (sec / 60) + ":" + String.format("%02d", sec % 60);
        }
    }

    private static String direction(MinecraftClient mc) {
        if (mc.player == null) return "North (-Z)";
        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
        String[] names = {"South (+Z)", "South West", "West (-X)", "North West", "North (-Z)", "North East", "East (+X)", "South East"};
        int idx = Math.floorMod(Math.round(yaw / 45f), 8);
        return names[idx];
    }

    public static final List<HudModule> MODULES = List.of(
        new TextModule("fps", "FPS", "Frames per second", true, 0.006f, 0.012f, "FPS", mc -> String.valueOf(mc.getCurrentFps())),
        new TextModule("cps", "CPS", "Clicks per second (left | right)", true, 0.006f, 0.07f, "CPS", mc -> cps(0) + " | " + cps(1)),
        new TextModule("coords", "Coordinates", "Your X Y Z position", true, 0.006f, 0.128f, "XYZ",
            mc -> mc.player == null ? "12, 64, -30" : String.format("%d, %d, %d", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ())),
        new Countdown(),
        new Keystrokes(),
        new Armor(),
        new Potions(),
        new NowPlaying(),
        new TextModule("direction", "Direction", "Which way you are facing", false, 0.006f, 0.80f, "Facing", Hud::direction),
        new TextModule("ping", "Ping", "Your connection to the server", false, 0.006f, 0.855f, "Ping",
            mc -> {
                if (mc.player == null || mc.getNetworkHandler() == null) return "0 ms";
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                return (e == null ? 0 : e.getLatency()) + " ms";
            }),
        new TextModule("clock", "Clock", "Your real-world time", false, 0.86f, 0.012f, "Time", mc -> String.format("%02d:%02d", LocalTime.now().getHour(), LocalTime.now().getMinute())),
        new TextModule("memory", "Memory", "How much RAM Minecraft is using", false, 0.86f, 0.07f, "Mem",
            mc -> {
                Runtime r = Runtime.getRuntime();
                long used = r.totalMemory() - r.freeMemory();
                return (used * 100 / r.maxMemory()) + "%";
            }),
        new TextModule("server", "Server Address", "The server you are playing on", false, 0.86f, 0.128f, "IP",
            mc -> {
                ServerInfo si = mc.getCurrentServerEntry();
                return si != null ? si.address : "Singleplayer";
            }),
        new TextModule("speed", "Speed", "How fast you move in blocks per second", false, 0.006f, 0.91f, "Speed", mc -> String.format("%.1f b/s", speed)),
        new TextModule("sprint_status", "Sprint Status", "Shows when Toggle Sprint is on", false, 0.006f, 0.96f, "",
            mc -> Mods.on("toggle_sprint") ? "Sprinting (Toggled)" : "Walking")
    );

    public static HudModule byId(String id) {
        for (HudModule m : MODULES) if (m.id.equals(id)) return m;
        return null;
    }

    /** Screen-space bounds {x, y, w, h} of a module in GUI pixels. */
    public static int[] bounds(MinecraftClient mc, HudModule m, int sw, int sh, boolean preview) {
        JalyConfig.HudStyle s = JalyConfig.get().style(m.id);
        float total = JalyConfig.get().hudScale * s.scale;
        int w = Math.round(m.width(mc, preview) * total), h = Math.round(m.height(mc, preview) * total);
        float[] p = m.pos();
        int x = Math.round(p[0] * sw), y = Math.round(p[1] * sh);
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
        return new int[]{x, y, w, h};
    }

    public static void drawModule(DrawContext ctx, MinecraftClient mc, HudModule m, int sw, int sh, boolean preview) {
        int[] b = bounds(mc, m, sw, sh, preview);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(b[0], b[1], 0);
        drawAt(ctx, mc, m, preview);
        ctx.getMatrices().pop();
    }

    /** Draws a module with its style at the current origin. */
    public static void drawAt(DrawContext ctx, MinecraftClient mc, HudModule m, boolean preview) {
        JalyConfig.HudStyle s = JalyConfig.get().style(m.id);
        float total = JalyConfig.get().hudScale * s.scale;
        ctx.getMatrices().push();
        ctx.getMatrices().scale(total, total, 1);
        if (!(m instanceof Keystrokes) && !(m instanceof Armor) && !(m instanceof Potions) && !(m instanceof NowPlaying)) {
            HudModule.box(ctx, m.width(mc, preview), m.height(mc, preview), s);
        }
        m.draw(ctx, mc, s, preview);
        ctx.getMatrices().pop();
    }

    /** Scaled size {w, h} of a module. */
    public static int[] size(MinecraftClient mc, HudModule m, boolean preview) {
        float total = JalyConfig.get().hudScale * JalyConfig.get().style(m.id).scale;
        return new int[]{Math.round(m.width(mc, preview) * total), Math.round(m.height(mc, preview) * total)};
    }

    /** Called from the HUD render callback. */
    public static void render(DrawContext ctx, MinecraftClient mc) {
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        if (Mods.on("frost")) frost(ctx, sw, sh);
        if (Mods.on("lights") && JalyConfig.get().snowAmount() > 0) Gfx.lights(ctx, 0, sw, 0, 16, 1f, Gfx.now(), 0.9f);
        for (HudModule m : MODULES) {
            if (Mods.on(m.id)) drawModule(ctx, mc, m, sw, sh, false);
        }
    }

    private static void frost(DrawContext ctx, int sw, int sh) {
        int steps = 10;
        for (int i = 0; i < steps; i++) {
            int a = (int) (70 * (1 - i / (float) steps));
            int col = (a << 24) | 0xD8ECFF;
            ctx.fill(i * 2, 0, i * 2 + 2, sh, col);
            ctx.fill(sw - i * 2 - 2, 0, sw - i * 2, sh, col);
            ctx.fill(0, i * 2, sw, i * 2 + 2, col);
            ctx.fill(0, sh - i * 2 - 2, sw, sh - i * 2, col);
        }
    }
}
