package dev.liquidfabric.api.unofficial.slime;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * Bucket item for the smallest vanilla slimes.
 *
 * Compatibility rule:
 * This does not replace vanilla slime behavior and does not globally hook all mobs.
 * It only consumes this custom bucket item to spawn a size-1 slime, fish-bucket style.
 */
public class SlimeBucketItem extends Item {
    public SlimeBucketItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!(context.getWorld() instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        BlockPos spawnPos = context.getBlockPos().offset(context.getSide());
        SlimeEntity slime = EntityType.SLIME.create(serverWorld, SpawnReason.BUCKET);
        if (slime == null) {
            return ActionResult.FAIL;
        }

        slime.refreshPositionAndAngles(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, context.getPlayerYaw(), 0.0F);
        slime.setSize(1, true);
        slime.setPersistent();
        ItemStack bucket = context.getStack();

        serverWorld.spawnEntity(slime);
        serverWorld.playSound(null, spawnPos, SoundEvents.ITEM_BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);

        if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
            bucket.decrement(1);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (bucket.isEmpty()) {
                context.getPlayer().setStackInHand(context.getHand(), empty);
            } else {
                context.getPlayer().getInventory().offerOrDrop(empty);
            }
        }

        return ActionResult.SUCCESS;
    }
}
