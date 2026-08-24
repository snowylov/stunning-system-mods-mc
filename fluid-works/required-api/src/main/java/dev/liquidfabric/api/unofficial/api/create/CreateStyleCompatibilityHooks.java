package dev.liquidfabric.api.unofficial.api.create;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight Create-style attachment/contraption compatibility surface.
 * Does not require Create. Create or other movement mods can query/register
 * bridges without this API having a hard dependency.
 */
public final class CreateStyleCompatibilityHooks {
    @FunctionalInterface
    public interface AttachmentPredicate {
        boolean canAttach(World world, BlockPos pos, BlockState state, Direction face);
    }

    @FunctionalInterface
    public interface MoveTogetherPredicate {
        boolean canMoveTogether(World world, BlockPos first, BlockPos second);
    }

    private static final List<AttachmentPredicate> ATTACHMENT_PREDICATES = new CopyOnWriteArrayList<>();
    private static final List<MoveTogetherPredicate> MOVE_TOGETHER_PREDICATES = new CopyOnWriteArrayList<>();

    private CreateStyleCompatibilityHooks() {}

    public static void registerAttachmentPredicate(AttachmentPredicate predicate) {
        ATTACHMENT_PREDICATES.add(predicate);
    }

    public static void registerMoveTogetherPredicate(MoveTogetherPredicate predicate) {
        MOVE_TOGETHER_PREDICATES.add(predicate);
    }

    public static boolean canAttach(World world, BlockPos pos, BlockState state, Direction face) {
        for (AttachmentPredicate predicate : ATTACHMENT_PREDICATES) {
            if (predicate.canAttach(world, pos, state, face)) return true;
        }
        return false;
    }

    public static boolean canMoveTogether(World world, BlockPos first, BlockPos second) {
        for (MoveTogetherPredicate predicate : MOVE_TOGETHER_PREDICATES) {
            if (predicate.canMoveTogether(world, first, second)) return true;
        }
        return false;
    }
}
