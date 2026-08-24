package com.alex.fluidworks.furniture;

import com.alex.fluidworks.FluidWorks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Two independent tabletop display slots: map first, food second. */
public final class DisplayTableBlockEntity extends BlockEntity implements Inventory {
    public static final int MAP_SLOT = 0;
    public static final int FOOD_SLOT = 1;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(2, ItemStack.EMPTY);

    public DisplayTableBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.DISPLAY_TABLE_BLOCK_ENTITY, pos, state);
    }

    @Override public int size() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack s = Inventories.splitStack(stacks, slot, amount); sync(); return s; }
    @Override public ItemStack removeStack(int slot) { ItemStack s = Inventories.removeStack(stacks, slot); sync(); return s; }
    @Override public void setStack(int slot, ItemStack stack) { stacks.set(slot, stack); sync(); }
    @Override public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }
    @Override public void clear() { stacks.clear(); sync(); }

    private void sync() {
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    @Override protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, stacks);
    }

    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, stacks);
    }

    @Override public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }
}
