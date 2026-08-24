package com.alex.fluidworks.pipe;

import dev.liquidfabric.api.unofficial.core.FluidUnits;

/** Defines transfer behavior without creating a separate block entity type per pipe. */
public enum PipeKind {
    STANDARD("fluid_pipe", 50, 1, PipeRedstoneMode.ALWAYS),
    REDSTONE_VALVE("redstone_fluid_valve", 50, 1, PipeRedstoneMode.SIGNAL_HIGH),
    EXTRACTION("extraction_fluid_pipe", 250, 1, PipeRedstoneMode.SIGNAL_HIGH),
    HIGH_PRESSURE("high_pressure_pipe", 1_000, 4, PipeRedstoneMode.ALWAYS),
    METER("meter_pipe", 250, 1, PipeRedstoneMode.ALWAYS),
    OVERFLOW("overflow_valve", 250, 1, PipeRedstoneMode.ALWAYS),
    PULSE("pulse_valve", 250, 1, PipeRedstoneMode.SIGNAL_HIGH),
    PRIORITY("priority_junction", 250, 1, PipeRedstoneMode.ALWAYS),
    DIODE("fluid_diode", 250, 1, PipeRedstoneMode.ALWAYS),
    FILTER("filter_pipe", 250, 1, PipeRedstoneMode.ALWAYS),
    MIXING("mixing_junction", 250, 4, PipeRedstoneMode.ALWAYS);

    private final String id;
    private final long transferRate;
    private final long bufferCapacity;
    private final PipeRedstoneMode defaultRedstoneMode;

    PipeKind(String id, long transferRateMb, long bufferBuckets, PipeRedstoneMode defaultRedstoneMode) {
        this.id = id;
        this.transferRate = FluidUnits.mbToDroplets(transferRateMb);
        this.bufferCapacity = FluidUnits.bucketsToDroplets(bufferBuckets);
        this.defaultRedstoneMode = defaultRedstoneMode;
    }

    public String id() {
        return id;
    }

    public long transferRate() {
        return transferRate;
    }

    public long bufferCapacity() {
        return bufferCapacity;
    }

    public PipeRedstoneMode defaultRedstoneMode() {
        return defaultRedstoneMode;
    }

    public boolean directional() {
        return this == EXTRACTION || this == DIODE || this == PULSE;
    }
}
