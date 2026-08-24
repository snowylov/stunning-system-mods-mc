package com.alex.fluidworks.device;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

public enum HardwareMaterial {
    IRON("iron", Blocks.IRON_BLOCK),
    NETHERITE("netherite", Blocks.NETHERITE_BLOCK);

    private final String id;
    private final Block materialBlock;

    HardwareMaterial(String id, Block materialBlock) {
        this.id = id;
        this.materialBlock = materialBlock;
    }

    public String id() {
        return id;
    }

    public Block materialBlock() {
        return materialBlock;
    }
}
