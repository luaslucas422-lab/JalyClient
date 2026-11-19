package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.ui.Gfx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/** PvP tools: hit markers, kill counter, totem pops, item cooldowns, armor break warning. */
public final class PvpExtras {
    private PvpExtras() {}

    public static int kills, streak, bestStreak;
    private static int countedId = -1, lastTarget = -1;
    private static long lastTargetAt, hitAt, armorFlashAt;
    private static boolean wasDead, armorWarned;
    public static final Map<String, Integer> TOTEMS = new LinkedHashMap<>();
    private static String lastPop = "";
    private static long lastPopAt;
    private static Object lastWorld;

    public static void onHit(Entity target) {
        hitAt = System.currentTimeMillis();
        lastTarget = target.getId();
        lastTargetAt = hitAt;
        if (Mods.on("hit_markers")) Sounds.play(Sounds.CLICK, 1.8f, 0.35f);
    }

    /** Called when any living entity pops a totem (status 35). */
    public static void onTotem(LivingEntity e) {
        String n = e.getName().getString();
        TOTEMS.merge(n, 1, Integer::sum);
        lastPop = n;
        lastPopAt = System.currentTimeMillis();
    }

    public static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        if (lastWorld != mc.world) {
            lastWorld = mc.world;
            TOTEMS.clear();
        }
        // kills: whatever you hit in the last 3 seconds and then dies
        if (lastTarget >= 0 && System.currentTimeMillis() - lastTargetAt < 3000 && lastTarget != countedId) {
            Entity e = mc.world.getEntityById(lastTarget);
            if (e instanceof LivingEntity le && (le.isDead() || le.getHealth() <= 0)) {
                countedId = lastTarget;
                kills++;
                streak++;
                bestStreak = Math.max(bestStreak, streak);
            }
        }
        boolean dead = mc.player.isDead();
        if (dead && !wasDead) streak = 0;
        wasDead = dead;
        // armor about to break
        if (Mods.on("armor_warning")) {
            boolean low = false;
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = mc.player.getEquippedStack(s);
                if (!st.isEmpty() && st.isDamageable() && (st.getMaxDamage() - st.getDamage()) < st.getMaxDamage() * JalyConfig.get().armorWarnAt) low = true;
            }
            if (low && !armorWarned) {
                armorFlashAt = System.currentTimeMillis();
                Sounds.play(Sounds.CHIME, 0.5f, 1f);
            }
            armorWarned = low;
        }
    }

    public static String killsText(MinecraftClient mc) {
        return kills + " | Streak " + streak + (bestStreak > 1 ? " (best " + bestStreak + ")" : "");
    }

    public static String totemText(MinecraftClient mc) {
        if (mc.world == null) return "Notch x2";
        if (TOTEMS.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        int n = 0;
        for (var e : TOTEMS.entrySet()) {
            if (n++ > 0) sb.append(", ");
            sb.append(e.getKey()).append(" x").append(e.getValue());
            if (n >= 3) break;
        }
        return sb.toString();
    }

    private static final Item[] COOLDOWN_ITEMS = {Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.WIND_CHARGE, Items.SHIELD, Items.GOAT_HORN, Items.TRIDENT};
    private static final String[] COOLDOWN_NAMES = {"Pearl", "Chorus", "Wind", "Shield", "Horn", "Trident"};

    public static String cooldownText(MinecraftClient mc) {
        if (mc.player == null) return "Pearl 64%";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < COOLDOWN_ITEMS.length; i++) {
            float p = Compat.cooldown(mc.player, COOLDOWN_ITEMS[i]);
            if (p > 0) {
                if (!sb.isEmpty()) sb.append("  ");
                sb.append(COOLDOWN_NAMES[i]).append(' ').append(Math.round(p * 100)).append('%');
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /** Hit markers around the crosshair, the armor warning flash and the "popped a totem" banner. */
    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        int w = mc.getWindow().getScaledWidth(), h = mc.getWindow().getScaledHeight();
        long now = System.currentTimeMillis();
        if (Mods.on("hit_markers") && now - hitAt < 260) {
            float a = 1f - (now - hitAt) / 260f;
            int c = Gfx.alpha(JalyConfig.get().hitMarkerColor, a);
            int cx = w / 2, cy = h / 2, o = 4 + (int) ((1 - a) * 2);
            for (int i = 0; i < 3; i++) {
                ctx.fill(cx - o - i, cy - o - i, cx - o - i + 1, cy - o - i + 1, c);
                ctx.fill(cx + o + i, cy - o - i, cx + o + i + 1, cy - o - i + 1, c);
                ctx.fill(cx - o - i, cy + o + i, cx - o - i + 1, cy + o + i + 1, c);
                ctx.fill(cx + o + i, cy + o + i, cx + o + i + 1, cy + o + i + 1, c);
            }
        }
        if (Mods.on("armor_warning") && armorWarned) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 150.0);
            String msg = "ARMOR BREAKING!";
            int tw = mc.textRenderer.getWidth(msg);
            Gfx.text(ctx, msg, w / 2 - tw / 2, h / 2 + 24, Gfx.alpha(0xFFFF5050, now - armorFlashAt < 3000 ? 1f : 0.35f + 0.4f * pulse), true);
        }
        if (Mods.on("totem_pops") && now - lastPopAt < 2500 && !lastPop.isEmpty()) {
            String msg = lastPop + " popped a totem! (" + TOTEMS.getOrDefault(lastPop, 1) + ")";
            int tw = mc.textRenderer.getWidth(msg);
            Gfx.text(ctx, msg, w / 2 - tw / 2, h / 3, Gfx.alpha(0xFFFFD94A, Math.min(1f, (2500 - (now - lastPopAt)) / 500f)), true);
        }
    }
}
