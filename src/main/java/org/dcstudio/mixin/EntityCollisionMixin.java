package org.dcstudio.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 关闭矿车与实体的物理碰撞。
@Mixin(Entity.class)
public abstract class EntityCollisionMixin {
    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$disableMinecartCollision(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractMinecartEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$disableMinecartPushable(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractMinecartEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "collidesWith", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$disableMinecartEntityCollision(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof AbstractMinecartEntity || other instanceof AbstractMinecartEntity) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$disableMinecartPushAway(Entity other, CallbackInfo ci) {
        if ((Object) this instanceof AbstractMinecartEntity || other instanceof AbstractMinecartEntity) {
            ci.cancel();
        }
    }
}
