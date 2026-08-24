package com.alex.fluidworks.furniture;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/** The twelve plank families available in Minecraft 1.21.11. */
public enum WoodVariant {
    OAK("oak", Blocks.OAK_PLANKS),
    SPRUCE("spruce", Blocks.SPRUCE_PLANKS),
    BIRCH("birch", Blocks.BIRCH_PLANKS),
    JUNGLE("jungle", Blocks.JUNGLE_PLANKS),
    ACACIA("acacia", Blocks.ACACIA_PLANKS),
    DARK_OAK("dark_oak", Blocks.DARK_OAK_PLANKS),
    MANGROVE("mangrove", Blocks.MANGROVE_PLANKS),
    CHERRY("cherry", Blocks.CHERRY_PLANKS),
    PALE_OAK("pale_oak", Blocks.PALE_OAK_PLANKS),
    BAMBOO("bamboo", Blocks.BAMBOO_PLANKS),
    CRIMSON("crimson", Blocks.CRIMSON_PLANKS),
    WARPED("warped", Blocks.WARPED_PLANKS);

    private final String id;
    private final Block plankBlock;

    WoodVariant(String id, Block plankBlock) {
        this.id = id;
        this.plankBlock = plankBlock;
    }

    public String id() {
        return id;
    }

    public Block plankBlock() {
        return plankBlock;
    }
}
