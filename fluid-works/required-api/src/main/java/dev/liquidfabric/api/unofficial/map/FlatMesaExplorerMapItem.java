package dev.liquidfabric.api.unofficial.map;

import dev.liquidfabric.api.unofficial.worldgen.ModWorldgen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

import java.util.List;

/**
 * Lightweight explorer-map token for Flat Mesa.
 *
 * It is intentionally a normal item rather than replacing vanilla filled-map
 * internals. Modpacks can later bridge it to vanilla MapState or an external
 * mapping mod. The item tells players it comes from Badlands mineshafts and
 * points them toward badlands-adjacent Flat Mesa terrain.
 */
public class FlatMesaExplorerMapItem extends Item {
    public FlatMesaExplorerMapItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, net.minecraft.util.Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            // Keep this safe if a pack removes/overrides the biome. Exact locate-map
            // conversion can be added later once MapState target-marker API is locked.
            user.sendMessage(Text.translatable("message.utilityapi.flat_mesa_map_hint"), true);
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.utilityapi.hold_shift"));
    }
}
