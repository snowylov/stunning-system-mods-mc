package com.alex.fluidworks.machine;

import com.alex.fluidworks.ExpandedContent;
import com.alex.fluidworks.fluid.FluidInteraction;
import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class CoolingCauldronBlock extends BlockWithEntity {
    private static final VoxelShape SHAPE = VoxelShapes.union(
        createCuboidShape(0, 0, 0, 16, 4, 16),
        createCuboidShape(0, 4, 0, 3, 16, 16), createCuboidShape(13, 4, 0, 16, 16, 16),
        createCuboidShape(3, 4, 0, 13, 16, 3), createCuboidShape(3, 4, 13, 13, 16, 16));
    private final MapCodec<CoolingCauldronBlock> codec = MapCodec.unit(this);

    public CoolingCauldronBlock(Settings settings) { super(settings); }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return codec; }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CoolingCauldronBlockEntity(pos, state);
    }
    @Override protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                                    ShapeContext context) { return SHAPE; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                                      ShapeContext context) { return SHAPE; }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof CoolingCauldronBlockEntity cauldron)
            || FluidItemComponentHelper.capacity(stack) <= 0) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.SUCCESS;
        return FluidInteraction.transfer((ServerWorld) world, pos, stack, cauldron.storage());
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ExpandedContent.COOLING_CAULDRON_BLOCK_ENTITY,
            (tickerWorld, pos, tickerState, cauldron) -> CoolingCauldronBlockEntity.serverTick(
                (ServerWorld) tickerWorld, pos, tickerState, cauldron));
    }
}
