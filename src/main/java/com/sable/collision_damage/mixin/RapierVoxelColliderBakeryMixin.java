package com.sable.collision_damage.mixin;

import com.sable.collision_damage.SablePreSolverDamage;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery")
public abstract class RapierVoxelColliderBakeryMixin {
    @Redirect(
            method = "buildPhysicsDataForBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/block/BlockWithSubLevelCollisionCallback;sable$getCallback(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/api/physics/callback/BlockSubLevelCollisionCallback;"
            ),
            remap = false
    )
    private BlockSubLevelCollisionCallback sablecollisiondamage$wrapCollisionCallback(final BlockState callbackState, final BlockState state) {
        return SablePreSolverDamage.getCallbackFor(state, BlockWithSubLevelCollisionCallback.sable$getCallback(callbackState));
    }
}
