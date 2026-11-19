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
    //#if >=1.21.9
    private static void jaly$lowFireStart(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vcp, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
    //#elif >=1.21.2
    private static void jaly$lowFireStart(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vcp, CallbackInfo ci) {
    //#else
    private static void jaly$lowFireStart(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        jaly$pushed = Mods.on("low_fire");
        if (jaly$pushed) {
            matrices.push();
            matrices.translate(0.0F, -0.3F, 0.0F);
        }
    }

    @Inject(method = "renderFireOverlay", at = @At("RETURN"))
    //#if >=1.21.9
    private static void jaly$lowFireEnd(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vcp, net.minecraft.client.texture.Sprite sprite, CallbackInfo ci) {
    //#elif >=1.21.2
    private static void jaly$lowFireEnd(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vcp, CallbackInfo ci) {
    //#else
    private static void jaly$lowFireEnd(MinecraftClient client, MatrixStack matrices, CallbackInfo ci) {
    //#endif
        if (jaly$pushed) {
            matrices.pop();
            jaly$pushed = false;
        }
    }
}
