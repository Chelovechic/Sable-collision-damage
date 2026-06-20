package com.sable.collision_damage.mixin;

import com.sable.collision_damage.SablePreSolverDamage;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery")
public abstract class RapierVoxelColliderBakeryMixin {
    @Inject(method = "getPhysicsDataForBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void sablecollisiondamage$skipFluidCollider(final BlockState state, final CallbackInfoReturnable<Object> cir) {
        if (SablePreSolverDamage.isFluidBlock(state)) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(
            method = "buildPhysicsDataForBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/api/block/BlockWithSubLevelCollisionCallback;sable$getCallback(Lnet/minecraft/world/level/block/state/BlockState;)Ldev/ryanhcode/sable/api/physics/callback/BlockSubLevelCollisionCallback;"
            ),
            remap = false
    )
    private BlockSubLevelCollisionCallback sablecollisiondamage$wrapCollisionCallback(final BlockState callbackState, final BlockState state) {
        return SablePreSolverDamage.getCallbackFor(callbackState, BlockWithSubLevelCollisionCallback.sable$getCallback(callbackState));
    }
}
