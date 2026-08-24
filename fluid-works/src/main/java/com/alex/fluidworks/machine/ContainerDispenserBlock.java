package com.alex.fluidworks.machine;

import com.alex.fluidworks.fluid.FluidInteraction;
import com.alex.fluidworks.item.CustomFluidBottleItem;
import com.alex.fluidworks.item.MaterialFluidBucketItem;
import com.mojang.serialization.MapCodec;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class ContainerDispenserBlock extends BlockWithEntity {
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    private final DispenserKind kind;
    private final MapCodec<ContainerDispenserBlock> codec = MapCodec.unit(this);

    public ContainerDispenserBlock(DispenserKind kind, Settings settings) {
        super(settings);
        this.kind = kind;
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    public DispenserKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return codec;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ContainerDispenserBlockEntity(pos, state);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof ContainerDispenserBlockEntity dispenser
            ? dispenser.storage().comparatorOutput() : 0;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.getBlockEntity(pos) instanceof ContainerDispenserBlockEntity dispenser)) {
            return ActionResult.PASS;
        }
        if (!accepts(stack)) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.SUCCESS;

        if (FluidItemComponentHelper.capacity(stack) > 0 && !FluidItemComponentHelper.isEmpty(stack)) {
            return FluidInteraction.transfer((ServerWorld) world, pos, stack, dispenser.storage());
        }
        return dispenser.fillHeld(player, hand);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!world.isClient() && world.getBlockEntity(pos) instanceof ContainerDispenserBlockEntity dispenser) {
            player.sendMessage(Text.translatable("message.fluidworks.dispenser.status",
                dispenser.fluidName(), dispenser.amountMb(), dispenser.capacityMb()), true);
        }
        return ActionResult.SUCCESS;
    }

    private boolean accepts(ItemStack stack) {
        if (kind == DispenserKind.BUCKET) {
            return stack.isOf(Items.BUCKET) || stack.getItem() instanceof MaterialFluidBucketItem;
        }
        return stack.isOf(Items.GLASS_BOTTLE) || stack.getItem() instanceof CustomFluidBottleItem;
    }
}
