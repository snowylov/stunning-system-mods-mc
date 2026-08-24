package dev.liquidfabric.api.unofficial.tank;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.tank.buildcraft.BaseStackableTankBlock;
import dev.liquidfabric.api.unofficial.tank.buildcraft.BaseStackableTankBlockEntity;
import dev.liquidfabric.api.unofficial.tank.drum.*;
import dev.liquidfabric.api.unofficial.tank.pipe.fluid.BaseFluidPipeBlock;
import dev.liquidfabric.api.unofficial.tank.pipe.fluid.BaseFluidPipeBlockEntity;
import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import dev.liquidfabric.api.unofficial.tank.filter.FluidFilterBlock;
import dev.liquidfabric.api.unofficial.tank.filter.FluidFilterItem;
import dev.liquidfabric.api.unofficial.tank.meter.FluidGaugeBlock;
import dev.liquidfabric.api.unofficial.tank.meter.FluidMeterItem;
import dev.liquidfabric.api.unofficial.tank.rain.RainCollectorBlock;
import dev.liquidfabric.api.unofficial.tank.rain.RainCollectorBlockEntity;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.EnumMap;
import java.util.Map;

public final class ModTanksAndPipes {
    public static BaseStackableTankBlock STACKABLE_TANK;
    public static BaseFluidPipeBlock FLUID_PIPE;
    public static FluidFilterBlock FLUID_FILTER_BLOCK;
    public static FluidGaugeBlock FLUID_GAUGE;
    public static Item FLUID_FILTER;
    public static Item FLUID_METER;
    public static RainCollectorBlock RAIN_COLLECTOR;

    public static CopperDrumBlock COPPER_DRUM, EXPOSED_COPPER_DRUM, WEATHERED_COPPER_DRUM, OXIDIZED_COPPER_DRUM;
    public static CopperDrumBlock WAXED_COPPER_DRUM, WAXED_EXPOSED_COPPER_DRUM, WAXED_WEATHERED_COPPER_DRUM, WAXED_OXIDIZED_COPPER_DRUM;

    public static FluidDrumBlock IRON_DRUM, GOLD_DRUM, DIAMOND_DRUM, NETHERITE_DRUM, OBSIDIAN_DRUM, VOID_DRUM, CREATIVE_DRUM;

    public static BlockEntityType<BaseStackableTankBlockEntity> STACKABLE_TANK_BE;
    public static BlockEntityType<BaseFluidPipeBlockEntity> FLUID_PIPE_BE;
    public static BlockEntityType<FluidDrumBlockEntity> FLUID_DRUM_BE;
    public static BlockEntityType<RainCollectorBlockEntity> RAIN_COLLECTOR_BE;

    private static final Map<CopperDrumBlock.Oxidation, CopperDrumBlock> UNWAXED_COPPER_DRUMS = new EnumMap<>(CopperDrumBlock.Oxidation.class);
    private static final Map<CopperDrumBlock.Oxidation, CopperDrumBlock> WAXED_COPPER_DRUMS = new EnumMap<>(CopperDrumBlock.Oxidation.class);

    private ModTanksAndPipes() {}

    public static void register() {
        STACKABLE_TANK = Registry.register(Registries.BLOCK, UtilityApiMod.id("stackable_tank"), new BaseStackableTankBlock(AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque()));
        FLUID_PIPE = Registry.register(Registries.BLOCK, UtilityApiMod.id("fluid_pipe"), new BaseFluidPipeBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque()));

        if (LiquidFabricConfig.enableFluidFilters) {
            FLUID_FILTER_BLOCK = Registry.register(Registries.BLOCK, UtilityApiMod.id("fluid_filter_block"), new FluidFilterBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)));
            FLUID_FILTER = Registry.register(Registries.ITEM, UtilityApiMod.id("fluid_filter"), new FluidFilterItem(new Item.Settings().maxCount(16)));
            Registry.register(Registries.ITEM, UtilityApiMod.id("fluid_filter_block"), new BlockItem(FLUID_FILTER_BLOCK, new Item.Settings()));
        }
        if (LiquidFabricConfig.enableFluidGauges) {
            FLUID_GAUGE = Registry.register(Registries.BLOCK, UtilityApiMod.id("fluid_gauge"), new FluidGaugeBlock(AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque()));
            Registry.register(Registries.ITEM, UtilityApiMod.id("fluid_gauge"), new BlockItem(FLUID_GAUGE, new Item.Settings()));
        }
        if (LiquidFabricConfig.enableFluidMeters) {
            FLUID_METER = Registry.register(Registries.ITEM, UtilityApiMod.id("fluid_meter"), new FluidMeterItem(new Item.Settings().maxCount(1)));
        }
        if (LiquidFabricConfig.enableRainCollector) {
            RAIN_COLLECTOR = Registry.register(Registries.BLOCK, UtilityApiMod.id("rain_collector"), new RainCollectorBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON)));
            Registry.register(Registries.ITEM, UtilityApiMod.id("rain_collector"), new BlockItem(RAIN_COLLECTOR, new Item.Settings()));
        }

        COPPER_DRUM = registerCopperDrum("copper_drum", CopperDrumBlock.Oxidation.UNAFFECTED, false);
        EXPOSED_COPPER_DRUM = registerCopperDrum("exposed_copper_drum", CopperDrumBlock.Oxidation.EXPOSED, false);
        WEATHERED_COPPER_DRUM = registerCopperDrum("weathered_copper_drum", CopperDrumBlock.Oxidation.WEATHERED, false);
        OXIDIZED_COPPER_DRUM = registerCopperDrum("oxidized_copper_drum", CopperDrumBlock.Oxidation.OXIDIZED, false);

        WAXED_COPPER_DRUM = registerCopperDrum("waxed_copper_drum", CopperDrumBlock.Oxidation.UNAFFECTED, true);
        WAXED_EXPOSED_COPPER_DRUM = registerCopperDrum("waxed_exposed_copper_drum", CopperDrumBlock.Oxidation.EXPOSED, true);
        WAXED_WEATHERED_COPPER_DRUM = registerCopperDrum("waxed_weathered_copper_drum", CopperDrumBlock.Oxidation.WEATHERED, true);
        WAXED_OXIDIZED_COPPER_DRUM = registerCopperDrum("waxed_oxidized_copper_drum", CopperDrumBlock.Oxidation.OXIDIZED, true);

        IRON_DRUM = registerDrumBlock("iron_drum", FluidContainerSizes.IRON_DRUM_DROPLETS, DrumMode.NORMAL, Blocks.IRON_BLOCK);
        GOLD_DRUM = registerDrumBlock("gold_drum", FluidContainerSizes.GOLD_DRUM_DROPLETS, DrumMode.NORMAL, Blocks.GOLD_BLOCK);
        DIAMOND_DRUM = registerDrumBlock("diamond_drum", FluidContainerSizes.DIAMOND_DRUM_DROPLETS, DrumMode.NORMAL, Blocks.DIAMOND_BLOCK);
        NETHERITE_DRUM = registerDrumBlock("netherite_drum", FluidContainerSizes.OBSIDIAN_DRUM_DROPLETS, DrumMode.NORMAL, Blocks.NETHERITE_BLOCK);
        OBSIDIAN_DRUM = registerDrumBlock("obsidian_drum", FluidContainerSizes.OBSIDIAN_DRUM_DROPLETS, DrumMode.NORMAL, Blocks.OBSIDIAN);
        VOID_DRUM = registerDrumBlock("void_drum", Long.MAX_VALUE / 8, DrumMode.VOID, Blocks.BLACK_CONCRETE);
        CREATIVE_DRUM = registerDrumBlock("creative_drum", Long.MAX_VALUE / 8, DrumMode.CREATIVE, Blocks.PURPLE_CONCRETE);

        Registry.register(Registries.ITEM, UtilityApiMod.id("stackable_tank"), new BlockItem(STACKABLE_TANK, new Item.Settings()));
        Registry.register(Registries.ITEM, UtilityApiMod.id("fluid_pipe"), new BlockItem(FLUID_PIPE, new Item.Settings()));

        STACKABLE_TANK_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, UtilityApiMod.id("stackable_tank"), BlockEntityType.Builder.create(BaseStackableTankBlockEntity::new, STACKABLE_TANK).build());
        FLUID_PIPE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, UtilityApiMod.id("fluid_pipe"), BlockEntityType.Builder.create(BaseFluidPipeBlockEntity::new, FLUID_PIPE).build());
        FLUID_DRUM_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, UtilityApiMod.id("fluid_drum"), BlockEntityType.Builder.create(
            FluidDrumBlockEntity::new,
            COPPER_DRUM, EXPOSED_COPPER_DRUM, WEATHERED_COPPER_DRUM, OXIDIZED_COPPER_DRUM,
            WAXED_COPPER_DRUM, WAXED_EXPOSED_COPPER_DRUM, WAXED_WEATHERED_COPPER_DRUM, WAXED_OXIDIZED_COPPER_DRUM,
            IRON_DRUM, GOLD_DRUM, DIAMOND_DRUM, NETHERITE_DRUM, OBSIDIAN_DRUM, VOID_DRUM, CREATIVE_DRUM
        ).build());

        if (LiquidFabricConfig.enableRainCollector) {
            RAIN_COLLECTOR_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, UtilityApiMod.id("rain_collector"), BlockEntityType.Builder.create(RainCollectorBlockEntity::new, RAIN_COLLECTOR).build());
        }

        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.fluidStorage, STACKABLE_TANK_BE);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.buffer, FLUID_PIPE_BE);
        FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.fluidStorage, FLUID_DRUM_BE);
        if (LiquidFabricConfig.enableRainCollector) {
            FluidStorage.SIDED.registerForBlockEntity((be, direction) -> be.fluidStorage, RAIN_COLLECTOR_BE);
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(STACKABLE_TANK);
            entries.add(FLUID_PIPE);
            entries.add(COPPER_DRUM);
            entries.add(IRON_DRUM);
            entries.add(GOLD_DRUM);
            entries.add(DIAMOND_DRUM);
            entries.add(NETHERITE_DRUM);
            entries.add(OBSIDIAN_DRUM);
            entries.add(VOID_DRUM);
            entries.add(CREATIVE_DRUM);
            if (FLUID_FILTER != null) entries.add(FLUID_FILTER);
            if (FLUID_FILTER_BLOCK != null) entries.add(FLUID_FILTER_BLOCK);
            if (FLUID_GAUGE != null) entries.add(FLUID_GAUGE);
            if (FLUID_METER != null) entries.add(FLUID_METER);
            if (RAIN_COLLECTOR != null) entries.add(RAIN_COLLECTOR);
        });
    }

    public static Block copperDrumFor(CopperDrumBlock.Oxidation oxidation, boolean waxed) {
        return (waxed ? WAXED_COPPER_DRUMS : UNWAXED_COPPER_DRUMS).get(oxidation);
    }

    private static CopperDrumBlock registerCopperDrum(String id, CopperDrumBlock.Oxidation oxidation, boolean waxed) {
        CopperDrumBlock block = Registry.register(
            Registries.BLOCK,
            UtilityApiMod.id(id),
            new CopperDrumBlock(CopperDrumBlock.settingsFor(oxidation, waxed), FluidContainerSizes.IRON_DRUM_DROPLETS, DrumMode.NORMAL, oxidation, waxed)
        );
        Registry.register(Registries.ITEM, UtilityApiMod.id(id), new FluidDrumBlockItem(block, new Item.Settings(), FluidContainerSizes.IRON_DRUM_DROPLETS, DrumMode.NORMAL));
        (waxed ? WAXED_COPPER_DRUMS : UNWAXED_COPPER_DRUMS).put(oxidation, block);
        return block;
    }

    private static FluidDrumBlock registerDrumBlock(String id, long capacity, DrumMode mode, Block copyFrom) {
        FluidDrumBlock block = Registry.register(Registries.BLOCK, UtilityApiMod.id(id), new FluidDrumBlock(AbstractBlock.Settings.copy(copyFrom), capacity, mode));
        Registry.register(Registries.ITEM, UtilityApiMod.id(id), new FluidDrumBlockItem(block, new Item.Settings(), capacity, mode));
        return block;
    }
}
