package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemTooltipHelper;
import dev.liquidfabric.api.unofficial.helper.item.NeedlePayloadItemHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class BaseNeedleItem extends Item {
    public BaseNeedleItem(Settings settings) {
        super(settings);
    }

    public long capacityDroplets() {
        return FluidContainerSizes.NEEDLE_DROPLETS;
    }

    public NeedlePayload getPayload(ItemStack stack) {
        return NeedlePayloadItemHelper.get(stack);
    }

    public ItemStack withPayload(ItemStack stack, NeedlePayload payload) {
        return NeedlePayloadItemHelper.set(stack, payload);
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return NeedlePayloadItemHelper.itemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return NeedlePayloadItemHelper.itemBarStep(stack, capacityDroplets());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        FluidItemTooltipHelper.appendNeedlePayloadTooltip(getPayload(stack), tooltip, FluidItemTooltipHelper.shouldShowExpanded(type));
    }
}
