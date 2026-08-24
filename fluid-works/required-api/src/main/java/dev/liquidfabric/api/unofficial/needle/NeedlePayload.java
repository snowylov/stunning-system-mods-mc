package dev.liquidfabric.api.unofficial.needle;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;

import java.util.List;

public record NeedlePayload(
        Identifier liquidId,
        long amountDroplets,
        List<StatusEffectInstance> potionEffects,
        int glowstoneBoostLevel,
        int redstoneBoostLevel,
        SourceFluidAttributes sourceAttributes
) {
    public static final NeedlePayload EMPTY = new NeedlePayload(Identifier.of("minecraft", "empty"), 0, List.of(), 0, 0, SourceFluidAttributes.EMPTY);

    public static final Codec<NeedlePayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("liquid_id").forGetter(NeedlePayload::liquidId),
            Codec.LONG.fieldOf("amount_droplets").forGetter(NeedlePayload::amountDroplets),
            StatusEffectInstance.CODEC.listOf().optionalFieldOf("potion_effects", List.of()).forGetter(NeedlePayload::potionEffects),
            Codec.intRange(0, 4).optionalFieldOf("glowstone_boost_level", 0).forGetter(NeedlePayload::glowstoneBoostLevel),
            Codec.intRange(0, 4).optionalFieldOf("redstone_boost_level", 0).forGetter(NeedlePayload::redstoneBoostLevel),
            SourceFluidAttributes.CODEC.optionalFieldOf("source_attributes", SourceFluidAttributes.EMPTY).forGetter(NeedlePayload::sourceAttributes)
    ).apply(instance, NeedlePayload::new));

    public boolean isEmpty() {
        return amountDroplets <= 0 || liquidId.equals(Identifier.of("minecraft", "empty"));
    }
}
