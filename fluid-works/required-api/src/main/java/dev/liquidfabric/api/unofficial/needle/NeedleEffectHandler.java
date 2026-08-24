package dev.liquidfabric.api.unofficial.needle;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;

public interface NeedleEffectHandler {
    boolean canHandle(NeedlePayload payload);
    void apply(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload);
}
