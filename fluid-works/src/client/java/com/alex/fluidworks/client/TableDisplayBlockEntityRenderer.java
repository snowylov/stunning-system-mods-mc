package com.alex.fluidworks.client;

import com.alex.fluidworks.furniture.DisplayTableBlock;
import com.alex.fluidworks.furniture.DisplayTableBlockEntity;
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

/** Displays the independent map and food slots together on the table top. */
public final class TableDisplayBlockEntityRenderer implements
        BlockEntityRenderer<DisplayTableBlockEntity, DisplayedItemsRenderState> {
    private final ItemModelManager itemModels;
    public TableDisplayBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        itemModels = context.itemModelManager();
    }
    @Override public DisplayedItemsRenderState createRenderState() { return new DisplayedItemsRenderState(2); }
    @Override public void updateRenderState(DisplayTableBlockEntity blockEntity,
            DisplayedItemsRenderState state, float tickProgress, Vec3d cameraPos,
            @Nullable ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(blockEntity, state, crumblingOverlay);
        for (int i = 0; i < 2; i++) itemModels.clearAndUpdate(state.items[i], blockEntity.getStack(i),
            ItemDisplayContext.FIXED, blockEntity.getWorld(), null, i);
    }
    @Override public void render(DisplayedItemsRenderState state, MatrixStack matrices,
            OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        Direction facing = state.blockState.get(DisplayTableBlock.FACING);
        float yaw = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
        matrices.push();
        matrices.translate(0.5, 0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.translate(-0.5, 0, -0.5);
        renderFlat(state.items[DisplayTableBlockEntity.MAP_SLOT], matrices, queue,
            state.lightmapCoordinates, 0.31F, 0.45F, 0.995F);
        renderFlat(state.items[DisplayTableBlockEntity.FOOD_SLOT], matrices, queue,
            state.lightmapCoordinates, 0.72F, 0.28F, 1.005F);
        matrices.pop();
    }
    private static void renderFlat(net.minecraft.client.render.item.ItemRenderState item,
            MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
            float x, float scale, float y) {
        if (item.isEmpty()) return;
        matrices.push(); matrices.translate(x, y, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.scale(scale, scale, scale);
        item.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();
    }
}
