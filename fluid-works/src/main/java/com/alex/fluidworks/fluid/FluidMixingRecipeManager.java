package com.alex.fluidworks.fluid;

import com.alex.fluidworks.FluidWorks;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Reloadable unordered two-fluid recipes loaded from data packs under fluid_mixing/*.json. */
public final class FluidMixingRecipeManager {
    private static volatile Map<Key, Identifier> recipes = Map.of();

    private FluidMixingRecipeManager() { }

    public static void initialize() {
        ResourceLoader.get(ResourceType.SERVER_DATA).registerReloader(
            FluidWorks.id("fluid_mixing_recipes"), (SynchronousResourceReloader) manager -> {
                    Map<Key, Identifier> loaded = new HashMap<>();
                    for (Map.Entry<Identifier, Resource> entry : manager.findResources("fluid_mixing",
                        id -> id.getPath().endsWith(".json")).entrySet()) {
                        try (Reader reader = entry.getValue().getReader()) {
                            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                            var inputs = json.getAsJsonArray("inputs");
                            if (inputs == null || inputs.size() != 2) {
                                throw new IllegalArgumentException("inputs must contain exactly two fluid ids");
                            }
                            Identifier first = requiredId(inputs.get(0), "inputs[0]");
                            Identifier second = requiredId(inputs.get(1), "inputs[1]");
                            Identifier result = requiredId(json.get("result"), "result");
                            requireFluid(first); requireFluid(second); requireFluid(result);
                            loaded.put(Key.of(first, second), result);
                        } catch (Exception exception) {
                            FluidWorks.LOGGER.error("Invalid fluid mixing recipe {}", entry.getKey(), exception);
                        }
                    }
                    recipes = Map.copyOf(loaded);
                    FluidWorks.LOGGER.info("Loaded {} JSON fluid mixing recipes", recipes.size());
            });
    }

    public static boolean tryMix(World world, BlockPos pos) {
        if (world.isClient()) return false;
        Fluid ownFluid = world.getFluidState(pos).getFluid();
        Identifier ownId = canonicalId(ownFluid);
        if (ownId == null) return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.offset(direction);
            Fluid neighborFluid = world.getFluidState(neighborPos).getFluid();
            Identifier neighborId = canonicalId(neighborFluid);
            if (neighborId == null) continue;
            Identifier resultId = recipes.get(Key.of(ownId, neighborId));
            if (resultId == null) continue;
            Fluid result = Registries.FLUID.get(resultId);
            if (!(result instanceof FlowableFluid flowable)) continue;
            BlockState resultState = flowable.getStill(false).getBlockState();
            world.setBlockState(pos, resultState, Block.NOTIFY_ALL);
            world.setBlockState(neighborPos, resultState, Block.NOTIFY_ALL);
            return true;
        }
        return false;
    }

    private static Identifier requiredId(JsonElement element, String field) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException(field + " must be a fluid identifier");
        }
        Identifier id = Identifier.tryParse(element.getAsString());
        if (id == null) throw new IllegalArgumentException("Invalid fluid id in " + field);
        return canonicalId(id);
    }

    private static void requireFluid(Identifier id) {
        if (!Registries.FLUID.containsId(id)) {
            throw new IllegalArgumentException("Unknown fluid " + id);
        }
    }

    private static Identifier canonicalId(Fluid fluid) {
        if (fluid == null) return null;
        return canonicalId(Registries.FLUID.getId(fluid));
    }

    private static Identifier canonicalId(Identifier id) {
        if (id == null) return null;
        String path = id.getPath();
        return path.startsWith("flowing_")
            ? Identifier.of(id.getNamespace(), path.substring("flowing_".length())) : id;
    }

    private record Key(String first, String second) {
        static Key of(Identifier a, Identifier b) {
            String first = a.toString();
            String second = b.toString();
            return first.compareTo(second) <= 0 ? new Key(first, second) : new Key(second, first);
        }
    }
}
