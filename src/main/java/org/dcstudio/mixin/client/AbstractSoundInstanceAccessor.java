package org.dcstudio.mixin.client;

import net.minecraft.client.sound.AbstractSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// 访问原版声音实例的音量字段。
@Mixin(AbstractSoundInstance.class)
public interface AbstractSoundInstanceAccessor {
    @Accessor("volume")
    float betterrailwaysystem$getVolume();

    @Accessor("volume")
    void betterrailwaysystem$setVolume(float value);
}
