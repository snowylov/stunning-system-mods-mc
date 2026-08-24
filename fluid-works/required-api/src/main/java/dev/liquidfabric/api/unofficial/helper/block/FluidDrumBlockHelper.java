package dev.liquidfabric.api.unofficial.helper.block;

import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.tank.drum.DrumMode;
import dev.liquidfabric.api.unofficial.tank.drum.FluidDrumBlockEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Drum mode, locking, comparator, and tooltip helpers.
 */
public final class FluidDrumBlockHelper {
    private FluidDrumBlockHelper() {}

    public static int comparatorOutput(FluidDrumBlockEntity drum) {
        return drum == null ? 0 : FluidStorageBlockHelper.comparatorOutput(drum.fluidStorage);
    }

    public static boolean canLock(DrumMode mode) {
        return mode != DrumMode.CREATIVE && mode != DrumMode.VOID;
    }

    public static Text lockMessage(boolean locked) {
        return Text.translatable(locked ? "message.utilityapi.drum_locked" : "message.utilityapi.drum_unlocked");
    }

    public static float lockSoundPitch(boolean locked) {
        return locked ? 1.2F : 0.8F;
    }

    public static void appendDrumTooltip(long capacityDroplets, DrumMode mode, boolean locked, List<Text> tooltip, boolean expanded) {
        if (!expanded) {
            tooltip.add(Text.translatable("tooltip.utilityapi.hold_shift").formatted(Formatting.DARK_GRAY));
            return;
        }

        tooltip.add(Text.translatable("tooltip.utilityapi.capacity_mb", FluidUnits.dropletsToMb(capacityDroplets)).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.utilityapi.capacity_buckets", capacityDroplets / FluidUnits.BUCKET_DROPLETS).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.utilityapi.drum.mode", mode.name().toLowerCase(java.util.Locale.ROOT)).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable(locked ? "tooltip.utilityapi.drum.locked" : "tooltip.utilityapi.drum.unlocked").formatted(locked ? Formatting.YELLOW : Formatting.DARK_GRAY));

        if (mode == DrumMode.VOID) {
            tooltip.add(Text.translatable("tooltip.utilityapi.drum.void_warning").formatted(Formatting.RED));
        } else if (mode == DrumMode.CREATIVE) {
            tooltip.add(Text.translatable("tooltip.utilityapi.drum.creative_warning").formatted(Formatting.LIGHT_PURPLE));
        }
    }

    public static boolean acceptsInsertMode(DrumMode mode) {
        return mode != null;
    }

    public static boolean destroysInsertedFluid(DrumMode mode) {
        return mode == DrumMode.VOID;
    }

    public static boolean infiniteExtract(DrumMode mode) {
        return mode == DrumMode.CREATIVE;
    }

    public static long displayedCapacity(long capacityDroplets, DrumMode mode) {
        return mode == DrumMode.CREATIVE ? Long.MAX_VALUE / 4 : capacityDroplets;
    }
}
