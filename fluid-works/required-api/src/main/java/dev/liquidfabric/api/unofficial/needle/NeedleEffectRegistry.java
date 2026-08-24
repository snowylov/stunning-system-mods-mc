package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.core.ModStatusEffects;
import dev.liquidfabric.api.unofficial.core.PotionLiquidUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class NeedleEffectRegistry {
    private static final List<NeedleEffectHandler> HANDLERS = new ArrayList<>();

    private NeedleEffectRegistry() {}

    public static void register(NeedleEffectHandler handler) {
        HANDLERS.add(handler);
    }

    public static void bootstrapDefaults() {
        if (!HANDLERS.isEmpty()) return;

        register(new NeedleEffectHandler() {
            @Override public boolean canHandle(NeedlePayload payload) { return !payload.potionEffects().isEmpty(); }
            @Override public void apply(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) {
                for (StatusEffectInstance effect : PotionLiquidUtil.strengthenedShort(payload.potionEffects(), 2, payload.redstoneBoostLevel(), payload.glowstoneBoostLevel())) {
                    target.addStatusEffect(effect, attacker);
                }
            }
        });

        register(new SimpleLiquidHandler(UtilityApiMod.id("milk")) {
            @Override protected void applyLiquid(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) {
                target.clearStatusEffects();
            }
        });

        register(new SimpleLiquidHandler(UtilityApiMod.id("chocolate_milk")) {
            @Override protected void applyLiquid(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) {
                clearNegativeEffects(target);
            }
        });

        register(new SimpleLiquidHandler(UtilityApiMod.id("hot_chocolate")) {
            @Override protected void applyLiquid(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) {
                clearNegativeEffects(target);
                target.addStatusEffect(new StatusEffectInstance(ModStatusEffects.SOUL_PROTECTED, 20 * 60 * 4, 0), attacker);
            }
        });
    }

    public static void apply(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) {
        if (payload == null || payload.isEmpty()) return;
        for (NeedleEffectHandler handler : HANDLERS) {
            if (handler.canHandle(payload)) {
                handler.apply(world, target, attacker, payload);
                return;
            }
        }
        // Unknown payloads intentionally do nothing besides base needle damage.
    }

    public static void clearNegativeEffects(LivingEntity target) {
        List<StatusEffectInstance> active = List.copyOf(target.getStatusEffects());
        for (StatusEffectInstance effect : active) {
            if (effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                target.removeStatusEffect(effect.getEffectType());
            }
        }
    }

    private abstract static class SimpleLiquidHandler implements NeedleEffectHandler {
        private final Identifier liquidId;
        private SimpleLiquidHandler(Identifier liquidId) { this.liquidId = liquidId; }
        @Override public boolean canHandle(NeedlePayload payload) { return liquidId.equals(payload.liquidId()); }
        @Override public void apply(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload) { applyLiquid(world, target, attacker, payload); }
        protected abstract void applyLiquid(ServerWorld world, LivingEntity target, @Nullable LivingEntity attacker, NeedlePayload payload);
    }
}
