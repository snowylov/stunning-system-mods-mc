package com.alex.fluidworks;

import com.alex.fluidworks.block.RetainingTankBlockItem;
import com.alex.fluidworks.block.FluidLabelBlock;
import com.alex.fluidworks.block.FluidLabelBlockEntity;
import com.alex.fluidworks.block.FluidMonitorBlock;
import com.alex.fluidworks.block.TankBlock;
import com.alex.fluidworks.block.TankBlockEntity;
import com.alex.fluidworks.block.StackableTankBlock;
import com.alex.fluidworks.block.IronPistonBlock;
import com.alex.fluidworks.channel.ChannelMaterial;
import com.alex.fluidworks.channel.FluidChannelBlock;
import com.alex.fluidworks.channel.FluidChannelBlockEntity;
import com.alex.fluidworks.furniture.FurnitureBlock;
import com.alex.fluidworks.furniture.DisplayTableBlock;
import com.alex.fluidworks.furniture.DisplayTableBlockEntity;
import com.alex.fluidworks.furniture.FurnitureKind;
import com.alex.fluidworks.furniture.MicroStairsBlock;
import com.alex.fluidworks.furniture.WoodVariant;
import com.alex.fluidworks.crate.CrateBlock;
import com.alex.fluidworks.crate.CrateBlockEntity;
import com.alex.fluidworks.item.CustomFluidBottleItem;
import com.alex.fluidworks.item.FuelCellItem;
import com.alex.fluidworks.item.MaterialFluidBucketItem;
import com.alex.fluidworks.item.UniversalFluidPotionItem;
import com.alex.fluidworks.entity.LingeringFluidMarkerEntity;
import com.alex.fluidworks.entity.UniversalFluidPotionEntity;
import com.alex.fluidworks.device.FluidDeviceBlock;
import com.alex.fluidworks.device.FluidDeviceBlockEntity;
import com.alex.fluidworks.device.FluidDeviceKind;
import com.alex.fluidworks.device.HardwareMaterial;
import com.alex.fluidworks.machine.ContainerDispenserBlock;
import com.alex.fluidworks.machine.ContainerDispenserBlockEntity;
import com.alex.fluidworks.machine.DispenserKind;
import com.alex.fluidworks.pipe.FluidPipeBlock;
import com.alex.fluidworks.pipe.FluidPipeBlockEntity;
import com.alex.fluidworks.pipe.PipeKind;
import com.alex.fluidworks.pump.FluidPumpBlock;
import com.alex.fluidworks.pump.FluidPumpBlockEntity;
import com.alex.fluidworks.fluid.FluidMixingRecipeManager;
import com.alex.fluidworks.fluid.SpecialFluidEffects;
import com.alex.fluidworks.reservoir.ReservoirCasingBlock;
import com.alex.fluidworks.reservoir.ReservoirControllerBlock;
import com.alex.fluidworks.reservoir.ReservoirControllerBlockEntity;
import com.alex.fluidworks.reservoir.ReservoirTier;
import com.alex.fluidworks.reservoir.ReservoirValveBlock;
import com.alex.fluidworks.reservoir.ReservoirValveBlockEntity;
import com.alex.fluidworks.reservoir.ReservoirWindowBlock;
import com.alex.fluidworks.thermal.ThermalContent;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerDefinition;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerDefinition;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.UtilityApiRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FluidWorks implements ModInitializer {
    public static final String MOD_ID = "fluidworks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static TankBlock TANK;
    public static TankBlock RESERVOIR_TANK;
    public static StackableTankBlock STACKABLE_TANK;
    public static IronPistonBlock IRON_PISTON_12;
    public static IronPistonBlock IRON_PISTON_14;
    public static Block DOUBLE_SMELTED_GLASS;
    public static ReservoirWindowBlock RESERVOIR_WINDOW;
    public static FluidMonitorBlock OBSERVATION_WINDOW;
    public static FluidLabelBlock FLUID_LABEL;
    public static ContainerDispenserBlock BUCKET_DISPENSER;
    public static ContainerDispenserBlock BOTTLE_DISPENSER;
    public static FluidPipeBlock FLUID_PIPE;
    public static FluidPipeBlock REDSTONE_FLUID_VALVE;
    public static FluidPipeBlock EXTRACTION_FLUID_PIPE;
    public static FluidPipeBlock HIGH_PRESSURE_PIPE;
    public static FluidPipeBlock METER_PIPE;
    public static FluidPipeBlock OVERFLOW_VALVE;
    public static FluidPipeBlock PULSE_VALVE;
    public static FluidPipeBlock PRIORITY_JUNCTION;
    public static FluidPipeBlock FLUID_DIODE;
    public static FluidPipeBlock FILTER_PIPE;
    public static FluidPipeBlock MIXING_JUNCTION;
    public static FluidPumpBlock FLUID_PUMP;
    public static FuelCellItem FUEL_CELL;
    public static CustomFluidBottleItem CUSTOM_GLASS_BOTTLE;
    public static MaterialFluidBucketItem COPPER_BUCKET;
    public static MaterialFluidBucketItem GOLD_BUCKET;
    public static MaterialFluidBucketItem DIAMOND_BUCKET;
    public static MaterialFluidBucketItem NETHERITE_BUCKET;
    public static UniversalFluidPotionItem UNIVERSAL_FLUID_SPLASH_POTION;
    public static UniversalFluidPotionItem UNIVERSAL_FLUID_LINGERING_POTION;
    public static final Map<FluidDeviceKind, FluidDeviceBlock> FLUID_DEVICES =
        new EnumMap<>(FluidDeviceKind.class);
    public static final Map<ChannelMaterial, FluidChannelBlock> FLUID_CHANNELS =
        new EnumMap<>(ChannelMaterial.class);
    public static final Map<FurnitureKind, Block> FURNITURE =
        new EnumMap<>(FurnitureKind.class);
    public static final Map<WoodVariant, Map<FurnitureKind, Block>> WOOD_FURNITURE =
        new EnumMap<>(WoodVariant.class);
    public static final Map<WoodVariant, CrateBlock> CRATES = new EnumMap<>(WoodVariant.class);
    public static final Map<WoodVariant, FluidDeviceBlock> WOOD_DRAIN_GRATES = new EnumMap<>(WoodVariant.class);
    public static final Map<WoodVariant, MicroStairsBlock> MICRO_STAIRS =
        new EnumMap<>(WoodVariant.class);
    public static final Map<HardwareMaterial, Map<FluidDeviceKind, FluidDeviceBlock>> MATERIAL_FLUID_DEVICES =
        new EnumMap<>(HardwareMaterial.class);
    public static final Map<HardwareMaterial, Map<PipeKind, FluidPipeBlock>> MATERIAL_PIPES =
        new EnumMap<>(HardwareMaterial.class);

    public static final Map<ReservoirTier, ReservoirCasingBlock> CASINGS = new EnumMap<>(ReservoirTier.class);
    public static final Map<ReservoirTier, ReservoirControllerBlock> CONTROLLERS = new EnumMap<>(ReservoirTier.class);
    public static final Map<ReservoirTier, ReservoirValveBlock> VALVES = new EnumMap<>(ReservoirTier.class);
    static final Map<Identifier, Item> CONTENT_ITEMS = new LinkedHashMap<>();

    public static BlockEntityType<TankBlockEntity> TANK_BLOCK_ENTITY;
    public static BlockEntityType<ReservoirControllerBlockEntity> RESERVOIR_CONTROLLER_BLOCK_ENTITY;
    public static BlockEntityType<ReservoirValveBlockEntity> RESERVOIR_VALVE_BLOCK_ENTITY;
    public static BlockEntityType<FluidLabelBlockEntity> FLUID_LABEL_BLOCK_ENTITY;
    public static BlockEntityType<ContainerDispenserBlockEntity> CONTAINER_DISPENSER_BLOCK_ENTITY;
    public static BlockEntityType<FluidPipeBlockEntity> FLUID_PIPE_BLOCK_ENTITY;
    public static BlockEntityType<FluidDeviceBlockEntity> FLUID_DEVICE_BLOCK_ENTITY;
    public static BlockEntityType<FluidChannelBlockEntity> FLUID_CHANNEL_BLOCK_ENTITY;
    public static BlockEntityType<DisplayTableBlockEntity> DISPLAY_TABLE_BLOCK_ENTITY;
    public static BlockEntityType<CrateBlockEntity> CRATE_BLOCK_ENTITY;
    public static BlockEntityType<FluidPumpBlockEntity> FLUID_PUMP_BLOCK_ENTITY;
    public static EntityType<UniversalFluidPotionEntity> UNIVERSAL_FLUID_POTION_ENTITY;
    public static EntityType<LingeringFluidMarkerEntity> LINGERING_FLUID_MARKER_ENTITY;
    public static ItemGroup FLUID_WORKS_TAB;

    @Override
    public void onInitialize() {
        registerBaseContent();
        ExpandedContent.registerContent();
        StorageCasesContent.registerContent();
        registerEntities();
        registerReservoirContent();
        registerBlockEntities();
        ExpandedContent.registerBlockEntity();
        StorageCasesContent.registerBlockEntity();
        registerFluidHooks();
        ExpandedContent.registerFluidHook();
        FluidMixingRecipeManager.initialize();
        SpecialFluidEffects.initialize();
        ThermalContent.initialize();
        registerItemGroup();
        ExpandedContent.registerStairsItemGroup();
        LOGGER.info("Fluid Works initialized with independent Easy Containers API support");
    }

    private static void registerBaseContent() {
        TANK = registerTank("tank", FluidUnits.bucketsToDroplets(16), false,
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));
        RESERVOIR_TANK = registerTank("reservoir_tank", FluidUnits.mbToDroplets(12), true,
            AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque());
        STACKABLE_TANK = (StackableTankBlock) registerSimpleBlock("stackable_tank",
            new StackableTankBlock(settingsFor("stackable_tank",
                AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).nonOpaque()
                    .pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK))));
        IRON_PISTON_12 = (IronPistonBlock) registerSimpleBlock("iron_piston_12",
            new IronPistonBlock(12, settingsFor("iron_piston_12",
                AbstractBlock.Settings.copy(Blocks.PISTON))));
        IRON_PISTON_14 = (IronPistonBlock) registerSimpleBlock("iron_piston_14",
            new IronPistonBlock(14, settingsFor("iron_piston_14",
                AbstractBlock.Settings.copy(Blocks.PISTON))));

        for (FurnitureKind kind : FurnitureKind.values()) {
            Block furniture = registerSimpleBlock(kind.id(), kind == FurnitureKind.CHAIR
                ? new FurnitureBlock(kind, settingsFor(kind.id(), AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque()))
                : new DisplayTableBlock(kind, settingsFor(kind.id(), AbstractBlock.Settings.copy(Blocks.OAK_PLANKS).nonOpaque())));
            FURNITURE.put(kind, furniture);
        }

        for (WoodVariant wood : WoodVariant.values()) {
            Map<FurnitureKind, Block> furnitureSet = new EnumMap<>(FurnitureKind.class);
            for (FurnitureKind kind : FurnitureKind.values()) {
                String path = wood.id() + "_" + kind.id();
                Block furniture = registerSimpleBlock(path, kind == FurnitureKind.CHAIR
                    ? new FurnitureBlock(kind, settingsFor(path, AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque()))
                    : new DisplayTableBlock(kind, settingsFor(path, AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque())));
                furnitureSet.put(kind, furniture);
            }
            WOOD_FURNITURE.put(wood, furnitureSet);

            String cratePath = wood.id() + "_crate";
            CrateBlock crate = (CrateBlock) registerSimpleBlock(cratePath,
                new CrateBlock(wood.id(), settingsFor(cratePath,
                    AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque())));
            CRATES.put(wood, crate);

            String gratePath = wood.id() + "_drain_grate";
            FluidDeviceBlock grate = (FluidDeviceBlock) registerSimpleBlock(gratePath,
                new FluidDeviceBlock(FluidDeviceKind.DRAIN_GRATE, settingsFor(gratePath,
                    AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque())));
            WOOD_DRAIN_GRATES.put(wood, grate);

            String stairPath = wood.id() + "_16_step_stairs";
            MicroStairsBlock stairs = (MicroStairsBlock) registerSimpleBlock(stairPath,
                new MicroStairsBlock(wood.plankBlock().getDefaultState(), settingsFor(stairPath,
                    AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque())));
            MICRO_STAIRS.put(wood, stairs);
        }

        for (ChannelMaterial material : ChannelMaterial.values()) {
            FluidChannelBlock channel = (FluidChannelBlock) registerSimpleBlock(material.blockId(),
                new FluidChannelBlock(material, settingsFor(material.blockId(),
                    AbstractBlock.Settings.copy(material.materialBlock()).nonOpaque())));
            FLUID_CHANNELS.put(material, channel);
        }

        DOUBLE_SMELTED_GLASS = registerSimpleBlock("double_smelted_glass",
            new Block(settingsFor("double_smelted_glass", AbstractBlock.Settings.copy(Blocks.GLASS).nonOpaque())));
        RESERVOIR_WINDOW = (ReservoirWindowBlock) registerSimpleBlock("reservoir_window",
            new ReservoirWindowBlock(settingsFor("reservoir_window",
                AbstractBlock.Settings.copy(Blocks.GLASS).strength(2.0F, 6.0F).nonOpaque())));

        OBSERVATION_WINDOW = (FluidMonitorBlock) registerSimpleBlock("observation_window",
            new FluidMonitorBlock(settingsFor("observation_window",
                AbstractBlock.Settings.copy(Blocks.GLASS).strength(1.0F).nonOpaque().noCollision())));
        FLUID_LABEL = (FluidLabelBlock) registerSimpleBlock("fluid_label",
            new FluidLabelBlock(settingsFor("fluid_label",
                AbstractBlock.Settings.copy(Blocks.IRON_TRAPDOOR).strength(1.0F).nonOpaque().noCollision())));

        BUCKET_DISPENSER = (ContainerDispenserBlock) registerSimpleBlock("bucket_dispenser",
            new ContainerDispenserBlock(DispenserKind.BUCKET, settingsFor("bucket_dispenser",
                AbstractBlock.Settings.copy(Blocks.DISPENSER))));
        BOTTLE_DISPENSER = (ContainerDispenserBlock) registerSimpleBlock("bottle_dispenser",
            new ContainerDispenserBlock(DispenserKind.BOTTLE, settingsFor("bottle_dispenser",
                AbstractBlock.Settings.copy(Blocks.DISPENSER))));

        FLUID_PIPE = registerPipe("fluid_pipe", PipeKind.STANDARD, Blocks.COPPER_BLOCK);
        REDSTONE_FLUID_VALVE = registerPipe("redstone_fluid_valve", PipeKind.REDSTONE_VALVE, Blocks.REDSTONE_BLOCK);
        EXTRACTION_FLUID_PIPE = registerPipe("extraction_fluid_pipe", PipeKind.EXTRACTION, Blocks.OAK_PLANKS);
        HIGH_PRESSURE_PIPE = registerPipe("high_pressure_pipe", PipeKind.HIGH_PRESSURE, Blocks.IRON_BLOCK);
        METER_PIPE = registerPipe("meter_pipe", PipeKind.METER, Blocks.LIGHT_BLUE_STAINED_GLASS);
        OVERFLOW_VALVE = registerPipe("overflow_valve", PipeKind.OVERFLOW, Blocks.ORANGE_CONCRETE);
        PULSE_VALVE = registerPipe("pulse_valve", PipeKind.PULSE, Blocks.REDSTONE_BLOCK);
        PRIORITY_JUNCTION = registerPipe("priority_junction", PipeKind.PRIORITY, Blocks.GOLD_BLOCK);
        FLUID_DIODE = registerPipe("fluid_diode", PipeKind.DIODE, Blocks.QUARTZ_BLOCK);
        FILTER_PIPE = registerPipe("filter_pipe", PipeKind.FILTER, Blocks.LIME_CONCRETE);
        MIXING_JUNCTION = registerPipe("mixing_junction", PipeKind.MIXING, Blocks.PURPLE_CONCRETE);
        FLUID_PUMP = (FluidPumpBlock) registerSimpleBlock("fluid_pump",
            new FluidPumpBlock(settingsFor("fluid_pump",
                AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque())));

        for (FluidDeviceKind kind : FluidDeviceKind.values()) {
            FluidDeviceBlock device = (FluidDeviceBlock) registerSimpleBlock(kind.id(),
                new FluidDeviceBlock(kind, settingsFor(kind.id(),
                    AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK).nonOpaque())));
            FLUID_DEVICES.put(kind, device);
        }

        for (HardwareMaterial material : HardwareMaterial.values()) {
            Map<FluidDeviceKind, FluidDeviceBlock> devices = new EnumMap<>(FluidDeviceKind.class);
            for (FluidDeviceKind kind : FluidDeviceKind.values()) {
                String path = material.id() + "_" + kind.id();
                FluidDeviceBlock device = (FluidDeviceBlock) registerSimpleBlock(path,
                    new FluidDeviceBlock(kind, settingsFor(path,
                        AbstractBlock.Settings.copy(material.materialBlock()).nonOpaque())));
                devices.put(kind, device);
            }
            MATERIAL_FLUID_DEVICES.put(material, devices);

            Map<PipeKind, FluidPipeBlock> pipes = new EnumMap<>(PipeKind.class);
            for (PipeKind kind : PipeKind.values()) {
                String path = material.id() + "_" + kind.id();
                pipes.put(kind, registerPipe(path, kind, material.materialBlock()));
            }
            MATERIAL_PIPES.put(material, pipes);
        }

        Identifier fuelCellId = id("fuel_cell");
        RegistryKey<Item> fuelCellKey = RegistryKey.of(Registries.ITEM.getKey(), fuelCellId);
        FUEL_CELL = Registry.register(Registries.ITEM, fuelCellKey,
            new FuelCellItem(new Item.Settings().registryKey(fuelCellKey), FluidUnits.mbToDroplets(500)));
        CONTENT_ITEMS.put(fuelCellId, FUEL_CELL);
        UtilityApiRegistries.registerCustomFluidContainerItem(fuelCellId, FUEL_CELL,
            FluidContainerDefinition.custom(FluidUnits.mbToDroplets(500), false, 1, fluidId -> true));

        CUSTOM_GLASS_BOTTLE = (CustomFluidBottleItem) registerContainerItem("custom_glass_bottle",
            new CustomFluidBottleItem(itemSettings("custom_glass_bottle")), CustomFluidBottleItem.CAPACITY);
        COPPER_BUCKET = (MaterialFluidBucketItem) registerContainerItem("copper_bucket",
            new MaterialFluidBucketItem(itemSettings("copper_bucket")), FluidUnits.BUCKET_DROPLETS);
        GOLD_BUCKET = (MaterialFluidBucketItem) registerContainerItem("gold_bucket",
            new MaterialFluidBucketItem(itemSettings("gold_bucket")), FluidUnits.BUCKET_DROPLETS);
        DIAMOND_BUCKET = (MaterialFluidBucketItem) registerContainerItem("diamond_bucket",
            new MaterialFluidBucketItem(itemSettings("diamond_bucket")), FluidUnits.BUCKET_DROPLETS);
        NETHERITE_BUCKET = (MaterialFluidBucketItem) registerContainerItem("netherite_bucket",
            new MaterialFluidBucketItem(itemSettings("netherite_bucket").fireproof()), FluidUnits.BUCKET_DROPLETS);

        UNIVERSAL_FLUID_SPLASH_POTION = (UniversalFluidPotionItem) registerContainerItem(
            "universal_fluid_splash_potion",
            new UniversalFluidPotionItem(itemSettings("universal_fluid_splash_potion"), false),
            UniversalFluidPotionItem.CAPACITY);
        UNIVERSAL_FLUID_LINGERING_POTION = (UniversalFluidPotionItem) registerContainerItem(
            "universal_fluid_lingering_potion",
            new UniversalFluidPotionItem(itemSettings("universal_fluid_lingering_potion"), true),
            UniversalFluidPotionItem.CAPACITY);
    }

    private static void registerEntities() {
        RegistryKey<EntityType<?>> potionKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(),
            id("universal_fluid_potion"));
        UNIVERSAL_FLUID_POTION_ENTITY = Registry.register(Registries.ENTITY_TYPE, potionKey,
            EntityType.Builder.<UniversalFluidPotionEntity>create(
                UniversalFluidPotionEntity::new, SpawnGroup.MISC)
                .dimensions(0.25F, 0.25F)
                .maxTrackingRange(4)
                .trackingTickInterval(10)
                .dropsNothing()
                .build(potionKey));

        RegistryKey<EntityType<?>> markerKey = RegistryKey.of(Registries.ENTITY_TYPE.getKey(),
            id("lingering_fluid_marker"));
        LINGERING_FLUID_MARKER_ENTITY = Registry.register(Registries.ENTITY_TYPE, markerKey,
            EntityType.Builder.<LingeringFluidMarkerEntity>create(
                LingeringFluidMarkerEntity::new, SpawnGroup.MISC)
                .dimensions(0.01F, 0.01F)
                .maxTrackingRange(1)
                .trackingTickInterval(20)
                .disableSummon()
                .dropsNothing()
                .build(markerKey));
    }

    private static void registerReservoirContent() {
        for (ReservoirTier tier : ReservoirTier.values()) {
            String prefix = tier.id() + "_reservoir_";
            ReservoirCasingBlock casing = (ReservoirCasingBlock) registerSimpleBlock(prefix + "casing",
                new ReservoirCasingBlock(tier, settingsFor(prefix + "casing",
                    AbstractBlock.Settings.copy(tier.materialBlock()).strength(
                        tier == ReservoirTier.NETHERITE ? 50.0F : 5.0F,
                        tier == ReservoirTier.NETHERITE ? 1200.0F : 12.0F))));
            CASINGS.put(tier, casing);

            ReservoirControllerBlock controller = (ReservoirControllerBlock) registerSimpleBlock(prefix + "controller",
                new ReservoirControllerBlock(tier, settingsFor(prefix + "controller",
                    AbstractBlock.Settings.copy(tier.materialBlock()))));
            CONTROLLERS.put(tier, controller);

            ReservoirValveBlock valve = (ReservoirValveBlock) registerSimpleBlock(prefix + "valve",
                new ReservoirValveBlock(tier, settingsFor(prefix + "valve",
                    AbstractBlock.Settings.copy(tier.materialBlock()))));
            VALVES.put(tier, valve);
        }
    }

    private static void registerBlockEntities() {
        TANK_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("tank"),
            FabricBlockEntityTypeBuilder.create(TankBlockEntity::new, TANK, RESERVOIR_TANK,
                STACKABLE_TANK).build());
        RESERVOIR_CONTROLLER_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            id("reservoir_controller"), FabricBlockEntityTypeBuilder.create(
                ReservoirControllerBlockEntity::new, CONTROLLERS.values().toArray(Block[]::new)).build());
        RESERVOIR_VALVE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            id("reservoir_valve"), FabricBlockEntityTypeBuilder.create(
                ReservoirValveBlockEntity::new, VALVES.values().toArray(Block[]::new)).build());
        FLUID_LABEL_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("fluid_label"),
            FabricBlockEntityTypeBuilder.create(FluidLabelBlockEntity::new, FLUID_LABEL).build());
        CONTAINER_DISPENSER_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            id("container_dispenser"), FabricBlockEntityTypeBuilder.create(
                ContainerDispenserBlockEntity::new, BUCKET_DISPENSER, BOTTLE_DISPENSER).build());
        FLUID_PIPE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("fluid_pipe"),
            FabricBlockEntityTypeBuilder.create(FluidPipeBlockEntity::new, allPipeBlocks()).build());
        FLUID_DEVICE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("fluid_device"),
            FabricBlockEntityTypeBuilder.create(FluidDeviceBlockEntity::new, allDeviceBlocks()).build());
        FLUID_CHANNEL_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("fluid_channel"),
            FabricBlockEntityTypeBuilder.create(FluidChannelBlockEntity::new,
                FLUID_CHANNELS.values().toArray(Block[]::new)).build());
        DISPLAY_TABLE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("display_table"),
            FabricBlockEntityTypeBuilder.create(DisplayTableBlockEntity::new, allTableBlocks()).build());
        CRATE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("crate"),
            FabricBlockEntityTypeBuilder.create(CrateBlockEntity::new,
                CRATES.values().toArray(Block[]::new)).build());
        FLUID_PUMP_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("fluid_pump"),
            FabricBlockEntityTypeBuilder.create(FluidPumpBlockEntity::new, FLUID_PUMP).build());
    }

    private static void registerFluidHooks() {
        UtilityApiRegistries.registerFluidStorage(TANK_BLOCK_ENTITY,
            (tank, side) -> tank.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(RESERVOIR_CONTROLLER_BLOCK_ENTITY,
            (controller, side) -> controller.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(RESERVOIR_VALVE_BLOCK_ENTITY,
            (valve, side) -> valve.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(CONTAINER_DISPENSER_BLOCK_ENTITY,
            (dispenser, side) -> dispenser.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(FLUID_PIPE_BLOCK_ENTITY,
            (pipe, side) -> pipe.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(FLUID_DEVICE_BLOCK_ENTITY,
            (device, side) -> device.liquidFabricStorage());
        UtilityApiRegistries.registerFluidStorage(FLUID_CHANNEL_BLOCK_ENTITY,
            (channel, side) -> channel.liquidFabricStorage());

        UtilityApiRegistries.registerFluidContainerBlock(id("tank"), TANK,
            new BlockFluidContainerDefinition(FluidUnits.bucketsToDroplets(16),
                BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
            (blockEntity, side) -> ((TankBlockEntity) blockEntity).liquidFabricStorage());
        UtilityApiRegistries.registerFluidContainerBlock(id("reservoir_tank"), RESERVOIR_TANK,
            new BlockFluidContainerDefinition(FluidUnits.mbToDroplets(12),
                new BlockFluidContainerDefinition.Bounds(1 / 16F, 0.001F, 1 / 16F,
                    15 / 16F, 0.999F, 15 / 16F), fluidId -> true),
            (blockEntity, side) -> ((TankBlockEntity) blockEntity).liquidFabricStorage());
        UtilityApiRegistries.registerFluidContainerBlock(id("stackable_tank"), STACKABLE_TANK,
            new BlockFluidContainerDefinition(StackableTankBlock.CAPACITY_PER_BLOCK,
                new BlockFluidContainerDefinition.Bounds(1 / 16F, 0.001F, 1 / 16F,
                    15 / 16F, 0.999F, 15 / 16F), fluidId -> true),
            (blockEntity, side) -> ((TankBlockEntity) blockEntity).liquidFabricStorage());

        long maximumMultiblockCapacity = 9L * 9L * 9L
            * ReservoirTier.NETHERITE.dropletsPerInteriorBlock();
        for (ReservoirTier tier : ReservoirTier.values()) {
            UtilityApiRegistries.registerFluidContainerBlock(id(tier.id() + "_reservoir_controller"),
                CONTROLLERS.get(tier), new BlockFluidContainerDefinition(maximumMultiblockCapacity,
                    BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
                (blockEntity, side) -> ((ReservoirControllerBlockEntity) blockEntity).liquidFabricStorage());
            UtilityApiRegistries.registerFluidContainerBlock(id(tier.id() + "_reservoir_valve"),
                VALVES.get(tier), new BlockFluidContainerDefinition(maximumMultiblockCapacity,
                    BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
                (blockEntity, side) -> ((ReservoirValveBlockEntity) blockEntity).liquidFabricStorage());
        }

        UtilityApiRegistries.registerFluidContainerBlock(id("bucket_dispenser"), BUCKET_DISPENSER,
            new BlockFluidContainerDefinition(ContainerDispenserBlockEntity.CAPACITY,
                BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
            (blockEntity, side) -> ((ContainerDispenserBlockEntity) blockEntity).liquidFabricStorage());
        UtilityApiRegistries.registerFluidContainerBlock(id("bottle_dispenser"), BOTTLE_DISPENSER,
            new BlockFluidContainerDefinition(ContainerDispenserBlockEntity.CAPACITY,
                BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
            (blockEntity, side) -> ((ContainerDispenserBlockEntity) blockEntity).liquidFabricStorage());
        registerPipeFluidHook("fluid_pipe", FLUID_PIPE);
        registerPipeFluidHook("redstone_fluid_valve", REDSTONE_FLUID_VALVE);
        registerPipeFluidHook("extraction_fluid_pipe", EXTRACTION_FLUID_PIPE);
        registerPipeFluidHook("high_pressure_pipe", HIGH_PRESSURE_PIPE);
        registerPipeFluidHook("meter_pipe", METER_PIPE);
        registerPipeFluidHook("overflow_valve", OVERFLOW_VALVE);
        registerPipeFluidHook("pulse_valve", PULSE_VALVE);
        registerPipeFluidHook("priority_junction", PRIORITY_JUNCTION);
        registerPipeFluidHook("fluid_diode", FLUID_DIODE);
        registerPipeFluidHook("filter_pipe", FILTER_PIPE);
        registerPipeFluidHook("mixing_junction", MIXING_JUNCTION);
        for (Map.Entry<FluidDeviceKind, FluidDeviceBlock> entry : FLUID_DEVICES.entrySet()) {
            FluidDeviceKind kind = entry.getKey();
            FluidDeviceBlock block = entry.getValue();
            UtilityApiRegistries.registerFluidContainerBlock(id(kind.id()), block,
                new BlockFluidContainerDefinition(kind.capacity(),
                    BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
                (blockEntity, side) -> ((FluidDeviceBlockEntity) blockEntity).liquidFabricStorage());
        }
        for (Map.Entry<WoodVariant, FluidDeviceBlock> entry : WOOD_DRAIN_GRATES.entrySet()) {
            String path = entry.getKey().id() + "_drain_grate";
            UtilityApiRegistries.registerFluidContainerBlock(id(path), entry.getValue(),
                new BlockFluidContainerDefinition(FluidDeviceKind.DRAIN_GRATE.capacity(),
                    BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
                (blockEntity, side) -> ((FluidDeviceBlockEntity) blockEntity).liquidFabricStorage());
        }
        for (Map.Entry<HardwareMaterial, Map<FluidDeviceKind, FluidDeviceBlock>> materialEntry
             : MATERIAL_FLUID_DEVICES.entrySet()) {
            for (Map.Entry<FluidDeviceKind, FluidDeviceBlock> entry : materialEntry.getValue().entrySet()) {
                FluidDeviceKind kind = entry.getKey();
                String path = materialEntry.getKey().id() + "_" + kind.id();
                UtilityApiRegistries.registerFluidContainerBlock(id(path), entry.getValue(),
                    new BlockFluidContainerDefinition(kind.capacity(),
                        BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
                    (blockEntity, side) -> ((FluidDeviceBlockEntity) blockEntity).liquidFabricStorage());
            }
        }
        for (Map.Entry<HardwareMaterial, Map<PipeKind, FluidPipeBlock>> materialEntry
             : MATERIAL_PIPES.entrySet()) {
            for (Map.Entry<PipeKind, FluidPipeBlock> entry : materialEntry.getValue().entrySet()) {
                registerPipeFluidHook(materialEntry.getKey().id() + "_" + entry.getKey().id(),
                    entry.getValue());
            }
        }
        for (Map.Entry<ChannelMaterial, FluidChannelBlock> entry : FLUID_CHANNELS.entrySet()) {
            FluidChannelBlock block = entry.getValue();
            UtilityApiRegistries.registerFluidContainerBlock(id(entry.getKey().blockId()), block,
                new BlockFluidContainerDefinition(FluidChannelBlock.CAPACITY,
                    new BlockFluidContainerDefinition.Bounds(5 / 16F, 5 / 16F, 0.001F,
                        11 / 16F, 8 / 16F, 0.999F), fluidId -> true),
                (blockEntity, side) -> ((FluidChannelBlockEntity) blockEntity).liquidFabricStorage());
        }
    }

    private static FluidPipeBlock registerPipe(String path, PipeKind kind, Block material) {
        return (FluidPipeBlock) registerSimpleBlock(path, new FluidPipeBlock(kind,
            settingsFor(path, AbstractBlock.Settings.copy(material).nonOpaque())));
    }

    private static void registerPipeFluidHook(String path, FluidPipeBlock block) {
        UtilityApiRegistries.registerFluidContainerBlock(id(path), block,
            new BlockFluidContainerDefinition(block.kind().bufferCapacity(),
                BlockFluidContainerDefinition.Bounds.fullBlockInset(), fluidId -> true),
            (blockEntity, side) -> ((FluidPipeBlockEntity) blockEntity).liquidFabricStorage());
    }

    private static Block[] allPipeBlocks() {
        List<Block> blocks = new ArrayList<>(List.of(
            FLUID_PIPE, REDSTONE_FLUID_VALVE, EXTRACTION_FLUID_PIPE, HIGH_PRESSURE_PIPE,
            METER_PIPE, OVERFLOW_VALVE, PULSE_VALVE, PRIORITY_JUNCTION, FLUID_DIODE,
            FILTER_PIPE, MIXING_JUNCTION));
        MATERIAL_PIPES.values().forEach(map -> blocks.addAll(map.values()));
        return blocks.toArray(Block[]::new);
    }

    private static Block[] allDeviceBlocks() {
        List<Block> blocks = new ArrayList<>(FLUID_DEVICES.values());
        MATERIAL_FLUID_DEVICES.values().forEach(map -> blocks.addAll(map.values()));
        blocks.addAll(WOOD_DRAIN_GRATES.values());
        return blocks.toArray(Block[]::new);
    }

    private static Block[] allTableBlocks() {
        List<Block> blocks = new ArrayList<>();
        FURNITURE.forEach((kind, block) -> { if (kind != FurnitureKind.CHAIR) blocks.add(block); });
        WOOD_FURNITURE.values().forEach(map -> map.forEach((kind, block) -> {
            if (kind != FurnitureKind.CHAIR) blocks.add(block);
        }));
        return blocks.toArray(Block[]::new);
    }

    private static TankBlock registerTank(String path, long capacity, boolean retainsFluid,
                                          AbstractBlock.Settings settings) {
        Identifier id = id(path);
        RegistryKey<Block> blockKey = RegistryKey.of(Registries.BLOCK.getKey(), id);
        TankBlock block = Registry.register(Registries.BLOCK, blockKey,
            new TankBlock(settings.registryKey(blockKey).pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK),
                capacity, retainsFluid));

        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item item = retainsFluid
            ? new RetainingTankBlockItem(block,
                new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey().maxCount(1))
            : new BlockItem(block,
                new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey());
        Registry.register(Registries.ITEM, itemKey, item);
        CONTENT_ITEMS.put(id, item);

        if (retainsFluid) {
            UtilityApiRegistries.registerCustomFluidContainerItem(id, item,
                FluidContainerDefinition.custom(capacity, false, 1, fluidId -> true));
        }
        return block;
    }

    public static Block registerSimpleBlock(String path, Block block) {
        Identifier id = id(path);
        Registry.register(Registries.BLOCK, id, block);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item item = Registry.register(Registries.ITEM, itemKey,
            new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
        CONTENT_ITEMS.put(id, item);
        return block;
    }

    private static Item registerContainerItem(String path, Item item, long capacity) {
        Identifier id = id(path);
        Registry.register(Registries.ITEM, id, item);
        CONTENT_ITEMS.put(id, item);
        UtilityApiRegistries.registerCustomFluidContainerItem(id, item,
            FluidContainerDefinition.custom(capacity, false, 1, fluidId -> true));
        return item;
    }

    private static Item.Settings itemSettings(String path) {
        RegistryKey<Item> key = RegistryKey.of(Registries.ITEM.getKey(), id(path));
        return new Item.Settings().registryKey(key);
    }

    public static AbstractBlock.Settings settingsFor(String path, AbstractBlock.Settings settings) {
        RegistryKey<Block> key = RegistryKey.of(Registries.BLOCK.getKey(), id(path));
        return settings.registryKey(key);
    }

    private static void registerItemGroup() {
        RegistryKey<ItemGroup> key = RegistryKey.of(Registries.ITEM_GROUP.getKey(), id("fluid_works"));
        FLUID_WORKS_TAB = Registry.register(Registries.ITEM_GROUP, key,
            FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.fluidworks.fluid_works"))
                .icon(() -> new ItemStack(TANK))
                .entries((context, entries) -> CONTENT_ITEMS.values().forEach(entries::add))
                .build());
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
