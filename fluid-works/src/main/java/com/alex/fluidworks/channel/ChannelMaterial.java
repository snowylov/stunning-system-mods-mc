package com.alex.fluidworks.channel;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public enum ChannelMaterial {
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
    WARPED("warped", Blocks.WARPED_PLANKS),
    COPPER("copper", Blocks.COPPER_BLOCK),
    IRON("iron", Blocks.IRON_BLOCK),
    GOLD("gold", Blocks.GOLD_BLOCK),
    STONE("stone", Blocks.STONE),
    COBBLESTONE("cobblestone", Blocks.COBBLESTONE);

    private final String id;
    private final Block materialBlock;

    ChannelMaterial(String id, Block materialBlock) {
        this.id = id;
        this.materialBlock = materialBlock;
    }

    public String id() {
        return id;
    }

    public String blockId() {
        return id + "_fluid_channel";
    }

    public Block materialBlock() {
        return materialBlock;
    }
}
