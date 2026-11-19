package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.disc.CustomDiscs;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererJukeboxMixin {
    @Inject(method = "playJukeboxSong", at = @At("HEAD"), cancellable = true, require = 0)
    private void jaly$customDisc(RegistryEntry<JukeboxSong> song, BlockPos pos, CallbackInfo ci) {
        if (CustomDiscs.onJukeboxStart(pos)) ci.cancel();
    }

    @Inject(method = "stopJukeboxSongAndUpdate", at = @At("HEAD"), require = 0)
    private void jaly$customDiscStop(BlockPos pos, CallbackInfo ci) {
        CustomDiscs.onJukeboxStop(pos);
    }
}
