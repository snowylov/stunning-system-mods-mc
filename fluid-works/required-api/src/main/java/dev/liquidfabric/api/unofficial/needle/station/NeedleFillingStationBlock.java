package dev.liquidfabric.api.unofficial.needle.station;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.needle.BaseNeedleItem;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

/**
 * Simple no-GUI needle/syringe filling station.
 *
 * Main hand: empty needle or syringe.
 * Offhand: any UtilityAPI liquid container with at least 100 mB stored.
 *
 * It transfers exactly 100 mB / 8100 droplets and preserves source attributes.
 */
public class NeedleFillingStationBlock extends Block {
    public NeedleFillingStationBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        ItemStack main = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offhand = player.getStackInHand(Hand.OFF_HAND);

        if (!(main.getItem() instanceof BaseNeedleItem needleItem)) {
            player.sendMessage(Text.translatable("message.utilityapi.needle_station.need_needle"), true);
            return ActionResult.SUCCESS;
        }

        NeedlePayload existing = main.getOrDefault(ModComponents.NEEDLE_PAYLOAD, NeedlePayload.EMPTY);
        if (!existing.isEmpty()) {
            player.sendMessage(Text.translatable("message.utilityapi.needle_station.needle_not_empty"), true);
            return ActionResult.SUCCESS;
        }

        StoredFluidComponent source = offhand.getOrDefault(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
        if (source.isEmpty() || source.amountDroplets() < FluidContainerSizes.NEEDLE_DROPLETS) {
            player.sendMessage(Text.translatable("message.utilityapi.needle_station.need_fluid"), true);
            return ActionResult.SUCCESS;
        }

        NeedlePayload payload = new NeedlePayload(
                source.liquidId(),
                FluidContainerSizes.NEEDLE_DROPLETS,
                List.of(),
                0,
                0,
                source.sourceAttributes()
        );

        main.set(ModComponents.NEEDLE_PAYLOAD, payload);

        if (!player.getAbilities().creativeMode) {
            long remaining = source.amountDroplets() - FluidContainerSizes.NEEDLE_DROPLETS;
            offhand.set(ModComponents.STORED_FLUID, remaining <= 0
                    ? StoredFluidComponent.EMPTY
                    : new StoredFluidComponent(source.liquidId(), remaining, source.sourceAttributes()));
        }

        player.sendMessage(Text.translatable("message.utilityapi.needle_station.filled"), true);
        return ActionResult.SUCCESS;
    }
}
