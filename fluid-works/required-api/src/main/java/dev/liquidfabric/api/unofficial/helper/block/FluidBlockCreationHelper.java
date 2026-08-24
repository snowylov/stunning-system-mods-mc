package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.FluidBlock;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registry and construction helpers for placeable fluid families.
 *
 * New fluids should use this as the shared pattern:
 * still fluid -> flowing fluid -> fluid block -> bucket item -> tags/assets.
 */
public final class FluidBlockCreationHelper {
    private FluidBlockCreationHelper() {}

    public record RegisteredFluidFamily(
            Identifier stillId,
            Identifier flowingId,
            Identifier blockId,
            Identifier bucketId,
            FlowableFluid still,
            FlowableFluid flowing,
            FluidBlock block,
            BucketItem bucket
    ) {}

    public static <T extends FlowableFluid> T registerFluid(String path, T fluid) {
        return Registry.register(Registries.FLUID, UtilityApiMod.id(path), fluid);
    }

    public static <T extends FlowableFluid> T registerFluid(Identifier id, T fluid) {
        return Registry.register(Registries.FLUID, id, fluid);
    }

    public static FluidBlock registerFluidBlock(String path, FlowableFluid still, AbstractBlock.Settings settings) {
        return Registry.register(Registries.BLOCK, UtilityApiMod.id(path), new FluidBlock(still, settings));
    }

    public static FluidBlock registerFluidBlock(Identifier id, FlowableFluid still, AbstractBlock.Settings settings) {
        return Registry.register(Registries.BLOCK, id, new FluidBlock(still, settings));
    }

    public static BucketItem registerBucket(String path, FlowableFluid still, Item.Settings settings) {
        return Registry.register(Registries.ITEM, UtilityApiMod.id(path), new BucketItem(still, settings.maxCount(1)));
    }

    public static BucketItem registerBucket(Identifier id, FlowableFluid still, Item.Settings settings) {
        return Registry.register(Registries.ITEM, id, new BucketItem(still, settings.maxCount(1)));
    }

    public static Identifier stillId(String basePath) {
        return UtilityApiMod.id(basePath);
    }

    public static Identifier flowingId(String basePath) {
        return UtilityApiMod.id("flowing_" + basePath);
    }

    public static Identifier blockId(String basePath) {
        return UtilityApiMod.id(basePath + "_block");
    }

    public static Identifier bucketId(String basePath) {
        return UtilityApiMod.id(basePath + "_bucket");
    }

    public static boolean isUtilityFluid(net.minecraft.fluid.Fluid fluid) {
        Identifier id = Registries.FLUID.getId(fluid);
        return id != null && UtilityApiMod.MOD_ID.equals(id.getNamespace());
    }

    public static boolean isUtilityFluidBlock(Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        return id != null && UtilityApiMod.MOD_ID.equals(id.getNamespace()) && block instanceof FluidBlock;
    }

    public static String modelPendingComment(String id) {
        return "MODEL_PENDING: add still/flowing textures and generated bucket model for " + id;
    }

    public static Item.Settings bucketSettings() {
        return new Item.Settings().maxCount(1);
    }
}
