package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModComponents {
    public static ComponentType<StoredFluidComponent> STORED_FLUID;

    private ModComponents() {}

    public static void register() {
        STORED_FLUID = Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                UtilityApiMod.id("stored_fluid"),
                ComponentType.<StoredFluidComponent>builder().codec(StoredFluidComponent.CODEC).build()
        );
    }
}
