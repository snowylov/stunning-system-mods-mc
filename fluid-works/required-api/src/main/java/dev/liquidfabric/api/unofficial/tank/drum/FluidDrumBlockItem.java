package dev.liquidfabric.api.unofficial.tank.drum;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.helper.block.FluidDrumBlockHelper;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemTooltipHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class FluidDrumBlockItem extends BlockItem {
    private final long capacity;
    private final DrumMode mode;

    public FluidDrumBlockItem(Block block, Settings settings, long capacity, DrumMode mode) {
        super(block, settings.maxCount(1));
        this.capacity = capacity;
        this.mode = mode;
    }

    public long capacityDroplets() { return capacity; }

    public DrumMode mode() { return mode; }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        FluidItemTooltipHelper.appendModelPending(tooltip);
        FluidDrumBlockHelper.appendDrumTooltip(capacity, mode, false, tooltip, FluidItemTooltipHelper.shouldShowExpanded(type));
    }
}
