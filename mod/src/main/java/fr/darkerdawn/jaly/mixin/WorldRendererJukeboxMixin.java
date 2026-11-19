package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.disc.CustomDiscs;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if >=1.21.2
@Mixin(net.minecraft.client.world.WorldEventHandler.class)
//#else
@Mixin(WorldRenderer.class)
//#endif
public abstract class WorldRendererJukeboxMixin {
    /** World events 1010/1011 = a jukebox starts/stops playing. Our custom discs replace the vanilla song. */
    @Inject(method = "processWorldEvent", at = @At("HEAD"), cancellable = true)
    private void jaly$customDisc(int eventId, BlockPos pos, int data, CallbackInfo ci) {
        if (eventId == 1010 && CustomDiscs.onJukeboxStart(pos)) ci.cancel();
        else if (eventId == 1011) CustomDiscs.onJukeboxStop(pos);
    }
}
