package dev.liquidfabric.api.unofficial.tank.drum;

import dev.liquidfabric.api.unofficial.tank.ModTanksAndPipes;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

/**
 * Copper drum variant that behaves like vanilla copper in a compatible, low-touch way:
 * - unwaxed drums can randomly oxidize
 * - honeycomb converts to the waxed block of the same oxidation stage
 * - axes scrape wax first, then scrape oxidation one stage
 *
 * This avoids global vanilla replacement and keeps all behavior local to these drum blocks.
 */
public class CopperDrumBlock extends FluidDrumBlock {
    public enum Oxidation {
        UNAFFECTED,
        EXPOSED,
        WEATHERED,
        OXIDIZED;

        public Oxidation next() {
            return switch (this) {
                case UNAFFECTED -> EXPOSED;
                case EXPOSED -> WEATHERED;
                case WEATHERED, OXIDIZED -> OXIDIZED;
            };
        }

        public Oxidation previous() {
            return switch (this) {
                case UNAFFECTED, EXPOSED -> UNAFFECTED;
                case WEATHERED -> EXPOSED;
                case OXIDIZED -> WEATHERED;
            };
        }
    }

    private final Oxidation oxidation;
    private final boolean waxed;

    public CopperDrumBlock(AbstractBlock.Settings settings, long capacity, DrumMode mode, Oxidation oxidation, boolean waxed) {
        super(settings, capacity, mode);
        this.oxidation = oxidation;
        this.waxed = waxed;
    }

    public Oxidation oxidation() { return oxidation; }
    public boolean waxed() { return waxed; }

    @Override
    public boolean hasRandomTicks(BlockState state) {
        return !waxed && oxidation != Oxidation.OXIDIZED;
    }

    @Override
    protected void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!waxed && oxidation != Oxidation.OXIDIZED && random.nextFloat() < 0.05688889F) {
            Block next = ModTanksAndPipes.copperDrumFor(oxidation.next(), false);
            if (next != null) {
                world.setBlockState(pos, next.getDefaultState().with(AXIS, state.get(AXIS)), Block.NOTIFY_ALL);
            }
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        ItemStack stack = player.getMainHandStack();

        if (stack.isOf(Items.HONEYCOMB) && !waxed) {
            Block waxedBlock = ModTanksAndPipes.copperDrumFor(oxidation, true);
            if (waxedBlock != null) {
                if (!world.isClient) {
                    world.setBlockState(pos, waxedBlock.getDefaultState().with(AXIS, state.get(AXIS)), Block.NOTIFY_ALL);
                    world.syncWorldEvent(player, 3003, pos, 0);
                    if (!player.getAbilities().creativeMode) stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            }
        }

        if (stack.getItem() instanceof AxeItem) {
            Block scraped = waxed
                ? ModTanksAndPipes.copperDrumFor(oxidation, false)
                : ModTanksAndPipes.copperDrumFor(oxidation.previous(), false);

            if (scraped != null && scraped != this) {
                if (!world.isClient) {
                    world.setBlockState(pos, scraped.getDefaultState().with(AXIS, state.get(AXIS)), Block.NOTIFY_ALL);
                    world.playSound(null, pos, SoundEvents.ITEM_AXE_SCRAPE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                    if (!player.getAbilities().creativeMode) {
                        stack.damage(1, player, EquipmentSlot.MAINHAND);
                    }
                }
                return ActionResult.SUCCESS;
            }
        }

        return super.onUse(state, world, pos, player, hit);
    }

    public static AbstractBlock.Settings settingsFor(Oxidation oxidation, boolean waxed) {
        Block copy = switch (oxidation) {
            case UNAFFECTED -> waxed ? Blocks.WAXED_COPPER_BLOCK : Blocks.COPPER_BLOCK;
            case EXPOSED -> waxed ? Blocks.WAXED_EXPOSED_COPPER : Blocks.EXPOSED_COPPER;
            case WEATHERED -> waxed ? Blocks.WAXED_WEATHERED_COPPER : Blocks.WEATHERED_COPPER;
            case OXIDIZED -> waxed ? Blocks.WAXED_OXIDIZED_COPPER : Blocks.OXIDIZED_COPPER;
        };
        return AbstractBlock.Settings.copy(copy).ticksRandomly();
    }
}
