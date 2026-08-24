package com.alex.fluidworks.entity;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;

/** Saved invisible timer that removes only fluid introduced by a lingering bottle. */
public final class LingeringFluidMarkerEntity extends Entity {
    private static final int MAX_SAVED_PROTECTED_POSITIONS = 2048;

    private final Set<BlockPos> protectedFluid = new HashSet<>();
    private BlockPos origin = BlockPos.ORIGIN;
    private Identifier fluidId = Identifier.ofVanilla("empty");
    private int remainingTicks = 200;

    public LingeringFluidMarkerEntity(EntityType<? extends LingeringFluidMarkerEntity> type,
                                      World world) {
        super(type, world);
        noClip = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public void configure(BlockPos origin, Identifier fluidId, Set<BlockPos> protectedFluid,
                          int remainingTicks) {
        this.origin = origin.toImmutable();
        this.fluidId = fluidId;
        this.protectedFluid.clear();
        protectedFluid.stream().limit(MAX_SAVED_PROTECTED_POSITIONS)
            .map(BlockPos::toImmutable).forEach(this.protectedFluid::add);
        this.remainingTicks = Math.max(1, remainingTicks);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) return;
        if (--remainingTicks > 0) return;
        cleanup(serverWorld);
        discard();
    }

    private void cleanup(ServerWorld world) {
        Fluid targetFluid = Registries.FLUID.get(fluidId);
        if (targetFluid == null || targetFluid == net.minecraft.fluid.Fluids.EMPTY) return;
        int radius = FluidPotionPlacement.cleanupRadius();
        BlockPos minimum = origin.add(-radius, -2, -radius);
        BlockPos maximum = origin.add(radius, 2, radius);
        BlockPos.iterate(minimum, maximum).forEach(pos -> {
            if (protectedFluid.contains(pos)) return;
            var currentState = world.getFluidState(pos);
            Fluid current = currentState.getFluid();
            if (!currentState.isEmpty() && targetFluid.matchesType(current)) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
            }
        });
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readCustomData(ReadView view) {
        origin = new BlockPos(view.getInt("OriginX", 0), view.getInt("OriginY", 0),
            view.getInt("OriginZ", 0));
        Identifier readFluid = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        fluidId = readFluid == null ? Identifier.ofVanilla("empty") : readFluid;
        remainingTicks = Math.max(1, view.getInt("RemainingTicks", 200));
        protectedFluid.clear();
        int count = Math.min(MAX_SAVED_PROTECTED_POSITIONS,
            Math.max(0, view.getInt("ProtectedCount", 0)));
        for (int index = 0; index < count; index++) {
            protectedFluid.add(new BlockPos(
                view.getInt("ProtectedX" + index, 0),
                view.getInt("ProtectedY" + index, 0),
                view.getInt("ProtectedZ" + index, 0)));
        }
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putInt("OriginX", origin.getX());
        view.putInt("OriginY", origin.getY());
        view.putInt("OriginZ", origin.getZ());
        view.putString("FluidId", fluidId.toString());
        view.putInt("RemainingTicks", remainingTicks);
        view.putInt("ProtectedCount", protectedFluid.size());
        int index = 0;
        for (BlockPos pos : protectedFluid) {
            view.putInt("ProtectedX" + index, pos.getX());
            view.putInt("ProtectedY" + index, pos.getY());
            view.putInt("ProtectedZ" + index, pos.getZ());
            index++;
        }
    }
}
