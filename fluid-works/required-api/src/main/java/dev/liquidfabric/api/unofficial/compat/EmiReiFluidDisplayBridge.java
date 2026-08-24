package dev.liquidfabric.api.unofficial.compat;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Soft bridge placeholder for recipe-viewer fluid displays.
 *
 * No hard EMI/REI imports live here, so the base API never requires either
 * mod. Dedicated compat jars can call isEmiPresent/isReiPresent and register
 * displays against the public liquid/container registries.
 */
public final class EmiReiFluidDisplayBridge {
    private EmiReiFluidDisplayBridge() {}

    public static boolean enabled() {
        return LiquidFabricConfig.enableEmiReiFluidDisplays;
    }

    public static boolean isEmiPresent() {
        return FabricLoader.getInstance().isModLoaded("emi");
    }

    public static boolean isReiPresent() {
        return FabricLoader.getInstance().isModLoaded("roughlyenoughitems");
    }

    public static void registerIfPresent() {
        if (!enabled()) return;
        // Intentionally no hard dependency. Addon compat modules should register
        // actual displays through EMI/REI APIs when those mods are loaded.
    }
}
