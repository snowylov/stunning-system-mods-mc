package dev.liquidfabric.api.unofficial.helper.item;

import dev.liquidfabric.api.unofficial.core.FluidContainerSizes;
import dev.liquidfabric.api.unofficial.core.ModComponents;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.needle.NeedleEffectRegistry;
import dev.liquidfabric.api.unofficial.needle.NeedlePayload;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * Shared helper for needle/syringe payload read/write, bars, stack rules, and
 * target application.  This keeps projectile/item code small.
 */
public final class NeedlePayloadItemHelper {
    private NeedlePayloadItemHelper() {}

    public static NeedlePayload get(ItemStack stack) {
        return stack.getOrDefault(ModComponents.NEEDLE_PAYLOAD, NeedlePayload.EMPTY);
    }

    public static ItemStack set(ItemStack stack, NeedlePayload payload) {
        stack.set(ModComponents.NEEDLE_PAYLOAD, payload == null ? NeedlePayload.EMPTY : payload);
        return stack;
    }

    public static ItemStack clear(ItemStack stack) {
        stack.set(ModComponents.NEEDLE_PAYLOAD, NeedlePayload.EMPTY);
        return stack;
    }

    public static boolean hasPayload(ItemStack stack) {
        return !get(stack).isEmpty();
    }

    public static int itemBarStep(ItemStack stack, long capacityDroplets) {
        NeedlePayload payload = get(stack);
        if (payload.isEmpty() || capacityDroplets <= 0) return 0;
        return Math.min(13, Math.max(1, Math.round(13.0f * payload.amountDroplets() / capacityDroplets)));
    }

    public static boolean itemBarVisible(ItemStack stack) {
        return hasPayload(stack);
    }

    public static NeedlePayload fromStoredFluid(StoredFluidComponent fluid, List<net.minecraft.entity.effect.StatusEffectInstance> effects, int glowstone, int redstone) {
        if (fluid == null || fluid.isEmpty()) return NeedlePayload.EMPTY;
        return new NeedlePayload(
                fluid.liquidId(),
                Math.min(fluid.amountDroplets(), FluidContainerSizes.NEEDLE_DROPLETS),
                effects == null ? List.of() : effects,
                Math.max(0, Math.min(4, glowstone)),
                Math.max(0, Math.min(4, redstone)),
                fluid.sourceAttributes()
        );
    }

    public static NeedlePayload copyWithAmount(NeedlePayload payload, long amountDroplets) {
        if (payload == null || payload.isEmpty() || amountDroplets <= 0) return NeedlePayload.EMPTY;
        return new NeedlePayload(
                payload.liquidId(),
                Math.min(payload.amountDroplets(), amountDroplets),
                payload.potionEffects(),
                payload.glowstoneBoostLevel(),
                payload.redstoneBoostLevel(),
                payload.sourceAttributes()
        );
    }

    public static boolean payloadEquals(ItemStack a, ItemStack b) {
        return get(a).equals(get(b));
    }

    public static boolean filledNeedlesCanStack(ItemStack a, ItemStack b) {
        NeedlePayload pa = get(a);
        NeedlePayload pb = get(b);
        if (pa.isEmpty() && pb.isEmpty()) return true;
        return pa.equals(pb);
    }

    public static boolean applyToTarget(ServerWorld world, LivingEntity target, LivingEntity attacker, ItemStack sourceStack, boolean consumePayload) {
        NeedlePayload payload = get(sourceStack);
        if (payload.isEmpty()) return false;
        NeedleEffectRegistry.apply(world, target, attacker, payload);
        if (consumePayload && attacker instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            clear(sourceStack);
        }
        return true;
    }

    public static boolean isLiquid(ItemStack stack, Identifier id) {
        NeedlePayload payload = get(stack);
        return !payload.isEmpty() && payload.liquidId().equals(id);
    }
}
