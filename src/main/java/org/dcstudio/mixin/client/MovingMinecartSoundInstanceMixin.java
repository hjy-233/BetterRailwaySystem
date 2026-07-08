package org.dcstudio.mixin.client;

import net.minecraft.client.sound.MovingMinecartSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 降低原版矿车轨道摩擦声。
@Mixin(MovingMinecartSoundInstance.class)
public abstract class MovingMinecartSoundInstanceMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void betterrailwaysystem$quietMinecartRidingSound(CallbackInfo ci) {
        AbstractSoundInstanceAccessor accessor = (AbstractSoundInstanceAccessor) this;
        accessor.betterrailwaysystem$setVolume(accessor.betterrailwaysystem$getVolume() * 0F);
    }
}
