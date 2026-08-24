package dev.liquidfabric.api.unofficial.core;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Datapack-driven liquid metadata loader.
 *
 * Files:
 * data/<namespace>/utilityapi/liquids/<name>.json
 *
 * Example:
 * {
 *   "id": "liquid-fabric-api-unofficial-fabric-api:hot_chocolate",
 *   "color": 10246625,
 *   "drinkable": true,
 *   "potion_like": false,
 *   "can_be_needled": true
 * }
 */
public final class LiquidDefinitionReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID = UtilityApiMod.id("liquid_definitions");
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        LiquidRegistry.bootstrapDefaults();

        Map<Identifier, net.minecraft.resource.Resource> resources =
                manager.findResources("utilityapi/liquids", path -> path.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (!parsed.isJsonObject()) continue;
                JsonObject json = parsed.getAsJsonObject();

                Identifier id = json.has("id")
                        ? Identifier.of(json.get("id").getAsString())
                        : inferId(entry.getKey());

                int color = json.has("color") ? json.get("color").getAsInt() : 0xFFFFFFFF;
                boolean drinkable = !json.has("drinkable") || json.get("drinkable").getAsBoolean();
                boolean potionLike = json.has("potion_like") && json.get("potion_like").getAsBoolean();
                boolean canBeNeedled = !json.has("can_be_needled") || json.get("can_be_needled").getAsBoolean();

                LiquidRegistry.register(id, color, drinkable, potionLike, canBeNeedled);
            } catch (Exception ignored) {
                // Bad datapack entries fail closed instead of breaking the game.
            }
        }
    }

    private static Identifier inferId(Identifier resourceId) {
        String path = resourceId.getPath();
        path = path.substring("utilityapi/liquids/".length(), path.length() - ".json".length());
        return Identifier.of(resourceId.getNamespace(), path);
    }
}
