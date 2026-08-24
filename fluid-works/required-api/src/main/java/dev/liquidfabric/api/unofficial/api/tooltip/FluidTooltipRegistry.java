package dev.liquidfabric.api.unofficial.api.tooltip;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Addon-facing tooltip provider registry. Providers are only used when the
 * caller decides advanced information is visible, normally while Shift is held.
 */
public final class FluidTooltipRegistry {
    @FunctionalInterface
    public interface Provider {
        void append(ItemStack stack, @Nullable StoredFluidComponent fluid, List<Text> tooltip);
    }

    private static final Map<Identifier, Provider> BY_LIQUID = new ConcurrentHashMap<>();
    private static final List<Provider> GLOBAL = new ArrayList<>();

    private FluidTooltipRegistry() {}

    public static void registerGlobal(Provider provider) {
        GLOBAL.add(provider);
    }

    public static void register(Identifier liquidId, Provider provider) {
        BY_LIQUID.put(liquidId, provider);
    }

    public static void append(ItemStack stack, @Nullable StoredFluidComponent fluid, List<Text> tooltip) {
        for (Provider provider : GLOBAL) provider.append(stack, fluid, tooltip);
        if (fluid != null && !fluid.isEmpty()) {
            Provider provider = BY_LIQUID.get(fluid.liquidId());
            if (provider != null) provider.append(stack, fluid, tooltip);
        }
    }

    public static void bootstrapDefaults() {
        registerGlobal((stack, fluid, tooltip) -> {
            if (fluid == null || fluid.isEmpty()) {
                tooltip.add(Text.literal("Liquid: Empty"));
                return;
            }
            tooltip.add(Text.literal("Liquid: " + fluid.liquidId()));
            tooltip.add(Text.literal("Amount: " + FluidUnits.toMillibuckets(fluid.amountDroplets()) + " mB"));
            if (fluid.sourceAttributes().notFromOcean()) tooltip.add(Text.literal("Source: Not from ocean"));
            if (fluid.sourceAttributes().fromCave()) tooltip.add(Text.literal("Source: Cave"));
        });
    }
}
