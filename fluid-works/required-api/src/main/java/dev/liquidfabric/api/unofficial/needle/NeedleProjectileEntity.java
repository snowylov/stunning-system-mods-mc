package dev.liquidfabric.api.unofficial.needle;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class NeedleProjectileEntity extends Entity {
    private NeedlePayload payload = NeedlePayload.EMPTY;
    private int ageTicks;

    public NeedleProjectileEntity(EntityType<? extends NeedleProjectileEntity> type, World world) {
        super(type, world);
        this.noClip = false;
    }

    public void setPayload(NeedlePayload payload) {
        this.payload = payload == null ? NeedlePayload.EMPTY : payload;
    }

    public NeedlePayload getPayload() {
        return payload;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        ageTicks++;
        if (ageTicks > 100) {
            discard();
            return;
        }

        if (!getWorld().isClient) {
            HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
            if (hit.getType() != HitResult.Type.MISS) {
                onCollision(hit);
            }
        }

        this.move(MovementType.SELF, this.getVelocity());
        this.setVelocity(this.getVelocity().multiply(0.99));
    }

    protected boolean canHit(Entity entity) {
        return entity instanceof LivingEntity && entity != getOwner() && entity.isAlive();
    }

    private void onCollision(HitResult hit) {
        if (hit instanceof EntityHitResult entityHitResult && getWorld() instanceof ServerWorld serverWorld) {
            if (entityHitResult.getEntity() instanceof LivingEntity target) {
                LivingEntity attacker = getOwner() instanceof LivingEntity living ? living : null;
                NeedleProjectileUtil.inject(serverWorld, target, attacker, getPayload(), getDamageSources().thrown(this, attacker));
            }
            discard();
        } else if (hit instanceof BlockHitResult) {
            // Needles do not break blocks.
            discard();
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        ageTicks = nbt.getInt("AgeTicks", 0);
        if (nbt.contains("NeedlePayload")) {
            NeedlePayload.CODEC.parse(getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE), nbt.get("NeedlePayload")).result().ifPresent(this::setPayload);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("AgeTicks", ageTicks);
        NeedlePayload.CODEC.encodeStart(getRegistryManager().getOps(net.minecraft.nbt.NbtOps.INSTANCE), getPayload()).result().ifPresent(tag -> nbt.put("NeedlePayload", tag));
    }
}
