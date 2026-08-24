package com.alex.fluidworks.block;

import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public final class RetainingTankBlockItem extends BlockItem {
    public RetainingTankBlockItem(TankBlock block, Settings settings) {
        super(block, settings);
    }

    @Override
    public ActionResult place(ItemPlacementContext context) {
        StoredFluidComponent stored = FluidItemComponentHelper.get(context.getStack());
        ActionResult result = super.place(context);
        if (result.isAccepted() && !context.getWorld().isClient()
            && context.getWorld().getBlockEntity(context.getBlockPos()) instanceof TankBlockEntity tank
            && !stored.isEmpty()) {
            net.minecraft.fluid.Fluid fluid = net.minecraft.registry.Registries.FLUID.get(stored.liquidId());
            tank.storage().load(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid), stored.amountDroplets());
            tank.markDirty();
            context.getWorld().updateListeners(context.getBlockPos(), tank.getCachedState(), tank.getCachedState(), 3);
        }
        return result;
    }
}
