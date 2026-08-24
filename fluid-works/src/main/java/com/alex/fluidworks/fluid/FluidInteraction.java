package com.alex.fluidworks.fluid;

import com.alex.fluidworks.item.FluidVisuals;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/** Server-authoritative transfer between API component items and block storage. */
public final class FluidInteraction {
    private FluidInteraction() {
    }

    public static ActionResult transfer(ServerWorld world, BlockPos pos, ItemStack stack,
                                        Storage<FluidVariant> storage) {
        long itemCapacity = FluidItemComponentHelper.capacity(stack);
        if (itemCapacity <= 0 || storage == null) return ActionResult.PASS;

        StoredFluidComponent heldFluid = FluidItemComponentHelper.get(stack);
        long moved = heldFluid.isEmpty()
            ? moveFromTankToItem(stack, storage, itemCapacity)
            : moveFromItemToTank(stack, storage, heldFluid);

        if (moved <= 0) return ActionResult.FAIL;
        world.playSound(null, pos,
            heldFluid.isEmpty() ? SoundEvents.ITEM_BUCKET_FILL : SoundEvents.ITEM_BUCKET_EMPTY,
            SoundCategory.BLOCKS, 0.7F, 1.0F);
        return ActionResult.SUCCESS;
    }

    private static long moveFromItemToTank(ItemStack stack, Storage<FluidVariant> storage,
                                           StoredFluidComponent heldFluid) {
        Fluid fluid = Registries.FLUID.get(heldFluid.liquidId());
        if (fluid == null || fluid == Fluids.EMPTY) return 0;

        long inserted;
        try (Transaction transaction = Transaction.openOuter()) {
            inserted = storage.insert(FluidVariant.of(fluid), heldFluid.amountDroplets(), transaction);
            if (inserted <= 0) return 0;
            transaction.commit();
        }
        FluidItemComponentHelper.removeDroplets(stack, inserted);
        FluidVisuals.sync(stack);
        return inserted;
    }

    private static long moveFromTankToItem(ItemStack stack, Storage<FluidVariant> storage,
                                           long itemCapacity) {
        FluidVariant variant = FluidTransferHelper.firstStoredVariant(storage).orElse(FluidVariant.blank());
        if (variant.isBlank()) return 0;

        long extracted;
        try (Transaction transaction = Transaction.openOuter()) {
            extracted = storage.extract(variant, itemCapacity, transaction);
            if (extracted <= 0) return 0;
            transaction.commit();
        }
        FluidItemComponentHelper.set(stack, new StoredFluidComponent(
            Registries.FLUID.getId(variant.getFluid()),
            extracted,
            SourceFluidAttributes.EMPTY
        ));
        FluidVisuals.sync(stack);
        return extracted;
    }
}
