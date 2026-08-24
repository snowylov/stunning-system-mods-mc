package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;

public final class ModStatusEffects {
    public static RegistryEntry<StatusEffect> SOUL_PROTECTED;

    private ModStatusEffects() {}

    public static void register() {
        SOUL_PROTECTED = Registry.registerReference(Registries.STATUS_EFFECT, UtilityApiMod.id("soul_protected"), new SoulProtectedStatusEffect());
    }
}
