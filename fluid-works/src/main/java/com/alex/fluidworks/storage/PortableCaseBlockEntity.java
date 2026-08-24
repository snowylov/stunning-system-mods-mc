package com.alex.fluidworks.storage;

import com.alex.fluidworks.StorageCasesContent;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.ContainerUser;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Forty-eight real slots. The menu supplies six disabled padding slots for vanilla's 9x6 screen. */
public final class PortableCaseBlockEntity extends BlockEntity implements Inventory {
    public static final int SIZE = 48;
    public static final int FOOD_SLOT_LIMIT = 512;
    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int viewers;
    private float lidProgress;
    private float lastLidProgress;

    public PortableCaseBlockEntity(BlockPos pos, BlockState state) {
        super(StorageCasesContent.PORTABLE_CASE_BLOCK_ENTITY, pos, state);
    }

    public DefaultedList<ItemStack> stacksView() { return stacks; }
    public float lidProgress(float tickProgress) {
        return lastLidProgress + (lidProgress - lastLidProgress) * tickProgress;
    }
    public static void clientTick(World world, BlockPos pos, BlockState state, PortableCaseBlockEntity storage) {
        storage.lastLidProgress = storage.lidProgress;
        float target = state.get(PortableCaseBlock.OPEN) ? 1.0F : 0.0F;
        if (storage.lidProgress < target) storage.lidProgress = Math.min(target, storage.lidProgress + 0.12F);
        else if (storage.lidProgress > target) storage.lidProgress = Math.max(target, storage.lidProgress - 0.12F);
    }
    private PortableCaseBlock owner() { return (PortableCaseBlock) getCachedState().getBlock(); }
    @Override public int size() { return SIZE; }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(stacks, slot, amount); sync(); return result;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(stacks, slot); sync(); return result;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !isValid(slot, stack)) return;
        int limit = getMaxCountPerStack();
        if (stack.getCount() > limit) stack.setCount(limit);
        stacks.set(slot, stack); sync();
    }
    @Override public int getMaxCountPerStack() { return owner().foodOnly() ? FOOD_SLOT_LIMIT : 64; }
    @Override public int getMaxCount(ItemStack stack) { return getMaxCountPerStack(); }
    @Override public boolean isValid(int slot, ItemStack stack) {
        return !owner().foodOnly() || stack.contains(DataComponentTypes.FOOD);
    }
    @Override public boolean canPlayerUse(PlayerEntity player) { return Inventory.canPlayerUse(this, player); }
    @Override public void clear() { stacks.clear(); sync(); }

    @Override public void onOpen(ContainerUser user) {
        if (!(user instanceof PlayerEntity player) || !player.isSpectator()) {
            if (++viewers == 1) setOpen(true);
        }
    }
    @Override public void onClose(ContainerUser user) {
        if (!(user instanceof PlayerEntity player) || !player.isSpectator()) {
            if (viewers > 0 && --viewers == 0) setOpen(false);
        }
    }
    private void setOpen(boolean open) {
        if (world != null && !world.isClient() && getCachedState().contains(PortableCaseBlock.OPEN)
            && getCachedState().get(PortableCaseBlock.OPEN) != open) {
            world.setBlockState(pos, getCachedState().with(PortableCaseBlock.OPEN, open), 3);
        }
    }

    private void sync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            if (!world.isClient()) ((net.minecraft.server.world.ServerWorld) world)
                .getChunkManager().markForUpdate(pos);
        }
    }
    @Override protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, stacks);
        applyCounts(view.getOptionalIntArray("CaseCounts").orElse(null));
    }
    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        if (owner().foodOnly()) {
            Inventories.writeData(view, prototypeStacks());
            view.putIntArray("CaseCounts", counts());
        } else Inventories.writeData(view, stacks);
    }
    @Override protected void readComponents(ComponentsAccess components) {
        super.readComponents(components);
        components.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(stacks);
        NbtComponent custom = components.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        applyCounts(custom.copyNbt().getIntArray("FluidWorksCaseCounts").orElse(null));
    }
    @Override protected void addComponents(ComponentMap.Builder builder) {
        super.addComponents(builder);
        builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(
            owner().foodOnly() ? prototypeStacks() : stacks));
        if (owner().foodOnly()) {
            NbtCompound custom = new NbtCompound();
            custom.putIntArray("FluidWorksCaseCounts", counts());
            builder.add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(custom));
        }
    }
    @Override public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    @Override public net.minecraft.nbt.NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    public DefaultedList<ItemStack> prototypeStacks() {
        DefaultedList<ItemStack> result = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) if (!stacks.get(i).isEmpty()) result.set(i, stacks.get(i).copyWithCount(1));
        return result;
    }
    public int[] counts() {
        int[] counts = new int[SIZE];
        for (int i = 0; i < SIZE; i++) counts[i] = stacks.get(i).getCount();
        return counts;
    }
    private void applyCounts(@org.jetbrains.annotations.Nullable int[] counts) {
        if (counts == null) return;
        for (int i = 0; i < Math.min(SIZE, counts.length); i++) {
            if (!stacks.get(i).isEmpty()) stacks.get(i).setCount(Math.max(1, Math.min(FOOD_SLOT_LIMIT, counts[i])));
        }
    }
}
