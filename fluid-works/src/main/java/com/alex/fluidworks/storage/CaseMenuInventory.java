package com.alex.fluidworks.storage;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** 48-slot case exposed through a 54-slot menu with six permanently disabled padding cells. */
public final class CaseMenuInventory implements Inventory {
    public static final int MENU_SIZE = 54;
    private final PortableCaseBlockEntity storage;
    public CaseMenuInventory(PortableCaseBlockEntity storage) { this.storage = storage; }
    @Override public int size() { return MENU_SIZE; }
    @Override public boolean isEmpty() { return storage.isEmpty(); }
    @Override public ItemStack getStack(int slot) { return slot < PortableCaseBlockEntity.SIZE ? storage.getStack(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeStack(int slot, int amount) { return slot < PortableCaseBlockEntity.SIZE ? storage.removeStack(slot, amount) : ItemStack.EMPTY; }
    @Override public ItemStack removeStack(int slot) { return slot < PortableCaseBlockEntity.SIZE ? storage.removeStack(slot) : ItemStack.EMPTY; }
    @Override public void setStack(int slot, ItemStack stack) { if (slot < PortableCaseBlockEntity.SIZE) storage.setStack(slot, stack); }
    @Override public int getMaxCountPerStack() { return storage.getMaxCountPerStack(); }
    @Override public int getMaxCount(ItemStack stack) { return storage.getMaxCount(stack); }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot < PortableCaseBlockEntity.SIZE && storage.isValid(slot, stack); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return storage.canPlayerUse(player); }
    @Override public void clear() { storage.clear(); }
    @Override public void markDirty() { storage.markDirty(); }
    @Override public void onOpen(ContainerUser user) { storage.onOpen(user); }
    @Override public void onClose(ContainerUser user) { storage.onClose(user); }
}
