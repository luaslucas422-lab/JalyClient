package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameOverlayRenderer.class)
public abstract class InGameOverlayRendererMixin {
    @Unique private static boolean jaly$pushed;

    @Inject(method = "renderFireOverlay", at = @At("HEAD"))
    private static void jaly$lowFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        jaly$pushed = Mods.on("low_fire");
        if (jaly$pushed) {
            matrices.push();
            matrices.translate(0.0F, -0.3F, 0.0F);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    private static void jaly$lowFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
        if (jaly$pushed) {
            matrices.pop();
            jaly$pushed = false;
        }
    }
}
