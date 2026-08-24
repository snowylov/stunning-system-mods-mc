package dev.liquidfabric.api.unofficial.liquid;

import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerItem;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import java.util.Optional;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidContainerItemHelper;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemTooltipHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class BaseLiquidContainerItem extends Item implements FluidOverlayItem, FluidContainerItem {
    private final long capacityDroplets;
    private final boolean potionAllowed;
    private final boolean drinkable;
    private final int sipCount;
    private final boolean returnsGlassBottle;
    private final int potionBaseSeconds;
    private final double potionDurationMultiplier;
    private final int amplifierBonus;

    public BaseLiquidContainerItem(Settings settings, long capacityDroplets, boolean potionAllowed, boolean drinkable, int sipCount,
                                   boolean returnsGlassBottle, int potionBaseSeconds, double potionDurationMultiplier, int amplifierBonus) {
        super(settings);
        this.capacityDroplets = capacityDroplets;
        this.potionAllowed = potionAllowed;
        this.drinkable = drinkable;
        this.sipCount = Math.max(1, sipCount);
        this.returnsGlassBottle = returnsGlassBottle;
        this.potionBaseSeconds = potionBaseSeconds;
        this.potionDurationMultiplier = potionDurationMultiplier;
        this.amplifierBonus = amplifierBonus;
    }

    @Override public long capacityDroplets() { return capacityDroplets; }

    @Override
    public long getCapacityDroplets(ItemStack stack) {
        return capacityDroplets;
    }

    @Override
    public Optional<StoredFluidComponent> getStoredFluidComponent(ItemStack stack) {
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        return stored.isEmpty() ? Optional.empty() : Optional.of(stored);
    }

    @Override
    public ItemStack withStoredFluidComponent(ItemStack stack, StoredFluidComponent fluid) {
        FluidItemComponentHelper.set(stack, fluid);
        return stack;
    }

    @Override public boolean canHoldPotionLiquids() { return potionAllowed; }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return drinkable && !getStoredFluid(stack).isEmpty() ? UseAction.DRINK : UseAction.NONE;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return drinkable ? 24 : 0;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!drinkable || getStoredFluid(stack).isEmpty()) return TypedActionResult.pass(stack);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        return FluidContainerItemHelper.consumeDrink(
                stack,
                world,
                user,
                potionAllowed,
                potionBaseSeconds,
                potionDurationMultiplier,
                amplifierBonus,
                sipCount,
                returnsGlassBottle,
                capacityDroplets
        );
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return FluidItemComponentHelper.itemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return FluidItemComponentHelper.itemBarStep(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        FluidItemTooltipHelper.appendShiftGate(stack, tooltip, FluidItemTooltipHelper.shouldShowExpanded(type));
    }
}
