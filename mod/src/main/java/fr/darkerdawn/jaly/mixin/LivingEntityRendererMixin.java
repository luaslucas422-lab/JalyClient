package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Own Nametag: show your own name above your head in third person. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    //#if >=1.21.2
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true)
    private void jaly$ownLabel(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
    //#else
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true)
    private void jaly$ownLabel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
    //#endif
        MinecraftClient mc = MinecraftClient.getInstance();
        if (entity == mc.player && Mods.on("own_nametag") && !mc.options.getPerspective().isFirstPerson() && MinecraftClient.isHudEnabled() && !entity.isInvisible()) {
            cir.setReturnValue(true);
        }
    }
}
