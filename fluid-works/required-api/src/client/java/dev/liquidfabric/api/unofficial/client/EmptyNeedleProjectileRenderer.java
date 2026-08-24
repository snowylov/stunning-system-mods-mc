package dev.liquidfabric.api.unofficial.client;

import dev.liquidfabric.api.unofficial.UtilityApiMod;
import dev.liquidfabric.api.unofficial.needle.NeedleProjectileEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class EmptyNeedleProjectileRenderer extends EntityRenderer<NeedleProjectileEntity> {
    public EmptyNeedleProjectileRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(NeedleProjectileEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Invisible projectile: intentionally renders nothing.
    }

    @Override
    public Identifier getTexture(NeedleProjectileEntity entity) {
        return UtilityApiMod.id("textures/entity/invisible_needle_projectile.png");
    }
}
