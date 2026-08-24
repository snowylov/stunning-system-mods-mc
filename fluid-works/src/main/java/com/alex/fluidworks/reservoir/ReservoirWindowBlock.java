package com.alex.fluidworks.reservoir;

import net.minecraft.block.TransparentBlock;

public final class ReservoirWindowBlock extends TransparentBlock {
    public ReservoirWindowBlock(Settings settings) {
        super(settings.nonOpaque());
    }
}
