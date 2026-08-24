package com.alex.fluidworks.thermal;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/** Nine-slot temperature machine inventory. All temperature changes happen server-side. */
public final class TemperatureMachineBlockEntity extends BlockEntity implements Inventory,
        ExtendedScreenHandlerFactory<Boolean> {
    public static final int SIZE = 9;
    private static final int ITEM_STEP = 100;

    private final DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
    private int targetTemperature;
    private final PropertyDelegate properties = new PropertyDelegate() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> targetTemperature;
                case 1 -> isHeating() ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) setTargetTemperature(value);
        }
        @Override public int size() { return 2; }
    };

    public TemperatureMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ThermalContent.TEMPERATURE_MACHINE_BLOCK_ENTITY, pos, state);
        targetTemperature = isHeatingState(state) ? ThermalApiBridge.HOT : ThermalApiBridge.COLD;
    }

    public boolean isHeating() { return isHeatingState(getCachedState()); }
    public int targetTemperature() { return targetTemperature; }
    public void setTargetTemperature(int temperature) {
        int clamped = isHeating()
            ? Math.max(ThermalApiBridge.STANDARD, Math.min(ThermalApiBridge.HOT, temperature))
            : Math.max(ThermalApiBridge.COLD, Math.min(ThermalApiBridge.STANDARD, temperature));
        if (clamped != targetTemperature) {
            targetTemperature = clamped;
            markDirty();
        }
    }

    private static boolean isHeatingState(BlockState state) {
        return state.getBlock() instanceof TemperatureMachineBlock block && block.heating();
    }

    public static void serverTick(World world, BlockPos pos, BlockState state,
                                  TemperatureMachineBlockEntity machine) {
        if (!(world instanceof ServerWorld serverWorld) || serverWorld.getTime() % 5 != 0) return;
        boolean changed = false;
        for (ItemStack stack : machine.stacks) {
            if (stack.isEmpty()) continue;
            int current = ThermalApiBridge.getItemTemperature(stack);
            int difference = machine.targetTemperature - current;
            if (difference == 0) continue;
            int next = current + Math.max(-ITEM_STEP, Math.min(ITEM_STEP, difference));
            ThermalApiBridge.setItemTemperature(stack, next);
            changed = true;
        }
        ThermalApiBridge.setBlockTemperature(serverWorld, pos, machine.targetTemperature);
        for (Direction direction : Direction.values()) {
            if (serverWorld.getBlockEntity(pos.offset(direction)) instanceof ThermalFluidCarrier carrier) {
                carrier.approachTemperature(machine.targetTemperature, ITEM_STEP);
            }
        }
        if (changed) machine.markDirty();
    }

    @Override public Text getDisplayName() {
        return Text.translatable(isHeating()
            ? "container.fluidworks.heater" : "container.fluidworks.cooler");
    }
    @Override public Boolean getScreenOpeningData(ServerPlayerEntity player) { return isHeating(); }
    @Override public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity player) {
        return new TemperatureMachineScreenHandler(syncId, inventory, this, properties,
            net.minecraft.screen.ScreenHandlerContext.create(world, pos));
    }
    @Override public int size() { return SIZE; }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return stacks.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(stacks, slot, amount);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public ItemStack removeStack(int slot) {
        ItemStack result = Inventories.removeStack(stacks, slot);
        if (!result.isEmpty()) markDirty();
        return result;
    }
    @Override public void setStack(int slot, ItemStack stack) {
        stacks.set(slot, stack);
        if (stack.getCount() > getMaxCount(stack)) stack.setCount(getMaxCount(stack));
        markDirty();
    }
    @Override public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }
    @Override public void clear() { stacks.clear(); markDirty(); }

    @Override protected void readData(ReadView view) {
        super.readData(view);
        Inventories.readData(view, stacks);
        targetTemperature = view.getInt("TargetTemperature",
            isHeating() ? ThermalApiBridge.HOT : ThermalApiBridge.COLD);
        setTargetTemperature(targetTemperature);
    }
    @Override protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, stacks);
        view.putInt("TargetTemperature", targetTemperature);
    }
}
