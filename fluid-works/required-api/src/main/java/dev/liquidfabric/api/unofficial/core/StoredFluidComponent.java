package dev.liquidfabric.api.unofficial.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Identifier;

public record StoredFluidComponent(Identifier liquidId, long amountDroplets, SourceFluidAttributes sourceAttributes) {
    public static final StoredFluidComponent EMPTY = new StoredFluidComponent(Identifier.of("minecraft", "empty"), 0, SourceFluidAttributes.EMPTY);

    public static final Codec<StoredFluidComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("liquid_id").forGetter(StoredFluidComponent::liquidId),
            Codec.LONG.fieldOf("amount_droplets").forGetter(StoredFluidComponent::amountDroplets),
            SourceFluidAttributes.CODEC.optionalFieldOf("source_attributes", SourceFluidAttributes.EMPTY).forGetter(StoredFluidComponent::sourceAttributes)
    ).apply(instance, StoredFluidComponent::new));

    public boolean isEmpty() {
        return amountDroplets <= 0 || liquidId.equals(Identifier.of("minecraft", "empty"));
    }

    public StoredFluidComponent clamped(long capacityDroplets) {
        if (isEmpty()) return EMPTY;
        return new StoredFluidComponent(liquidId, Math.min(Math.max(0, amountDroplets), capacityDroplets), sourceAttributes);
    }
}
