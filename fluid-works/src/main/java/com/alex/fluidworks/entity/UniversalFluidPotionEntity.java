package com.alex.fluidworks.entity;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.item.UniversalFluidPotionItem;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Carries the full component-backed item stack so arbitrary fluid IDs survive flight. */
public final class UniversalFluidPotionEntity extends ThrownItemEntity {
    public UniversalFluidPotionEntity(EntityType<? extends UniversalFluidPotionEntity> type,
                                      World world) {
        super(type, world);
    }

    public UniversalFluidPotionEntity(World world, LivingEntity owner, ItemStack stack) {
        super(FluidWorks.UNIVERSAL_FLUID_POTION_ENTITY, owner, world, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return FluidWorks.UNIVERSAL_FLUID_SPLASH_POTION;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!(getEntityWorld() instanceof ServerWorld serverWorld)) return;

        ItemStack stack = getStack();
        StoredFluidComponent stored = FluidItemComponentHelper.get(stack);
        Fluid fluid = stored.isEmpty() ? null : Registries.FLUID.get(stored.liquidId());
        boolean lingering = stack.getItem() instanceof UniversalFluidPotionItem potion
            && potion.lingering();

        if (fluid != null) {
            BlockPos impact = hitResult instanceof BlockHitResult blockHit
                ? blockHit.getBlockPos().offset(blockHit.getSide())
                : BlockPos.ofFloored(hitResult.getPos());
            FluidPotionPlacement.splash(serverWorld, impact, fluid, lingering);
        }

        serverWorld.playSound(null, getBlockPos(), SoundEvents.ENTITY_SPLASH_POTION_BREAK,
            SoundCategory.NEUTRAL, 1.0F, 1.0F);
        discard();
    }
}
