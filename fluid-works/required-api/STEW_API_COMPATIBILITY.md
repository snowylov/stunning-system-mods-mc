# Stew API compatibility

Stew API 1.0.0 is an optional integration, not a hard dependency. Liquid Fabric
API 1.3 reads the same `data/<namespace>/stew_bowls/*.json` files and mirrors
Stew API's code-registered bowl families through reflection when `stew_api` is
installed. No Stew API classes are linked while it is absent.

The supplied Stew API requires Minecraft 1.21.11, Loader 0.18.4, Java 21, and
Fabric API 0.141.6+1.21.11. This project now uses that same baseline.

## One descriptor for both APIs

The first three fields are Stew API's native format. The remaining fields are
optional Liquid Fabric extensions that Stew API safely ignores:

```json
{
  "bowl": "example:copper_bowl",
  "mushroom_stew": "example:copper_bowl_mushroom_stew",
  "suspicious_stew": "example:copper_bowl_suspicious_stew",
  "mushroom_liquid": "example:mushroom_stew",
  "suspicious_liquid": "example:suspicious_stew",
  "amount_mb": 250,
  "universal_overlay_bowl": "example:universal_copper_stew_bowl",
  "fluid_overlay_tint_index": 1,
  "automatic_suspicious_transfer": false,
  "priority": 10
}
```

If the liquid IDs are omitted, the filled item IDs are used. If the amount is
omitted, one bowl is 250 mB. The optional universal item is registered as a
component-backed fluid container that accepts the two declared stew liquids.
Give that item the normal two-layer model and a matching
`assets/<namespace>/utilityapi/container_renderers/*.json` file so layer 1 is
tinted by its current stew liquid.

`StewFluidBindingRegistry` provides non-mutating lookup and conversion helpers:

- `findContents(stack)` describes one filled bowl as a `StoredFluidComponent`.
- `emptyContainer(stack)` resolves its matching custom bowl.
- `fillOne(bowl, fluid, allowUnsafeSuspiciousTransfer)` resolves the filled item.
- `toUniversalOverlayBowl(stack)` and `fromUniversalOverlayBowl(...)` bridge
  discrete Stew API items to the reusable overlay item.

Inventory, tank, pipe, and recipe integrations should perform their own
transaction and only replace a stack after the matching amount was accepted.
Suspicious-stew automation is disabled by default because converting it to a
plain logical liquid cannot preserve per-stack suspicious-stew effects. An
integration must opt in explicitly after preserving those components itself.

## Code registration

Mods can use the bridge without Stew API:

```java
UtilityApiRegistries.registerStewFluidBinding(
        Identifier.of("example", "copper_bowl"),
        ModItems.COPPER_BOWL,
        ModItems.COPPER_MUSHROOM_STEW,
        ModItems.COPPER_SUSPICIOUS_STEW,
        Identifier.of("example", "mushroom_stew"),
        Identifier.of("example", "suspicious_stew"),
        FluidContainerSizes.BOWL_DROPLETS,
        ModItems.UNIVERSAL_COPPER_STEW_BOWL,
        false,
        10
);
```

For the conventional 250 mB mapping with liquid IDs inferred from the two
filled items, the four-argument overload is enough:

```java
UtilityApiRegistries.registerStewFluidBinding(
        Identifier.of("example", "copper_bowl"),
        ModItems.COPPER_BOWL,
        ModItems.COPPER_MUSHROOM_STEW,
        ModItems.COPPER_SUSPICIOUS_STEW
);
```

If another mod registers with `StewApi.registerBowl` after datapack reload,
call `StewApiCompatibility.refreshFromInstalledApi()` once after that
registration. The bridge uses registry IDs, soft-fails an incompatible Stew API
version, and never bundles or shadows the standalone Stew API JAR.

Common item tags `c:bowls` and `c:stews` are supplied with additive vanilla
defaults so recipe and food mods can share tag-based behavior.
