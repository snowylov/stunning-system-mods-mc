package dev.liquidfabric.api.unofficial.compat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.api.stew.StewFluidBindingRegistry;
import dev.liquidfabric.api.unofficial.api.container.CustomFluidContainerItemRegistry;
import dev.liquidfabric.api.unofficial.api.container.FluidContainerDefinition;
import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.LiquidRegistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Optional Stew API 1.x bridge. All foreign calls are reflection-isolated. */
public final class StewApiCompatibility {
    private static final Gson GSON = new Gson();
    private static final Set<Identifier> MANAGED_BINDINGS = new HashSet<>();
    private static final Set<Identifier> MANAGED_CONTAINERS = new HashSet<>();
    private static final int DEFAULT_MUSHROOM_COLOR = 0x8B5A36;
    private static final int DEFAULT_SUSPICIOUS_COLOR = 0xB58A45;

    private StewApiCompatibility() {}

    /** Reloads shared stew_bowls JSON and then mirrors code-registered Stew API bindings. */
    public static void reload(ResourceManager manager) {
        MANAGED_BINDINGS.forEach(StewFluidBindingRegistry::unregister);
        MANAGED_CONTAINERS.forEach(CustomFluidContainerItemRegistry::unregister);
        MANAGED_BINDINGS.clear();
        MANAGED_CONTAINERS.clear();
        loadSharedDescriptors(manager);
        refreshFromInstalledApi();
    }

    /**
     * Re-scans Stew API's public registry. Addons that register late may call this
     * once after their StewApi.registerBowl invocation.
     */
    public static void refreshFromInstalledApi() {
        if (!ModCompatibility.isLoaded("stew_api")) return;
        ModCompatibility.runOptional("stew_api_registry_bridge", () -> {
            try {
                Class<?> apiClass = Class.forName("com.alex.stewapi.api.StewApi", false,
                        StewApiCompatibility.class.getClassLoader());
                Method getAllBindings = apiClass.getMethod("getAllBindings");
                Object result = getAllBindings.invoke(null);
                if (!(result instanceof Iterable<?> bindings)) return;
                for (Object foreignBinding : bindings) registerReflected(foreignBinding);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Unsupported Stew API registry shape", exception);
            }
        });
    }

    private static void loadSharedDescriptors(ResourceManager manager) {
        Map<Identifier, Resource> resources = manager.findResources("stew_bowls",
                id -> id.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
                if (!parsed.isJsonObject()) throw new IllegalArgumentException("root must be an object");
                JsonObject json = parsed.getAsJsonObject();
                Identifier id = definitionId(entry.getKey());
                Item bowl = requiredItem(json, "bowl");
                Item mushroom = requiredItem(json, "mushroom_stew");
                Item suspicious = requiredItem(json, "suspicious_stew");
                Identifier mushroomLiquid = optionalId(json, "mushroom_liquid", Registries.ITEM.getId(mushroom));
                Identifier suspiciousLiquid = optionalId(json, "suspicious_liquid", Registries.ITEM.getId(suspicious));
                long amount = json.has("amount_droplets") ? json.get("amount_droplets").getAsLong()
                        : json.has("amount_mb") ? FluidUnits.mbToDroplets(json.get("amount_mb").getAsLong())
                        : FluidContainerSizes.BOWL_DROPLETS;
                int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
                boolean automaticSuspicious = json.has("automatic_suspicious_transfer")
                        && json.get("automatic_suspicious_transfer").getAsBoolean();
                Item universalBowl = json.has("universal_overlay_bowl")
                        ? requiredItem(json, "universal_overlay_bowl") : null;
                registerManaged(id, bowl, mushroom, suspicious, mushroomLiquid, suspiciousLiquid, amount,
                        universalBowl, automaticSuspicious, priority,
                        color(json, "mushroom_color", DEFAULT_MUSHROOM_COLOR),
                        color(json, "suspicious_color", DEFAULT_SUSPICIOUS_COLOR));
                if (universalBowl != null) {
                    Identifier containerId = Identifier.of(id.getNamespace(), id.getPath() + "/overlay_container");
                    int tintIndex = json.has("fluid_overlay_tint_index")
                            ? json.get("fluid_overlay_tint_index").getAsInt() : 1;
                    CustomFluidContainerItemRegistry.register(containerId, universalBowl,
                            FluidContainerDefinition.custom(amount, false, tintIndex,
                                    fluidId -> fluidId.equals(mushroomLiquid) || fluidId.equals(suspiciousLiquid)));
                    MANAGED_CONTAINERS.add(containerId);
                }
            } catch (Exception exception) {
                UtilityApiMod.LOGGER.warn("Skipped invalid stew bridge {}: {}", entry.getKey(), exception.getMessage());
            }
        }
    }

    private static void registerReflected(Object foreignBinding) throws ReflectiveOperationException {
        Class<?> type = foreignBinding.getClass();
        Item bowl = (Item) type.getMethod("bowl").invoke(foreignBinding);
        Item mushroom = (Item) type.getMethod("mushroomStew").invoke(foreignBinding);
        Item suspicious = (Item) type.getMethod("suspiciousStew").invoke(foreignBinding);
        Identifier bowlId = Registries.ITEM.getId(bowl);
        Identifier id = Identifier.of("stew_api", "code/" + bowlId.getNamespace() + "/" + bowlId.getPath());

        // Shared JSON wins over the inferred reflection defaults for the same bowl.
        boolean alreadyCovered = StewFluidBindingRegistry.values().stream()
                .anyMatch(binding -> binding.bowl() == bowl);
        if (alreadyCovered) return;

        registerManaged(id, bowl, mushroom, suspicious, Registries.ITEM.getId(mushroom),
                Registries.ITEM.getId(suspicious), FluidContainerSizes.BOWL_DROPLETS,
                null, false, -100, DEFAULT_MUSHROOM_COLOR, DEFAULT_SUSPICIOUS_COLOR);
    }

    private static void registerManaged(Identifier id, Item bowl, Item mushroom, Item suspicious,
                                        Identifier mushroomLiquid, Identifier suspiciousLiquid, long amount,
                                        Item universalBowl, boolean automaticSuspicious, int priority,
                                        int mushroomColor, int suspiciousColor) {
        StewFluidBindingRegistry.register(id, bowl, mushroom, suspicious, mushroomLiquid, suspiciousLiquid,
                amount, universalBowl, automaticSuspicious, priority);
        MANAGED_BINDINGS.add(id);
        if (LiquidRegistry.get(mushroomLiquid).isEmpty()) {
            LiquidRegistry.register(mushroomLiquid, mushroomColor, true, false, true);
        }
        if (LiquidRegistry.get(suspiciousLiquid).isEmpty()) {
            LiquidRegistry.register(suspiciousLiquid, suspiciousColor, true, true, false);
        }
    }

    private static Item requiredItem(JsonObject json, String field) {
        if (!json.has(field)) throw new IllegalArgumentException("missing '" + field + "'");
        Identifier id = Identifier.of(json.get(field).getAsString());
        Item item = Registries.ITEM.get(id);
        if (!id.equals(Registries.ITEM.getId(item))) throw new IllegalArgumentException("unknown item " + id);
        return item;
    }

    private static Identifier optionalId(JsonObject json, String field, Identifier fallback) {
        return json.has(field) ? Identifier.of(json.get(field).getAsString()) : fallback;
    }

    private static int color(JsonObject json, String field, int fallback) {
        return json.has(field) ? json.get(field).getAsInt() : fallback;
    }

    private static Identifier definitionId(Identifier resourceId) {
        String path = resourceId.getPath();
        return Identifier.of(resourceId.getNamespace(),
                "stew/" + path.substring("stew_bowls/".length(), path.length() - 5));
    }
}
