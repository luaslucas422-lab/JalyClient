package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.Mods;
import fr.darkerdawn.jaly.cosmetic.NameStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Custom tab list name: your festive name with the JalyClient logo. */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void jaly$tabName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && Mods.on("name_style") && fr.darkerdawn.jaly.Compat.profileId(entry.getProfile()).equals(mc.player.getUuid())) {
            cir.setReturnValue(NameStyle.mine(fr.darkerdawn.jaly.Compat.profileName(entry.getProfile())));
        }
    }
}
