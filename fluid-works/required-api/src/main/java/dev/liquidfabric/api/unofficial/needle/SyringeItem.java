package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import dev.liquidfabric.api.unofficial.helper.item.NeedlePayloadItemHelper;

public class SyringeItem extends BaseNeedleItem {
    public SyringeItem(Settings settings) {
        super(settings.maxCount(16));
    }

    @Override
    public long capacityDroplets() {
        return FluidContainerSizes.SYRINGE_DROPLETS;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.getWorld() instanceof ServerWorld serverWorld) {
            NeedlePayloadItemHelper.applyToTarget(serverWorld, target, attacker, stack, true);
        }
        return true;
    }
}
