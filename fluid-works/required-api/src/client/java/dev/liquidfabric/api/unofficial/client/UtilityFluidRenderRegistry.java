package dev.liquidfabric.api.unofficial.client;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.fluid.Fluid;
import net.minecraft.util.Identifier;

import java.util.Objects;

/** Public client hook for rendering real and fluidlogged fluid states. */
public final class UtilityFluidRenderRegistry {
    private UtilityFluidRenderRegistry() {}

    public static void register(Fluid still, Fluid flowing, Identifier stillTexture,
                                Identifier flowingTexture, int tintRgb) {
        Objects.requireNonNull(still, "still");
        Objects.requireNonNull(flowing, "flowing");
        Objects.requireNonNull(stillTexture, "stillTexture");
        Objects.requireNonNull(flowingTexture, "flowingTexture");
        FluidRenderHandlerRegistry.INSTANCE.register(still, flowing,
                new SimpleFluidRenderHandler(stillTexture, flowingTexture, 0xFF000000 | (tintRgb & 0xFFFFFF)));
        BlockRenderLayerMap.INSTANCE.putFluids(RenderLayer.getTranslucent(), still, flowing);
    }

    public static void register(Fluid fluid, Identifier stillTexture, Identifier flowingTexture, int tintRgb) {
        register(fluid, fluid, stillTexture, flowingTexture, tintRgb);
    }
}
