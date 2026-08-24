package dev.liquidfabric.api.unofficial.core.source;

import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Best-effort source tagging for collected world liquids.
 *
 * These are the same source flags already used by UtilityAPI liquid containers:
 * - not_from_ocean
 * - from_cave
 *
 * Vanilla fluid blocks cannot store arbitrary per-block component data, so placed
 * fluid attributes are tracked separately while the server is running. If no
 * placed-fluid metadata is known, this helper infers source flags from the world.
 */
public final class FluidSourceAttributeUtil {
    private FluidSourceAttributeUtil() {}

    public static SourceFluidAttributes resolve(World world, BlockPos pos) {
        SourceFluidAttributes tracked = PlacedFluidAttributeTracker.get(world, pos);
        return tracked != null ? tracked : infer(world, pos);
    }

    public static SourceFluidAttributes infer(World world, BlockPos pos) {
        boolean notFromOcean = true;
        try {
            notFromOcean = !world.getBiome(pos).isIn(BiomeTags.IS_OCEAN);
        } catch (Throwable ignored) {
            // Some dimensions/modded biome sources may not expose ocean tags cleanly.
            // Fail safely toward "not ocean" instead of dropping the source flag.
            notFromOcean = true;
        }

        boolean fromCave;
        try {
            fromCave = !world.isSkyVisible(pos.up());
        } catch (Throwable ignored) {
            fromCave = false;
        }

        return new SourceFluidAttributes(notFromOcean, fromCave);
    }

    public static boolean isMeaningful(SourceFluidAttributes attributes) {
        return attributes != null && (attributes.notFromOcean() || attributes.fromCave());
    }
}
