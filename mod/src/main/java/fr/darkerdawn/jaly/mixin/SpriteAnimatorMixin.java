package fr.darkerdawn.jaly.mixin;
//#if >=1.21.11

import fr.darkerdawn.jaly.visual.WindGrass;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tall Smooth Grass wind on 1.21.11: ticks the grass animation faster in wind and rain. */
@Mixin(SpriteContents.Animator.class)
public abstract class SpriteAnimatorMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void jaly$wind(CallbackInfo ci) {
        if (WindGrass.ticking) return;
        int n = WindGrass.ticksFor(this);
        if (n < 0 || n == 1) return;
        ci.cancel();
        WindGrass.ticking = true;
        try {
            for (int i = 0; i < n; i++) ((SpriteContents.Animator) (Object) this).tick();
        } finally {
            WindGrass.ticking = false;
        }
    }
}
//#endif
