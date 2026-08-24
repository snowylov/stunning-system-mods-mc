package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.*;
import dev.liquidfabric.api.unofficial.needle.NeedleEffectRegistry;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Drinking/sipping/filling helper for liquid container items.
 */
public final class FluidContainerItemHelper {
    private FluidContainerItemHelper() {}

    public static boolean isDrinkableAndFilled(boolean drinkable, ItemStack stack) {
        return drinkable && FluidItemComponentHelper.hasFluid(stack);
    }

    public static ItemStack consumeDrink(ItemStack stack,
                                         World world,
                                         LivingEntity user,
                                         boolean potionAllowed,
                                         int potionBaseSeconds,
                                         double potionDurationMultiplier,
                                         int amplifierBonus,
                                         int sipCount,
                                         boolean returnsGlassBottle,
                                         long capacityDroplets) {
        if (!(world instanceof ServerWorld)) return stack;

        StoredFluidComponent fluid = FluidItemComponentHelper.get(stack);
        if (fluid.isEmpty()) return stack;

        applyLiquidDrinkEffects(stack, user, fluid, potionAllowed, potionBaseSeconds, potionDurationMultiplier, amplifierBonus);

        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            long sipSize = Math.max(1, capacityDroplets / Math.max(1, sipCount));
            drainAfterDrink(stack, player, fluid, sipSize, returnsGlassBottle);
        }

        return stack;
    }

    public static void applyLiquidDrinkEffects(ItemStack stack,
                                               LivingEntity user,
                                               StoredFluidComponent fluid,
                                               boolean potionAllowed,
                                               int potionBaseSeconds,
                                               double potionDurationMultiplier,
                                               int amplifierBonus) {
        Identifier milk = UtilityApiMod.id("milk");
        Identifier chocolateMilk = UtilityApiMod.id("chocolate_milk");
        Identifier hotChocolate = UtilityApiMod.id("hot_chocolate");
        Identifier potion = UtilityApiMod.id("potion");

        if (fluid.liquidId().equals(milk)) {
            user.clearStatusEffects();
            return;
        }

        if (fluid.liquidId().equals(chocolateMilk)) {
            NeedleEffectRegistry.clearNegativeEffects(user);
            if (user instanceof PlayerEntity player) player.getHungerManager().add(2, 0.2f);
            return;
        }

        if (fluid.liquidId().equals(hotChocolate)) {
            NeedleEffectRegistry.clearNegativeEffects(user);
            user.addStatusEffect(new StatusEffectInstance(ModStatusEffects.SOUL_PROTECTED, 20 * 60 * 4, 0));
            if (user instanceof PlayerEntity player) player.getHungerManager().add(2, 0.2f);
            return;
        }

        if (potionAllowed && fluid.liquidId().equals(potion)) {
            NeedlePayload payload = stack.getOrDefault(ModComponents.NEEDLE_PAYLOAD, NeedlePayload.EMPTY);
            int duration = (int) Math.round(potionBaseSeconds * potionDurationMultiplier);
            for (StatusEffectInstance effect : PotionLiquidUtil.strengthenedShort(payload.potionEffects(), duration, payload.redstoneBoostLevel(), payload.glowstoneBoostLevel())) {
                user.addStatusEffect(new StatusEffectInstance(effect.getEffectType(), effect.getDuration(), effect.getAmplifier() + amplifierBonus));
            }
        }
    }

    public static void drainAfterDrink(ItemStack stack,
                                       PlayerEntity player,
                                       StoredFluidComponent fluid,
                                       long sipSizeDroplets,
                                       boolean returnsGlassBottle) {
        long remaining = fluid.amountDroplets() - Math.max(1, sipSizeDroplets);
        if (remaining <= 0) {
            stack.decrement(1);
            if (returnsGlassBottle) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!stack.isEmpty()) player.getInventory().offerOrDrop(bottle);
            }
            return;
        }

        FluidItemComponentHelper.set(stack, new StoredFluidComponent(fluid.liquidId(), remaining, fluid.sourceAttributes()));
    }

    public static boolean canAcceptLiquid(ItemStack stack, Identifier liquidId, boolean allowPotion) {
        if (stack.isEmpty() || liquidId == null) return false;
        if (FluidItemComponentHelper.capacity(stack) <= 0) return false;
        if (UtilityApiMod.id("potion").equals(liquidId) && !allowPotion) return false;
        return FluidItemComponentHelper.canAcceptFluid(stack, liquidId);
    }

    public static long fillOrTopOff(ItemStack stack, StoredFluidComponent fluid, long maxDroplets) {
        if (fluid == null || fluid.isEmpty() || maxDroplets <= 0) return 0;
        StoredFluidComponent current = FluidItemComponentHelper.get(stack);
        if (current.isEmpty()) {
            long inserted = Math.min(maxDroplets, Math.min(fluid.amountDroplets(), FluidItemComponentHelper.capacity(stack)));
            FluidItemComponentHelper.set(stack, new StoredFluidComponent(fluid.liquidId(), inserted, fluid.sourceAttributes()));
            return inserted;
        }

        if (!current.liquidId().equals(fluid.liquidId()) || !current.sourceAttributes().equals(fluid.sourceAttributes())) return 0;
        long inserted = Math.min(maxDroplets, FluidItemComponentHelper.freeSpace(stack));
        if (inserted > 0) {
            FluidItemComponentHelper.set(stack, new StoredFluidComponent(current.liquidId(), current.amountDroplets() + inserted, current.sourceAttributes()));
        }
        return inserted;
    }
}
