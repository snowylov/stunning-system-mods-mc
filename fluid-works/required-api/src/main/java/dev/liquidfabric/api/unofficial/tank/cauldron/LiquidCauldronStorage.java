package dev.liquidfabric.api.unofficial.tank.cauldron;

import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight runtime storage for compatibility cauldrons.
 *
 * It avoids replacing vanilla cauldron block entities or block classes. This is
 * deliberately runtime-only for now: cauldron liquids are a convenience behavior,
 * not a world-storage system. If persistence is needed later, it should become a
 * schema-versioned PersistentState with pruning.
 */
public final class LiquidCauldronStorage {
    private static final Map<Key, StoredFluidComponent> STORED = new HashMap<>();

    private LiquidCauldronStorage() {}

    public static void put(World world, BlockPos pos, StoredFluidComponent component) {
        STORED.put(Key.of(world, pos), component);
    }

    public static StoredFluidComponent get(World world, BlockPos pos) {
        return STORED.getOrDefault(Key.of(world, pos), StoredFluidComponent.EMPTY);
    }

    public static void remove(World world, BlockPos pos) {
        STORED.remove(Key.of(world, pos));
    }

    private record Key(String worldKey, long packedPos) {
        static Key of(World world, BlockPos pos) {
            return new Key(String.valueOf(world.getRegistryKey().getValue()), pos.asLong());
        }
    }
}
