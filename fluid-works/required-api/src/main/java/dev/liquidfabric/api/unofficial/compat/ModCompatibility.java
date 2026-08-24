package dev.liquidfabric.api.unofficial.compat;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central soft-compatibility helpers.
 *
 * Keep optional integrations behind this class so the rest of the API never
 * hard-loads classes from optional mods and so failures in an optional hook do
 * not crash a world unless the pack explicitly disables fail-soft behavior.
 */
public final class ModCompatibility {
    private static final Set<String> WARNED_ONCE = ConcurrentHashMap.newKeySet();

    private ModCompatibility() {}

    public static boolean isLoaded(String modId) {
        try {
            return FabricLoader.getInstance().isModLoaded(modId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isCreateLoaded() {
        return isLoaded("create");
    }

    public static boolean isEmiLoaded() {
        return isLoaded("emi");
    }

    public static boolean isReiLoaded() {
        return isLoaded("roughlyenoughitems");
    }

    public static boolean isTerraBlenderLoaded() {
        return isLoaded("terrablender");
    }

    public static boolean isStewApiLoaded() {
        return isLoaded("stew_api");
    }

    public static void runOptional(String key, Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (!LiquidFabricConfig.failSoftOnCompatibilityErrors) {
                if (throwable instanceof RuntimeException runtime) throw runtime;
                if (throwable instanceof Error error) throw error;
                throw new RuntimeException(throwable);
            }
            warnOnce(key, "Optional compatibility hook failed and was skipped: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    public static void warnOnce(String key, String message) {
        if (WARNED_ONCE.add(key)) {
            UtilityApiMod.LOGGER.warn("[Liquid Fabric API compatibility] {}", message);
        }
    }
}
