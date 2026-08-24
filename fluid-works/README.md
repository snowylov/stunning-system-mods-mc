# Fluid Works

Fabric 1.21.11 fluid machinery mod with independent fluid-container and optional temperature API integration.

## Imported APIs

- `libs/fabric-fluid-api-1.3.0.jar` — required at compile time and runtime; mod ID `liquid-fabric-api-unofficial-fabric-api`.
- `libs/temperature-api-1.0.0-dev.jar` — available to development runs; Fluid Works discovers mod ID `temperature_api` through its reflection bridge and remains loadable without it.

The API JARs remain separate Fabric mods and are not shaded into the Fluid Works output.

Fluid Works is a Fabric 1.21.11 industrial fluid-storage mod. It requires the
independent **Fabric Fluid API Easy Containers 1.3.0** mod at compile time and
runtime. The dependency is not embedded or shaded into Fluid Works.

## Content

- Tank: 16 buckets, full-cube industrial tank.
- Reservoir Tank: 12 mB, 14-pixel-wide retained portable tank.
- Fuel Cell: 500 mB portable API container with a fluid-tinted overlay.
- Double Smelted Glass and Reservoir Window.
- Copper, iron, gold, and netherite reservoir casings, controllers, and valves.
- Hollow 3×3×3 through 11×11×11 multiblock reservoirs.
- Observation Window and persistent Fluid Label attachments.
- Bucket and Bottle Dispensers with four-bucket internal buffers.
- Copper, gold, diamond, and netherite universal one-bucket containers.
- Custom 250 mB glass bottle with a fluid-colored overlay.
- BuildCraft-style thin Fluid Pipes with one-bucket buffers and toggle/redstone modes.
- Redstone Extraction Pipe: powered extraction from its back face to its output face.
- High-Pressure Pipe: 1,000 mB/t transfer and a four-bucket safety buffer.
- Meter Pipe: reports mB/t and emits comparator strength for measured flow.
- Overflow Valve: transfers only while its input storage is at least 75% full.
- Pulse Valve: moves exactly 250 mB on each rising redstone edge.
- Priority Junction: sends fluid toward its placed facing before other outputs.
- Fluid Diode: one-way back-to-front transfer.
- Filter Pipe: copies a filter from any filled API container; sneak-use clears it.
- Mixing Junction: four-bucket same-fluid manifold and foundation for recipe-driven mixing.
- Universal Fluid Splash Potion: 250 mB throwable container that places a five-block flowing splash.
- Universal Fluid Lingering Potion: 250 mB throwable container whose flowing liquid lasts 10 seconds.
- Fluid Sprinkler: draws from its rear and distributes flowing liquid across a five-block fan.
- Vacuum Drain: collects source and flowing blocks in a five-block-wide intake volume.
- Fluid Cannon: spends one bucket on a redstone pulse to launch a flowing block up to eight blocks.
- Spill Tray and Drain Grate: directional collectors for fluid blocks touching their intake face.
- Pressure Sensor: comparator output mirrors the fullness of the storage behind it.
- Emergency Shutoff: transfers back-to-front normally and closes immediately while powered.
- Sampling Valve: moves exactly 100 mB from back to front on each redstone pulse.
- Fluid Router: buffers fluid from the rear and distributes it round-robin with facing priority.
- Heat Exchanger: 500 mB/t directional dual-coil transfer foundation for temperature-aware fluids.
- Fluid Separator: copies a fluid filter from a filled container and routes only that fluid.
- Mist Nozzle: consumes 25 mB per spray and emits a visible fluid mist.
- Pipe Cover: compact 250 mB/t directional pass-through with a framed cosmetic shape.
- Fluid Trap: releases one bucket of flowing liquid when powered or when an entity enters its face.
- Remote Tank Link: while powered, sends fluid to a facing link up to 64 loaded blocks away.

## Fluid-device controls

- Place a device against the face it should point toward; all devices support six directions.
- Use a device with an empty hand to enable or disable it.
- Sneak-use a device to read its fluid, stored amount, and most recent operation amount.
- Use any filled API-compatible container on a Fluid Separator to copy its filter.
- Fluid Cannons and Sampling Valves activate on rising redstone edges.
- Remote Tank Links transfer only while powered and never force-load unloaded chunks.

## Pipe controls

- Use a pipe with an empty hand to enable or disable it.
- Sneak-use ordinary pipes to cycle always/high-signal/low-signal modes.
- Place directional pipes against the face they should output toward.
- Use a filled fluid container on a Filter Pipe to select that fluid.
- Sneak-use a Filter Pipe with an empty hand to clear its filter.
- Use a Meter Pipe to read its rolling 20-tick flow rate.

## Build (only when explicitly requested)

Place `fabric-fluid-api-1.3.0.jar` in `libs/`, then run:

```bash
./gradlew build
```

For a fast source check that deliberately does not produce release JARs:

```bash
./gradlew fastCompile
```

The repository workflow builds the independent API first. With GitHub CLI:

```bash
gh workflow run build.yml -f export_jars=false
gh run watch
```

Release JARs are opt-in: run `gh workflow run build.yml -f export_jars=true`
only when an export is wanted.
