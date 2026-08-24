package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.bucket.BlockMaterialBucketRegistry;
import dev.liquidfabric.api.unofficial.bucket.BlockMaterialBucketHooks;
import dev.liquidfabric.api.unofficial.bucket.BucketEntityCaptureHooks;
import dev.liquidfabric.api.unofficial.core.bucket.BucketFluidInteractionHooks;
import dev.liquidfabric.api.unofficial.slime.SlimeBucketHooks;
import dev.liquidfabric.api.unofficial.api.color.FluidColorRegistry;
import dev.liquidfabric.api.unofficial.tank.cauldron.FluidCauldronSupport;
import dev.liquidfabric.api.unofficial.compat.EmiReiFluidDisplayBridge;
import dev.liquidfabric.api.unofficial.compat.ModCompatibility;
import dev.liquidfabric.api.unofficial.api.tooltip.FluidTooltipRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

/**
 * Common extension bootstrap for addon-facing registries and reload hooks.
 */
public final class UtilityApiCommonEvents {
    private UtilityApiCommonEvents() {}

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new LiquidDefinitionReloadListener());
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new FluidApiDefinitionReloadListener());

        FluidColorRegistry.bootstrapDefaults();
        FluidTooltipRegistry.bootstrapDefaults();

        // Registry-first: content registers data, but vanilla-intercepting callbacks
        // live in the optional vanilla-hooks path and are not attached by core/content.
        BlockMaterialBucketRegistry.bootstrapVanillaLikeDefaults();

        // These callbacks are always installed but return PASS unless an explicit
        // config or datapack rule matches. This keeps vanilla and other mods first.
        ModCompatibility.runOptional("universal_bucket_hooks", BucketFluidInteractionHooks::register);
        ModCompatibility.runOptional("block_material_bucket_hooks", BlockMaterialBucketHooks::register);
        ModCompatibility.runOptional("entity_bucket_hooks", BucketEntityCaptureHooks::register);

        if (LiquidFabricConfig.enableCauldronFluidSupport) {
            ModCompatibility.runOptional("cauldron_support", FluidCauldronSupport::register);
        }
        if (LiquidFabricConfig.enableEmiReiFluidDisplays) {
            ModCompatibility.runOptional("emi_rei_bridge", EmiReiFluidDisplayBridge::registerIfPresent);
        }
    }
}
