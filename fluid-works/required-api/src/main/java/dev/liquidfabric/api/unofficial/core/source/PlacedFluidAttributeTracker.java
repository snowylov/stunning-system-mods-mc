package dev.liquidfabric.api.unofficial.core.source;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sidecar metadata for placed fluid sources.
 *
 * ServerWorld uses PersistentState so source tags survive restarts. The small
 * runtime map remains as a fallback for non-server worlds and for old compile
 * targets where PersistentState signatures may need mapping adjustment.
 */
public final class PlacedFluidAttributeTracker {
    private static final Map<RegistryKey<World>, Map<Long, SourceFluidAttributes>> FALLBACK_BY_WORLD = new ConcurrentHashMap<>();

    private PlacedFluidAttributeTracker() {}

    public static void put(World world, BlockPos pos, SourceFluidAttributes attributes) {
        if (world.isClient) return;
        if (!FluidSourceAttributeUtil.isMeaningful(attributes)) {
            remove(world, pos);
            return;
        }
        if (world instanceof ServerWorld serverWorld) {
            PlacedFluidAttributePersistentState.get(serverWorld).put(pos, attributes);
            return;
        }
        FALLBACK_BY_WORLD.computeIfAbsent(world.getRegistryKey(), key -> new ConcurrentHashMap<>()).put(pos.asLong(), attributes);
    }

    public static SourceFluidAttributes get(World world, BlockPos pos) {
        if (world instanceof ServerWorld serverWorld) {
            return PlacedFluidAttributePersistentState.get(serverWorld).get(pos);
        }
        Map<Long, SourceFluidAttributes> map = FALLBACK_BY_WORLD.get(world.getRegistryKey());
        return map == null ? null : map.get(pos.asLong());
    }

    public static void remove(World world, BlockPos pos) {
        if (world.isClient) return;
        if (world instanceof ServerWorld serverWorld) {
            PlacedFluidAttributePersistentState.get(serverWorld).remove(pos);
            return;
        }
        Map<Long, SourceFluidAttributes> map = FALLBACK_BY_WORLD.get(world.getRegistryKey());
        if (map != null) {
            map.remove(pos.asLong());
            if (map.isEmpty()) FALLBACK_BY_WORLD.remove(world.getRegistryKey());
        }
    }

    public static int cleanup(ServerWorld world) {
        return PlacedFluidAttributePersistentState.get(world).cleanup(world);
    }

    public static void clearWorld(World world) {
        FALLBACK_BY_WORLD.remove(world.getRegistryKey());
    }
}
