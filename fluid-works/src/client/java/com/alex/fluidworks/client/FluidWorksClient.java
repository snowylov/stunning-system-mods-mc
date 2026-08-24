package com.alex.fluidworks.client;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.ExpandedContent;
import com.alex.fluidworks.StorageCasesContent;
import com.alex.fluidworks.fluid.MetalFluidFamily;
import com.alex.fluidworks.fluid.SpecialFluidFamily;
import com.alex.fluidworks.thermal.ThermalContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;

public final class FluidWorksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 1.21.11 derives the material pipeline from the baked block model.
        HandledScreens.register(ThermalContent.TEMPERATURE_MACHINE_SCREEN_HANDLER,
            TemperatureMachineScreen::new);
        EntityRendererRegistry.register(FluidWorks.UNIVERSAL_FLUID_POTION_ENTITY,
            FlyingItemEntityRenderer::new);
        EntityRendererRegistry.register(FluidWorks.LINGERING_FLUID_MARKER_ENTITY,
            EmptyEntityRenderer::new);
        BlockEntityRendererFactories.register(StorageCasesContent.PORTABLE_CASE_BLOCK_ENTITY,
            FoodCoolerBlockEntityRenderer::new);
        BlockEntityRendererFactories.register(FluidWorks.DISPLAY_TABLE_BLOCK_ENTITY,
            TableDisplayBlockEntityRenderer::new);
        for (MetalFluidFamily family : ExpandedContent.METAL_FLUIDS) {
            FluidRenderHandlerRegistry.INSTANCE.register(family.still, family.flowing,
                SimpleFluidRenderHandler.coloredWater(family.tint));
        }
        for (SpecialFluidFamily family : ExpandedContent.SPECIAL_FLUIDS) {
            FluidRenderHandlerRegistry.INSTANCE.register(family.still, family.flowing,
                SimpleFluidRenderHandler.coloredWater(family.tint));
        }
    }
}
