package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.visual.WindGrass;
import net.minecraft.client.texture.SpriteContents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin {
    @Shadow
    public abstract Identifier getId();

    //#if >=1.21.11
    @Inject(method = "createAnimator", at = @At("RETURN"), require = 0)
    private void jaly$wind(CallbackInfoReturnable<SpriteContents.Animator> cir) {
        if (cir.getReturnValue() != null && WindGrass.isGrass(getId())) WindGrass.track(cir.getReturnValue());
    }
    //#else
    @Inject(method = "createAnimator", at = @At("RETURN"), cancellable = true, require = 0)
    private void jaly$wind(CallbackInfoReturnable<net.minecraft.client.texture.Animator> cir) {
        net.minecraft.client.texture.Animator a = cir.getReturnValue();
        if (a != null && WindGrass.isGrass(getId())) cir.setReturnValue(new WindGrass.Animator(a));
    }
    //#endif
}
