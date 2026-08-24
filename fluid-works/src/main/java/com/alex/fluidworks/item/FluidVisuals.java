package com.alex.fluidworks.item;

import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/** Keeps the vanilla potion tint source synchronized with the independent API component. */
public final class FluidVisuals {
    private FluidVisuals() {
    }

    public static void sync(ItemStack stack) {
        if (!(stack.getItem() instanceof TintedFluidContainerItem)) return;
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        if (stored.isEmpty()) {
            stack.remove(DataComponentTypes.POTION_CONTENTS);
            return;
        }
        stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
            Optional.empty(), Optional.of(colorFor(stored.liquidId())), List.of(), Optional.empty()));
    }

    public static int colorFor(Identifier fluidId) {
        if (fluidId.equals(Identifier.ofVanilla("water"))) return 0x3F76E4;
        if (fluidId.equals(Identifier.ofVanilla("lava"))) return 0xFF6A00;
        if (fluidId.getPath().contains("milk")) return 0xF8F8F0;
        int hash = fluidId.toString().hashCode();
        int red = 72 + ((hash >>> 16) & 0x7F);
        int green = 72 + ((hash >>> 8) & 0x7F);
        int blue = 72 + (hash & 0x7F);
        return (red << 16) | (green << 8) | blue;
    }
}
