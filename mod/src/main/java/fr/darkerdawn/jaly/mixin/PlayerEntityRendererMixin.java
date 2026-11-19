package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.cosmetic.NameStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Festive nametag: logo + colour + effect on your name. */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Unique private static boolean jaly$isMe;

    //#if >=1.21.9
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("HEAD"))
    private void jaly$who(net.minecraft.client.render.entity.state.PlayerEntityRenderState state, MatrixStack matrices, net.minecraft.client.render.command.OrderedRenderCommandQueue queue, net.minecraft.client.render.state.CameraRenderState cam, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && state.id == mc.player.getId() && state.displayName != null && Mods.on("name_style"))
            state.displayName = NameStyle.mine(state.displayName.getString());
    }
    //#elif >=1.21.2
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"))
    private void jaly$who(net.minecraft.client.render.entity.state.PlayerEntityRenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vcp, int light, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        jaly$isMe = mc.player != null && state.id == mc.player.getId();
    }
    //#else
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"))
    private void jaly$who(AbstractClientPlayerEntity player, Text text, MatrixStack matrices, VertexConsumerProvider vcp, int light, float tickDelta, CallbackInfo ci) {
        jaly$isMe = player == MinecraftClient.getInstance().player;
    }
    //#endif

    //#if <1.21.9
    //#if >=1.21.2
    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), argsOnly = true)
    //#else
    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V", at = @At("HEAD"), argsOnly = true)
    //#endif
    private Text jaly$styled(Text text) {
        if (jaly$isMe && Mods.on("name_style")) return NameStyle.mine(text.getString());
        return text;
    }
    //#endif
}
