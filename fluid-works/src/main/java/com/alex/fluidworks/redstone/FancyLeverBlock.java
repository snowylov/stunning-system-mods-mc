package com.alex.fluidworks.redstone;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Seven visual designs share normal lever placement and redstone behavior. */
public final class FancyLeverBlock extends LeverBlock {
    private final int design;
    private final MapCodec<LeverBlock> codec = MapCodec.unit(this);

    public FancyLeverBlock(int design, Settings settings) {
        super(settings);
        if (design < 1 || design > 7) throw new IllegalArgumentException("Lever design must be 1-7");
        this.design = design;
    }

    public int design() { return design; }

    @Override
    public MapCodec<LeverBlock> getCodec() { return codec; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        BlockFace face = state.get(FACE);
        Direction facing = state.get(FACING);
        if (face == BlockFace.FLOOR) return createCuboidShape(3, 0, 3, 13, 13, 13);
        if (face == BlockFace.CEILING) return createCuboidShape(3, 3, 3, 13, 16, 13);
        return switch (facing) {
            case NORTH -> createCuboidShape(3, 3, 3, 13, 13, 16);
            case SOUTH -> createCuboidShape(3, 3, 0, 13, 13, 13);
            case WEST -> createCuboidShape(3, 3, 3, 16, 13, 13);
            case EAST -> createCuboidShape(0, 3, 3, 13, 13, 13);
            default -> throw new IllegalStateException("Lever facing must be horizontal");
        };
    }
}
