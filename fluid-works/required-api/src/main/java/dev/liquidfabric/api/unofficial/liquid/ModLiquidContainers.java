package dev.liquidfabric.api.unofficial.liquid;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.*;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModLiquidContainers {
    public static final Map<String, Item> CONTAINERS_BY_NAME = new LinkedHashMap<>();

    public static BaseLiquidContainerItem GLASS_TEST_TUBE;
    public static BaseLiquidContainerItem TRIPLE_VIAL;
    public static BaseLiquidContainerItem SQUARE_GLASS;
    public static BaseLiquidContainerItem FANCY_GLASS;
    public static BaseLiquidContainerItem MILK_BOTTLE;
    public static BaseLiquidContainerItem CHOCOLATE_MILK_BOTTLE;
    public static BaseLiquidContainerItem HOT_CHOCOLATE_BOTTLE;

    private ModLiquidContainers() {}

    public static void register() {
        GLASS_TEST_TUBE = registerContainer("glass_test_tube", new BaseLiquidContainerItem(new Item.Settings().maxCount(24), FluidContainerSizes.GLASS_TEST_TUBE_DROPLETS, true, true, 1, false, 8, 1.0, 1));
        TRIPLE_VIAL = registerContainer("triple_vial", new BaseLiquidContainerItem(new Item.Settings().maxCount(8), FluidContainerSizes.TRIPLE_VIAL_DROPLETS, true, true, 4, false, 8, 1.0, 0));
        SQUARE_GLASS = registerContainer("square_glass", new BaseLiquidContainerItem(new Item.Settings().maxCount(16), FluidContainerSizes.SQUARE_GLASS_DROPLETS, true, true, 1, false, 8, 1.0, 0));
        FANCY_GLASS = registerContainer("fancy_glass", new BaseLiquidContainerItem(new Item.Settings().maxCount(16), FluidContainerSizes.FANCY_GLASS_DROPLETS, true, true, 1, false, 8, 2.0, 0));
        MILK_BOTTLE = registerContainer("milk_bottle", new BaseLiquidContainerItem(new Item.Settings().maxCount(16), FluidContainerSizes.BOTTLE_DROPLETS, false, true, 1, true, 0, 1, 0));
        CHOCOLATE_MILK_BOTTLE = registerContainer("chocolate_milk_bottle", new BaseLiquidContainerItem(new Item.Settings().maxCount(16), FluidContainerSizes.BOTTLE_DROPLETS, false, true, 1, true, 0, 1, 0));
        HOT_CHOCOLATE_BOTTLE = registerContainer("hot_chocolate_bottle", new BaseLiquidContainerItem(new Item.Settings().maxCount(16), FluidContainerSizes.BOTTLE_DROPLETS, false, true, 1, true, 0, 1, 0));

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof CowEntity) || hand != Hand.MAIN_HAND) return ActionResult.PASS;
            ItemStack held = player.getStackInHand(hand);
            if (!held.isOf(Items.GLASS_BOTTLE)) return ActionResult.PASS;
            if (!world.isClient) {
                held.decrement(1);
                ItemStack filled = new ItemStack(MILK_BOTTLE);
                filled.set(ModComponents.STORED_FLUID, new StoredFluidComponent(UtilityApiMod.id("milk"), FluidContainerSizes.BOTTLE_DROPLETS, SourceFluidAttributes.EMPTY));
                player.getInventory().offerOrDrop(filled);
            }
            return ActionResult.SUCCESS;
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(GLASS_TEST_TUBE);
            entries.add(TRIPLE_VIAL);
            entries.add(SQUARE_GLASS);
            entries.add(FANCY_GLASS);
            entries.add(MILK_BOTTLE);
            entries.add(CHOCOLATE_MILK_BOTTLE);
            entries.add(HOT_CHOCOLATE_BOTTLE);
        });
    }

    private static BaseLiquidContainerItem registerContainer(String id, BaseLiquidContainerItem item) {
        BaseLiquidContainerItem registered = Registry.register(Registries.ITEM, UtilityApiMod.id(id), item);
        CONTAINERS_BY_NAME.put(UtilityApiMod.id(id).toString(), registered);
        CONTAINERS_BY_NAME.put(id, registered);
        return registered;
    }
}
