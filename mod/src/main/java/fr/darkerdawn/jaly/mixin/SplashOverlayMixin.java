package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.screen.LoadingScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Paints the JalyClient loading screen over the vanilla one, following its fade exactly. */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow private float progress;
    @Shadow private long reloadCompleteTime;
    @Shadow private long reloadStartTime;
    @Shadow @Final private boolean reloading;

    @Inject(method = "render", at = @At("TAIL"))
    private void jaly$loading(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!JalyConfig.get().loadingScreen) return;
        long l = Util.getMeasuringTimeMs();
        float f = this.reloadCompleteTime > -1L ? (float) (l - this.reloadCompleteTime) / 1000.0F : -1.0F;
        float g = this.reloadStartTime > -1L ? (float) (l - this.reloadStartTime) / 500.0F : -1.0F;
        float alpha;
        if (f >= 1.0F) alpha = 1.0F - MathHelper.clamp(f - 1.0F, 0.0F, 1.0F);
        else if (this.reloading) alpha = MathHelper.clamp(g, 0.0F, 1.0F);
        else alpha = 1.0F;
        if (alpha <= 0.01F) return;
        LoadingScreen.render(context, context.getScaledWindowWidth(), context.getScaledWindowHeight(), alpha, this.progress, this.reloading || this.reloadCompleteTime > -1L);
    }
}
