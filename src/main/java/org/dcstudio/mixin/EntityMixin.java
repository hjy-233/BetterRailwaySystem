package org.dcstudio.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

// 放宽矿车玩家乘客数量，并隐藏同车其他乘客。
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "canAddPassenger", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$allowMorePlayers(Entity passenger, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof AbstractMinecartEntity) || !(passenger instanceof PlayerEntity)) {
            return;
        }

        int playerPassengers = 0;
        List<Entity> passengers = self.getPassengerList();
        for (int index = 0; index < passengers.size(); index++) {
            if (passengers.get(index) instanceof PlayerEntity) {
                playerPassengers++;
            }
        }
        cir.setReturnValue(playerPassengers < org.dcstudio.BetterRailwaySystem.config().maxPassengers);
    }

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$hideOtherMinecartPassengers(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self == player) {
            return;
        }
        if (self instanceof PlayerEntity && self.getVehicle() instanceof AbstractMinecartEntity) {
            cir.setReturnValue(true);
        }
    }
}
