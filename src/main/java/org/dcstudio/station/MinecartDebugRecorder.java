package org.dcstudio.station;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import org.dcstudio.minecart.MinecartDebugSnapshot;

// 统一记录方块最近经过的矿车调试快照。
public final class MinecartDebugRecorder {
    private MinecartDebugRecorder() {
    }

    public static MinecartDebugSnapshot snapshot(AbstractMinecartEntity minecart) {
        return MinecartDebugSnapshot.from(minecart);
    }

    public static void sync(BlockEntity blockEntity) {
        blockEntity.markDirty();
        if (blockEntity.getWorld() != null) {
            blockEntity.getWorld().updateListeners(blockEntity.getPos(), blockEntity.getCachedState(), blockEntity.getCachedState(), Block.NOTIFY_ALL);
        }
    }
}
