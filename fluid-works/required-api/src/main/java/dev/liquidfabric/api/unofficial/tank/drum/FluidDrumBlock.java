package dev.liquidfabric.api.unofficial.tank.drum;

import dev.liquidfabric.api.unofficial.helper.block.FluidDrumBlockHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Axis-aware fluid drum block.
 *
 * Placement rule:
 * - normal placement: upright / vertical drum, AXIS=Y
 * - sneak placement: align sideways/vertical to clicked face axis, exactly like a log-style axis block
 *
 * The block entity owns fluid state; the block state only owns orientation.
 */
public class FluidDrumBlock extends BlockWithEntity {
    public static final MapCodec<FluidDrumBlock> CODEC = createCodec(settings -> new FluidDrumBlock(settings, 81000, DrumMode.NORMAL));
    public static final net.minecraft.state.property.EnumProperty<Direction.Axis> AXIS = Properties.AXIS;

    private final long capacity;
    private final DrumMode mode;

    public FluidDrumBlock(Settings settings, long capacity, DrumMode mode) {
        super(settings);
        this.capacity = capacity;
        this.mode = mode;
        this.setDefaultState(this.stateManager.getDefaultState().with(AXIS, Direction.Axis.Y));
    }

    public long capacity() { return capacity; }
    public DrumMode mode() { return mode; }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction.Axis axis = ctx.getPlayer() != null && ctx.getPlayer().isSneaking()
            ? ctx.getSide().getAxis()
            : Direction.Axis.Y;
        return this.getDefaultState().with(AXIS, axis);
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FluidDrumBlockEntity(pos, state, capacity, mode);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return be instanceof FluidDrumBlockEntity drum ? FluidDrumBlockHelper.comparatorOutput(drum) : 0;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!player.isSneaking()) return ActionResult.PASS;
        if (!world.isClient && world.getBlockEntity(pos) instanceof FluidDrumBlockEntity drum) {
            drum.toggleLocked();
            world.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.BLOCKS, 0.6F, FluidDrumBlockHelper.lockSoundPitch(drum.locked()));
            player.sendMessage(FluidDrumBlockHelper.lockMessage(drum.locked()), true);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    protected ItemStack getPickStack(BlockState state, net.minecraft.util.hit.HitResult target, BlockView world, BlockPos pos, net.minecraft.entity.player.PlayerEntity player) {
        return new ItemStack(this.asItem());
    }
}
