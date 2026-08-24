package dev.liquidfabric.api.unofficial.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Runtime config loaded from the Minecraft instance/run directory, not from
 * the normal config folder.  This is intentional for compatibility testing:
 * packs can opt into vanilla-intercepting hooks explicitly without the mod
 * silently changing global bucket behavior on first boot.
 *
 * File created in the instance root:
 * liquid-fabric-api-unofficial-fabric-api.compat.properties
 */
public final class LiquidFabricConfig {
    public static final String COMPAT_FILE_NAME = "liquid-fabric-api-unofficial-fabric-api.compat.properties";

    public static int needleGunCooldownTicks = 0;
    public static float needleGunDamage = 1.0f;
    public static long fluidPipeTransferRateDropletsPerTick = 810L;
    public static int fluidPipeTransferIntervalTicks = 5;

    /*
     * Compatibility hooks.  All default to false because each one intercepts
     * a vanilla interaction path that another mod may also use.
     */
    public static boolean vanillaMilkBucketsPourMilkLiquid = false;
    public static boolean waterBucketsKeepSourceTags = false;
    public static boolean fallbackUniversalBucketsForUnknownFluids = false;
    public static boolean blockMaterialBucketPickupFromVanillaBucket = false;
    public static boolean entityBucketCaptureFromVanillaBucket = false;

    /*
     * Secondary milk-fluid compatibility hook.  This is separate from "milk
     * buckets pour instead of drink" so packs can allow pickup of Utility milk
     * fluid without disabling vanilla milk drinking.
     */
    public static boolean emptyBucketsPickupUtilityMilkFluid = false;

    /*
     * Addon API feature toggles. These are safe by default, but exposed so
     * modpacks can disable whole systems during compatibility triage.
     */
    public static boolean enableFluidFilters = true;
    public static boolean enableFluidGauges = true;
    public static boolean enableFluidMeters = true;
    public static boolean enableCauldronFluidSupport = true;
    public static boolean enableRainCollector = true;
    public static boolean enableEmiReiFluidDisplays = true;
    public static boolean enableCreateStyleCompatibilityHooks = true;
    public static boolean enablePlacedFluidAttributeCleanup = false;
    public static boolean enableFlatMesaWorldgen = true;
    public static boolean enableFlatMesaExplorerMapLoot = false;
    public static boolean failSoftOnCompatibilityErrors = true;
    public static boolean logCompatibilityDecisions = false;

    private LiquidFabricConfig() {}

    public static void load() {
        Path path = Path.of(COMPAT_FILE_NAME);
        Properties props = defaultProperties();

        if (Files.exists(path)) {
            try (var in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException ignored) {
            }
        } else {
            writeDefaults(path, props);
        }

        needleGunCooldownTicks = parseInt(props.getProperty("needle_gun_cooldown_ticks"), 0);
        needleGunDamage = parseFloat(props.getProperty("needle_gun_damage"), 1.0f);
        fluidPipeTransferRateDropletsPerTick = parseLong(props.getProperty("fluid_pipe_transfer_rate_droplets_per_tick"), 810L);
        fluidPipeTransferIntervalTicks = parseInt(props.getProperty("fluid_pipe_transfer_interval_ticks"), 5);

        vanillaMilkBucketsPourMilkLiquid = parseBool(props.getProperty("vanilla_milk_buckets_pour_milk_liquid"), false);
        waterBucketsKeepSourceTags = parseBool(props.getProperty("water_buckets_keep_source_tags"), false);
        fallbackUniversalBucketsForUnknownFluids = parseBool(props.getProperty("fallback_universal_buckets_for_unknown_fluids"), false);
        blockMaterialBucketPickupFromVanillaBucket = parseBool(props.getProperty("block_material_bucket_pickup_from_vanilla_bucket"), false);
        entityBucketCaptureFromVanillaBucket = parseBool(props.getProperty("entity_bucket_capture_from_vanilla_bucket"), false);
        emptyBucketsPickupUtilityMilkFluid = parseBool(props.getProperty("empty_buckets_pickup_utility_milk_fluid"), false);

        enableFluidFilters = parseBool(props.getProperty("enable_fluid_filters"), true);
        enableFluidGauges = parseBool(props.getProperty("enable_fluid_gauges"), true);
        enableFluidMeters = parseBool(props.getProperty("enable_fluid_meters"), true);
        enableCauldronFluidSupport = parseBool(props.getProperty("enable_cauldron_fluid_support"), true);
        enableRainCollector = parseBool(props.getProperty("enable_rain_collector"), true);
        enableEmiReiFluidDisplays = parseBool(props.getProperty("enable_emi_rei_fluid_displays"), true);
        enableCreateStyleCompatibilityHooks = parseBool(props.getProperty("enable_create_style_compatibility_hooks"), true);
        enablePlacedFluidAttributeCleanup = parseBool(props.getProperty("enable_placed_fluid_attribute_cleanup"), false);
        enableFlatMesaWorldgen = parseBool(props.getProperty("enable_flat_mesa_worldgen"), true);
        enableFlatMesaExplorerMapLoot = parseBool(props.getProperty("enable_flat_mesa_explorer_map_loot"), false);
        failSoftOnCompatibilityErrors = parseBool(props.getProperty("fail_soft_on_compatibility_errors"), true);
        logCompatibilityDecisions = parseBool(props.getProperty("log_compatibility_decisions"), false);

        /*
         * If an old or hand-edited config is missing new keys, write the full
         * key set back out without changing the user's existing values.
         */
        ensureAllKeysPresent(path, props);
    }

    private static Properties defaultProperties() {
        Properties props = new Properties();

        props.setProperty("needle_gun_cooldown_ticks", "0");
        props.setProperty("needle_gun_damage", "1.0");
        props.setProperty("fluid_pipe_transfer_rate_droplets_per_tick", "810");
        props.setProperty("fluid_pipe_transfer_interval_ticks", "5");

        props.setProperty("vanilla_milk_buckets_pour_milk_liquid", "false");
        props.setProperty("water_buckets_keep_source_tags", "false");
        props.setProperty("fallback_universal_buckets_for_unknown_fluids", "false");
        props.setProperty("block_material_bucket_pickup_from_vanilla_bucket", "false");
        props.setProperty("entity_bucket_capture_from_vanilla_bucket", "false");
        props.setProperty("empty_buckets_pickup_utility_milk_fluid", "false");

        props.setProperty("enable_fluid_filters", "true");
        props.setProperty("enable_fluid_gauges", "true");
        props.setProperty("enable_fluid_meters", "true");
        props.setProperty("enable_cauldron_fluid_support", "true");
        props.setProperty("enable_rain_collector", "true");
        props.setProperty("enable_emi_rei_fluid_displays", "true");
        props.setProperty("enable_create_style_compatibility_hooks", "true");
        props.setProperty("enable_placed_fluid_attribute_cleanup", "false");
        props.setProperty("enable_flat_mesa_worldgen", "true");
        props.setProperty("enable_flat_mesa_explorer_map_loot", "false");
        props.setProperty("fail_soft_on_compatibility_errors", "true");
        props.setProperty("log_compatibility_decisions", "false");

        return props;
    }

    private static void writeDefaults(Path path, Properties props) {
        try (var out = Files.newOutputStream(path)) {
            props.store(out, """
                    Liquid Fabric API compatibility config.
                    This file lives in the Minecraft instance/run directory.
                    Vanilla-intercepting hooks are false by default for modpack safety.
                    Set a hook to true only when you want Liquid Fabric API to consume that vanilla interaction.
                    """);
        } catch (IOException ignored) {
        }
    }

    private static void ensureAllKeysPresent(Path path, Properties loaded) {
        Properties defaults = defaultProperties();
        boolean changed = false;
        for (String key : defaults.stringPropertyNames()) {
            if (!loaded.containsKey(key)) {
                loaded.setProperty(key, defaults.getProperty(key));
                changed = true;
            }
        }
        if (changed) {
            writeDefaults(path, loaded);
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String s, long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static float parseFloat(String s, float fallback) {
        try {
            return Float.parseFloat(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean parseBool(String s, boolean fallback) {
        if (s == null) return fallback;
        if ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s) || "1".equals(s)) return true;
        if ("false".equalsIgnoreCase(s) || "no".equalsIgnoreCase(s) || "0".equals(s)) return false;
        return fallback;
    }
}
