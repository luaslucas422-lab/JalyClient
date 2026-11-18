package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Time Changer: your own sky time (only you see it). */
@Mixin(ClientWorld.Properties.class)
public abstract class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("RETURN"), cancellable = true)
    private void jaly$time(CallbackInfoReturnable<Long> cir) {
        if (!Mods.on("time_changer")) return;
        long day = cir.getReturnValue() / 24000L * 24000L;
        long t = switch (JalyConfig.get().timeOfDay) {
            case "morning" -> 1000L;
            case "noon" -> 6000L;
            case "sunset" -> 12500L;
            default -> 18000L;
        };
        cir.setReturnValue(day + t);
    }
}
