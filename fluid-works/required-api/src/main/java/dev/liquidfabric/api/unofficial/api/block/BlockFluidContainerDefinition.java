package dev.liquidfabric.api.unofficial.api.block;

import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.function.Predicate;

/** Shared capacity, filtering, and fluid-render bounds for container blocks. */
public record BlockFluidContainerDefinition(
        long capacityDroplets,
        Bounds renderBounds,
        Predicate<Identifier> fluidFilter
) {
    public BlockFluidContainerDefinition {
        if (capacityDroplets <= 0) throw new IllegalArgumentException("capacityDroplets must be positive");
        renderBounds = Objects.requireNonNull(renderBounds, "renderBounds");
        fluidFilter = Objects.requireNonNull(fluidFilter, "fluidFilter");
    }

    public boolean accepts(Identifier fluidId) {
        return fluidId != null && fluidFilter.test(fluidId);
    }

    public record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        public Bounds {
            if (minX < 0 || minY < 0 || minZ < 0 || maxX > 1 || maxY > 1 || maxZ > 1
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("Fluid render bounds must be ordered inside 0..1");
            }
        }

        public static Bounds fullBlockInset() {
            return new Bounds(0.001f, 0.001f, 0.001f, 0.999f, 0.999f, 0.999f);
        }
    }
}
