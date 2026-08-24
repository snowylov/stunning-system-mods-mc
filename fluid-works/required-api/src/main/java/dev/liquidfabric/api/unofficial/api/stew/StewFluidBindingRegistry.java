package dev.liquidfabric.api.unofficial.api.stew;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loader-neutral bridge between discrete bowl/stew items and logical liquids.
 *
 * <p>The registry has no compile-time dependency on Stew API. Any food, farming,
 * cooking, pipe, or storage mod can register the same shape directly. Querying
 * contents never mutates a stack; callers remain responsible for transactional
 * inventory and tank changes.</p>
 */
public final class StewFluidBindingRegistry {
    public record Binding(
            Identifier id,
            Item bowl,
            Item mushroomStew,
            Item suspiciousStew,
            Identifier mushroomLiquid,
            Identifier suspiciousLiquid,
            long amountDroplets,
            Item universalOverlayBowl,
            boolean automaticSuspiciousTransfer,
            int priority
    ) {
        public Binding {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(bowl, "bowl");
            Objects.requireNonNull(mushroomStew, "mushroomStew");
            Objects.requireNonNull(suspiciousStew, "suspiciousStew");
            Objects.requireNonNull(mushroomLiquid, "mushroomLiquid");
            Objects.requireNonNull(suspiciousLiquid, "suspiciousLiquid");
            if (amountDroplets <= 0) throw new IllegalArgumentException("amountDroplets must be positive");
        }
    }

    /** A read-only view of a filled bowl's logical fluid contents. */
    public record Portion(Binding binding, StoredFluidComponent fluid, boolean suspicious) {
        public boolean safeForAutomaticTransfer() {
            return !suspicious || binding.automaticSuspiciousTransfer();
        }
    }

    private static final CopyOnWriteArrayList<Binding> BINDINGS = new CopyOnWriteArrayList<>();

    private StewFluidBindingRegistry() {}

    public static void register(Binding binding) {
        Objects.requireNonNull(binding, "binding");
        unregister(binding.id());
        BINDINGS.add(binding);
        BINDINGS.sort(Comparator.comparingInt(Binding::priority).reversed()
                .thenComparing(value -> value.id().toString()));
    }

    public static void register(Identifier id, Item bowl, Item mushroomStew, Item suspiciousStew,
                                Identifier mushroomLiquid, Identifier suspiciousLiquid,
                                long amountDroplets, Item universalOverlayBowl,
                                boolean automaticSuspiciousTransfer, int priority) {
        register(new Binding(id, bowl, mushroomStew, suspiciousStew, mushroomLiquid, suspiciousLiquid,
                amountDroplets, universalOverlayBowl, automaticSuspiciousTransfer, priority));
    }

    public static boolean unregister(Identifier id) {
        return BINDINGS.removeIf(binding -> binding.id().equals(id));
    }

    public static List<Binding> values() {
        return List.copyOf(BINDINGS);
    }

    public static Optional<Binding> findForBowl(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        return BINDINGS.stream().filter(binding -> binding.bowl() == stack.getItem()).findFirst();
    }

    public static Optional<Portion> findContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        for (Binding binding : BINDINGS) {
            if (binding.mushroomStew() == stack.getItem()) {
                return Optional.of(new Portion(binding,
                        new StoredFluidComponent(binding.mushroomLiquid(), binding.amountDroplets(), SourceFluidAttributes.EMPTY),
                        false));
            }
            if (binding.suspiciousStew() == stack.getItem()) {
                return Optional.of(new Portion(binding,
                        new StoredFluidComponent(binding.suspiciousLiquid(), binding.amountDroplets(), SourceFluidAttributes.EMPTY),
                        true));
            }
        }
        return Optional.empty();
    }

    /** Returns the empty bowl for one filled item without changing the input. */
    public static Optional<ItemStack> emptyContainer(ItemStack filledStack) {
        return findContents(filledStack).map(portion -> new ItemStack(portion.binding().bowl()));
    }

    /**
     * Resolves one filled stew item for a bowl and logical liquid.
     * The supplied component must contain at least one complete configured portion.
     */
    public static Optional<ItemStack> fillOne(ItemStack bowlStack, StoredFluidComponent fluid,
                                              boolean allowUnsafeSuspiciousTransfer) {
        if (bowlStack == null || bowlStack.isEmpty() || fluid == null || fluid.isEmpty()) return Optional.empty();
        for (Binding binding : BINDINGS) {
            if (binding.bowl() != bowlStack.getItem() || fluid.amountDroplets() < binding.amountDroplets()) continue;
            if (binding.mushroomLiquid().equals(fluid.liquidId())) return Optional.of(new ItemStack(binding.mushroomStew()));
            if (binding.suspiciousLiquid().equals(fluid.liquidId())
                    && (allowUnsafeSuspiciousTransfer || binding.automaticSuspiciousTransfer())) {
                return Optional.of(new ItemStack(binding.suspiciousStew()));
            }
        }
        return Optional.empty();
    }

    /** Converts a discrete stew to its configured one-item overlay container. */
    public static Optional<ItemStack> toUniversalOverlayBowl(ItemStack filledStack) {
        Optional<Portion> portion = findContents(filledStack);
        if (portion.isEmpty() || portion.get().binding().universalOverlayBowl() == null) return Optional.empty();
        ItemStack result = new ItemStack(portion.get().binding().universalOverlayBowl());
        FluidItemComponentHelper.set(result, portion.get().fluid());
        return FluidItemComponentHelper.hasFluid(result) ? Optional.of(result) : Optional.empty();
    }

    /** Converts a configured overlay bowl back to the matching discrete stew item. */
    public static Optional<ItemStack> fromUniversalOverlayBowl(ItemStack overlayStack,
                                                               boolean allowUnsafeSuspiciousTransfer) {
        if (overlayStack == null || overlayStack.isEmpty()) return Optional.empty();
        StoredFluidComponent fluid = FluidItemComponentHelper.get(overlayStack);
        if (fluid.isEmpty()) return Optional.empty();
        for (Binding binding : BINDINGS) {
            if (binding.universalOverlayBowl() != overlayStack.getItem()
                    || fluid.amountDroplets() < binding.amountDroplets()) continue;
            if (binding.mushroomLiquid().equals(fluid.liquidId())) return Optional.of(new ItemStack(binding.mushroomStew()));
            if (binding.suspiciousLiquid().equals(fluid.liquidId())
                    && (allowUnsafeSuspiciousTransfer || binding.automaticSuspiciousTransfer())) {
                return Optional.of(new ItemStack(binding.suspiciousStew()));
            }
        }
        return Optional.empty();
    }
}
