package com.alex.fluidworks;

import com.alex.fluidworks.furniture.WoodVariant;
import com.alex.fluidworks.storage.PortableCaseBlock;
import com.alex.fluidworks.storage.PortableCaseBlockEntity;
import com.alex.fluidworks.storage.RetainingCaseBlockItem;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Isolated registration surface for retained portable coolers and item cases. */
public final class StorageCasesContent {
    public static final Map<String, PortableCaseBlock> FOOD_COOLERS = new LinkedHashMap<>();
    public static final Map<WoodVariant, PortableCaseBlock> WOOD_ITEM_CASES = new EnumMap<>(WoodVariant.class);
    public static final Map<PortableCaseBlock, Block> MATERIALS = new LinkedHashMap<>();
    public static PortableCaseBlock IRON_ITEM_CASE;
    public static PortableCaseBlock GOLD_ITEM_CASE;
    public static BlockEntityType<PortableCaseBlockEntity> PORTABLE_CASE_BLOCK_ENTITY;

    private StorageCasesContent() { }

    public static void registerContent() {
        Map<String, Block> glass = new LinkedHashMap<>();
        glass.put("glass", Blocks.GLASS);
        glass.put("tinted_glass", Blocks.TINTED_GLASS);
        String[] colors = {"white","orange","magenta","light_blue","yellow","lime","pink","gray",
            "light_gray","cyan","purple","blue","brown","green","red","black"};
        for (String color : colors) glass.put(color + "_stained_glass",
            Registries.BLOCK.get(Identifier.of("minecraft", color + "_stained_glass")));
        glass.forEach((id, material) -> FOOD_COOLERS.put(id,
            register(id + "_portable_food_cooler", material, true, true)));

        for (WoodVariant wood : WoodVariant.values()) {
            WOOD_ITEM_CASES.put(wood, register(wood.id() + "_item_case", wood.plankBlock(), false, false));
        }
        IRON_ITEM_CASE = register("iron_item_case", Blocks.IRON_BLOCK, false, false);
        GOLD_ITEM_CASE = register("gold_item_case", Blocks.GOLD_BLOCK, false, false);
    }

    public static void registerBlockEntity() {
        List<Block> blocks = new ArrayList<>(FOOD_COOLERS.values());
        blocks.addAll(WOOD_ITEM_CASES.values()); blocks.add(IRON_ITEM_CASE); blocks.add(GOLD_ITEM_CASE);
        PORTABLE_CASE_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            FluidWorks.id("portable_case"), FabricBlockEntityTypeBuilder.create(
                PortableCaseBlockEntity::new, blocks.toArray(Block[]::new)).build());
    }

    private static PortableCaseBlock register(String path, Block material, boolean foodOnly, boolean showItems) {
        Identifier id = FluidWorks.id(path);
        RegistryKey<Block> blockKey = RegistryKey.of(Registries.BLOCK.getKey(), id);
        PortableCaseBlock block = Registry.register(Registries.BLOCK, blockKey,
            new PortableCaseBlock(foodOnly, showItems, AbstractBlock.Settings.copy(material)
                .registryKey(blockKey).nonOpaque().pistonBehavior(net.minecraft.block.piston.PistonBehavior.BLOCK)));
        RegistryKey<Item> itemKey = RegistryKey.of(Registries.ITEM.getKey(), id);
        Item item = Registry.register(Registries.ITEM, itemKey, new RetainingCaseBlockItem(block,
            new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey().maxCount(1)));
        FluidWorks.CONTENT_ITEMS.put(id, item);
        MATERIALS.put(block, material);
        return block;
    }
}
