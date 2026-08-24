package com.alex.fluidworks.furniture;

import net.minecraft.block.BlockState;

/** Backward-compatible name for the original sixteen-rise micro stair. */
public final class MicroStairsBlock extends SteppedStairsBlock {
    public MicroStairsBlock(BlockState baseBlockState, Settings settings) {
        super(baseBlockState, 16, settings);
    }
}
