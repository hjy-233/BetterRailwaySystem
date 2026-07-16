package org.dcstudio.mixin;

import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.world.WorldView;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Debug 跟踪矿车时允许玩家观察视角穿过方块，避免服务端移动校验回滚。
@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "isPlayerNotCollidingWithBlocks", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$allowDebugTrackingThroughBlocks(WorldView world, Box box, double newX, double newY, double newZ, CallbackInfoReturnable<Boolean> cir) {
        if (BetterRailwaySystemNetworking.isDebugTracking(player)) {
            cir.setReturnValue(false);
        }
    }
}
