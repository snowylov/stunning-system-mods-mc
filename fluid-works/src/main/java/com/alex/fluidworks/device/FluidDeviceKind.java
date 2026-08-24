package com.alex.fluidworks.device;

import dev.liquidfabric.api.unofficial.core.FluidUnits;

/** Gameplay configuration for the shared directional fluid-device runtime. */
public enum FluidDeviceKind {
    SPRINKLER("fluid_sprinkler", 4, 250, 20, ShapeFamily.NOZZLE),
    VACUUM_DRAIN("vacuum_drain", 8, 1_000, 5, ShapeFamily.INTAKE),
    FLUID_CANNON("fluid_cannon", 8, 1_000, 1, ShapeFamily.CANNON),
    SPILL_TRAY("spill_tray", 4, 1_000, 5, ShapeFamily.TRAY),
    PRESSURE_SENSOR("pressure_sensor", 1, 0, 10, ShapeFamily.SENSOR),
    EMERGENCY_SHUTOFF("emergency_shutoff", 2, 1_000, 1, ShapeFamily.VALVE),
    SAMPLING_VALVE("sampling_valve", 1, 100, 1, ShapeFamily.VALVE),
    FLUID_ROUTER("fluid_router", 4, 1_000, 1, ShapeFamily.ROUTER),
    HEAT_EXCHANGER("heat_exchanger", 8, 500, 1, ShapeFamily.EXCHANGER),
    FLUID_SEPARATOR("fluid_separator", 8, 500, 1, ShapeFamily.ROUTER),
    MIST_NOZZLE("mist_nozzle", 2, 25, 10, ShapeFamily.NOZZLE),
    DRAIN_GRATE("drain_grate", 8, 1_000, 5, ShapeFamily.GRATE),
    PIPE_COVER("pipe_cover", 1, 250, 1, ShapeFamily.COVER),
    FLUID_TRAP("fluid_trap", 4, 1_000, 1, ShapeFamily.TRAP),
    REMOTE_TANK_LINK("remote_tank_link", 4, 1_000, 1, ShapeFamily.LINK);

    private final String id;
    private final long capacity;
    private final long operationAmount;
    private final int interval;
    private final ShapeFamily shapeFamily;

    FluidDeviceKind(String id, long capacityBuckets, long operationAmountMb, int interval,
                    ShapeFamily shapeFamily) {
        this.id = id;
        this.capacity = FluidUnits.bucketsToDroplets(capacityBuckets);
        this.operationAmount = FluidUnits.mbToDroplets(operationAmountMb);
        this.interval = interval;
        this.shapeFamily = shapeFamily;
    }

    public String id() {
        return id;
    }

    public long capacity() {
        return capacity;
    }

    public long operationAmount() {
        return operationAmount;
    }

    public int interval() {
        return interval;
    }

    public ShapeFamily shapeFamily() {
        return shapeFamily;
    }

    public enum ShapeFamily {
        NOZZLE, INTAKE, CANNON, TRAY, SENSOR, VALVE, ROUTER, EXCHANGER, GRATE, COVER, TRAP, LINK
    }
}
