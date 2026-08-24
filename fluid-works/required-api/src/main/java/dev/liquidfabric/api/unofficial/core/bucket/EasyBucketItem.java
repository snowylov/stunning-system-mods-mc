package dev.liquidfabric.api.unofficial.core.bucket;

import dev.liquidfabric.api.unofficial.api.container.EasyBucketMaterial;

import java.util.Objects;

/** A universal placeable bucket backed by an {@link EasyBucketMaterial}. */
public final class EasyBucketItem extends UniversalFluidBucketItem {
    private final EasyBucketMaterial material;

    public EasyBucketItem(Settings settings, EasyBucketMaterial material) {
        super(settings);
        this.material = Objects.requireNonNull(material, "material");
    }

    public EasyBucketMaterial material() {
        return material;
    }

    @Override
    public long capacityDroplets() {
        return material.capacityDroplets();
    }
}
