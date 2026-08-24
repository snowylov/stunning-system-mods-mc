package dev.liquidfabric.api.unofficial.client;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.LiquidRegistry;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import dev.liquidfabric.api.unofficial.tank.drum.FluidDrumBlockItem;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class UtilityShiftTooltips {
    private UtilityShiftTooltips() {}

    public static void registerClient() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!Screen.hasShiftDown()) return;

            StoredFluidComponent stored = stack.getOrDefault(ModComponents.STORED_FLUID, StoredFluidComponent.EMPTY);
            if (!stored.isEmpty()) {
                addStoredFluid(lines, stored);
            }

            NeedlePayload payload = stack.getOrDefault(ModComponents.NEEDLE_PAYLOAD, NeedlePayload.EMPTY);
            if (!payload.isEmpty()) {
                lines.add(Text.translatable("tooltip.utilityapi.liquid", payload.liquidId().toString()));
                lines.add(Text.translatable("tooltip.utilityapi.amount_mb", FluidUnits.dropletsToMb(payload.amountDroplets())));
                lines.add(Text.translatable("tooltip.utilityapi.payload"));
                if (payload.glowstoneBoostLevel() > 0) {
                    lines.add(Text.translatable("tooltip.utilityapi.glowstone_boost", payload.glowstoneBoostLevel()));
                }
                if (payload.redstoneBoostLevel() > 0) {
                    lines.add(Text.translatable("tooltip.utilityapi.redstone_boost", payload.redstoneBoostLevel()));
                }
                addSource(lines, payload.sourceAttributes());
            }

            if (stack.getItem() instanceof FluidDrumBlockItem drum) {
                lines.add(Text.translatable("tooltip.utilityapi.capacity_mb_buckets",
                        FluidUnits.dropletsToMb(drum.capacityDroplets()),
                        FluidUnits.dropletsToBuckets(drum.capacityDroplets())));
                lines.add(Text.translatable("tooltip.utilityapi.mode", drum.mode().name().toLowerCase()));
                if (drum.mode().name().equals("VOID")) {
                    lines.add(Text.translatable("tooltip.utilityapi.void_warning"));
                }
                if (drum.mode().name().equals("CREATIVE")) {
                    lines.add(Text.translatable("tooltip.utilityapi.creative_warning"));
                }
            }

            if (stack.isOf(dev.liquidfabric.api.unofficial.map.ModUtilityMaps.FLAT_MESA_EXPLORER_MAP)) {
                lines.add(Text.translatable("tooltip.utilityapi.flat_mesa_map_1"));
                lines.add(Text.translatable("tooltip.utilityapi.flat_mesa_map_2"));
            }
        });
    }

    private static void addStoredFluid(java.util.List<Text> lines, StoredFluidComponent stored) {
        String liquidName = LiquidRegistry.get(stored.liquidId())
                .map(type -> type.id().toString())
                .orElse(stored.liquidId().toString());

        lines.add(Text.translatable("tooltip.utilityapi.liquid", liquidName));
        lines.add(Text.translatable("tooltip.utilityapi.amount_mb", FluidUnits.dropletsToMb(stored.amountDroplets())));
        lines.add(Text.translatable("tooltip.utilityapi.amount_buckets", FluidUnits.dropletsToBuckets(stored.amountDroplets())));
        addSource(lines, stored.sourceAttributes());
    }

    private static void addSource(java.util.List<Text> lines, SourceFluidAttributes attributes) {
        if (attributes.notFromOcean()) {
            lines.add(Text.translatable("tooltip.utilityapi.source.not_from_ocean"));
        }
        if (attributes.fromCave()) {
            lines.add(Text.translatable("tooltip.utilityapi.source.from_cave"));
        }
    }
}
