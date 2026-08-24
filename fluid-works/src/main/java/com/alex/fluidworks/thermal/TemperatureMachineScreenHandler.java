package com.alex.fluidworks.thermal;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;

/** Server-authoritative 3x3 inventory and validated 0..1000 temperature slider. */
public final class TemperatureMachineScreenHandler extends ScreenHandler {
    private static final int MACHINE_START = 0;
    private static final int MACHINE_END = 9;
    private static final int PLAYER_START = 9;
    private static final int PLAYER_END = 45;

    private final Inventory inventory;
    private final PropertyDelegate properties;
    private final ScreenHandlerContext context;

    public TemperatureMachineScreenHandler(int syncId, PlayerInventory playerInventory, Boolean heating) {
        this(syncId, playerInventory, new SimpleInventory(TemperatureMachineBlockEntity.SIZE),
            clientProperties(heating), ScreenHandlerContext.EMPTY);
    }

    public TemperatureMachineScreenHandler(int syncId, PlayerInventory playerInventory,
                                           Inventory inventory, PropertyDelegate properties,
                                           ScreenHandlerContext context) {
        super(ThermalContent.TEMPERATURE_MACHINE_SCREEN_HANDLER, syncId);
        checkSize(inventory, TemperatureMachineBlockEntity.SIZE);
        checkDataCount(properties, 2);
        this.inventory = inventory;
        this.properties = properties;
        this.context = context;
        inventory.onOpen(playerInventory.player);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(inventory, column + row * 3, 62 + column * 18, 17 + row * 18));
            }
        }
        addPlayerSlots(playerInventory, 8, 84);
        addProperties(properties);
    }

    private static PropertyDelegate clientProperties(Boolean heating) {
        ArrayPropertyDelegate delegate = new ArrayPropertyDelegate(2);
        delegate.set(0, Boolean.TRUE.equals(heating) ? ThermalApiBridge.HOT : ThermalApiBridge.COLD);
        delegate.set(1, Boolean.TRUE.equals(heating) ? 1 : 0);
        return delegate;
    }

    public int targetTemperature() { return properties.get(0); }
    public boolean heating() { return properties.get(1) != 0; }

    @Override public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id > 1000) return false;
        int target = heating()
            ? ThermalApiBridge.STANDARD + Math.round((ThermalApiBridge.HOT - ThermalApiBridge.STANDARD) * id / 1000.0F)
            : ThermalApiBridge.COLD + Math.round((ThermalApiBridge.STANDARD - ThermalApiBridge.COLD) * id / 1000.0F);
        properties.set(0, target);
        sendContentUpdates();
        return true;
    }

    @Override public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return ItemStack.EMPTY;
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (slotIndex < MACHINE_END) {
            if (!insertItem(stack, PLAYER_START, PLAYER_END, true)) return ItemStack.EMPTY;
        } else if (!insertItem(stack, MACHINE_START, MACHINE_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY); else slot.markDirty();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, stack);
        return original;
    }

    @Override public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }
}
