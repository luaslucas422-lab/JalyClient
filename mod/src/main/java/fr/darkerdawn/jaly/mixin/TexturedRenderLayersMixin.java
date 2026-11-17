package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.render.TexturedRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TexturedRenderLayers.class)
public abstract class TexturedRenderLayersMixin {
    @ModifyVariable(method = "getChestTextureId(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/block/enums/ChestType;Z)Lnet/minecraft/client/util/SpriteIdentifier;", at = @At("HEAD"), argsOnly = true)
    private static boolean jaly$christmasChests(boolean christmas) {
        return christmas || Mods.on("chests");
    }
}
