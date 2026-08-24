# Liquid Fabric API quick start

This API is deliberately additive. It does not replace vanilla registries,
bucket items, fluid handlers, or other mods' blocks. JSON files attach behavior
to objects that were already registered during mod initialization; Minecraft
does not permit datapacks to create normal item/block registry entries after
the registry freeze.

## Directory map

| Feature | Resource path |
| --- | --- |
| Container behavior | `data/<namespace>/utilityapi/containers/<name>.json` |
| Universal bucket mapping | `data/<namespace>/utilityapi/universal_buckets/<name>.json` |
| Fluid container block metadata | `data/<namespace>/utilityapi/fluid_container_blocks/<name>.json` |
| Fluidlogging mapping | `data/<namespace>/utilityapi/fluidloggable_blocks/<name>.json` |
| Fluidlogging client mirror | `assets/<namespace>/utilityapi/fluidloggable_blocks/<name>.json` |
| Fluid renderer | `assets/<namespace>/utilityapi/fluid_renderers/<name>.json` |
| Container overlay renderer | `assets/<namespace>/utilityapi/container_renderers/<name>.json` |
| Stew API / liquid bridge | `data/<namespace>/stew_bowls/<name>.json` |

Copy-ready files are in `examples/`, and JSON Schemas are in `schemas/`.

## Stew API and cooking mods

One `stew_bowls` descriptor can be consumed by both Stew API 1.0 and this API.
Liquid Fabric adds optional liquid IDs, amount, priority, colors, and a reusable
overlay-bowl item while preserving Stew API's original three required fields.
See `STEW_API_COMPATIBILITY.md` and the copy-ready copper-bowl example. The
integration is soft: either mod works independently, code-registered Stew API
bindings are discovered reflectively, and suspicious-stew automation is off by
default to avoid losing its per-stack effects.

## Universal bucket

Register one overlay bucket item in Java:

```java
EasyBucketMaterial copper = EasyBucketMaterial.of(
        Identifier.of("example", "copper"),
        Identifier.of("example", "item/copper_bucket_base"),
        Identifier.of("example", "item/bucket_fluid_overlay")
);

Item copperBucket = UtilityApiRegistries.registerEasyBucket(
        Identifier.of("example", "copper_fluid_bucket"),
        copper,
        new Item.Settings()
);
```

Then map fluids to it with `utilityapi/universal_buckets/*.json`. Explicit JSON
mappings work without enabling the global unknown-fluid fallback. A fluid that
already owns a normal bucket remains untouched unless a higher-priority explicit
mapping targets it.

## Existing custom container item

Register the item normally, then add both files:

- `data/example/utilityapi/containers/canteen.json` for capacity and filtering.
- `assets/example/utilityapi/container_renderers/canteen.json` for client tinting.

The item model uses the untinted shell as layer 0 and a white/grayscale fluid
mask as layer 1. Java registration is also available through
`registerCustomFluidContainerItem`.

## Fluid container blocks

Implement the small storage interface on the block entity:

```java
public final class GlassTankBlockEntity extends BlockEntity
        implements FluidStorageBlockEntity {
    private final UtilityFluidStorage storage = new UtilityFluidStorage(this, 81_000L * 16L);

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return storage;
    }
}
```

Expose it through Fabric Transfer API during initialization:

```java
UtilityApiRegistries.registerFluidStorage(
        ModBlockEntities.GLASS_TANK,
        (tank, side) -> tank.liquidFabricStorage()
);
```

The block JSON supplies capacity, accepted fluids, and normalized render bounds.
Code can discover it through `BlockFluidContainerRegistry`. Storage remains owned
by the block entity, so saves, transactions, sided access, and multiplayer sync
stay compatible with Fabric Transfer API.

## Rendering a custom fluid

Put a client JSON in `assets/<namespace>/utilityapi/fluid_renderers/`. It registers
the still/flowing pair, texture identifiers, tint, and translucent render layer.
The renderer applies to normal fluid blocks and to fluidlogged blocks whose
`getFluidState` exposes that fluid.

The same hook is available in client Java:

```java
UtilityFluidRenderRegistry.register(
        ModFluids.HONEY,
        ModFluids.FLOWING_HONEY,
        Identifier.of("example", "block/honey_still"),
        Identifier.of("example", "block/honey_flow"),
        0xFFAA00
);
```

## Fluidlogging any chosen liquid

The simplest code-only block is:

```java
Block honeyGrate = UtilityApiRegistries.registerBlockWithItem(
        Identifier.of("example", "honey_grate"),
        new EasyFluidloggableBlock(
                AbstractBlock.Settings.copy(Blocks.IRON_BARS).nonOpaque(),
                () -> ModFluids.HONEY
        ),
        new Item.Settings()
);
```

For JSON selection, register `JsonFluidloggableBlock` through
`registerJsonFluidloggableBlock`, then add matching `fluidloggable_blocks` JSON
files under both `data` and `assets`. The data copy owns server fill/drain rules;
the asset copy lets a multiplayer client expose and render the same fluid state.
Bucket fill/drain uses vanilla `FluidFillable` and `FluidDrainable`, and the
block's fluid state uses the fluid's normal renderer and tick rate.

### Engine limitation

A block-state-only implementation can safely store one selected fluid in one
boolean property. Storing an arbitrary fluid ID per placed block requires a
block entity or invasive mixins because block-state properties have fixed values.
For true multi-fluid blocks, use `FluidStorageBlockEntity` and Transfer API
storage instead. This API intentionally does not patch every block globally.

## Compatibility rules

- Runtime callbacks return `PASS` unless a config or explicit JSON rule matches.
- Fluids with their own bucket behavior win unless explicitly overridden.
- No global registry replacement and no required mixins.
- All data reload errors are isolated to the bad file and logged with its ID.
- JSON filters support `allowed_fluids` and `denied_fluids`.
- Block storage uses Fabric transactions and sided lookup hooks.
