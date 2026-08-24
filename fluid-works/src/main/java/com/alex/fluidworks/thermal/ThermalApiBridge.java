package com.alex.fluidworks.thermal;

import com.alex.fluidworks.FluidWorks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Reflection boundary for the optional Temperature API. Fluid Works remains fully
 * loadable without the API while using its native item/block/fluid temperatures
 * whenever {@code temperature_api} is installed.
 */
public final class ThermalApiBridge {
    public static final int STANDARD = 100;
    public static final int HOT = 10_000;
    public static final int COLD = -10_000;

    private static Method getItem;
    private static Method setItem;
    private static Method getBlock;
    private static Method setBlock;
    private static Method registerFluid;
    private static Constructor<?> profileConstructor;
    private static boolean available;

    private ThermalApiBridge() { }

    public static void initialize() {
        if (!FabricLoader.getInstance().isModLoaded("temperature_api")) {
            FluidWorks.LOGGER.info("Temperature API not installed; thermal content will retain local pipe temperatures");
            return;
        }
        try {
            Class<?> api = Class.forName("com.snowylov.temperatureapi.TemperatureApi");
            Class<?> registries = Class.forName("com.snowylov.temperatureapi.TemperatureRegistries");
            Class<?> profile = Class.forName("com.snowylov.temperatureapi.api.TemperatureProfile");
            getItem = api.getMethod("getItemTemperature", ItemStack.class);
            setItem = api.getMethod("setItemTemperature", ItemStack.class, int.class);
            getBlock = api.getMethod("getBlockTemperature", World.class, BlockPos.class);
            setBlock = api.getMethod("setBlockTemperature", ServerWorld.class, BlockPos.class, int.class);
            registerFluid = registries.getMethod("registerFluid", Fluid.class, profile);
            profileConstructor = profile.getConstructor(int.class, float.class, float.class,
                float.class, boolean.class, boolean.class);
            available = true;
            registerCommonFluids();
            FluidWorks.LOGGER.info("Temperature API compatibility active for #c:hot_liquids and #c:cold_liquids");
        } catch (ReflectiveOperationException exception) {
            available = false;
            FluidWorks.LOGGER.warn("Temperature API was present but its 1.0.0 contract could not be linked", exception);
        }
    }

    public static boolean isAvailable() {
        return available;
    }

    public static int getItemTemperature(ItemStack stack) {
        if (!available || stack.isEmpty()) return STANDARD;
        try {
            return (int) getItem.invoke(null, stack);
        } catch (ReflectiveOperationException exception) {
            return STANDARD;
        }
    }

    public static void setItemTemperature(ItemStack stack, int temperature) {
        if (!available || stack.isEmpty()) return;
        try {
            setItem.invoke(null, stack, temperature);
        } catch (ReflectiveOperationException exception) {
            FluidWorks.LOGGER.debug("Could not set item temperature", exception);
        }
    }

    public static int getBlockTemperature(World world, BlockPos pos) {
        if (!available) return STANDARD;
        try {
            return (int) getBlock.invoke(null, world, pos);
        } catch (ReflectiveOperationException exception) {
            return STANDARD;
        }
    }

    public static void setBlockTemperature(ServerWorld world, BlockPos pos, int temperature) {
        if (!available) return;
        try {
            setBlock.invoke(null, world, pos, temperature);
        } catch (ReflectiveOperationException exception) {
            FluidWorks.LOGGER.debug("Could not set block temperature", exception);
        }
    }

    private static void registerCommonFluids() throws ReflectiveOperationException {
        Object hotProfile = profileConstructor.newInstance(HOT, 8.0F, 0.9F, 0.3F, true, true);
        Object coldProfile = profileConstructor.newInstance(COLD, 8.0F, 0.75F, 0.1F, false, true);
        registerIds(hotProfile, List.of(
            Identifier.of("minecraft", "lava"),
            Identifier.of("minecraft", "flowing_lava"),
            FluidWorks.id("liquid_iron"), FluidWorks.id("flowing_liquid_iron"),
            FluidWorks.id("liquid_copper"), FluidWorks.id("flowing_liquid_copper"),
            FluidWorks.id("liquid_gold"), FluidWorks.id("flowing_liquid_gold")));
        registerIds(coldProfile, List.of(
            FluidWorks.id("liquid_nitrogen"), FluidWorks.id("flowing_liquid_nitrogen"),
            FluidWorks.id("cryogen"), FluidWorks.id("flowing_cryogen"),
            FluidWorks.id("liquid_cryogen"), FluidWorks.id("flowing_liquid_cryogen")));
    }

    private static void registerIds(Object profile, List<Identifier> ids) throws ReflectiveOperationException {
        for (Identifier id : ids) {
            Fluid fluid = Registries.FLUID.get(id);
            if (fluid != null && Registries.FLUID.getId(fluid).equals(id)) {
                registerFluid.invoke(null, fluid, profile);
            }
        }
    }
}
