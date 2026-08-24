package dev.liquidfabric.api.unofficial.core.bucket;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.core.fluid.ModUtilityFluids;
import dev.liquidfabric.api.unofficial.core.source.FluidSourceAttributeUtil;
import dev.liquidfabric.api.unofficial.core.source.PlacedFluidAttributeTracker;
import dev.liquidfabric.api.unofficial.api.bucket.UniversalBucketRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server-authoritative bucket fallback behavior.
 *
 * Source flags preserved:
 * - not_from_ocean
 * - from_cave
 *
 * Compatibility model:
 * - Vanilla water source pickup is intercepted only to attach UtilityAPI source
 *   metadata to the resulting minecraft:water_bucket stack.
 * - Tagged water buckets place normal water and restore the sidecar metadata for
 *   that source position.
 * - Untagged vanilla buckets fall through when no metadata behavior is needed.
 * - Modded fluids with proper bucket items still use their own behavior.
 * - Modded source fluids without bucket items use liquid-fabric-api-unofficial-fabric-api:fluid_bucket.
 */
public final class BucketFluidInteractionHooks {
    private static boolean registered;

    private BucketFluidInteractionHooks() {}

    public static void register() {
        if (registered) return;
        registered = true;
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack held = player.getStackInHand(hand);

            if (held.isOf(Items.WATER_BUCKET)) {
                if (!LiquidFabricConfig.waterBucketsKeepSourceTags) {
                    return ActionResult.PASS;
                }

                StoredFluidComponent stored = held.getOrDefault(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
                if (!stored.isEmpty() && stored.liquidId().equals(Identifier.ofVanilla("water"))) {
                    BlockPos placePos = findBucketPlacementPos(world.getBlockState(hitResult.getBlockPos()), hitResult.getBlockPos(), hitResult.getSide());
                    if (!world.isInBuildLimit(placePos) || (!world.getBlockState(placePos).isAir() && !world.getBlockState(placePos).getFluidState().isEmpty())) {
                        return ActionResult.PASS;
                    }

                    if (!world.isClient) {
                        world.setBlockState(placePos, Blocks.WATER.getDefaultState(), 11);
                        PlacedFluidAttributeTracker.put(world, placePos, stored.sourceAttributes());
                        if (!player.getAbilities().creativeMode) {
                            held.decrement(1);
                            ItemStack empty = new ItemStack(Items.BUCKET);
                            if (held.isEmpty()) player.setStackInHand(hand, empty);
                            else player.getInventory().offerOrDrop(empty);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
                return ActionResult.PASS;
            }

            if (!held.isOf(Items.BUCKET)) return ActionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            FluidState fluidState = world.getFluidState(pos);
            if (fluidState.isEmpty() || !fluidState.isStill()) return ActionResult.PASS;

            Fluid fluid = fluidState.getFluid();
            Identifier fluidId = Registries.FLUID.getId(fluid);
            if (fluidId == null || fluid == Fluids.EMPTY) return ActionResult.PASS;

            boolean isWater = fluid == Fluids.WATER || fluidId.equals(Identifier.ofVanilla("water"));
            boolean isUtilityMilk = fluid == ModUtilityFluids.MILK || fluidId.equals(UtilityApiMod.id("milk"));
            boolean hasNormalBucket = fluid.getBucketItem() != Items.AIR && fluid.getBucketItem() != Items.BUCKET;
            boolean hasExplicitUniversalBucket = UniversalBucketRegistry.find(fluidId).isPresent();

            /*
             * Each branch below consumes a vanilla empty bucket interaction, so each
             * branch is feature-gated.  With all gates false, this callback returns
             * PASS and vanilla/other mods keep control.
             */
            if (isWater && !LiquidFabricConfig.waterBucketsKeepSourceTags) return ActionResult.PASS;
            if (isUtilityMilk && !LiquidFabricConfig.emptyBucketsPickupUtilityMilkFluid) return ActionResult.PASS;
            if (!isWater && !isUtilityMilk && !hasExplicitUniversalBucket
                    && !LiquidFabricConfig.fallbackUniversalBucketsForUnknownFluids) return ActionResult.PASS;

            /*
             * Water normally has a vanilla bucket item, but when enabled we handle
             * source pickup so UtilityAPI can keep origin tags on the ItemStack
             * component. Other fluids with their own bucket item are left alone.
             */
            if (!isWater && !isUtilityMilk && hasNormalBucket && !hasExplicitUniversalBucket) return ActionResult.PASS;

            SourceFluidAttributes attributes = FluidSourceAttributeUtil.resolve(world, pos);

            if (!world.isClient) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
                PlacedFluidAttributeTracker.remove(world, pos);
                held.decrement(1);

                ItemStack result;
                if (isWater) {
                    result = new ItemStack(Items.WATER_BUCKET);
                    result.set(ModComponents.STORED_FLUID, new StoredFluidComponent(
                            Identifier.ofVanilla("water"),
                            FluidUnits.BUCKET_DROPLETS,
                            attributes
                    ));
                } else if (isUtilityMilk) {
                    result = new ItemStack(Items.MILK_BUCKET);
                    result.set(ModComponents.STORED_FLUID, new StoredFluidComponent(
                            UtilityApiMod.id("milk"),
                            FluidUnits.BUCKET_DROPLETS,
                            attributes
                    ));
                } else {
                    result = UniversalBucketRegistry.createFilledStack(fluid, attributes);
                    if (result.isEmpty()) result = UniversalFluidBucketItem.filledWith(fluidId, attributes);
                }

                if (held.isEmpty()) {
                    player.setStackInHand(hand, result);
                } else {
                    player.getInventory().offerOrDrop(result);
                }
            }
            return ActionResult.SUCCESS;
        });
    }

    private static BlockPos findBucketPlacementPos(BlockState targetedState, BlockPos targetedPos, net.minecraft.util.math.Direction side) {
        if (targetedState.isAir() || !targetedState.getFluidState().isEmpty()) {
            return targetedPos;
        }
        return targetedPos.offset(side);
    }
}
