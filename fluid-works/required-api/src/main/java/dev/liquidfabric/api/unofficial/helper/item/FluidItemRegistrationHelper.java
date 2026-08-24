package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Small registration helper for UtilityAPI fluid item families.
 *
 * It is intentionally thin around vanilla Registry.register, but it gives future
 * addon-facing item factories one place to standardize ids, settings, capacity,
 * and default fluid components.
 */
public final class FluidItemRegistrationHelper {
    private FluidItemRegistrationHelper() {}

    private static final Map<Identifier, Item> REGISTERED_FLUID_ITEMS = new LinkedHashMap<>();

    public static <T extends Item> T register(String path, T item) {
        Identifier id = UtilityApiMod.id(path);
        T registered = Registry.register(Registries.ITEM, id, item);
        REGISTERED_FLUID_ITEMS.put(id, registered);
        return registered;
    }

    public static <T extends Item> T register(Identifier id, T item) {
        T registered = Registry.register(Registries.ITEM, id, item);
        REGISTERED_FLUID_ITEMS.put(id, registered);
        return registered;
    }

    public static <T extends Item> T registerWithFactory(String path, Function<Item.Settings, T> factory, Item.Settings settings) {
        return register(path, factory.apply(settings));
    }

    public static ItemStack stackOf(Item item, StoredFluidComponent fluid) {
        ItemStack stack = new ItemStack(item);
        if (fluid != null && !fluid.isEmpty()) {
            FluidItemComponentHelper.set(stack, fluid);
        }
        return stack;
    }

    public static ItemStack bucketStack(Item item, Identifier fluidId, dev.liquidfabric.api.unofficial.core.SourceFluidAttributes attributes) {
        ItemStack stack = new ItemStack(item);
        FluidItemComponentHelper.setBucket(stack, fluidId, attributes);
        return stack;
    }

    public static Map<Identifier, Item> registeredFluidItemsView() {
        return java.util.Collections.unmodifiableMap(REGISTERED_FLUID_ITEMS);
    }

    public static boolean isRegisteredFluidItem(Item item) {
        return REGISTERED_FLUID_ITEMS.containsValue(item);
    }
}
