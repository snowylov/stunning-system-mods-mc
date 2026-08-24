package com.alex.fluidworks.redstone;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BlockFace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** A functional vanilla button with an exact centered 8x8 or 12x12 face. */
public final class SquareButtonBlock extends ButtonBlock {
    private final int width;
    private final MapCodec<ButtonBlock> codec = MapCodec.unit(this);

    public SquareButtonBlock(BlockSetType type, int pressTicks, int width, Settings settings) {
        super(type, pressTicks, settings);
        if (width != 8 && width != 12) throw new IllegalArgumentException("Button width must be 8 or 12");
        this.width = width;
    }

    public int width() { return width; }

    @Override
    public MapCodec<ButtonBlock> getCodec() { return codec; }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        double inset = (16 - width) / 2.0D;
        double thickness = state.get(POWERED) ? 1 : 2;
        BlockFace face = state.get(FACE);
        Direction facing = state.get(FACING);
        if (face == BlockFace.FLOOR)
            return createCuboidShape(inset, 0, inset, 16 - inset, thickness, 16 - inset);
        if (face == BlockFace.CEILING)
            return createCuboidShape(inset, 16 - thickness, inset, 16 - inset, 16, 16 - inset);
        return switch (facing) {
            case NORTH -> createCuboidShape(inset, inset, 16 - thickness, 16 - inset, 16 - inset, 16);
            case SOUTH -> createCuboidShape(inset, inset, 0, 16 - inset, 16 - inset, thickness);
            case WEST -> createCuboidShape(16 - thickness, inset, inset, 16, 16 - inset, 16 - inset);
            case EAST -> createCuboidShape(0, inset, inset, thickness, 16 - inset, 16 - inset);
            default -> throw new IllegalStateException("Button facing must be horizontal");
        };
    }
}
