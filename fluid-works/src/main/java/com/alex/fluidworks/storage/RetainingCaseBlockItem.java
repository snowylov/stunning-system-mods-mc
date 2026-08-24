package com.alex.fluidworks.storage;

import net.minecraft.item.BlockItem;

/** Container components are copied into the placed block entity by vanilla BlockItem placement. */
public final class RetainingCaseBlockItem extends BlockItem {
    public RetainingCaseBlockItem(PortableCaseBlock block, Settings settings) { super(block, settings); }
    @Override public boolean canBeNested() { return false; }
}
