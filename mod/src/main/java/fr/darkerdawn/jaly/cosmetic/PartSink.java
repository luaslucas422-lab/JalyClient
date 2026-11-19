package fr.darkerdawn.jaly.cosmetic;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/** Where cosmetic model parts get drawn: straight into buffers, or (1.21.9+) queued with the game's render commands. */
public interface PartSink {
    void part(ModelPart part, MatrixStack matrices, RenderLayer layer, int light);

    static PartSink of(VertexConsumerProvider vc) {
        return (p, ms, layer, light) -> p.render(ms, vc.getBuffer(layer), light, OverlayTexture.DEFAULT_UV);
    }

    //#if >=1.21.9
    static PartSink of(net.minecraft.client.render.command.OrderedRenderCommandQueue queue) {
        return (p, ms, layer, light) -> queue.submitModelPart(p, ms, layer, light, OverlayTexture.DEFAULT_UV, null);
    }
    //#endif
}
