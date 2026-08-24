package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.core.UtilityTooltipTexts;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import dev.liquidfabric.api.unofficial.api.tooltip.FluidTooltipRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Shared item tooltip builder for UtilityAPI fluid items.
 *
 * The caller decides whether the user is holding the "more info" key.  Keeping
 * the shift decision outside this class avoids hard references to client-only
 * Screen classes in common item code.
 */
public final class FluidItemTooltipHelper {
    private FluidItemTooltipHelper() {}

    public static void appendShiftGate(ItemStack stack, List<Text> tooltip, boolean expanded) {
        StoredFluidComponent fluid = FluidItemComponentHelper.get(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.fluid_empty").formatted(Formatting.GRAY));
            return;
        }
        if (!expanded) {
            tooltip.add(UtilityTooltipTexts.holdShift().copy().formatted(Formatting.DARK_GRAY));
            return;
        }
        appendStoredFluidTooltip(fluid, FluidItemComponentHelper.capacity(stack), tooltip);
    }

    public static void appendStoredFluidTooltip(StoredFluidComponent fluid, long capacityDroplets, List<Text> tooltip) {
        if (fluid == null || fluid.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.fluid_empty").formatted(Formatting.GRAY));
            return;
        }

        long mb = FluidUnits.dropletsToMb(fluid.amountDroplets());
        long bucketDroplets = FluidUnits.BUCKET_DROPLETS;
        double buckets = bucketDroplets == 0 ? 0.0D : (double) fluid.amountDroplets() / (double) bucketDroplets;

        tooltip.add(Text.translatable("tooltip.utilityapi.liquid_id", fluid.liquidId().toString()).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.utilityapi.amount_mb", mb).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.utilityapi.amount_buckets", String.format(java.util.Locale.ROOT, "%.3f", buckets)).formatted(Formatting.GRAY));
        if (capacityDroplets > 0) {
            tooltip.add(Text.translatable("tooltip.utilityapi.capacity_mb", FluidUnits.dropletsToMb(capacityDroplets)).formatted(Formatting.DARK_GRAY));
        }
        appendSourceAttributes(fluid.sourceAttributes(), tooltip);
        FluidTooltipRegistry.append(ItemStack.EMPTY, fluid, tooltip);
    }

    public static void appendSourceAttributes(SourceFluidAttributes attributes, List<Text> tooltip) {
        if (attributes == null || attributes.equals(SourceFluidAttributes.EMPTY)) {
            tooltip.add(Text.translatable("tooltip.utilityapi.source_unknown").formatted(Formatting.DARK_GRAY));
            return;
        }
        if (attributes.notFromOcean()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.source.not_from_ocean").formatted(Formatting.BLUE));
        }
        if (attributes.fromCave()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.source.from_cave").formatted(Formatting.DARK_PURPLE));
        }
    }

    public static void appendNeedlePayloadTooltip(NeedlePayload payload, List<Text> tooltip, boolean expanded) {
        if (payload == null || payload.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.fluid_empty").formatted(Formatting.GRAY));
            return;
        }
        if (!expanded) {
            tooltip.add(UtilityTooltipTexts.holdShift().copy().formatted(Formatting.DARK_GRAY));
            return;
        }

        tooltip.add(Text.translatable("tooltip.utilityapi.liquid_id", payload.liquidId().toString()).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.utilityapi.amount_mb", FluidUnits.dropletsToMb(payload.amountDroplets())).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.utilityapi.needle.glowstone", payload.glowstoneBoostLevel()).formatted(Formatting.YELLOW));
        tooltip.add(Text.translatable("tooltip.utilityapi.needle.redstone", payload.redstoneBoostLevel()).formatted(Formatting.RED));
        appendSourceAttributes(payload.sourceAttributes(), tooltip);
        if (!payload.potionEffects().isEmpty()) {
            tooltip.add(Text.translatable("tooltip.utilityapi.needle.effects", payload.potionEffects().size()).formatted(Formatting.LIGHT_PURPLE));
        }
    }

    public static void appendSimpleLiquidId(Identifier id, List<Text> tooltip) {
        tooltip.add(Text.translatable("tooltip.utilityapi.liquid_id", id.toString()).formatted(Formatting.AQUA));
    }

    public static void appendModelPending(List<Text> tooltip) {
        tooltip.add(Text.translatable("tooltip.utilityapi.model_pending").formatted(Formatting.GOLD));
    }

    public static boolean shouldShowExpanded(net.minecraft.item.tooltip.TooltipType type) {
        // Vanilla exposes advanced tooltips everywhere; client-side code may
        // replace this with a real shift-state bridge.  This keeps common code
        // server-safe and still gives F3+H/advanced users full diagnostics.
        return type != null && type.isAdvanced();
    }
}
