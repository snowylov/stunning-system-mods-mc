package dev.liquidfabric.api.unofficial.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Small raised flat mesa cap generator.
 *
 * This is intentionally biome-local and chunk-local. It does not replace the
 * global chunk generator and does not force-load chunks.
 */
public class FlatMesaPlateauFeature extends Feature<DefaultFeatureConfig> {
    public FlatMesaPlateauFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();

        int radius = 25 + random.nextInt(26); // 50-100 block diameter.
        int topY = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, origin.getX(), origin.getZ()) + 6 + random.nextInt(5);
        int baseY = topY - 7 - random.nextInt(5);

        boolean placed = false;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double normalized = Math.sqrt(dx * dx + dz * dz) / (double) radius;
                if (normalized > 1.0D) continue;

                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;

                // Flat on top with rougher sides so it reads as a raised basin/mesa.
                int columnTop = topY;
                if (normalized > 0.82D) {
                    columnTop -= random.nextInt(3);
                }

                int localGround = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z);
                int fillBottom = Math.min(baseY, localGround - 2);

                for (int y = fillBottom; y <= columnTop; y++) {
                    BlockState state;
                    int depth = columnTop - y;
                    if (depth == 0) {
                        state = Blocks.RED_SAND.getDefaultState();
                    } else if (depth <= 2) {
                        state = (random.nextBoolean() ? Blocks.RED_TERRACOTTA : Blocks.ORANGE_TERRACOTTA).getDefaultState();
                    } else {
                        state = Blocks.TERRACOTTA.getDefaultState();
                    }

                    world.setBlockState(new BlockPos(x, y, z), state, 3);
                    placed = true;
                }
            }
        }

        return placed;
    }
}
