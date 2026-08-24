package dev.liquidfabric.api.unofficial.tank.meter;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Low-cost redstone fluid gauge. It does not store fluid and does not claim
 * ownership of adjacent storage. It simply samples neighboring Fabric Transfer
 * storages and returns a comparator level.
 */
public class FluidGaugeBlock extends Block {
    public FluidGaugeBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return sampleLevel(world, pos);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) player.sendMessage(Text.literal("Fluid gauge: " + sampleLevel(world, pos) + "/15"), true);
        return ActionResult.SUCCESS;
    }

    public static int sampleLevel(World world, BlockPos pos) {
        int best = 0;
        for (Direction direction : Direction.values()) {
            var storage = FluidStorage.SIDED.find(world, pos.offset(direction), direction.getOpposite());
            if (storage == null) continue;
            long amount = 0;
            long capacity = 0;
            for (var view : storage) {
                if (!view.getResource().isBlank()) amount += view.getAmount();
                capacity += Math.max(0, view.getCapacity());
            }
            if (capacity > 0) best = Math.max(best, Math.min(15, (int)Math.ceil((amount * 15.0) / capacity)));
        }
        return best;
    }
}
