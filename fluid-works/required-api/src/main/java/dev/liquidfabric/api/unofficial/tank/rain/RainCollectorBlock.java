package dev.liquidfabric.api.unofficial.tank.rain;

import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class RainCollectorBlock extends BlockWithEntity {
    public RainCollectorBlock(Settings settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new RainCollectorBlockEntity(pos, state);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BlockWithEntity> getCodec() {
        return createCodec(RainCollectorBlock::new);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return type == ModTanksAndPipes.RAIN_COLLECTOR_BE ? (w, p, s, be) -> ((RainCollectorBlockEntity) be).collectRainTick() : null;
    }
}
