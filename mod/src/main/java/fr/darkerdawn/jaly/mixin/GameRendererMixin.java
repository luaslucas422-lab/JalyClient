package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Features;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import fr.darkerdawn.jaly.Mods;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    //#if >=1.21.2
    private void jaly$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue((float) Features.applyZoom(cir.getReturnValue()));
    }
    //#else
    private void jaly$zoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(Features.applyZoom(cir.getReturnValue()));
    }
    //#endif

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void jaly$noHurtCam(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (Mods.on("no_hurt_cam")) ci.cancel();
    }
}
