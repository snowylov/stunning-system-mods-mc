package com.alex.fluidworks.reservoir;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public enum ReservoirTier {
    COPPER("copper", Blocks.COPPER_BLOCK, FluidUnits.bucketsToDroplets(4)),
    IRON("iron", Blocks.IRON_BLOCK, FluidUnits.bucketsToDroplets(8)),
    GOLD("gold", Blocks.GOLD_BLOCK, FluidUnits.bucketsToDroplets(16)),
    NETHERITE("netherite", Blocks.NETHERITE_BLOCK, FluidUnits.bucketsToDroplets(64));

    private final String id;
    private final Block materialBlock;
    private final long dropletsPerInteriorBlock;

    ReservoirTier(String id, Block materialBlock, long dropletsPerInteriorBlock) {
        this.id = id;
        this.materialBlock = materialBlock;
        this.dropletsPerInteriorBlock = dropletsPerInteriorBlock;
    }

    public String id() {
        return id;
    }

    public Block materialBlock() {
        return materialBlock;
    }

    public long dropletsPerInteriorBlock() {
        return dropletsPerInteriorBlock;
    }
}
