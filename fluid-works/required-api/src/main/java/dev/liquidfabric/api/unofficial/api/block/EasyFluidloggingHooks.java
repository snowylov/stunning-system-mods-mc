package dev.liquidfabric.api.unofficial.api.block;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.bucket.UniversalFluidBucketItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Delegates the exact vanilla fill/drain operations for opt-in custom blocks. */
public final class EasyFluidloggingHooks {
    public static final BooleanProperty FLUIDLOGGED = BooleanProperty.of("fluidlogged");

    private EasyFluidloggingHooks() {}

    public static Fluid configuredFluid(BlockState state) {
        return FluidloggingRegistry.find(state.getBlock()).map(FluidloggingRegistry.Entry::fluid).orElse(Fluids.EMPTY);
    }

    public static Optional<BooleanProperty> property(BlockState state) {
        String propertyName = FluidloggingRegistry.find(state.getBlock())
                .map(FluidloggingRegistry.Entry::propertyName)
                .orElse(FLUIDLOGGED.getName());
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty booleanProperty && property.getName().equals(propertyName)) {
                return Optional.of(booleanProperty);
            }
        }
        return Optional.empty();
    }

    public static BlockState placementState(ItemPlacementContext context, BlockState defaultState, Fluid configuredFluid) {
        Optional<BooleanProperty> property = property(defaultState);
        if (property.isEmpty()) return defaultState;
        Fluid present = context.getWorld().getFluidState(context.getBlockPos()).getFluid();
        return defaultState.with(property.get(), present == configuredFluid);
    }

    public static FluidState fluidState(BlockState state, Fluid configuredFluid) {
        return property(state).filter(property -> state.get(property))
                .map(ignored -> configuredFluid.getDefaultState())
                .orElse(Fluids.EMPTY.getDefaultState());
    }

    public static boolean canFill(@Nullable LivingEntity filler, BlockView world, BlockPos pos,
                                  BlockState state, Fluid incoming, Fluid configuredFluid) {
        return configuredFluid != Fluids.EMPTY && incoming == configuredFluid
                && property(state).map(property -> !state.get(property)).orElse(false);
    }

    public static boolean tryFill(WorldAccess world, BlockPos pos, BlockState state,
                                  FluidState incoming, Fluid configuredFluid) {
        Optional<BooleanProperty> property = property(state);
        if (property.isEmpty() || state.get(property.get()) || incoming.getFluid() != configuredFluid) return false;
        if (!world.isClient()) {
            world.setBlockState(pos, state.with(property.get(), true), Block.NOTIFY_ALL);
            world.scheduleFluidTick(pos, configuredFluid, configuredFluid.getTickRate(world));
        }
        return true;
    }

    public static ItemStack tryDrain(@Nullable LivingEntity drainer, WorldAccess world, BlockPos pos,
                                     BlockState state, Fluid configuredFluid) {
        Optional<BooleanProperty> property = property(state);
        if (property.isEmpty() || !state.get(property.get()) || configuredFluid == Fluids.EMPTY) return ItemStack.EMPTY;
        if (!world.isClient()) world.setBlockState(pos, state.with(property.get(), false), Block.NOTIFY_ALL);

        Item bucket = configuredFluid.getBucketItem();
        if (bucket != Items.AIR) return new ItemStack(bucket);
        return UniversalFluidBucketItem.filledWith(
                net.minecraft.registry.Registries.FLUID.getId(configuredFluid), SourceFluidAttributes.EMPTY);
    }

    public static Optional<SoundEvent> bucketFillSound(Fluid configuredFluid) {
        return configuredFluid.getBucketFillSound();
    }
}
