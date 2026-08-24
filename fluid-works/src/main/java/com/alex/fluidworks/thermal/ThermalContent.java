package com.alex.fluidworks.thermal;

import com.alex.fluidworks.FluidWorks;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerDefinition;
import dev.liquidfabric.api.unofficial.core.UtilityApiRegistries;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.EnumMap;
import java.util.Map;

/** Owns the isolated registration slice for thermal pipes and temperature machines. */
public final class ThermalContent {
    public static final Map<HeaterPipeMaterial, HeaterPipeBlock> HEATER_PIPES =
        new EnumMap<>(HeaterPipeMaterial.class);

    public static TemperatureMachineBlock HEATER;
    public static TemperatureMachineBlock COOLER;
    public static BlockEntityType<HeaterPipeBlockEntity> HEATER_PIPE_BLOCK_ENTITY;
    public static BlockEntityType<TemperatureMachineBlockEntity> TEMPERATURE_MACHINE_BLOCK_ENTITY;
    public static ExtendedScreenHandlerType<TemperatureMachineScreenHandler, Boolean>
        TEMPERATURE_MACHINE_SCREEN_HANDLER;

    private static boolean initialized;

    private ThermalContent() { }

    public static void initialize() {
        if (initialized) return;
        initialized = true;

        for (HeaterPipeMaterial material : HeaterPipeMaterial.values()) {
            String path = material.id() + "_heater_pipe";
            Block source = material == HeaterPipeMaterial.COPPER ? Blocks.COPPER_BLOCK : Blocks.IRON_BLOCK;
            HeaterPipeBlock block = (HeaterPipeBlock) FluidWorks.registerSimpleBlock(path,
                new HeaterPipeBlock(material, FluidWorks.settingsFor(path,
                    AbstractBlock.Settings.copy(source).nonOpaque())));
            HEATER_PIPES.put(material, block);
        }
        HEATER = (TemperatureMachineBlock) FluidWorks.registerSimpleBlock("heater",
            new TemperatureMachineBlock(true, FluidWorks.settingsFor("heater",
                AbstractBlock.Settings.copy(Blocks.BLAST_FURNACE).nonOpaque())));
        COOLER = (TemperatureMachineBlock) FluidWorks.registerSimpleBlock("cooler",
            new TemperatureMachineBlock(false, FluidWorks.settingsFor("cooler",
                AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque())));

        HEATER_PIPE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            FluidWorks.id("heater_pipe"), FabricBlockEntityTypeBuilder.create(
                HeaterPipeBlockEntity::new, HEATER_PIPES.values().toArray(Block[]::new)).build());
        TEMPERATURE_MACHINE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            FluidWorks.id("temperature_machine"), FabricBlockEntityTypeBuilder.create(
                TemperatureMachineBlockEntity::new, HEATER, COOLER).build());
        TEMPERATURE_MACHINE_SCREEN_HANDLER = Registry.register(Registries.SCREEN_HANDLER,
            FluidWorks.id("temperature_machine"),
            new ExtendedScreenHandlerType<>(TemperatureMachineScreenHandler::new, PacketCodecs.BOOLEAN));

        UtilityApiRegistries.registerFluidStorage(HEATER_PIPE_BLOCK_ENTITY,
            (pipe, side) -> pipe.liquidFabricStorage());
        for (Map.Entry<HeaterPipeMaterial, HeaterPipeBlock> entry : HEATER_PIPES.entrySet()) {
            String path = entry.getKey().id() + "_heater_pipe";
            UtilityApiRegistries.registerFluidContainerBlock(FluidWorks.id(path), entry.getValue(),
                new BlockFluidContainerDefinition(HeaterPipeBlockEntity.CAPACITY,
                    new BlockFluidContainerDefinition.Bounds(3 / 16F, 3 / 16F, 0.001F,
                        13 / 16F, 13 / 16F, 0.999F), fluidId -> true),
                (blockEntity, side) -> ((HeaterPipeBlockEntity) blockEntity).liquidFabricStorage());
        }

        ThermalApiBridge.initialize();
    }
}
