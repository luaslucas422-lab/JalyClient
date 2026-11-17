package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Features;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void jaly$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!Mods.on("crosshair")) return;
        if (!client.options.getPerspective().isFirstPerson()) return;
        if (client.getDebugHud().shouldShowDebugHud()) return;
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == net.minecraft.world.GameMode.SPECTATOR) return;
        ci.cancel();
        Features.drawCrosshair(context, client);
    }
}
