package com.sable.collision_damage;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import com.sable.collision_damage.particle.SableImpactParticles;

public final class SablePreSolverDamage {
    private static final Object2ObjectOpenHashMap<SubLevelPhysicsSystem, ObjectArrayList<PendingBlockBreak>> PENDING_BLOCK_BREAKS = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectOpenHashMap<SubLevelPhysicsSystem, ObjectArrayList<PendingContactSlowdown>> PENDING_CONTACT_SLOWDOWNS = new Object2ObjectOpenHashMap<>();

    private SablePreSolverDamage() {
    }

    public static @Nullable BlockSubLevelCollisionCallback getCallbackFor(final BlockState state, final @Nullable BlockSubLevelCollisionCallback originalCallback) {
        if (state.isAir()) {
            return null;
        }

        if (shouldPreserveOriginalCallback(originalCallback)) {
            return originalCallback;
        }

        return new DestroySpeedAwareCallback(state);
    }

    private static boolean shouldPreserveOriginalCallback(final @Nullable BlockSubLevelCollisionCallback originalCallback) {
        if (originalCallback == null) {
            return false;
        }

        return !"FragileBlockCallback".equals(originalCallback.getClass().getSimpleName());
    }

    public static void onPostPhysicsTick(final ForgeSablePostPhysicsTickEvent event) {
        flushPendingBlockBreaks(event);
        flushPendingSlowdowns(event);
    }

    private static void flushPendingBlockBreaks(final ForgeSablePostPhysicsTickEvent event) {
        final ObjectArrayList<PendingBlockBreak> pendingBreaks = PENDING_BLOCK_BREAKS.remove(event.getPhysicsSystem());
        if (pendingBreaks == null || pendingBreaks.isEmpty()) {
            return;
        }

        final ObjectOpenHashSet<PendingBlockBreakKey> processedBreaks = new ObjectOpenHashSet<>();

        for (final PendingBlockBreak pendingBreak : pendingBreaks) {
            final CollisionTarget target = pendingBreak.target();
            if (!processedBreaks.add(new PendingBlockBreakKey(target.subLevel(), target.blockPos()))) {
                continue;
            }

            if (!destroyBlockFragileLike(event.getPhysicsSystem().getLevel(), target)) {
                continue;
            }

            SableImpactParticles.emitImpact(event.getPhysicsSystem().getLevel(), pendingBreak.originalState(), pendingBreak.globalHitPos(), pendingBreak.impactVelocity());

            final ServerSubLevel slowdownTarget = target.subLevel() != null
                    ? target.subLevel()
                    : findIntersectingShip(event.getPhysicsSystem().getLevel(), pendingBreak.globalHitPos());
            queueSlowdown(event.getPhysicsSystem(), slowdownTarget, pendingBreak.globalHitPos());
        }
    }

    private static void flushPendingSlowdowns(final ForgeSablePostPhysicsTickEvent event) {
        final ObjectArrayList<PendingContactSlowdown> pendingSlowdowns = PENDING_CONTACT_SLOWDOWNS.remove(event.getPhysicsSystem());
        if (pendingSlowdowns == null || pendingSlowdowns.isEmpty()) {
            return;
        }

        final Vector3d linearVelocity = new Vector3d();
        final Vector3d pointVelocity = new Vector3d();
        final Vector3d localImpulse = new Vector3d();

        for (final PendingContactSlowdown pendingSlowdown : pendingSlowdowns) {
            final ServerSubLevel subLevel = pendingSlowdown.subLevel();
            if (subLevel == null || subLevel.isRemoved()) {
                continue;
            }

            final RigidBodyHandle handle = event.getPhysicsSystem().getPhysicsHandle(subLevel);
            if (handle == null || !handle.isValid()) {
                continue;
            }

            pointVelocity.set(Sable.HELPER.getVelocity(subLevel.getLevel(), subLevel, pendingSlowdown.plotContactPoint(), linearVelocity));
            if (pointVelocity.lengthSquared() <= 1.0E-12D) {
                handle.getLinearVelocity(pointVelocity);
            }

            final double speed = pointVelocity.length();
            if (speed <= 1.0E-6D) {
                continue;
            }

            final double appliedSlowdown = Math.min(pendingSlowdown.slowdown(), speed);
            if (appliedSlowdown <= 1.0E-6D) {
                continue;
            }

            final double mass = Math.max(subLevel.getMassTracker().getMass(), 1.0E-6D);
            subLevel.logicalPose().transformNormalInverse(pointVelocity, localImpulse).normalize().mul(-mass * appliedSlowdown);
            handle.applyImpulseAtPoint(pendingSlowdown.plotContactPoint(), localImpulse);
        }
    }

    private static void queueBlockBreak(final SubLevelPhysicsSystem system, final CollisionTarget target, final Vector3d globalHitPos,
                                        final double impactVelocity) {
        if (system == null) {
            return;
        }

        PENDING_BLOCK_BREAKS
                .computeIfAbsent(system, key -> new ObjectArrayList<>())
                .add(new PendingBlockBreak(target, target.state(), new Vector3d(globalHitPos), impactVelocity));
    }

    private static void queueSlowdown(final SubLevelPhysicsSystem system, final @Nullable ServerSubLevel subLevel, final Vector3d globalHitPos) {
        final double slowdownPerBlock = Config.STATIC_SLOWDOWN_PER_BLOCK.get();
        if (system == null || subLevel == null || subLevel.isRemoved() || slowdownPerBlock <= 0.0D) {
            return;
        }

        final Vector3d plotContactPoint = toPlotPosition(subLevel, globalHitPos, new Vector3d());
        PENDING_CONTACT_SLOWDOWNS
                .computeIfAbsent(system, key -> new ObjectArrayList<>())
                .add(new PendingContactSlowdown(subLevel, new Vector3d(plotContactPoint), new Vector3d(globalHitPos), slowdownPerBlock));
    }

    private static Vector3d toPlotPosition(final ServerSubLevel subLevel, final Vector3d globalPoint, final Vector3d dest) {
        final Pose3d pose = subLevel.logicalPose();
        dest.set(globalPoint).sub(pose.position());
        pose.orientation().transformInverse(dest);
        dest.add(pose.rotationPoint());
        return dest;
    }

    private static @Nullable ServerSubLevel findIntersectingShip(final ServerLevel level, final Vector3d globalPoint) {
        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(
                level,
                new BoundingBox3d(BlockPos.containing(globalPoint.x, globalPoint.y, globalPoint.z))
        );

        for (final SubLevel candidate : intersecting) {
            if (candidate instanceof final ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                return serverSubLevel;
            }
        }

        return null;
    }

    private static @Nullable CollisionTarget resolveCollisionTarget(final ServerLevel level, final BlockPos blockPos, final Vector3d globalHitPos) {
        final CollisionTarget shipTarget = resolveShipTarget(level, blockPos, globalHitPos);
        if (shipTarget != null) {
            return shipTarget;
        }

        final BlockState worldState = level.getBlockState(blockPos);
        if (worldState.isAir()) {
            return null;
        }

        return new CollisionTarget(null, blockPos, worldState);
    }

    private static float getDestroySpeed(final CollisionTarget target, final ServerLevel level) {
        return target.subLevel() == null
                ? target.state().getDestroySpeed(level, target.blockPos())
                : target.state().getDestroySpeed(target.subLevel().getPlot().getEmbeddedLevelAccessor(), target.blockPos());
    }

    private static float getCounterpartDestroySpeed(final CollisionTarget target, final ServerLevel level, final BlockState sourceState) {
        if (target.subLevel() == null) {
            return sourceState.getDestroySpeed(level, target.blockPos());
        }

        final BlockState worldState = level.getBlockState(target.blockPos());
        if (!worldState.isAir()) {
            return worldState.getDestroySpeed(level, target.blockPos());
        }

        return sourceState.getDestroySpeed(level, target.blockPos());
    }

    private static @Nullable CollisionTarget resolveShipTarget(final ServerLevel level, final BlockPos localBlockPos, final Vector3d globalHitPos) {
        final Iterable<SubLevel> intersecting = Sable.HELPER.getAllIntersecting(
                level,
                new BoundingBox3d(BlockPos.containing(globalHitPos.x, globalHitPos.y, globalHitPos.z))
        );

        CollisionTarget bestTarget = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (final SubLevel candidate : intersecting) {
            if (!(candidate instanceof final ServerSubLevel serverSubLevel) || serverSubLevel.isRemoved()) {
                continue;
            }

            final EmbeddedPlotLevelAccessor accessor = serverSubLevel.getPlot().getEmbeddedLevelAccessor();
            final BlockState state = accessor.getBlockState(localBlockPos);
            if (state.isAir()) {
                continue;
            }

            final double distanceSquared = distanceSquaredToLocalBlock(serverSubLevel, localBlockPos, globalHitPos);
            if (distanceSquared < bestDistanceSquared) {
                bestDistanceSquared = distanceSquared;
                bestTarget = new CollisionTarget(serverSubLevel, localBlockPos, state);
            }
        }

        return bestTarget;
    }

    private static double distanceSquaredToLocalBlock(final ServerSubLevel subLevel, final BlockPos localBlockPos, final Vector3d globalHitPos) {
        final Pose3d pose = subLevel.logicalPose();
        final Vector3d blockCenter = new Vector3d(
                localBlockPos.getX() + 0.5D - pose.rotationPoint().x,
                localBlockPos.getY() + 0.5D - pose.rotationPoint().y,
                localBlockPos.getZ() + 0.5D - pose.rotationPoint().z
        );
        pose.orientation().transform(blockCenter).add(pose.position());
        return blockCenter.distanceSquared(globalHitPos);
    }

    private static boolean destroyBlockFragileLike(final ServerLevel level, final CollisionTarget target) {
        final BlockState state = target.state();
        if (state.isAir()) {
            return false;
        }

        final boolean destroyed;
        final BlockState afterState;

        if (target.subLevel() == null) {
            destroyed = level.destroyBlock(target.blockPos(), true);
            afterState = level.getBlockState(target.blockPos());
        } else {
            final EmbeddedPlotLevelAccessor accessor = target.subLevel().getPlot().getEmbeddedLevelAccessor();
            destroyed = accessor.destroyBlock(target.blockPos(), true);
            afterState = accessor.getBlockState(target.blockPos());
        }

        if (!destroyed || !afterState.isAir()) {
            return false;
        }

        if (state.getBlock() instanceof IceBlock) {
            if (target.subLevel() == null) {
                final BlockState belowState = level.getBlockState(target.blockPos().below());
                if (belowState.blocksMotion() || belowState.liquid()) {
                    level.setBlockAndUpdate(target.blockPos(), IceBlock.meltsInto());
                }
            } else {
                final EmbeddedPlotLevelAccessor accessor = target.subLevel().getPlot().getEmbeddedLevelAccessor();
                final BlockState belowState = accessor.getBlockState(target.blockPos().below());
                if (belowState.blocksMotion() || belowState.liquid()) {
                    accessor.setBlock(target.blockPos(), IceBlock.meltsInto(), 3, 0);
                }
            }
        }

        return true;
    }

    private record CollisionTarget(@Nullable ServerSubLevel subLevel, BlockPos blockPos, BlockState state) {
    }

    private record PendingBlockBreak(CollisionTarget target, BlockState originalState, Vector3d globalHitPos, double impactVelocity) {
    }

    private record PendingBlockBreakKey(@Nullable ServerSubLevel subLevel, BlockPos blockPos) {
    }

    private record PendingContactSlowdown(ServerSubLevel subLevel, Vector3d plotContactPoint, Vector3d globalHitPos, double slowdown) {
    }

    private static final class DestroySpeedAwareCallback implements BlockSubLevelCollisionCallback {
        private final BlockState sourceState;

        private DestroySpeedAwareCallback(final BlockState sourceState) {
            this.sourceState = sourceState;
        }

        @Override
        public CollisionResult sable$onCollision(final BlockPos pos, final Vector3d hitPos, final double impactVelocity) {
            final double baseTriggerVelocity = Config.MIN_BREAK_SPEED.get();
            if (impactVelocity * impactVelocity < baseTriggerVelocity * baseTriggerVelocity) {
                return CollisionResult.NONE;
            }

            final SubLevelPhysicsSystem system = SubLevelPhysicsSystem.getCurrentlySteppingSystem();
            final ServerLevel level = system.getLevel();
            final CollisionTarget target = resolveCollisionTarget(level, pos, hitPos);

            if (target == null) {
                return CollisionResult.NONE;
            }

            final BlockState state = target.state();
            if (state.getBlock() instanceof LeavesBlock && state.getValue(LeavesBlock.PERSISTENT)) {
                return CollisionResult.NONE;
            }

            final float destroySpeed = getDestroySpeed(target, level);
            if (destroySpeed < 0.0F) {
                return CollisionResult.NONE;
            }

            final float counterpartDestroySpeed = getCounterpartDestroySpeed(target, level, this.sourceState);
            if (counterpartDestroySpeed < 0.0F) {
                return CollisionResult.NONE;
            }

            final double ownHardnessPenalty = Math.max(0.0F, destroySpeed) * Config.DESTROY_SPEED_HARDNESS_FACTOR.get();
            final double softerCounterpartBonus = Math.max(0.0F, destroySpeed - counterpartDestroySpeed) * Config.COUNTERPART_HARDNESS_FACTOR.get();
            final double requiredVelocity = baseTriggerVelocity
                    + ownHardnessPenalty
                    + softerCounterpartBonus;
            if (impactVelocity * impactVelocity < requiredVelocity * requiredVelocity) {
                return CollisionResult.NONE;
            }

            queueBlockBreak(system, target, hitPos, impactVelocity);

            return new CollisionResult(JOMLConversion.ZERO, true);
        }
    }
}
