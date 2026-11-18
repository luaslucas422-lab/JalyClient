package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.screen.CosmeticsScreen;
import fr.darkerdawn.jaly.screen.GiftsScreen;
import fr.darkerdawn.jaly.screen.HudEditorScreen;
import fr.darkerdawn.jaly.screen.LodgeScreen;
import fr.darkerdawn.jaly.screen.ModSettingsScreen;
import fr.darkerdawn.jaly.screen.ModsScreen;
import fr.darkerdawn.jaly.screen.PacksScreen;
import fr.darkerdawn.jaly.screen.SettingsScreen;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.time.LocalDate;

/** Automatic test run used by the build server only (-Djaly.smoke=true). Never runs for players. */
final class Smoke {
    private Smoke() {}

    static final boolean ON = Boolean.getBoolean("jaly.smoke");
    private static int ticks, worldTicks, titleTicks = -1, fails;
    private static boolean loadingShot;

    private static void shot(MinecraftClient c, String what) {
        System.out.println("[JALY-SMOKE] screenshot " + what);
        ScreenshotRecorder.saveScreenshot(c.runDirectory, c.getFramebuffer(), t -> {});
    }

    private static void check(String what, boolean ok) {
        System.out.println("[JALY-SMOKE] " + (ok ? "PASS " : "FAIL ") + what);
        if (!ok) fails++;
    }

    static void tick(MinecraftClient c) {
        ticks++;
        if (c.getOverlay() != null) {
            if (!loadingShot && ticks > 30) { loadingShot = true; shot(c, "loading screen"); }
            return;
        }
        if (titleTicks >= 0) {
            titleTicks++;
            if (titleTicks == 120) {
                check("title screen is the Jaly menu", c.currentScreen instanceof fr.darkerdawn.jaly.screen.JalyTitleScreen);
                shot(c, "main menu");
            }
            if (titleTicks == 150) {
                check("config file saved", Files.exists(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("jalyclient.json")));
                System.out.println("[JALY-SMOKE] done, failures=" + fails);
                c.scheduleStop();
            }
            return;
        }
        if (c.player == null || c.world == null) {
            if (ticks == 1500) { System.out.println("[JALY-SMOKE] no world"); titleTicks = 0; }
            return;
        }
        worldTicks++;
        JalyConfig cfg = JalyConfig.get();
        switch (worldTicks) {
            case 20 -> {
                cfg.firstDay = LocalDate.now().toEpochDay() - 9;
                cfg.opened.clear();
                for (int i = 1; i <= 5; i++) cfg.opened.add(i);
                cfg.hat = "santa";
                cfg.cape = "candy_cane";
                cfg.snowfallLevel = "medium";
                cfg.set("mob_hats", true);
                cfg.set("frost", true);
                cfg.set("now_playing", true);
                cfg.set("sky_lyrics", true);
                cfg.set("clock", true);
                cfg.set("minimap", true);
                cfg.set("items", true);
                cfg.set("combo", true);
                cfg.set("biome", true);
                cfg.set("chat_time", true);
                cfg.set("radio", true);
                cfg.wings = "angel";
                cfg.pet = "snowman";
                cfg.trail = "snow";
                Features.zoomForced = true;
                double fov = 70;
                for (int i = 0; i < 80; i++) fov = Features.applyZoom(70);
                check("zoom works (fov " + Math.round(fov) + ")", fov < 30);
                Features.zoomForced = false;
                for (int i = 0; i < 80; i++) fov = Features.applyZoom(70);
                check("zoom releases", fov > 65);
                check("4 Christmas packs available", PacksScreen.profileId(c.getResourcePackManager(), "jaly_frost") != null && PacksScreen.profileId(c.getResourcePackManager(), "jaly_candy") != null);
                check("gift exclusive starts locked", !fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "present"));
                c.inGameHud.getChatHud().addMessage(Text.literal("Merry Christmas from the JalyClient test!"));
                Music.fake("Jingle Bell Rock", "Test Artist", "[00:00.00] Jingle bell, jingle bell\n[00:03.00] Jingle bell rock\n[00:40.00] Jingle bells swing and jingle bells ring\n[00:50.00] Snowing and blowing up bushels of fun", 41000, 130000);
                check("lyrics parsed", Music.lyrics.size() == 4 && Music.currentLine().startsWith("Jingle bells swing"));
                check("Jaly Christmas pack on by default", PacksScreen.enabled(c.getResourcePackManager(), "jaly_christmas"));
                check("Jaly Winter pack available", PacksScreen.profileId(c.getResourcePackManager(), "jaly_winter") != null);
                check("all mods registered", Mods.ALL.size() >= 25);
                c.player.setYaw(0);
                c.player.setPitch(-8);
                var srv = c.getServer();
                if (srv != null) srv.execute(() -> {
                    for (ServerPlayerEntity p : srv.getPlayerManager().getPlayerList()) {
                        p.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                        p.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                        p.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
                        p.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 2400, 1));
                        ServerWorld w = p.getServerWorld();
                        BlockPos base = p.getBlockPos();
                        w.setBlockState(base.add(1, 0, 3), Blocks.CHEST.getDefaultState());
                        w.setBlockState(base.add(2, 0, 3), Blocks.OAK_LEAVES.getDefaultState());
                        ZombieEntity z = EntityType.HUSK.create(w);
                        if (z != null) {
                            z.refreshPositionAndAngles(p.getX() - 1.5, p.getY(), p.getZ() + 3.5, 180, 0);
                            z.setAiDisabled(true);
                            z.setInvulnerable(true);
                            w.spawnEntity(z);
                        }
                        w.setTimeOfDay(6000);
                    }
                });
            }
            case 80 -> shot(c, "in-game hud");
            case 82 -> c.player.setPitch(-28);
            case 84 -> shot(c, "sky lyrics");
            case 86 -> c.player.setPitch(-8);
            case 88 -> { c.options.setPerspective(Perspective.THIRD_PERSON_BACK); c.player.setPitch(10); }
            case 110 -> shot(c, "cape from behind");
            case 112 -> { c.options.setPerspective(Perspective.THIRD_PERSON_FRONT); cfg.hat = "antlers"; cfg.wings = "fairy"; cfg.opened.add(14); }
            case 135 -> shot(c, "front with antlers");
            case 137 -> { c.options.setPerspective(Perspective.FIRST_PERSON); c.setScreen(new LodgeScreen(null)); }
            case 165 -> shot(c, "lodge");
            case 167 -> c.setScreen(new ModsScreen(null));
            case 185 -> shot(c, "mods");
            case 187 -> c.setScreen(new ModSettingsScreen(null, Mods.byId("now_playing")));
            case 205 -> shot(c, "now playing settings");
            case 207 -> {
                CosmeticsScreen s = new CosmeticsScreen(null);
                c.setScreen(s);
                s.showTab(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS);
            }
            case 230 -> shot(c, "cosmetics");
            case 232 -> {
                GiftsScreen g = new GiftsScreen(null);
                c.setScreen(g);
                check("door 7 opens", g.openDoor(7));
                check("door 7 unlocked its pet", fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "present"));
                check("door 20 is still locked", !Festive.canOpen(20, LocalDate.now(), cfg.firstDay, cfg.opened));
            }
            case 262 -> shot(c, "gift card");
            case 264 -> c.setScreen(new PacksScreen(null));
            case 280 -> shot(c, "packs");
            case 282 -> c.setScreen(SettingsScreen.create(null));
            case 298 -> shot(c, "settings");
            case 300 -> c.setScreen(new HudEditorScreen(null));
            case 316 -> shot(c, "hud editor");
            case 320 -> {
                c.setScreen(null);
                c.world.disconnect();
                c.disconnect(new MessageScreen(Text.literal("Saving")));
                c.setScreen(new TitleScreen());
                titleTicks = 0;
            }
            default -> { }
        }
    }
}
