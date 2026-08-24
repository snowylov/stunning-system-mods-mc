package dev.liquidfabric.api.unofficial.core;

public final class FluidContainerSizes {
    public static final long NEEDLE_DROPLETS = 8_100L;
    public static final long SYRINGE_DROPLETS = 8_100L;
    public static final long GLASS_TEST_TUBE_DROPLETS = 8_100L;
    public static final long TRIPLE_VIAL_DROPLETS = FluidUnits.mbToDroplets(750);
    public static final long SQUARE_GLASS_DROPLETS = FluidUnits.mbToDroplets(222);
    public static final long FANCY_GLASS_DROPLETS = FluidUnits.mbToDroplets(250);
    public static final long BOTTLE_DROPLETS = FluidUnits.mbToDroplets(250);
    public static final long BOWL_DROPLETS = FluidUnits.mbToDroplets(250);
    public static final long STACKABLE_TANK_DROPLETS = FluidUnits.bucketsToDroplets(16);
    public static final long IRON_DRUM_DROPLETS = FluidUnits.bucketsToDroplets(256);
    public static final long GOLD_DRUM_DROPLETS = FluidUnits.bucketsToDroplets(1024);
    public static final long DIAMOND_DRUM_DROPLETS = FluidUnits.bucketsToDroplets(4096);
    public static final long OBSIDIAN_DRUM_DROPLETS = FluidUnits.bucketsToDroplets(16384);
    private FluidContainerSizes() {}
}
