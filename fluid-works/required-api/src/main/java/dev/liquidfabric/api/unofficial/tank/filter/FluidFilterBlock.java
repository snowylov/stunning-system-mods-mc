package dev.liquidfabric.api.unofficial.tank.filter;

import net.minecraft.block.Block;

/**
 * Placeable filter marker block. Pipes and drums can query adjacent filters in
 * a later routing pass without hard dependency on one specific pipe class.
 */
public class FluidFilterBlock extends Block {
    public FluidFilterBlock(Settings settings) {
        super(settings);
    }
}
