package dev.liquidfabric.api.unofficial.core;

/**
 * Legacy shim retained for source compatibility inside older UtilityAPI passes.
 * New code should use LiquidFabricConfig.
 */
@Deprecated(forRemoval = false)
public final class ModConfig {
    private ModConfig() {}

    public static void load() {
        LiquidFabricConfig.load();
    }

    public static int needleGunCooldownTicks() { return LiquidFabricConfig.needleGunCooldownTicks; }
    public static float needleGunDamage() { return LiquidFabricConfig.needleGunDamage; }
}
