package dev.liquidfabric.api.unofficial.tank.cauldron;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;

/**
 * Conservative cauldron support.
 *
 * Compatible behavior:
 * - no mixins
 * - does not replace vanilla cauldron behavior
 * - only consumes interactions for registered potion/liquid containers
 * - only stores a small runtime component on water-cauldron positions
 */
public final class FluidCauldronSupport {
    private static boolean registered;

    private FluidCauldronSupport() {}

    public static void register() {
        if (registered || !LiquidFabricConfig.enableCauldronFluidSupport) return;
        registered = true;
        LiquidCauldronBehaviorRegistry.bootstrapDefaults();

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!LiquidFabricConfig.enableCauldronFluidSupport) return ActionResult.PASS;

            var pos = hit.getBlockPos();
            var state = world.getBlockState(pos);
            var stack = player.getStackInHand(hand);

            if (state.isOf(Blocks.CAULDRON)) {
                StoredFluidComponent component = componentFromStack(stack);
                if (component.isEmpty() || !LiquidCauldronBehaviorRegistry.canStore(component.liquidId())) return ActionResult.PASS;

                if (!world.isClient) {
                    world.setBlockState(pos, Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3), 3);
                    LiquidCauldronStorage.put(world, pos, component);
                    consumeFilledContainer(player, hand, stack);
                }
                return ActionResult.SUCCESS;
            }

            if (state.isOf(Blocks.WATER_CAULDRON)) {
                StoredFluidComponent stored = LiquidCauldronStorage.get(world, pos);
                if (stored.isEmpty()) return ActionResult.PASS;

                if (stack.isOf(Items.GLASS_BOTTLE)) {
                    if (!world.isClient) {
                        stack.decrement(1);
                        ItemStack result = new ItemStack(Items.POTION);
                        // Potion NBT/component reconstruction is intentionally conservative.
                        // Non-vanilla/custom potion liquids should use Utility containers.
                        player.getInventory().offerOrDrop(result);
                        decrementOrClear(world, pos, state);
                    }
                    return ActionResult.SUCCESS;
                }

                if (FluidItemComponentHelper.capacity(stack) >= FluidContainerSizes.BOTTLE_DROPLETS
                        && FluidItemComponentHelper.isEmpty(stack)
                        && LiquidCauldronBehaviorRegistry.canStore(stored.liquidId())) {
                    if (!world.isClient) {
                        FluidItemComponentHelper.set(stack, stored.clamped(Math.min(FluidItemComponentHelper.capacity(stack), FluidContainerSizes.BOTTLE_DROPLETS)));
                        decrementOrClear(world, pos, state);
                    }
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static StoredFluidComponent componentFromStack(ItemStack stack) {
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        if (!stored.isEmpty()) return stored;

        if (stack.isOf(Items.POTION)) {
            // Keep this compile-stable across 1.21.x component churn: vanilla
            // potion details stay on the consumed potion item, while the cauldron
            // stores a generic potion liquid marker. Utility containers preserve
            // their exact StoredFluidComponent when used instead.
            return new StoredFluidComponent(
                    net.minecraft.util.Identifier.ofVanilla("potion"),
                    FluidContainerSizes.BOTTLE_DROPLETS,
                    dev.liquidfabric.api.unofficial.core.SourceFluidAttributes.EMPTY
            );
        }
        return StoredFluidComponent.EMPTY;
    }

    private static void consumeFilledContainer(net.minecraft.entity.player.PlayerEntity player, net.minecraft.util.Hand hand, ItemStack stack) {
        if (player.getAbilities().creativeMode) return;
        stack.decrement(1);
        if (stack.isEmpty()) player.setStackInHand(hand, new ItemStack(Items.GLASS_BOTTLE));
        else player.getInventory().offerOrDrop(new ItemStack(Items.GLASS_BOTTLE));
    }

    private static void decrementOrClear(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.BlockState state) {
        int level = state.contains(LeveledCauldronBlock.LEVEL) ? state.get(LeveledCauldronBlock.LEVEL) : 3;
        if (level <= 1) {
            world.setBlockState(pos, Blocks.CAULDRON.getDefaultState(), 3);
            LiquidCauldronStorage.remove(world, pos);
        } else {
            world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level - 1), 3);
        }
    }
}
