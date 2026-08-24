package com.alex.fluidworks.reservoir;

import net.minecraft.block.Block;

public final class ReservoirCasingBlock extends Block implements TieredReservoirPart {
    private final ReservoirTier tier;

    public ReservoirCasingBlock(ReservoirTier tier, Settings settings) {
        super(settings);
        this.tier = tier;
    }

    @Override
    public ReservoirTier tier() {
        return tier;
    }
}
