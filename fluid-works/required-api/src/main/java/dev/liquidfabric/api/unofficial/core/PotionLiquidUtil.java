package dev.liquidfabric.api.unofficial.core;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PotionLiquidUtil {
    private PotionLiquidUtil() {}

    public static List<StatusEffectInstance> strengthenedShort(Collection<StatusEffectInstance> input, int baseSeconds, int redstoneLevel, int glowstoneLevel) {
        List<StatusEffectInstance> out = new ArrayList<>();
        int duration = baseSeconds * 20;
        for (int i = 0; i < Math.min(4, Math.max(0, redstoneLevel)); i++) duration *= 2;
        int amplifierBonus = 1 + Math.min(4, Math.max(0, glowstoneLevel));
        for (StatusEffectInstance effect : input) {
            RegistryEntry<StatusEffect> type = effect.getEffectType();
            out.add(new StatusEffectInstance(type, duration, effect.getAmplifier() + amplifierBonus));
        }
        return out;
    }
}
