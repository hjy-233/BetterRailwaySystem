package org.dcstudio.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// 覆盖可骑乘矿车的交互，让同一矿车可容纳多名玩家。
@Mixin(MinecartEntity.class)
public abstract class MinecartEntityMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$allowStackedPlayers(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        MinecartEntity minecart = (MinecartEntity) (Object) this;
        if (player.shouldCancelInteraction() || player.hasVehicle()) {
            return;
        }

        if (minecart.getWorld().isClient) {
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        cir.setReturnValue(player.startRiding(minecart) ? ActionResult.CONSUME : ActionResult.PASS);
    }
}
