package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class NeedleGunItem extends Item {
    public NeedleGunItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack gun = user.getStackInHand(hand);
        ItemStack ammo = NeedleAmmoFinder.find(user);
        if (ammo.isEmpty()) return TypedActionResult.fail(gun);

        if (world instanceof ServerWorld serverWorld) {
            BaseNeedleItem needle = (BaseNeedleItem) ammo.getItem();
            NeedlePayload payload = needle.getPayload(ammo);
            NeedleProjectileEntity projectile = new NeedleProjectileEntity(ModNeedles.NEEDLE_PROJECTILE, serverWorld);
            projectile.setOwner(user);
            projectile.setPayload(payload);
            projectile.setPosition(user.getX(), user.getEyeY() - 0.1, user.getZ());
            projectile.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 3.2f, 0.0f);
            serverWorld.spawnEntity(projectile);

            if (!user.getAbilities().creativeMode) {
                ammo.decrement(1);
            }
            if (LiquidFabricConfig.needleGunCooldownTicks > 0) {
                user.getItemCooldownManager().set(this.getDefaultStack(), LiquidFabricConfig.needleGunCooldownTicks);
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.7f, 1.8f);
        }

        return TypedActionResult.success(gun, world.isClient());
    }
}
