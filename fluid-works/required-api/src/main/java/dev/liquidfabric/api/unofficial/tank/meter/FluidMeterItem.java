package dev.liquidfabric.api.unofficial.tank.meter;

import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

/**
 * Handheld fluid meter. Right-click a tank/drum/pipe/storage block to inspect
 * the first stored fluid without extracting anything.
 */
public class FluidMeterItem extends Item {
    public FluidMeterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();
        var storage = FluidStorage.SIDED.find(world, context.getBlockPos(), context.getSide());
        if (storage == null) return ActionResult.PASS;
        if (!world.isClient && context.getPlayer() != null) {
            FluidVariant variant = FluidTransferHelper.firstStoredVariant(storage).orElse(FluidVariant.blank());
            if (variant.isBlank()) {
                context.getPlayer().sendMessage(Text.literal("Fluid: Empty"), true);
            } else {
                long amount = FluidTransferHelper.storedAmount(storage, variant);
                context.getPlayer().sendMessage(Text.literal("Fluid: " + Registries.FLUID.getId(variant.getFluid()) + " / " + FluidUnits.toMillibuckets(amount) + " mB"), true);
            }
        }
        return ActionResult.SUCCESS;
    }
}
