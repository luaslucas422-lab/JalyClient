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

    public static final boolean ON = Boolean.getBoolean("jaly.smoke");
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
            if (titleTicks == 125) {
                fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.put(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "frost_dragon");
                fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.put(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS, "dragon");
                c.setScreen(new fr.darkerdawn.jaly.screen.CosmeticsScreen(c.currentScreen));
            }
            if (titleTicks == 140) shot(c, "cosmetics preview without a world");
            if (titleTicks == 142) { fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.clear(); c.setScreen(new fr.darkerdawn.jaly.screen.JalyTitleScreen()); }
            if (titleTicks == 160) {
                check("config file saved", Files.exists(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("jalyclient.json")));
                System.out.println("[JALY-SMOKE] done, failures=" + fails);
                c.scheduleStop();
            }
            return;
        }
        if (c.player == null || c.world == null) {
            // Production launchers can't pass --quickPlaySingleplayer, so open the test world ourselves.
            if (ticks == 160 && c.getServer() == null && c.currentScreen instanceof fr.darkerdawn.jaly.screen.JalyTitleScreen
                && Files.exists(c.runDirectory.toPath().resolve("saves/smoke"))) {
                System.out.println("[JALY-SMOKE] opening the test world");
                c.createIntegratedServerLoader().start("smoke", () -> {});
            }
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
                cfg.wings = "dragon";
                cfg.nameFx = "candy";
                cfg.pet = "snowman";
                cfg.trail = "snow";
                boolean smooth = cfg.zoomSmooth;
                cfg.zoomSmooth = false;
                Features.zoomForced = true;
                double fov = 70;
                for (int i = 0; i < 80; i++) fov = Features.applyZoom(70);
                check("zoom works (fov " + Math.round(fov) + ")", fov < 30);
                Features.zoomForced = false;
                for (int i = 0; i < 80; i++) fov = Features.applyZoom(70);
                check("zoom releases", fov > 65);
                cfg.zoomSmooth = smooth;
                check("6 packs available", PacksScreen.profileId(c.getResourcePackManager(), "jaly_pvp") != null && PacksScreen.profileId(c.getResourcePackManager(), "jaly_crystal") != null);
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
            case 76 -> {
                for (String id : new String[]{"target", "next_gift", "hunger", "durability", "snow_steps", "low_health"}) {
                    check("new mod " + id + " registered", Mods.byId(id) != null);
                    cfg.set(id, true);
                }
                cfg.lowHealthAt = 40f; // force the low-health edges on for the screenshot
                fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.put(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "reindeer");
            }
            case 80 -> { shot(c, "in-game hud"); cfg.lowHealthAt = 6f; for (String id : new String[]{"target", "next_gift", "hunger", "durability", "snow_steps", "low_health"}) cfg.set(id, false); }
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
                check("OG bundle is gift 1", Festive.REWARDS[0].equals("bundle:og") && fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.NAME_COLOR, "og"));
                check("door 7 unlocked its pet", fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "present"));
                check("door 20 is still locked", !Festive.canOpen(20, LocalDate.now(), cfg.firstDay, cfg.opened));
            }
            case 262 -> shot(c, "gift card");
            case 264 -> c.setScreen(new PacksScreen(null));
            case 280 -> shot(c, "packs");
            case 282 -> c.setScreen(SettingsScreen.create(null));
            case 298 -> shot(c, "settings");
            case 300 -> c.setScreen(new HudEditorScreen(null));
            case 318 -> {
                LodgeScreen l = new LodgeScreen(null);
                c.setScreen(l);
                l.pokeCharacter();
            }
            case 330 -> shot(c, "lodge character grumpy");
            case 332, 340 -> ((LodgeScreen) c.currentScreen).pokeCharacter();
            case 346 -> {
                shot(c, "lodge what do you want");
                ((LodgeScreen) c.currentScreen).choose("mods");
            }
            case 375 -> {
                check("character opens the Mods tab", c.currentScreen instanceof fr.darkerdawn.jaly.screen.ModrinthScreen);
                shot(c, "modrinth mods tab");
                check("update version compare", fr.darkerdawn.jaly.net.Updates.newer("1.2.1", "1.2.0") && !fr.darkerdawn.jaly.net.Updates.newer("1.2.0", "1.2.0"));
                fr.darkerdawn.jaly.net.UserContent.install(fr.darkerdawn.jaly.net.Modrinth.Kind.MOD, new fr.darkerdawn.jaly.net.Modrinth.Project("EsAfCjCV", "appleskin", "AppleSkin", "", "", 0));
                fr.darkerdawn.jaly.net.UserContent.install(fr.darkerdawn.jaly.net.Modrinth.Kind.MOD, new fr.darkerdawn.jaly.net.Modrinth.Project("YL57xq9U", "iris", "Iris Shaders", "", "", 0));
            }
            case 380 -> {
                c.setScreen(null);
                c.options.hudHidden = true;
                c.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);
                fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.put(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "reindeer");
            }
            case 392 -> shot(c, "pet reindeer on the floor");
            case 394 -> fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.put(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "frost_dragon");
            case 404 -> shot(c, "pet frost dragon flying");
            case 406 -> {
                c.options.hudHidden = false;
                c.options.setPerspective(net.minecraft.client.option.Perspective.FIRST_PERSON);
                fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.clear();
            }
            case 470 -> {
                var data = fr.darkerdawn.jaly.net.UserContent.data();
                check("mod installed from Modrinth in game (" + data.mods.size() + ")", data.mods.containsKey("EsAfCjCV") || data.mods.containsKey("YL57xq9U"));
                check("restart flagged after install", fr.darkerdawn.jaly.net.UserContent.restartNeeded());
                fr.darkerdawn.jaly.net.UserContent.uninstall(fr.darkerdawn.jaly.net.Modrinth.Kind.MOD, "YL57xq9U");
                check("new mod removed again", !data.mods.containsKey("YL57xq9U"));
                c.setScreen(new fr.darkerdawn.jaly.screen.ModrinthScreen(null, fr.darkerdawn.jaly.net.Modrinth.Kind.RESOURCEPACK));
            }
            case 540 -> shot(c, "resource packs tab");
            case 542 -> c.setScreen(new fr.darkerdawn.jaly.screen.ModrinthScreen(null, fr.darkerdawn.jaly.net.Modrinth.Kind.SHADER));
            case 560 -> shot(c, "shaders tab");
            case 316 -> shot(c, "hud editor");
            case 562 -> c.setScreen(new fr.darkerdawn.jaly.screen.CustomizerScreen(null));
            case 580 -> shot(c, "menu customizer");
            case 582 -> {
                fr.darkerdawn.jaly.screen.CosmeticsScreen cs = new fr.darkerdawn.jaly.screen.CosmeticsScreen(null);
                c.setScreen(cs);
                cs.showTab(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.NAME_FX);
            }
            case 598 -> shot(c, "name effects");
            case 599 -> c.setScreen(new fr.darkerdawn.jaly.screen.ShopScreen(null));
            case 615 -> shot(c, "jolly shop");
            case 617, 625, 633 -> ((fr.darkerdawn.jaly.screen.ShopScreen) c.currentScreen).pokeShopkeeper();
            case 640 -> {
                shot(c, "shopkeeper angry");
                check("shop catalog loaded", fr.darkerdawn.jaly.net.ShopApi.PACKS.size() == 4 && fr.darkerdawn.jaly.net.ShopApi.ITEMS.size() >= 15);
                check("premium items start locked", !fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS, "frost_dragon"));
                ((fr.darkerdawn.jaly.screen.ShopScreen) c.currentScreen).openItems("wings:frost_dragon");
            }
            case 655 -> {
                shot(c, "shop try-on");
                check("try-on preview shows the wings", "frost_dragon".equals(fr.darkerdawn.jaly.cosmetic.Cosmetics.selected(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS)));
                ((fr.darkerdawn.jaly.screen.ShopScreen) c.currentScreen).openItems("hat:santa_mech");
            }
            case 665 -> shot(c, "shop try-on hat");
            case 667 -> ((fr.darkerdawn.jaly.screen.ShopScreen) c.currentScreen).openCoins();
            case 680 -> shot(c, "coin packs");
            case 682 -> c.setScreen(new LodgeScreen(null));
            case 690 -> {
                fr.darkerdawn.jaly.net.Friends.fakeForTest();
                fr.darkerdawn.jaly.net.ShopApi.signedIn = true;
                c.setScreen(new fr.darkerdawn.jaly.screen.FriendsScreen(null));
            }
            case 697 -> {
                shot(c, "friends list");
                fr.darkerdawn.jaly.net.ShopApi.signedIn = false;
                check("Ginger Buddy pet locked until a real friend", !fr.darkerdawn.jaly.cosmetic.Cosmetics.unlocked(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.PET, "buddy"));
                check("Modrinth icons loaded (" + fr.darkerdawn.jaly.net.RemoteImages.loaded() + ")", fr.darkerdawn.jaly.net.RemoteImages.loaded() > 0);
            }
            case 698 -> c.setScreen(new fr.darkerdawn.jaly.screen.CreditsScreen(null));
            case 704 -> { shot(c, "credits"); c.setScreen(new fr.darkerdawn.jaly.screen.CreditsScreen.MoreCreditsScreen(null)); }
            case 709 -> shot(c, "more credits");
            case 686 -> check("try-on cleared after leaving the shop", fr.darkerdawn.jaly.cosmetic.Cosmetics.PREVIEW.isEmpty()
                && "none".equals(fr.darkerdawn.jaly.cosmetic.Cosmetics.selected(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS)) || !"frost_dragon".equals(fr.darkerdawn.jaly.cosmetic.Cosmetics.selected(fr.darkerdawn.jaly.cosmetic.Cosmetics.Type.WINGS)));
            case 712 -> {
                cfg.logoStyle = "minecraft";
                cfg.splashText = "Merry Christmas from the test!";
                cfg.buttonStyle = "aurora";
                cfg.panorama = "aurora";
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
