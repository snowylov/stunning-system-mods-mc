package dev.liquidfabric.api.unofficial;

import dev.liquidfabric.api.unofficial.core.ModComponents;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UtilityApiMod implements ModInitializer {
    public static final String MOD_ID = "liquid-fabric-api-unofficial-fabric-api";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        ModComponents.register();
        LOGGER.info("Easy Containers and fluid storage API initialized");
    }
}
