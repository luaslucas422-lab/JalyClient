package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.render.TexturedRenderLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TexturedRenderLayers.class)
public abstract class TexturedRenderLayersMixin {
    //#if >=1.21.9
    @ModifyVariable(method = "getChestTextureId(Lnet/minecraft/client/render/block/entity/state/ChestBlockEntityRenderState$Variant;Lnet/minecraft/block/enums/ChestType;)Lnet/minecraft/client/util/SpriteIdentifier;", at = @At("HEAD"), argsOnly = true)
    private static net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState.Variant jaly$christmasChests(net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState.Variant v) {
        if (!Mods.on("chests") || v == net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState.Variant.ENDER_CHEST) return v;
        return net.minecraft.client.render.block.entity.state.ChestBlockEntityRenderState.Variant.CHRISTMAS;
    }
    //#else
    @ModifyVariable(method = "getChestTextureId(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/block/enums/ChestType;Z)Lnet/minecraft/client/util/SpriteIdentifier;", at = @At("HEAD"), argsOnly = true)
    private static boolean jaly$christmasChests(boolean christmas) {
        return christmas || Mods.on("chests");
    }
    //#endif
}
