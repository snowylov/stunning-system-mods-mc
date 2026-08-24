package dev.liquidfabric.api.unofficial.bucket;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

/**
 * Generic fish-bucket style entity capture.
 *
 * Addons can register custom tiny mobs or fish-like entities without mixins:
 * BucketEntityCaptureRegistry.register(EntityType.X, ModItems.X_BUCKET, predicate)
 */
public final class BucketEntityCaptureHooks {
    private static boolean registered;

    private BucketEntityCaptureHooks() {}

    public static void register() {
        if (registered) return;
        registered = true;

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!LiquidFabricConfig.entityBucketCaptureFromVanillaBucket) {
                return ActionResult.PASS;
            }

            ItemStack held = player.getStackInHand(hand);
            if (!held.isOf(Items.BUCKET) || entity.hasPassengers() || entity.hasVehicle() || !entity.isAlive() || !(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            return BucketEntityCaptureRegistry.tryCreate(serverWorld, entity)
                    .map(filled -> {
                        if (!player.getAbilities().creativeMode) {
                            held.decrement(1);
                            if (held.isEmpty()) player.setStackInHand(hand, filled);
                            else player.getInventory().offerOrDrop(filled);
                        }
                        entity.discard();
                        world.playSound(null, entity.getBlockPos(), SoundEvents.ITEM_BUCKET_FILL_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
                        return ActionResult.SUCCESS;
                    })
                    .orElse(ActionResult.PASS);
        });
    }
}
