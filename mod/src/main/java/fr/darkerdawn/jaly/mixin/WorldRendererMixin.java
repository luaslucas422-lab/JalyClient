package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Festive block outline colours. */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @ModifyArgs(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;drawCuboidShapeOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/util/shape/VoxelShape;DDDFFFF)V"))
    private void jaly$outline(Args args) {
        if (!Mods.on("block_outline")) return;
        long t = System.currentTimeMillis();
        float r, g, b;
        switch (JalyConfig.get().outlineStyle) {
            case "gold" -> { r = 1f; g = 0.85f; b = 0.3f; }
            case "ice" -> { r = 0.6f; g = 0.85f; b = 1f; }
            case "green" -> { r = 0.25f; g = 0.85f; b = 0.35f; }
            case "lights" -> {
                int[] c = {0xE0384F, 0xFDE68A, 0x4CC3FF, 0x5CFF7A};
                int col = c[(int) (t / 500 % 4)];
                r = ((col >> 16) & 255) / 255f; g = ((col >> 8) & 255) / 255f; b = (col & 255) / 255f;
            }
            default -> { boolean red = (t / 400) % 2 == 0; r = red ? 0.88f : 1f; g = red ? 0.15f : 1f; b = red ? 0.25f : 1f; }
        }
        args.set(6, r);
        args.set(7, g);
        args.set(8, b);
        args.set(9, 0.9f);
    }
}
