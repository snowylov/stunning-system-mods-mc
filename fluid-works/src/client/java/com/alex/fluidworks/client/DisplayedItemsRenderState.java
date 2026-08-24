package com.alex.fluidworks.client;

import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;

/** Mutable render-thread snapshot; gameplay block entities remain free of client classes. */
public final class DisplayedItemsRenderState extends BlockEntityRenderState {
    public final ItemRenderState[] items;
    public float lidProgress;
    public DisplayedItemsRenderState(int count) {
        items = new ItemRenderState[count];
        for (int i = 0; i < count; i++) items[i] = new ItemRenderState();
    }
}
