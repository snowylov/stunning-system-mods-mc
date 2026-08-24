package com.alex.fluidworks.fluid;

import com.alex.fluidworks.ExpandedContent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.fluid.Fluid;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative contact behavior for ender and cryogenic liquids. */
public final class SpecialFluidEffects {
    private static final int TELEPORT_RADIUS = 512;
    private static final long TELEPORT_COOLDOWN = 100;
    private static final Map<UUID, Long> LAST_TELEPORT = new HashMap<>();

    private SpecialFluidEffects() { }

    public static void initialize() {
        ServerTickEvents.END_WORLD_TICK.register(SpecialFluidEffects::tickWorld);
    }

    private static void tickWorld(ServerWorld world) {
        long time = world.getTime();
        if ((time & 3L) != 0L) return;
        for (ServerPlayerEntity player : world.getPlayers()) {
            Fluid fluid = world.getFluidState(player.getBlockPos()).getFluid();
            boolean ender = ExpandedContent.LIQUID_ENDER.matches(fluid)
                || ExpandedContent.CRYOGEN.matches(fluid);
            if (!ender || time - LAST_TELEPORT.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2)
                < TELEPORT_COOLDOWN) continue;
            int x = player.getBlockX() + world.random.nextBetween(-TELEPORT_RADIUS, TELEPORT_RADIUS);
            int z = player.getBlockZ() + world.random.nextBetween(-TELEPORT_RADIUS, TELEPORT_RADIUS);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            player.requestTeleport(x + 0.5D, y + 1.0D, z + 0.5D);
            LAST_TELEPORT.put(player.getUuid(), time);
        }
    }
}
