package com.alex.fluidworks.crate;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A six-row page backed directly by one member of a deterministic crate cluster. */
public final class CrateClusterInventory implements Inventory {
    private final List<CrateBlockEntity> parts;
    private final CrateBlockEntity page;

    private CrateClusterInventory(List<CrateBlockEntity> parts, CrateBlockEntity page) {
        this.parts = parts;
        this.page = page;
    }

    public static CrateClusterInventory create(World world, BlockPos clicked, CrateBlock block) {
        return create(world, clicked, block, -1);
    }

    /** Selects a deterministic Y/Z/X page; a negative index retains the clicked-module behavior. */
    public static CrateClusterInventory create(World world, BlockPos clicked, CrateBlock block, int pageIndex) {
        List<CrateBlockEntity> parts = new ArrayList<>();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.HashSet<BlockPos> seen = new java.util.HashSet<>();
        queue.add(clicked); seen.add(clicked);
        while (!queue.isEmpty() && parts.size() < 729) {
            BlockPos pos = queue.removeFirst();
            if (!world.getBlockState(pos).isOf(block)) continue;
            if (world.getBlockEntity(pos) instanceof CrateBlockEntity crate) parts.add(crate);
            for (net.minecraft.util.math.Direction d : net.minecraft.util.math.Direction.values()) {
                BlockPos next = pos.offset(d);
                if (seen.add(next) && world.getBlockState(next).isOf(block)) queue.add(next);
            }
        }
        parts.sort(Comparator.comparingInt((CrateBlockEntity e) -> e.getPos().getY())
            .thenComparingInt(e -> e.getPos().getZ()).thenComparingInt(e -> e.getPos().getX()));
        CrateBlockEntity clickedPart = world.getBlockEntity(clicked) instanceof CrateBlockEntity c ? c : parts.getFirst();
        CrateBlockEntity selected = pageIndex < 0 ? clickedPart : parts.get(Math.floorMod(pageIndex, parts.size()));
        return new CrateClusterInventory(parts, selected);
    }

    public int pageNumber() { return parts.indexOf(page) + 1; }
    public int pageCount() { return parts.size(); }
    public int totalSlots() { return parts.size() * CrateBlockEntity.SIZE; }
    @Override public int size() { return CrateBlockEntity.SIZE; }
    @Override public boolean isEmpty() { return parts.stream().allMatch(CrateBlockEntity::isEmpty); }
    @Override public ItemStack getStack(int slot) { return page.getStack(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { return page.removeStack(slot, amount); }
    @Override public ItemStack removeStack(int slot) { return page.removeStack(slot); }
    @Override public void setStack(int slot, ItemStack stack) { page.setStack(slot, stack); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return parts.stream().anyMatch(p -> p.canPlayerUse(player)); }
    @Override public void clear() { parts.forEach(CrateBlockEntity::clear); }
    @Override public void markDirty() { parts.forEach(CrateBlockEntity::markDirty); }
}
