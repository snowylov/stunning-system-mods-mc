package dev.liquidfabric.api.unofficial.api.container;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import net.minecraft.util.Identifier;

import java.util.Objects;

/**
 * Reusable visual/capacity definition for easy buckets.
 * Addon item models should use {@code baseTexture} as layer0 and
 * {@code fluidOverlayTexture} as layer1.
 */
public record EasyBucketMaterial(
        Identifier id,
        Identifier baseTexture,
        Identifier fluidOverlayTexture,
        long capacityDroplets
) {
    public EasyBucketMaterial {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseTexture, "baseTexture");
        Objects.requireNonNull(fluidOverlayTexture, "fluidOverlayTexture");
        if (capacityDroplets <= 0) throw new IllegalArgumentException("capacityDroplets must be positive");
    }

    public static EasyBucketMaterial of(Identifier id, Identifier baseTexture, Identifier fluidOverlayTexture) {
        return new EasyBucketMaterial(id, baseTexture, fluidOverlayTexture, FluidUnits.BUCKET_DROPLETS);
    }
}
