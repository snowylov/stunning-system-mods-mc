package dev.liquidfabric.api.unofficial.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SourceFluidAttributes(boolean notFromOcean, boolean fromCave) {
    public static final SourceFluidAttributes EMPTY = new SourceFluidAttributes(false, false);

    public static final Codec<SourceFluidAttributes> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("not_from_ocean", false).forGetter(SourceFluidAttributes::notFromOcean),
            Codec.BOOL.optionalFieldOf("from_cave", false).forGetter(SourceFluidAttributes::fromCave)
    ).apply(instance, SourceFluidAttributes::new));
}
