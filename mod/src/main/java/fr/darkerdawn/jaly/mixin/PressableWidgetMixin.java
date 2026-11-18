package fr.darkerdawn.jaly.mixin;

import fr.darkerdawn.jaly.JalyConfig;
import fr.darkerdawn.jaly.ui.JalyButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Buttons everywhere": every Minecraft button uses your chosen JalyClient button style. */
@Mixin(PressableWidget.class)
public abstract class PressableWidgetMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void jaly$style(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Object self = this;
        JalyConfig c = JalyConfig.get();
        if (!(self instanceof ButtonWidget) || self instanceof JalyButton || !"everywhere".equals(c.buttonScope) || "vanilla".equals(c.buttonStyle)) return;
        JalyButton.renderStyled(ctx, (ClickableWidget) self, c.buttonStyle, false, false);
        ci.cancel();
    }
}
