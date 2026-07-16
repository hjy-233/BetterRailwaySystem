package org.dcstudio.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.dcstudio.client.DebugMinecartTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Debug 跟踪矿车时拦截左键，避免原版攻击矿车。
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$trackMinecartOnAttack(CallbackInfoReturnable<Boolean> cir) {
        if (DebugMinecartTracker.handleAttack((MinecraftClient) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
