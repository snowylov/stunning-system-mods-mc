package com.alex.fluidworks.entity;

import com.alex.fluidworks.FluidWorks;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Server-authoritative flowing-fluid placement shared by both throwable bottles. */
public final class FluidPotionPlacement {
    private static final int FLOW_LEVEL = 6;
    private static final int CLEANUP_RADIUS = 8;

    private FluidPotionPlacement() {
    }

    public static boolean splash(ServerWorld world, BlockPos impact, Fluid fluid,
                                 boolean lingering) {
        if (!(fluid instanceof FlowableFluid flowable)) return false;

        Set<BlockPos> protectedFluid = lingering
            ? findExistingFluid(world, impact, fluid) : Set.of();
        BlockState flowingState = flowable.getFlowing(FLOW_LEVEL, false).getBlockState();
        List<BlockPos> targets = new ArrayList<>();
        targets.add(impact.toImmutable());
        for (Direction direction : Direction.Type.HORIZONTAL) {
            targets.add(impact.offset(direction).toImmutable());
        }

        int placed = 0;
        for (BlockPos target : targets) {
            if (!world.getBlockState(target).isAir()) continue;
            if (world.setBlockState(target, flowingState, 11)) placed++;
        }
        if (placed == 0) return false;

        if (lingering) {
            LingeringFluidMarkerEntity marker = new LingeringFluidMarkerEntity(
                FluidWorks.LINGERING_FLUID_MARKER_ENTITY, world);
            marker.configure(impact, Registries.FLUID.getId(fluid), protectedFluid, 200);
            marker.setPosition(impact.getX() + 0.5D, impact.getY() + 0.5D, impact.getZ() + 0.5D);
            world.spawnEntity(marker);
        }
        return true;
    }

    private static Set<BlockPos> findExistingFluid(ServerWorld world, BlockPos origin,
                                                    Fluid fluid) {
        Set<BlockPos> protectedPositions = new HashSet<>();
        BlockPos minimum = origin.add(-CLEANUP_RADIUS, -2, -CLEANUP_RADIUS);
        BlockPos maximum = origin.add(CLEANUP_RADIUS, 2, CLEANUP_RADIUS);
        BlockPos.iterate(minimum, maximum).forEach(pos -> {
            var existingState = world.getFluidState(pos);
            Fluid existing = existingState.getFluid();
            if (!existingState.isEmpty() && fluid.matchesType(existing)) {
                protectedPositions.add(pos.toImmutable());
            }
        });
        return protectedPositions;
    }

    public static int cleanupRadius() {
        return CLEANUP_RADIUS;
    }
}
