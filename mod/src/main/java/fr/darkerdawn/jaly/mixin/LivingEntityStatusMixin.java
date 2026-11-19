package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.PvpExtras;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityStatusMixin {
    @Inject(method = "handleStatus", at = @At("HEAD"))
    private void jaly$totem(byte status, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (status == 35 && fr.darkerdawn.jaly.Compat.isClient(fr.darkerdawn.jaly.Compat.world(self))) PvpExtras.onTotem(self);
    }
}
