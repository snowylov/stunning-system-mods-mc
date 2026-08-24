package dev.liquidfabric.api.unofficial.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.block.Block;
import dev.liquidfabric.api.unofficial.api.block.FluidloggingRegistry;
import dev.liquidfabric.api.unofficial.api.color.FluidColorRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Client asset JSON for fluid-state rendering and item overlay tint hooks. */
public final class FluidClientDefinitionReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID = UtilityApiMod.id("fluid_client_definitions");
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        loadFluidRenderers(manager);
        loadContainerRenderers(manager);
        loadFluidloggingRenderers(manager);
    }

    private static void loadFluidRenderers(ResourceManager manager) {
        forEachJson(manager, "utilityapi/fluid_renderers", (resourceId, json) -> {
            Identifier stillId = requiredId(json, "fluid");
            Identifier flowingId = json.has("flowing_fluid") ? requiredId(json, "flowing_fluid") : stillId;
            Fluid still = Registries.FLUID.get(stillId);
            Fluid flowing = Registries.FLUID.get(flowingId);
            requireMatch(stillId, Registries.FLUID.getId(still), "fluid");
            requireMatch(flowingId, Registries.FLUID.getId(flowing), "flowing fluid");
            int tint = json.has("tint") ? json.get("tint").getAsInt() : 0xFFFFFF;
            FluidColorRegistry.register(stillId, tint);
            FluidColorRegistry.register(flowingId, tint);
            UtilityFluidRenderRegistry.register(
                    still,
                    flowing,
                    requiredId(json, "still_texture"),
                    requiredId(json, "flowing_texture"),
                    tint
            );
        });
    }

    private static void loadContainerRenderers(ResourceManager manager) {
        forEachJson(manager, "utilityapi/container_renderers", (resourceId, json) -> {
            Identifier itemId = requiredId(json, "item");
            Item item = Registries.ITEM.get(itemId);
            requireMatch(itemId, Registries.ITEM.getId(item), "item");
            UtilityItemColorProviders.registerOverlayItem(item,
                    json.has("fluid_overlay_tint_index") ? json.get("fluid_overlay_tint_index").getAsInt() : 1);
        });
    }

    private static void loadFluidloggingRenderers(ResourceManager manager) {
        forEachJson(manager, "utilityapi/fluidloggable_blocks", (resourceId, json) -> {
            Identifier blockId = requiredId(json, "block");
            Identifier fluidId = requiredId(json, "fluid");
            Block block = Registries.BLOCK.get(blockId);
            Fluid fluid = Registries.FLUID.get(fluidId);
            requireMatch(blockId, Registries.BLOCK.getId(block), "block");
            requireMatch(fluidId, Registries.FLUID.getId(fluid), "fluid");
            String path = resourceId.getPath();
            Identifier hookId = Identifier.of(resourceId.getNamespace(),
                    "client/" + path.substring("utilityapi/fluidloggable_blocks/".length(), path.length() - 5));
            FluidloggingRegistry.register(hookId, block, fluid,
                    json.has("property") ? json.get("property").getAsString() : "fluidlogged");
        });
    }

    private static void forEachJson(ResourceManager manager, String directory, JsonConsumer consumer) {
        Map<Identifier, Resource> resources = manager.findResources(directory, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (!parsed.isJsonObject()) throw new IllegalArgumentException("root must be an object");
                consumer.accept(entry.getKey(), parsed.getAsJsonObject());
            } catch (Exception exception) {
                UtilityApiMod.LOGGER.warn("Skipped invalid client definition {}: {}", entry.getKey(), exception.getMessage());
            }
        }
    }

    private static Identifier requiredId(JsonObject json, String field) {
        if (!json.has(field)) throw new IllegalArgumentException("missing '" + field + "'");
        return Identifier.of(json.get(field).getAsString());
    }

    private static void requireMatch(Identifier requested, Identifier resolved, String type) {
        if (!requested.equals(resolved)) throw new IllegalArgumentException("unknown " + type + " " + requested);
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(Identifier resourceId, JsonObject json) throws Exception;
    }
}
