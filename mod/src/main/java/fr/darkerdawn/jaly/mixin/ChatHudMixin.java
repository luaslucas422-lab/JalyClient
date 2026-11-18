package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;

/** Chat timestamps like [21:37]. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", at = @At("HEAD"), argsOnly = true)
    private Text jaly$timestamp(Text message) {
        if (!Mods.on("chat_time")) return message;
        LocalTime n = LocalTime.now();
        return Text.literal(String.format("[%02d:%02d] ", n.getHour(), n.getMinute())).formatted(Formatting.GOLD).append(message);
    }
}
