package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.cosmetic.Cosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
//#if >=1.21.9
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
//#else
import net.minecraft.client.util.SkinTextures;
//#endif
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    //#if >=1.21.9
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    //#else
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    //#endif
    private void jaly$cape(CallbackInfoReturnable<SkinTextures> cir) {
        Identifier cape = Cosmetics.capeFor((AbstractClientPlayerEntity) (Object) this);
        if (cape == null) return;
        SkinTextures s = cir.getReturnValue();
        //#if >=1.21.9
        AssetInfo.TextureAssetInfo c = new AssetInfo.TextureAssetInfo(cape, cape);
        cir.setReturnValue(new SkinTextures(s.body(), c, c, s.model(), s.secure()));
        //#else
        cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), cape, cape, s.model(), s.secure()));
        //#endif
    }
}
