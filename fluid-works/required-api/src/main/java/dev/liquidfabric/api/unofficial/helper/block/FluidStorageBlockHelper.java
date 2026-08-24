package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.tank.common.UtilityFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Shared helpers for tank/drum/pipe block entities that expose fluid storage.
 */
public final class FluidStorageBlockHelper {
    private FluidStorageBlockHelper() {}

    public static int comparatorOutput(UtilityFluidStorage storage) {
        if (storage == null) return 0;
        long capacity = storage.getCapacityView();
        long amount = storage.amountView();
        if (capacity <= 0 || amount <= 0) return 0;
        return Math.max(1, Math.min(15, (int) Math.floor((double) amount * 15.0D / (double) capacity)));
    }

    public static boolean isFull(UtilityFluidStorage storage) {
        return storage != null && storage.amountView() >= storage.getCapacityView();
    }

    public static boolean isEmpty(UtilityFluidStorage storage) {
        return storage == null || storage.variantView().isBlank() || storage.amountView() <= 0;
    }

    public static long freeSpace(UtilityFluidStorage storage) {
        if (storage == null) return 0;
        return Math.max(0, storage.getCapacityView() - storage.amountView());
    }

    public static boolean canAccept(UtilityFluidStorage storage, FluidVariant variant) {
        if (storage == null || variant == null || variant.isBlank()) return false;
        return storage.variantView().isBlank() || storage.variantView().equals(variant);
    }

    public static void readStorage(UtilityFluidStorage storage, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (storage != null && nbt != null) storage.readNbt(nbt, lookup);
    }

    public static void writeStorage(UtilityFluidStorage storage, NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        if (storage != null && nbt != null) storage.writeNbt(nbt, lookup);
    }

    public static void appendStorageTooltip(UtilityFluidStorage storage, List<Text> tooltip) {
        if (storage == null || isEmpty(storage)) {
            tooltip.add(Text.translatable("tooltip.utilityapi.fluid_empty").formatted(Formatting.GRAY));
            return;
        }
        tooltip.add(Text.literal(storage.variantView().getFluid().toString()).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.utilityapi.amount_mb", FluidUnits.dropletsToMb(storage.amountView())).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.utilityapi.capacity_mb", FluidUnits.dropletsToMb(storage.getCapacityView())).formatted(Formatting.DARK_GRAY));
    }

    public static String debugString(UtilityFluidStorage storage) {
        if (storage == null || isEmpty(storage)) return "empty";
        return storage.variantView() + " " + storage.amountView() + "/" + storage.getCapacityView() + " droplets";
    }
}
