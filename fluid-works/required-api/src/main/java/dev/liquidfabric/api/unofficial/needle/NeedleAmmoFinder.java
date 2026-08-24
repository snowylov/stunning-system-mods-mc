package dev.liquidfabric.api.unofficial.needle;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public final class NeedleAmmoFinder {
    private NeedleAmmoFinder() {}

    public static ItemStack find(PlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BaseNeedleItem needle && !needle.getPayload(stack).isEmpty()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
