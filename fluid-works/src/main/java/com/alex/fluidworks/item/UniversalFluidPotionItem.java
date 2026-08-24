package com.alex.fluidworks.item;

import com.alex.fluidworks.entity.UniversalFluidPotionEntity;
import dev.liquidfabric.api.unofficial.core.FluidOverlayItem;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** A 250 mB universal fluid bottle that throws its stored fluid into the world. */
public final class UniversalFluidPotionItem extends Item
        implements FluidOverlayItem, TintedFluidContainerItem {
    public static final long CAPACITY = FluidUnits.mbToDroplets(250);

    private final boolean lingering;

    public UniversalFluidPotionItem(Settings settings, boolean lingering) {
        super(settings.maxCount(1));
        this.lingering = lingering;
    }

    public boolean lingering() {
        return lingering;
    }

    @Override
    public long capacityDroplets() {
        return CAPACITY;
    }

    @Override
    public boolean canHoldPotionLiquids() {
        return false;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack held = user.getStackInHand(hand);
        StoredFluidComponent stored = FluidItemComponentHelper.get(held);
        Fluid fluid = stored.isEmpty() ? null : Registries.FLUID.get(stored.liquidId());
        if (!(fluid instanceof FlowableFluid)) return ActionResult.FAIL;

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_SPLASH_POTION_THROW,
            SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        if (world instanceof ServerWorld serverWorld) {
            ItemStack projectileStack = held.copyWithCount(1);
            UniversalFluidPotionEntity projectile = new UniversalFluidPotionEntity(
                serverWorld, user, projectileStack);
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), -20.0F, 0.5F, 1.0F);
            serverWorld.spawnEntity(projectile);
            if (!user.getAbilities().creativeMode) held.decrement(1);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return FluidItemComponentHelper.itemBarVisible(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return FluidItemComponentHelper.itemBarStep(stack);
    }
}
