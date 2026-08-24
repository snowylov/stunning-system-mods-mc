package dev.liquidfabric.api.unofficial.core;

public final class FluidUnits {
    public static final long DROPLETS_PER_BUCKET = 81_000L;
    public static final long MILLIBUCKETS_PER_BUCKET = 1_000L;
    public static final long DROPLETS_PER_MB = 81L;
    public static final long BUCKET_DROPLETS = DROPLETS_PER_BUCKET;

    private FluidUnits() {}

    public static long mbToDroplets(long mb) {
        return Math.max(0, mb) * DROPLETS_PER_MB;
    }

    public static long dropletsToMb(long droplets) {
        return Math.max(0, droplets) / DROPLETS_PER_MB;
    }

    public static long toMillibuckets(long droplets) {
        return dropletsToMb(droplets);
    }

    public static double dropletsToBuckets(long droplets) {
        return Math.max(0, droplets) / (double) DROPLETS_PER_BUCKET;
    }

    public static long bucketsToDroplets(long buckets) {
        return Math.max(0, buckets) * DROPLETS_PER_BUCKET;
    }
}
