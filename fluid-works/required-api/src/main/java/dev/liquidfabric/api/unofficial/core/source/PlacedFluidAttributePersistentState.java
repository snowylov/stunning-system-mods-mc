package dev.liquidfabric.api.unofficial.core.source;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent sidecar data for vanilla/modded fluid source blocks that cannot
 * store UtilityAPI components themselves.
 *
 * This keeps "from_cave" and "not_from_ocean" through server restarts as best
 * as possible without replacing vanilla water or modded fluid blocks.
 */
public final class PlacedFluidAttributePersistentState extends PersistentState {
    private static final String NAME = UtilityApiMod.MOD_ID + "_placed_fluid_attributes";

    private final Map<Long, SourceFluidAttributes> attributesByPos = new HashMap<>();

    public static PlacedFluidAttributePersistentState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(
                        NAME,
                        PlacedFluidAttributePersistentState::new,
                        PlacedFluidAttributePersistentState::fromNbt,
                        null
                )
        );
    }

    public static PlacedFluidAttributePersistentState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        PlacedFluidAttributePersistentState state = new PlacedFluidAttributePersistentState();
        NbtList list = nbt.getList("Entries", NbtCompound.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            long pos = entry.getLong("Pos");
            SourceFluidAttributes attributes = new SourceFluidAttributes(
                    entry.getBoolean("NotFromOcean"),
                    entry.getBoolean("FromCave")
            );
            if (FluidSourceAttributeUtil.isMeaningful(attributes)) {
                state.attributesByPos.put(pos, attributes);
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<Long, SourceFluidAttributes> entry : attributesByPos.entrySet()) {
            SourceFluidAttributes attributes = entry.getValue();
            if (!FluidSourceAttributeUtil.isMeaningful(attributes)) continue;
            NbtCompound tag = new NbtCompound();
            tag.putLong("Pos", entry.getKey());
            tag.putBoolean("NotFromOcean", attributes.notFromOcean());
            tag.putBoolean("FromCave", attributes.fromCave());
            list.add(tag);
        }
        nbt.put("Entries", list);
        return nbt;
    }

    public void put(BlockPos pos, SourceFluidAttributes attributes) {
        if (!FluidSourceAttributeUtil.isMeaningful(attributes)) {
            remove(pos);
            return;
        }
        attributesByPos.put(pos.asLong(), attributes);
        markDirty();
    }

    public SourceFluidAttributes get(BlockPos pos) {
        return attributesByPos.get(pos.asLong());
    }


    public int cleanup(ServerWorld world) {
        int before = attributesByPos.size();
        attributesByPos.entrySet().removeIf(entry -> {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            return !world.getFluidState(pos).isStill();
        });
        int removed = before - attributesByPos.size();
        if (removed > 0) markDirty();
        return removed;
    }

    public void remove(BlockPos pos) {
        if (attributesByPos.remove(pos.asLong()) != null) {
            markDirty();
        }
    }
}
