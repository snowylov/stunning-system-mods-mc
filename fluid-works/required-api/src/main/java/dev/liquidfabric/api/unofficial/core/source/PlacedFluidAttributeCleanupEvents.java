package dev.liquidfabric.api.unofficial.core.source;

import dev.liquidfabric.api.unofficial.core.LiquidFabricConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class PlacedFluidAttributeCleanupEvents {
    private static boolean registered;
    private static int tickCounter;

    private PlacedFluidAttributeCleanupEvents() {}

    public static void register() {
        if (registered) return;
        registered = true;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!LiquidFabricConfig.enablePlacedFluidAttributeCleanup) return;
            if (++tickCounter < 200) return;
            tickCounter = 0;
            for (var world : server.getWorlds()) {
                PlacedFluidAttributeTracker.cleanup(world);
            }
        });
    }
}
