package com.alex.fluidworks.crate;

import com.alex.fluidworks.FluidWorks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** One crate always owns exactly 54 slots, even while participating in a structure. */
public final class CrateBlockEntity extends BlockEntity implements Inventory {
    public static final int SIZE = 54;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int selectedPage;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.CRATE_BLOCK_ENTITY, pos, state);
    }
    @Override public int size() { return SIZE; }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack s=Inventories.splitStack(stacks,slot,amount); markDirty(); return s; }
    @Override public ItemStack removeStack(int slot) { ItemStack s=Inventories.removeStack(stacks,slot); markDirty(); return s; }
    @Override public void setStack(int slot, ItemStack stack) { stacks.set(slot, stack); markDirty(); }
    @Override public boolean canPlayerUse(net.minecraft.entity.player.PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public void clear() { stacks.clear(); markDirty(); }
    public int selectedPage() { return selectedPage; }
    public int advancePage(int pageCount) { selectedPage = Math.floorMod(selectedPage + 1, Math.max(1, pageCount)); markDirty(); return selectedPage; }
    @Override protected void readData(ReadView view) { super.readData(view); Inventories.readData(view, stacks); selectedPage=view.getInt("SelectedPage",0); }
    @Override protected void writeData(WriteView view) { super.writeData(view); Inventories.writeData(view, stacks); view.putInt("SelectedPage",selectedPage); }
}
