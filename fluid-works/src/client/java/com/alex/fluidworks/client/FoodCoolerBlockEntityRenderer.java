package com.alex.fluidworks.client;

import com.alex.fluidworks.storage.PortableCaseBlock;
import com.alex.fluidworks.storage.PortableCaseBlockEntity;
import com.alex.fluidworks.StorageCasesContent;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/** Renders all forty-eight food cells as a compact 8x6 display behind the front glass. */
public final class FoodCoolerBlockEntityRenderer implements
        BlockEntityRenderer<PortableCaseBlockEntity, DisplayedItemsRenderState> {
    private final ItemModelManager itemModels;
    public FoodCoolerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        itemModels = context.itemModelManager();
    }
    @Override public DisplayedItemsRenderState createRenderState() {
        return new DisplayedItemsRenderState(PortableCaseBlockEntity.SIZE);
    }
    @Override public void updateRenderState(PortableCaseBlockEntity blockEntity,
            DisplayedItemsRenderState state, float tickProgress, Vec3d cameraPos,
            @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(blockEntity, state, crumblingOverlay);
        state.lidProgress = blockEntity.lidProgress(tickProgress);
        boolean visible = blockEntity.getCachedState().getBlock() instanceof PortableCaseBlock block
            && block.showsItems();
        for (int i = 0; i < state.items.length; i++) {
            itemModels.clearAndUpdate(state.items[i], visible ? blockEntity.getStack(i)
                : net.minecraft.item.ItemStack.EMPTY, ItemDisplayContext.FIXED,
                blockEntity.getWorld(), null, i);
        }
    }
    @Override public void render(DisplayedItemsRenderState state, MatrixStack matrices,
            OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        Direction facing = state.blockState.get(PortableCaseBlock.FACING);
        float yaw = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.translate(-0.5, 0, -0.5);
        renderLid(state, matrices, queue);
        for (int i = 0; i < state.items.length; i++) {
            if (state.items[i].isEmpty()) continue;
            int column = i % 8;
            int row = i / 8;
            matrices.push();
            matrices.translate(0.09 + column * 0.117, 0.19 + row * 0.105, 0.302);
            matrices.scale(0.085F, 0.085F, 0.085F);
            state.items[i].render(matrices, queue, state.lightmapCoordinates,
                OverlayTexture.DEFAULT_UV, 0);
            matrices.pop();
        }
        matrices.pop();
    }

    /** Submits the upper case shell as a real hinged model, interpolated over roughly eight ticks. */
    private static void renderLid(DisplayedItemsRenderState state, MatrixStack matrices,
                                  OrderedRenderCommandQueue queue) {
        if (!(state.blockState.getBlock() instanceof PortableCaseBlock owner)) return;
        net.minecraft.block.Block material = StorageCasesContent.MATERIALS.get(owner);
        if (material == null) return;
        matrices.push();
        matrices.translate(0, 7 / 16.0, 11 / 16.0);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F * state.lidProgress));
        submitCuboid(queue, matrices, material.getDefaultState(), state.lightmapCoordinates,
            0, 0, -6 / 16.0, 1, 7 / 16.0, 6 / 16.0);
        net.minecraft.block.BlockState iron = net.minecraft.block.Blocks.IRON_BLOCK.getDefaultState();
        submitCuboid(queue, matrices, iron, state.lightmapCoordinates,
            6 / 16.0, 7 / 16.0, -4.5 / 16.0, 1 / 16.0, 2 / 16.0, 3 / 16.0);
        submitCuboid(queue, matrices, iron, state.lightmapCoordinates,
            9 / 16.0, 7 / 16.0, -4.5 / 16.0, 1 / 16.0, 2 / 16.0, 3 / 16.0);
        submitCuboid(queue, matrices, iron, state.lightmapCoordinates,
            7 / 16.0, 8 / 16.0, -4.5 / 16.0, 2 / 16.0, 1 / 16.0, 3 / 16.0);
        matrices.pop();
    }

    private static void submitCuboid(OrderedRenderCommandQueue queue, MatrixStack matrices,
            net.minecraft.block.BlockState block, int light, double x, double y, double z,
            double width, double height, double depth) {
        matrices.push(); matrices.translate(x, y, z);
        matrices.scale((float) width, (float) height, (float) depth);
        queue.submitBlock(matrices, block, light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();
    }
}
