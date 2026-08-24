package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public final class NeedleProjectileUtil {
    private NeedleProjectileUtil() {}

    public static void inject(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload, DamageSource damageSource) {
        target.damage(world, damageSource, LiquidFabricConfig.needleGunDamage);
        NeedleEffectRegistry.apply(world, target, attacker, payload);
    }
}
