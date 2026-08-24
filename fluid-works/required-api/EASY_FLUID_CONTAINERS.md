# Easy fluid containers

The new API keeps one item per bucket or bottle design and stores the current
fluid in UtilityAPI's existing `stored_fluid` data component. Fluid color is
rendered by tinting the model's overlay layer, so a mod does not need a separate
item and texture for every fluid.

## Easy bucket

Register the bucket and its reusable material during your mod initializer:

```java
EasyBucketMaterial copper = EasyBucketMaterial.of(
        Identifier.of("example", "copper"),
        Identifier.of("example", "item/copper_bucket_base"),
        Identifier.of("example", "item/bucket_fluid_overlay")
);

public static final Item COPPER_FLUID_BUCKET = UtilityApiRegistries.registerEasyBucket(
        Identifier.of("example", "copper_fluid_bucket"),
        copper,
        new Item.Settings()
);
```

An empty easy bucket picks up source-fluid blocks. A filled easy bucket places
the stored fluid and becomes the same empty bucket. Source attributes and the
existing droplet/component system are preserved.

## Easy bottle

```java
public static final Item JUICE_BOTTLE = UtilityApiRegistries.registerEasyBottle(
        Identifier.of("example", "juice_bottle"),
        new Item.Settings().maxCount(16),
        FluidContainerSizes.BOTTLE_DROPLETS,
        false,
        1,
        true
);
```

The final three arguments control potion-liquid support, number of sips, and
whether an empty vanilla glass bottle is returned after the last sip.

## Hook an existing custom item

The item does not have to subclass a UtilityAPI class. Registering it opts its
stacks into the same data component, capacity, transfer helpers, filters, and
client overlay tinting:

```java
UtilityApiRegistries.registerCustomFluidContainerItem(
        Identifier.of("example", "canteen_hook"),
        ModItems.CANTEEN,
        FluidContainerDefinition.custom(
                FluidUnits.BUCKET_DROPLETS * 2,
                false,
                1,
                fluidId -> !fluidId.equals(Identifier.ofVanilla("lava"))
        )
);
```

Custom-item hooks provide storage/transfer and rendering integration. The
custom item's own `use`, `useOnBlock`, or GUI code decides how players interact
with it and should call `FluidItemComponentHelper` for reads and writes.

## Overlay model

Every easy item needs a normal item model in the addon's namespace. Layer 0 is
the untinted shell and layer 1 is a white or grayscale fluid mask:

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "example:item/copper_bucket_base",
    "layer1": "example:item/bucket_fluid_overlay"
  }
}
```

For Minecraft 1.21.11, also provide the item-definition wrapper:

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "example:item/copper_fluid_bucket"
  }
}
```

Put that second file at `assets/example/items/copper_fluid_bucket.json`.
