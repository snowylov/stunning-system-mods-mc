package com.alex.fluidworks.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/** A normal redstone piston with an iron-textured 12- or 14-pixel centered extension. */
public final class IronPistonBlock extends PistonBlock {
    private final Map<Direction, VoxelShape> extendedShapes;

    public IronPistonBlock(int armWidth, Settings settings) {
        super(false, settings);
        if (armWidth != 12 && armWidth != 14) {
            throw new IllegalArgumentException("Iron piston arm width must be 12 or 14 pixels");
        }
        this.extendedShapes = createExtendedShapes(armWidth);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                         ShapeContext context) {
        return state.get(EXTENDED) ? extendedShapes.get(state.get(FACING)) : VoxelShapes.fullCube();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                           ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    private static Map<Direction, VoxelShape> createExtendedShapes(int width) {
        double inset = (16 - width) / 2.0D;
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, VoxelShapes.union(
            Block.createCuboidShape(0, 0, 6, 16, 16, 16),
            Block.createCuboidShape(inset, inset, 1, 16 - inset, 16 - inset, 6),
            Block.createCuboidShape(0, 0, 0, 16, 16, 1)));
        shapes.put(Direction.SOUTH, VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 16, 10),
            Block.createCuboidShape(inset, inset, 10, 16 - inset, 16 - inset, 15),
            Block.createCuboidShape(0, 0, 15, 16, 16, 16)));
        shapes.put(Direction.WEST, VoxelShapes.union(
            Block.createCuboidShape(6, 0, 0, 16, 16, 16),
            Block.createCuboidShape(1, inset, inset, 6, 16 - inset, 16 - inset),
            Block.createCuboidShape(0, 0, 0, 1, 16, 16)));
        shapes.put(Direction.EAST, VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 10, 16, 16),
            Block.createCuboidShape(10, inset, inset, 15, 16 - inset, 16 - inset),
            Block.createCuboidShape(15, 0, 0, 16, 16, 16)));
        shapes.put(Direction.UP, VoxelShapes.union(
            Block.createCuboidShape(0, 0, 0, 16, 10, 16),
            Block.createCuboidShape(inset, 10, inset, 16 - inset, 15, 16 - inset),
            Block.createCuboidShape(0, 15, 0, 16, 16, 16)));
        shapes.put(Direction.DOWN, VoxelShapes.union(
            Block.createCuboidShape(0, 6, 0, 16, 16, 16),
            Block.createCuboidShape(inset, 1, inset, 16 - inset, 6, 16 - inset),
            Block.createCuboidShape(0, 0, 0, 16, 1, 16)));
        return Map.copyOf(shapes);
    }
}
