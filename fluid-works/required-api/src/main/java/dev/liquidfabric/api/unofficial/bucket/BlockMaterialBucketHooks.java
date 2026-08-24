package dev.liquidfabric.api.unofficial.bucket;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

/**
 * Generic bucket pickup hook for non-fluid block materials registered in
 * BlockMaterialBucketRegistry. Addons provide tags and bucket items.
 */
public final class BlockMaterialBucketHooks {
    private static boolean registered;

    private BlockMaterialBucketHooks() {}

    public static void register() {
        if (registered) return;
        registered = true;

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!LiquidFabricConfig.blockMaterialBucketPickupFromVanillaBucket) {
                return ActionResult.PASS;
            }

            ItemStack held = player.getStackInHand(hand);
            if (!held.isOf(Items.BUCKET)) return ActionResult.PASS;

            var pos = hitResult.getBlockPos();
            if (!world.isInBuildLimit(pos)) return ActionResult.PASS;
            var state = world.getBlockState(pos);

            return BlockMaterialBucketRegistry.match(state).map(entry -> {
                if (!world.isClient) {
                    if (state.hasBlockEntity()) return ActionResult.PASS;
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
                    ItemStack filled = new ItemStack(entry.bucketItem());
                    held.decrement(1);
                    if (held.isEmpty()) player.setStackInHand(hand, filled);
                    else player.getInventory().offerOrDrop(filled);
                }
                return ActionResult.SUCCESS;
            }).orElse(ActionResult.PASS);
        });
    }
}
