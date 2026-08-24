package com.alex.fluidworks.block;

import com.alex.fluidworks.fluid.FluidInteraction;
import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class TankBlock extends BlockWithEntity {
    private static final VoxelShape FULL_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 16);
    private static final VoxelShape RESERVOIR_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 16, 15);

    private final MapCodec<TankBlock> codec;
    private final long capacityDroplets;
    private final boolean retainsFluid;

    public TankBlock(Settings settings, long capacityDroplets, boolean retainsFluid) {
        super(settings);
        this.codec = MapCodec.unit(this);
        this.capacityDroplets = capacityDroplets;
        this.retainsFluid = retainsFluid;
    }

    public long capacityDroplets() {
        return capacityDroplets;
    }

    public boolean retainsFluid() {
        return retainsFluid;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TankBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return retainsFluid ? RESERVOIR_SHAPE : FULL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return retainsFluid ? RESERVOIR_SHAPE : FULL_SHAPE;
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos,
                                      net.minecraft.util.math.Direction direction) {
        return world.getBlockEntity(pos) instanceof TankBlockEntity tank
            ? tank.storage().comparatorOutput() : 0;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof TankBlockEntity tank)) return ActionResult.PASS;
        if (world.isClient()) {
            return FluidItemComponentHelper.capacity(stack) > 0 ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        return FluidInteraction.transfer((ServerWorld) world, pos, stack, tank.storage());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            String fluidName = tank.storage().variantView().isBlank()
                ? "Empty"
                : Registries.FLUID.getId(tank.storage().variantView().getFluid()).toString();
            player.sendMessage(Text.literal(fluidName + ": "
                + FluidUnits.dropletsToMb(tank.storage().amountView()) + " / "
                + FluidUnits.dropletsToMb(capacityDroplets) + " mB"), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (retainsFluid && !moved && world.getBlockEntity(pos) instanceof TankBlockEntity tank) {
            ItemStack drop = new ItemStack(asItem());
            if (!tank.storage().variantView().isBlank() && tank.storage().amountView() > 0) {
                FluidItemComponentHelper.set(drop, new StoredFluidComponent(
                    Registries.FLUID.getId(tank.storage().variantView().getFluid()),
                    tank.storage().amountView(),
                    SourceFluidAttributes.EMPTY
                ));
            }
            Block.dropStack(world, pos, drop);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
