package fr.darkerdawn.jaly;

import fr.darkerdawn.jaly.cosmetic.HatFeature;
import fr.darkerdawn.jaly.hud.Hud;
import fr.darkerdawn.jaly.screen.HudEditorScreen;
import fr.darkerdawn.jaly.screen.LodgeScreen;
import fr.darkerdawn.jaly.ui.JalyButton;
import fr.darkerdawn.jaly.ui.Snow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JalyClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("JalyClient");
    public static final String VERSION = FabricLoader.getInstance().getModContainer("jalyclient").map(c -> c.getMetadata().getVersion().getFriendlyString().split("\\+")[0]).orElse("1.0.0");
    private static final String CATEGORY = "key.categories.jalyclient";
    public static KeyBinding MENU_KEY, ZOOM_KEY, HUD_KEY, DISC_KEY;
    private static final Snow PAUSE_SNOW = new Snow(70, 21);

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void onInitializeClient() {
        JalyConfig.load(FabricLoader.getInstance().getConfigDir());
        Mods.init();
        //#if >=1.21.6
        net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry.register(c -> new fr.darkerdawn.jaly.cosmetic.AvatarPreview.Renderer(c.vertexConsumers()));
        //#endif
        LOG.info("JalyClient {} loaded with {} mods. Merry Christmas!", VERSION, Mods.ALL.size());

        MENU_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jalyclient.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
        ZOOM_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jalyclient.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));
        HUD_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jalyclient.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY));
        DISC_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.jalyclient.disc", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        FabricLoader.getInstance().getModContainer("jalyclient").ifPresent(c -> {
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_christmas"), c, Text.literal("Jaly Christmas"), ResourcePackActivationType.DEFAULT_ENABLED);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_winter"), c, Text.literal("Jaly Winter Wonderland"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_frost"), c, Text.literal("Jaly Frost PvP"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_candy"), c, Text.literal("Jaly Candy Land"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_pvp"), c, Text.literal("Jaly PvP"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_crystal"), c, Text.literal("Jaly Crystal"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_grass"), c, Text.literal("Jaly Tall Smooth Grass"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_grass_xl"), c, Text.literal("Jaly Extra Tall Grass"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_rain"), c, Text.literal("Jaly Realistic Rain"), ResourcePackActivationType.NORMAL);
            ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("jalyclient", "jaly_rain_blocky"), c, Text.literal("Jaly Blocky Rain"), ResourcePackActivationType.NORMAL);
        });

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client -> fr.darkerdawn.jaly.net.Friends.leaving());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Smoke.ON) Smoke.tick(client);
            while (MENU_KEY.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new LodgeScreen(null));
            }
            while (DISC_KEY.wasPressed()) {
                if (client.currentScreen == null && fr.darkerdawn.jaly.disc.CustomDiscs.enabled()) client.setScreen(new fr.darkerdawn.jaly.disc.CustomDiscScreen(null));
            }
            while (HUD_KEY.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new HudEditorScreen(null));
            }
            Features.zoomHeld = Features.zoomKeyDown(client);
            Features.tick(client);
            fr.darkerdawn.jaly.net.Friends.tick(client);
            fr.darkerdawn.jaly.visual.Ambience.tick(client);
            fr.darkerdawn.jaly.visual.BetterRain.tick(client);
            fr.darkerdawn.jaly.visual.BetterGlass.tick(client);
            fr.darkerdawn.jaly.visual.InteractiveBlocks.tick(client);
            fr.darkerdawn.jaly.visual.BetterSounds.tick(client);
            PvpExtras.tick(client);
            fr.darkerdawn.jaly.disc.CustomDiscs.tick(client);
            Music.tick();
            Radio.tick(client);
            if (client.world == null && client.getOverlay() == null && client.currentScreen != null && JalyConfig.get().menuMusic
                && !(client.currentScreen instanceof fr.darkerdawn.jaly.screen.ShopScreen)) {
                Sounds.tickMenuMusic(client);
            } else {
                Sounds.stopMenuMusic(client);
            }
        });

        HudRenderCallback.EVENT.register((ctx, tickCounter) -> Hud.render(ctx, MinecraftClient.getInstance()));
        WorldRenderEvents.AFTER_ENTITIES.register(SkyLyrics::render);
        WorldRenderEvents.AFTER_ENTITIES.register(fr.darkerdawn.jaly.visual.Ambience::render);
        WorldRenderEvents.AFTER_ENTITIES.register(fr.darkerdawn.jaly.visual.BetterRain::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(fr.darkerdawn.jaly.visual.BetterGlass::render);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient && world.getBlockState(hit.getBlockPos()).isOf(net.minecraft.block.Blocks.JUKEBOX)) {
                fr.darkerdawn.jaly.disc.CustomDiscs.rememberInsert(hit.getBlockPos(), player.getStackInHand(hand));
            }
            return ActionResult.PASS;
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, context) -> {
            if (renderer.getModel() instanceof BipedEntityModel) {
                helper.register((net.minecraft.client.render.entity.feature.FeatureRenderer) new HatFeature((FeatureRendererContext) renderer));
            }
            if (renderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer) {
                helper.register((net.minecraft.client.render.entity.feature.FeatureRenderer) new fr.darkerdawn.jaly.cosmetic.PlayerExtrasFeature((FeatureRendererContext) renderer));
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient) {
                Features.onAttack(entity);
                PvpExtras.onHit(entity);
            }
            return ActionResult.PASS;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof GameMenuScreen) {
                Screens.getButtons(screen).add(new JalyButton(4, 4, 76, 18, Text.literal("Jaly Lodge"), b -> client.setScreen(new LodgeScreen(screen))));
                ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> PAUSE_SNOW.renderFull(ctx, s.width, s.height, 0.8f));
            }
        });
    }

    /** Title screen hook (menu music is handled in the tick above). */
    public static void tickMenuMusicSafe() {}
}
