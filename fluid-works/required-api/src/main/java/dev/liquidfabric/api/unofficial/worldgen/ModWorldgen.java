package dev.liquidfabric.api.unofficial.worldgen;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;

/**
 * Worldgen bootstrap.
 *
 * Flat Mesa biome registration is data-driven. Actual biome distribution is
 * activated through the optional TerraBlender entrypoint when TerraBlender is
 * present. Without TerraBlender, datapacks/modpacks can still place the biome
 * through their own biome source/datapack tooling.
 */
public final class ModWorldgen {
    public static final RegistryKey<Biome> FLAT_MESA = RegistryKey.of(RegistryKeys.BIOME, UtilityApiMod.id("flat_mesa"));

    public static Feature<DefaultFeatureConfig> FLAT_MESA_PLATEAU_FEATURE;

    private ModWorldgen() {}

    public static void register() {
        FLAT_MESA_PLATEAU_FEATURE = Registry.register(
                Registries.FEATURE,
                UtilityApiMod.id("flat_mesa_plateau"),
                new FlatMesaPlateauFeature(DefaultFeatureConfig.CODEC)
        );
    }
}
