package dev.liquidfabric.api.unofficial.api.transfer;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;

import java.util.Optional;

/**
 * Small safe wrappers around Fabric Transfer API transactions.
 * These helpers never force chunk loading and never commit partial exact moves.
 */
public final class FluidTransferHelper {
    private FluidTransferHelper() {}

    public static long tryMove(Storage<FluidVariant> from, Storage<FluidVariant> to, long maxDroplets) {
        if (from == null || to == null || maxDroplets <= 0) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(from, to, v -> true, maxDroplets, tx);
            if (moved > 0) tx.commit();
            return moved;
        }
    }

    public static long tryMoveMatching(Storage<FluidVariant> from, Storage<FluidVariant> to, FluidVariant variant, long maxDroplets) {
        if (from == null || to == null || variant == null || variant.isBlank() || maxDroplets <= 0) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long moved = net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil.move(from, to, v -> v.equals(variant), maxDroplets, tx);
            if (moved > 0) tx.commit();
            return moved;
        }
    }

    public static boolean insertExact(Storage<FluidVariant> to, FluidVariant variant, long droplets) {
        if (to == null || variant == null || variant.isBlank() || droplets <= 0) return false;
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = to.insert(variant, droplets, tx);
            if (inserted == droplets) {
                tx.commit();
                return true;
            }
            return false;
        }
    }

    public static boolean extractExact(Storage<FluidVariant> from, FluidVariant variant, long droplets) {
        if (from == null || variant == null || variant.isBlank() || droplets <= 0) return false;
        try (Transaction tx = Transaction.openOuter()) {
            long extracted = from.extract(variant, droplets, tx);
            if (extracted == droplets) {
                tx.commit();
                return true;
            }
            return false;
        }
    }

    public static Optional<FluidVariant> firstStoredVariant(Storage<FluidVariant> storage) {
        if (storage == null) return Optional.empty();
        for (var view : storage) {
            FluidVariant variant = view.getResource();
            if (!variant.isBlank() && view.getAmount() > 0) return Optional.of(variant);
        }
        return Optional.empty();
    }

    public static long storedAmount(Storage<FluidVariant> storage, FluidVariant variant) {
        if (storage == null || variant == null || variant.isBlank()) return 0;
        long amount = 0;
        for (var view : storage) {
            if (variant.equals(view.getResource())) amount += view.getAmount();
        }
        return amount;
    }
}
