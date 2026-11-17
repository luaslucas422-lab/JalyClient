package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.screen.JalyTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen jaly$titleScreen(Screen screen) {
        if (screen instanceof TitleScreen && JalyConfig.get().jalyTitle) return new JalyTitleScreen();
        return screen;
    }
}
