package dev.liquidfabric.api.unofficial.core.fluid;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Placeable fluids owned by utility-fluid-core.
 *
 * Milk deliberately does not get a new bucket item. Vanilla Items.MILK_BUCKET is
 * adapted by interaction hooks so other mods do not have to special-case a second
 * milk bucket.
 */
public final class ModUtilityFluids {
    public static FlowableFluid MILK;
    public static FlowableFluid FLOWING_MILK;
    public static FluidBlock MILK_BLOCK;

    private ModUtilityFluids() {}

    public static void register() {
        MILK = Registry.register(Registries.FLUID, UtilityApiMod.id("milk"), new MilkFluid.Still());
        FLOWING_MILK = Registry.register(Registries.FLUID, UtilityApiMod.id("flowing_milk"), new MilkFluid.Flowing());
        MILK_BLOCK = Registry.register(
                Registries.BLOCK,
                UtilityApiMod.id("milk"),
                new FluidBlock(MILK, AbstractBlock.Settings.copy(Blocks.WATER).noCollision().dropsNothing()) {}
        );
    }
}
