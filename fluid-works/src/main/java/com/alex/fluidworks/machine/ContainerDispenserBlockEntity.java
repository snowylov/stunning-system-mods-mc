package com.alex.fluidworks.machine;

import com.alex.fluidworks.FluidWorks;
import com.alex.fluidworks.fluid.FluidWorksStorage;
import com.alex.fluidworks.item.CustomFluidBottleItem;
import com.alex.fluidworks.item.FluidVisuals;
import dev.liquidfabric.api.unofficial.api.block.FluidStorageBlockEntity;
import dev.liquidfabric.api.unofficial.api.transfer.FluidTransferHelper;
import dev.liquidfabric.api.unofficial.core.FluidUnits;
import dev.liquidfabric.api.unofficial.core.SourceFluidAttributes;
import dev.liquidfabric.api.unofficial.core.StoredFluidComponent;
import dev.liquidfabric.api.unofficial.helper.item.FluidItemComponentHelper;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ContainerDispenserBlockEntity extends BlockEntity implements FluidStorageBlockEntity {
    public static final long CAPACITY = FluidUnits.bucketsToDroplets(4);
    private final FluidWorksStorage storage = new FluidWorksStorage(this, CAPACITY);

    public ContainerDispenserBlockEntity(BlockPos pos, BlockState state) {
        super(FluidWorks.CONTAINER_DISPENSER_BLOCK_ENTITY, pos, state);
    }

    public FluidWorksStorage storage() {
        return storage;
    }

    public ActionResult fillHeld(PlayerEntity player, Hand hand) {
        ItemStack held = player.getStackInHand(hand);
        FluidVariant variant = FluidTransferHelper.firstStoredVariant(storage).orElse(FluidVariant.blank());
        if (variant.isBlank()) return ActionResult.FAIL;

        ItemStack filled;
        long amount;
        if (held.isOf(Items.BUCKET)) {
            amount = FluidUnits.BUCKET_DROPLETS;
            Fluid fluid = variant.getFluid();
            if (fluid.getBucketItem() == Items.AIR || fluid.getBucketItem() == Items.BUCKET) {
                return ActionResult.FAIL;
            }
            filled = new ItemStack(fluid.getBucketItem());
        } else if (held.isOf(Items.GLASS_BOTTLE)) {
            if (variant.getFluid() != Fluids.WATER) return ActionResult.FAIL;
            amount = CustomFluidBottleItem.CAPACITY;
            filled = PotionContentsComponent.createStack(Items.POTION, Potions.WATER);
        } else {
            amount = FluidItemComponentHelper.capacity(held);
            if (amount <= 0 || !FluidItemComponentHelper.isEmpty(held)) return ActionResult.FAIL;
            filled = held.copyWithCount(1);
            Identifier fluidId = Registries.FLUID.getId(variant.getFluid());
            FluidItemComponentHelper.set(filled,
                new StoredFluidComponent(fluidId, amount, SourceFluidAttributes.EMPTY));
            FluidVisuals.sync(filled);
        }

        if (!FluidTransferHelper.extractExact(storage, variant, amount)) return ActionResult.FAIL;
        replaceOne(player, hand, held, filled);
        if (world != null) {
            world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.BLOCKS, 0.8F, 1.1F);
        }
        return ActionResult.SUCCESS;
    }

    private static void replaceOne(PlayerEntity player, Hand hand, ItemStack held, ItemStack filled) {
        if (held.getCount() == 1) {
            player.setStackInHand(hand, filled);
            return;
        }
        held.decrement(1);
        player.getInventory().offerOrDrop(filled);
    }

    public String fluidName() {
        return storage.variantView().isBlank() ? "Empty"
            : Registries.FLUID.getId(storage.variantView().getFluid()).toString();
    }

    public long amountMb() {
        return FluidUnits.dropletsToMb(storage.amountView());
    }

    public long capacityMb() {
        return FluidUnits.dropletsToMb(CAPACITY);
    }

    @Override
    public Storage<FluidVariant> liquidFabricStorage() {
        return storage;
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        Identifier id = Identifier.tryParse(view.getString("FluidId", "minecraft:empty"));
        Fluid fluid = id == null ? Fluids.EMPTY : Registries.FLUID.get(id);
        storage.load(fluid == null || fluid == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(fluid),
            view.getLong("FluidAmount", 0));
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        if (!storage.variantView().isBlank() && storage.amountView() > 0) {
            view.putString("FluidId", Registries.FLUID.getId(storage.variantView().getFluid()).toString());
            view.putLong("FluidAmount", storage.amountView());
        }
    }
}
