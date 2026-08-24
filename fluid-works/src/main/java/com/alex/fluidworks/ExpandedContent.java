package com.alex.fluidworks;

import com.alex.fluidworks.fluid.MetalFluidFamily;
import com.alex.fluidworks.fluid.MixingMetalFluidBlock;
import com.alex.fluidworks.fluid.MoltenMetalFluid;
import com.alex.fluidworks.fluid.SpecialFluid;
import com.alex.fluidworks.fluid.SpecialFluidBlock;
import com.alex.fluidworks.fluid.SpecialFluidFamily;
import com.alex.fluidworks.furniture.SteppedStairsBlock;
import com.alex.fluidworks.furniture.WoodVariant;
import com.alex.fluidworks.machine.CoolingCauldronBlock;
import com.alex.fluidworks.machine.CoolingCauldronBlockEntity;
import com.alex.fluidworks.redstone.FancyLeverBlock;
import com.alex.fluidworks.redstone.SquareButtonBlock;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerDefinition;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.UtilityApiRegistries;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registration for expanded architectural, redstone, and molten-metal content. */
public final class ExpandedContent {
    public static final int[] STEP_COUNTS = {4, 5, 6, 16};
    public static final Map<String, Map<Integer, SteppedStairsBlock>> STAIRS = new LinkedHashMap<>();
    public static final Map<String, Map<Integer, SquareButtonBlock>> BUTTONS = new LinkedHashMap<>();
    public static final Map<String, FancyLeverBlock> LEVERS = new LinkedHashMap<>();
    private static final List<Item> STAIR_ITEMS = new ArrayList<>();

    public static final MetalFluidFamily GOLD_FLUID = new MetalFluidFamily("gold", 0xFFFFC629);
    public static final MetalFluidFamily IRON_FLUID = new MetalFluidFamily("iron", 0xFFD8D8D8);
    public static final MetalFluidFamily DIAMOND_FLUID = new MetalFluidFamily("diamond", 0xFF4DE6DB);
    public static final MetalFluidFamily COPPER_FLUID = new MetalFluidFamily("copper", 0xFFD87349);
    public static final MetalFluidFamily ROSE_GOLD_FLUID = new MetalFluidFamily("rose_gold", 0xFFF09A8B);
    public static final List<MetalFluidFamily> METAL_FLUIDS = List.of(
        GOLD_FLUID, IRON_FLUID, DIAMOND_FLUID, COPPER_FLUID, ROSE_GOLD_FLUID);
    public static final SpecialFluidFamily LIQUID_ENDER =
        new SpecialFluidFamily("ender", 0xFF5522A8, true, false);
    public static final SpecialFluidFamily LIQUID_NITROGEN =
        new SpecialFluidFamily("nitrogen", 0xFFBDEEFF, false, true);
    public static final SpecialFluidFamily CRYOGEN =
        new SpecialFluidFamily("cryogen", 0xFF54BFE8, true, true);
    public static final List<SpecialFluidFamily> SPECIAL_FLUIDS = List.of(
        LIQUID_ENDER, LIQUID_NITROGEN, CRYOGEN);

    public static Block ROSE_GOLD_BLOCK;
    public static CoolingCauldronBlock COOLING_CAULDRON;
    public static BlockEntityType<CoolingCauldronBlockEntity> COOLING_CAULDRON_BLOCK_ENTITY;
    public static ItemGroup STAIRS_TAB;

    private ExpandedContent() { }

    public static void registerContent() {
        for (WoodVariant wood : WoodVariant.values()) registerWoodStairMaterial(wood);
        for (MaterialSpec material : concreteMaterials()) registerStairMaterial(material.id, material.block);
        for (MaterialSpec material : terracottaMaterials()) registerStairMaterial(material.id, material.block);

        List<ButtonSpec> buttons = new ArrayList<>();
        for (WoodVariant wood : WoodVariant.values())
            buttons.add(new ButtonSpec(wood.id(), wood.plankBlock(), woodButtonType(wood), 30));
        buttons.add(new ButtonSpec("stone", Blocks.STONE, BlockSetType.STONE, 20));
        buttons.add(new ButtonSpec("polished_blackstone", Blocks.POLISHED_BLACKSTONE,
            BlockSetType.POLISHED_BLACKSTONE, 20));
        for (MaterialSpec material : concreteMaterials())
            buttons.add(new ButtonSpec(material.id, material.block, BlockSetType.STONE, 20));
        for (MaterialSpec material : terracottaMaterials())
            buttons.add(new ButtonSpec(material.id, material.block, BlockSetType.STONE, 20));
        buttons.forEach(ExpandedContent::registerButtons);

        registerLever("copper_gear_lever", 1, Blocks.COPPER_BLOCK);
        registerLever("copper_valve_lever", 2, Blocks.COPPER_BLOCK);
        registerLever("iron_safety_lever", 3, Blocks.IRON_BLOCK);
        registerLever("iron_breaker_lever", 4, Blocks.IRON_BLOCK);
        registerLever("gold_precision_lever", 5, Blocks.GOLD_BLOCK);
        registerLever("gold_toggle_lever", 6, Blocks.GOLD_BLOCK);
        registerLever("trimetal_control_lever", 7, Blocks.IRON_BLOCK);

        ROSE_GOLD_BLOCK = FluidWorks.registerSimpleBlock("rose_gold_block", new Block(
            FluidWorks.settingsFor("rose_gold_block", AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK))));
        COOLING_CAULDRON = (CoolingCauldronBlock) FluidWorks.registerSimpleBlock("cooling_cauldron",
            new CoolingCauldronBlock(FluidWorks.settingsFor("cooling_cauldron",
                AbstractBlock.Settings.copy(Blocks.CAULDRON).nonOpaque())));
        METAL_FLUIDS.forEach(ExpandedContent::registerMetalFluid);
        SPECIAL_FLUIDS.forEach(ExpandedContent::registerSpecialFluid);
    }

    private static void registerStairMaterial(String materialId, Block materialBlock) {
        Map<Integer, SteppedStairsBlock> family = STAIRS.computeIfAbsent(materialId, ignored -> new LinkedHashMap<>());
        for (int steps : STEP_COUNTS) {
            String path = materialId + "_" + steps + "_step_stairs";
            SteppedStairsBlock stair = (SteppedStairsBlock) FluidWorks.registerSimpleBlock(path,
                new SteppedStairsBlock(materialBlock.getDefaultState(), steps,
                    FluidWorks.settingsFor(path, AbstractBlock.Settings.copy(materialBlock).nonOpaque())));
            family.put(steps, stair);
            STAIR_ITEMS.add(stair.asItem());
        }
    }

    private static void registerWoodStairMaterial(WoodVariant wood) {
        Map<Integer, SteppedStairsBlock> family = STAIRS.computeIfAbsent(wood.id(), ignored -> new LinkedHashMap<>());
        for (int steps : new int[]{4, 5, 6}) {
            String path = wood.id() + "_" + steps + "_step_stairs";
            SteppedStairsBlock stair = (SteppedStairsBlock) FluidWorks.registerSimpleBlock(path,
                new SteppedStairsBlock(wood.plankBlock().getDefaultState(), steps,
                    FluidWorks.settingsFor(path, AbstractBlock.Settings.copy(wood.plankBlock()).nonOpaque())));
            family.put(steps, stair);
            STAIR_ITEMS.add(stair.asItem());
        }
        SteppedStairsBlock micro = FluidWorks.MICRO_STAIRS.get(wood);
        family.put(16, micro);
        STAIR_ITEMS.add(micro.asItem());
    }

    private static void registerButtons(ButtonSpec spec) {
        Map<Integer, SquareButtonBlock> sizes = new LinkedHashMap<>();
        for (int size : new int[]{12, 8}) {
            String path = spec.id + "_" + size + "x" + size + "_button";
            SquareButtonBlock button = (SquareButtonBlock) FluidWorks.registerSimpleBlock(path,
                new SquareButtonBlock(spec.type, spec.pressTicks, size,
                    FluidWorks.settingsFor(path, AbstractBlock.Settings.copy(spec.block).noCollision())));
            sizes.put(size, button);
        }
        BUTTONS.put(spec.id, sizes);
    }

    private static void registerLever(String path, int design, Block material) {
        LEVERS.put(path, (FancyLeverBlock) FluidWorks.registerSimpleBlock(path,
            new FancyLeverBlock(design, FluidWorks.settingsFor(path,
                AbstractBlock.Settings.copy(material).noCollision()))));
    }

    private static void registerMetalFluid(MetalFluidFamily family) {
        family.still = Registry.register(Registries.FLUID, FluidWorks.id("liquid_" + family.id),
            new MoltenMetalFluid.Still(family));
        family.flowing = Registry.register(Registries.FLUID, FluidWorks.id("flowing_liquid_" + family.id),
            new MoltenMetalFluid.Flowing(family));
        String blockPath = "liquid_" + family.id + "_block";
        RegistryKey<Block> blockKey = RegistryKey.of(Registries.BLOCK.getKey(), FluidWorks.id(blockPath));
        family.block = Registry.register(Registries.BLOCK, blockKey,
            new MixingMetalFluidBlock(family.still, family,
                AbstractBlock.Settings.copy(Blocks.LAVA).registryKey(blockKey).dropsNothing()));

        String bucketPath = "liquid_" + family.id + "_bucket";
        Identifier bucketId = FluidWorks.id(bucketPath);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), bucketId);
        family.bucket = Registry.register(Registries.ITEM, itemKey, new BucketItem(family.still,
            new Item.Settings().registryKey(itemKey).recipeRemainder(Items.BUCKET).maxCount(1)));
        FluidWorks.CONTENT_ITEMS.put(bucketId, family.bucket);
    }

    private static void registerSpecialFluid(SpecialFluidFamily family) {
        family.still = Registry.register(Registries.FLUID, FluidWorks.id("liquid_" + family.id),
            new SpecialFluid.Still(family));
        family.flowing = Registry.register(Registries.FLUID, FluidWorks.id("flowing_liquid_" + family.id),
            new SpecialFluid.Flowing(family));
        String blockPath = "liquid_" + family.id + "_block";
        RegistryKey<Block> blockKey = RegistryKey.of(Registries.BLOCK.getKey(), FluidWorks.id(blockPath));
        family.block = Registry.register(Registries.BLOCK, blockKey,
            new SpecialFluidBlock(family.still, family,
                AbstractBlock.Settings.copy(Blocks.WATER).registryKey(blockKey).dropsNothing()));

        String bucketPath = "liquid_" + family.id + "_bucket";
        Identifier bucketId = FluidWorks.id(bucketPath);
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), bucketId);
        family.bucket = Registry.register(Registries.ITEM, itemKey, new BucketItem(family.still,
            new Item.Settings().registryKey(itemKey).recipeRemainder(Items.BUCKET).maxCount(1)));
        FluidWorks.CONTENT_ITEMS.put(bucketId, family.bucket);
    }

    public static void registerBlockEntity() {
        COOLING_CAULDRON_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            FluidWorks.id("cooling_cauldron"), FabricBlockEntityTypeBuilder.create(
                CoolingCauldronBlockEntity::new, COOLING_CAULDRON).build());
    }

    public static void registerFluidHook() {
        UtilityApiRegistries.registerFluidStorage(COOLING_CAULDRON_BLOCK_ENTITY,
            (cauldron, side) -> cauldron.liquidFabricStorage());
        UtilityApiRegistries.registerFluidContainerBlock(FluidWorks.id("cooling_cauldron"), COOLING_CAULDRON,
            new BlockFluidContainerDefinition(FluidUnits.BUCKET_DROPLETS,
                new BlockFluidContainerDefinition.Bounds(3 / 16F, 4 / 16F, 3 / 16F,
                    13 / 16F, 15 / 16F, 13 / 16F),
                fluidId -> fluidId.equals(Registries.FLUID.getId(ROSE_GOLD_FLUID.still))),
            (blockEntity, side) -> ((CoolingCauldronBlockEntity) blockEntity).liquidFabricStorage());
    }

    public static void registerStairsItemGroup() {
        RegistryKey<ItemGroup> key = RegistryKey.of(Registries.ITEM_GROUP.getKey(), FluidWorks.id("custom_stairs"));
        STAIRS_TAB = Registry.register(Registries.ITEM_GROUP, key, FabricItemGroup.builder()
            .displayName(Text.translatable("itemGroup.fluidworks.custom_stairs"))
            .icon(() -> new ItemStack(STAIRS.get("oak").get(4)))
            .entries((context, entries) -> STAIR_ITEMS.forEach(entries::add)).build());
    }

    private static BlockSetType woodButtonType(WoodVariant wood) {
        return switch (wood) {
            case OAK -> BlockSetType.OAK; case SPRUCE -> BlockSetType.SPRUCE;
            case BIRCH -> BlockSetType.BIRCH; case JUNGLE -> BlockSetType.JUNGLE;
            case ACACIA -> BlockSetType.ACACIA; case DARK_OAK -> BlockSetType.DARK_OAK;
            case MANGROVE -> BlockSetType.MANGROVE; case CHERRY -> BlockSetType.CHERRY;
            case PALE_OAK -> BlockSetType.PALE_OAK; case BAMBOO -> BlockSetType.BAMBOO;
            case CRIMSON -> BlockSetType.CRIMSON; case WARPED -> BlockSetType.WARPED;
        };
    }

    private static List<MaterialSpec> concreteMaterials() { return coloredMaterials("concrete"); }
    private static List<MaterialSpec> terracottaMaterials() {
        List<MaterialSpec> result = new ArrayList<>();
        result.add(new MaterialSpec("terracotta", Blocks.TERRACOTTA));
        result.addAll(coloredMaterials("terracotta"));
        return result;
    }
    private static List<MaterialSpec> coloredMaterials(String suffix) {
        String[] colors = {"white","orange","magenta","light_blue","yellow","lime","pink","gray",
            "light_gray","cyan","purple","blue","brown","green","red","black"};
        List<MaterialSpec> result = new ArrayList<>();
        for (String color : colors) {
            String id = color + "_" + suffix;
            result.add(new MaterialSpec(id, Registries.BLOCK.get(Identifier.of("minecraft", id))));
        }
        return result;
    }

    private record MaterialSpec(String id, Block block) { }
    private record ButtonSpec(String id, Block block, BlockSetType type, int pressTicks) { }
}
