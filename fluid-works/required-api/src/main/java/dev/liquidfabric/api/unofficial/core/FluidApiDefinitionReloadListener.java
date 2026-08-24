package dev.liquidfabric.api.unofficial.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerDefinition;
import dev.liquidfabric.api.unofficial.api.block.BlockFluidContainerRegistry;
import dev.liquidfabric.api.unofficial.api.block.FluidloggingRegistry;
import dev.liquidfabric.api.unofficial.api.bucket.UniversalBucketRegistry;
import dev.liquidfabric.api.unofficial.api.container.CustomFluidContainerItemRegistry;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerDefinition;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import dev.liquidfabric.api.unofficial.compat.StewApiCompatibility;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Loads additive hooks for already-registered items, blocks, and fluids. */
public final class FluidApiDefinitionReloadListener implements SimpleSynchronousResourceReloadListener {
    public static final Identifier ID = UtilityApiMod.id("fluid_api_definitions");
    private static final Gson GSON = new Gson();

    private final Set<Identifier> loadedContainerHooks = new HashSet<>();
    private final Set<Identifier> loadedBucketHooks = new HashSet<>();
    private final Set<Identifier> loadedBlockContainerHooks = new HashSet<>();
    private final Set<Identifier> loadedFluidloggingHooks = new HashSet<>();

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        clearPreviousDataEntries();
        loadContainers(manager);
        loadUniversalBuckets(manager);
        loadBlockContainers(manager);
        loadFluidlogging(manager);
        StewApiCompatibility.reload(manager);
    }

    private void loadContainers(ResourceManager manager) {
        forEachJson(manager, "utilityapi/containers", (resourceId, json) -> {
            Identifier hookId = definitionId(resourceId, "utilityapi/containers");
            Identifier itemId = requiredId(json, "item");
            Item item = Registries.ITEM.get(itemId);
            requireRegistryMatch(itemId, Registries.ITEM.getId(item), "item");

            long capacity = capacity(json);
            boolean potionAllowed = bool(json, "potion_liquids_allowed", false);
            int tintIndex = integer(json, "fluid_overlay_tint_index", 1);
            FluidContainerDefinition definition = FluidContainerDefinition.custom(
                    capacity, potionAllowed, tintIndex, fluidFilter(json));
            CustomFluidContainerItemRegistry.register(hookId, item, definition);
            loadedContainerHooks.add(hookId);
        });
    }

    private void loadUniversalBuckets(ResourceManager manager) {
        forEachJson(manager, "utilityapi/universal_buckets", (resourceId, json) -> {
            Identifier hookId = definitionId(resourceId, "utilityapi/universal_buckets");
            Identifier fluidId = requiredId(json, "fluid");
            Identifier itemId = requiredId(json, "bucket_item");
            Fluid fluid = Registries.FLUID.get(fluidId);
            Item item = Registries.ITEM.get(itemId);
            requireRegistryMatch(fluidId, Registries.FLUID.getId(fluid), "fluid");
            requireRegistryMatch(itemId, Registries.ITEM.getId(item), "bucket item");
            if (item == Items.AIR) throw new IllegalArgumentException("bucket_item cannot be minecraft:air");
            if (FluidItemComponentHelper.capacity(new ItemStack(item)) <= 0) {
                throw new IllegalArgumentException("bucket_item must implement FluidContainerItem or have a containers JSON hook");
            }

            UniversalBucketRegistry.register(hookId, fluidId, item, integer(json, "priority", 0));
            loadedBucketHooks.add(hookId);
        });
    }

    private void loadBlockContainers(ResourceManager manager) {
        forEachJson(manager, "utilityapi/fluid_container_blocks", (resourceId, json) -> {
            Identifier hookId = definitionId(resourceId, "utilityapi/fluid_container_blocks");
            Identifier blockId = requiredId(json, "block");
            Block block = Registries.BLOCK.get(blockId);
            requireRegistryMatch(blockId, Registries.BLOCK.getId(block), "block");

            JsonObject boundsJson = json.has("render_bounds") ? json.getAsJsonObject("render_bounds") : new JsonObject();
            BlockFluidContainerDefinition.Bounds bounds = new BlockFluidContainerDefinition.Bounds(
                    decimal(boundsJson, "min_x", 0.001f), decimal(boundsJson, "min_y", 0.001f),
                    decimal(boundsJson, "min_z", 0.001f), decimal(boundsJson, "max_x", 0.999f),
                    decimal(boundsJson, "max_y", 0.999f), decimal(boundsJson, "max_z", 0.999f));
            BlockFluidContainerRegistry.register(hookId, block,
                    new BlockFluidContainerDefinition(capacity(json), bounds, fluidFilter(json)));
            loadedBlockContainerHooks.add(hookId);
        });
    }

    private void loadFluidlogging(ResourceManager manager) {
        forEachJson(manager, "utilityapi/fluidloggable_blocks", (resourceId, json) -> {
            Identifier hookId = definitionId(resourceId, "utilityapi/fluidloggable_blocks");
            Identifier blockId = requiredId(json, "block");
            Identifier fluidId = requiredId(json, "fluid");
            Block block = Registries.BLOCK.get(blockId);
            Fluid fluid = Registries.FLUID.get(fluidId);
            requireRegistryMatch(blockId, Registries.BLOCK.getId(block), "block");
            requireRegistryMatch(fluidId, Registries.FLUID.getId(fluid), "fluid");
            if (fluid == Fluids.EMPTY) throw new IllegalArgumentException("fluid cannot be minecraft:empty");

            String property = string(json, "property", "fluidlogged");
            boolean hasProperty = block.getDefaultState().getProperties().stream()
                    .anyMatch(candidate -> candidate.getName().equals(property)
                            && candidate instanceof net.minecraft.state.property.BooleanProperty);
            if (!hasProperty) throw new IllegalArgumentException("block is missing boolean property '" + property + "'");

            FluidloggingRegistry.register(hookId, block, fluid, property);
            loadedFluidloggingHooks.add(hookId);
        });
    }

    private void clearPreviousDataEntries() {
        loadedContainerHooks.forEach(CustomFluidContainerItemRegistry::unregister);
        loadedBucketHooks.forEach(UniversalBucketRegistry::unregister);
        loadedBlockContainerHooks.forEach(BlockFluidContainerRegistry::unregister);
        loadedFluidloggingHooks.forEach(FluidloggingRegistry::unregister);
        loadedContainerHooks.clear();
        loadedBucketHooks.clear();
        loadedBlockContainerHooks.clear();
        loadedFluidloggingHooks.clear();
    }

    private static void forEachJson(ResourceManager manager, String directory, JsonConsumer consumer) {
        Map<Identifier, Resource> resources = manager.findResources(directory, id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (!parsed.isJsonObject()) throw new IllegalArgumentException("root must be an object");
                consumer.accept(entry.getKey(), parsed.getAsJsonObject());
            } catch (Exception exception) {
                UtilityApiMod.LOGGER.warn("Skipped invalid {} definition {}: {}", directory, entry.getKey(), exception.getMessage());
            }
        }
    }

    private static Predicate<Identifier> fluidFilter(JsonObject json) {
        Set<Identifier> allowed = idSet(json, "allowed_fluids");
        Set<Identifier> denied = idSet(json, "denied_fluids");
        return id -> (allowed.isEmpty() || allowed.contains(id)) && !denied.contains(id);
    }

    private static Set<Identifier> idSet(JsonObject json, String field) {
        Set<Identifier> ids = new LinkedHashSet<>();
        if (!json.has(field)) return ids;
        JsonArray values = json.getAsJsonArray(field);
        for (JsonElement value : values) ids.add(Identifier.of(value.getAsString()));
        return Set.copyOf(ids);
    }

    private static long capacity(JsonObject json) {
        if (json.has("capacity_droplets")) return json.get("capacity_droplets").getAsLong();
        if (json.has("capacity_mb")) return FluidUnits.mbToDroplets(json.get("capacity_mb").getAsLong());
        throw new IllegalArgumentException("capacity_droplets or capacity_mb is required");
    }

    private static Identifier definitionId(Identifier resourceId, String directory) {
        String path = resourceId.getPath();
        String prefix = directory + "/";
        return Identifier.of(resourceId.getNamespace(), path.substring(prefix.length(), path.length() - 5));
    }

    private static Identifier requiredId(JsonObject json, String field) {
        if (!json.has(field)) throw new IllegalArgumentException("missing '" + field + "'");
        return Identifier.of(json.get(field).getAsString());
    }

    private static void requireRegistryMatch(Identifier requested, Identifier resolved, String type) {
        if (!requested.equals(resolved)) throw new IllegalArgumentException("unknown " + type + " " + requested);
    }

    private static boolean bool(JsonObject json, String field, boolean fallback) {
        return json.has(field) ? json.get(field).getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String field, int fallback) {
        return json.has(field) ? json.get(field).getAsInt() : fallback;
    }

    private static float decimal(JsonObject json, String field, float fallback) {
        return json.has(field) ? json.get(field).getAsFloat() : fallback;
    }

    private static String string(JsonObject json, String field, String fallback) {
        return json.has(field) ? json.get(field).getAsString() : fallback;
    }

    @FunctionalInterface
    private interface JsonConsumer {
        void accept(Identifier resourceId, JsonObject json) throws Exception;
    }
}
