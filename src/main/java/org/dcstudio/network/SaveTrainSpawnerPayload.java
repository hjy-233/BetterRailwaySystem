package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 客户端保存发车器配置时发回服务端的数据包。
public record SaveTrainSpawnerPayload(
        BlockPos pos,
        String cityName,
        String lineId,
        String lineThemeColor,
        String direction,
        int targetTrainCount,
        int flags
) {
    public static final Identifier ID = BetterRailwaySystem.id("save_train_spawner");

    public static SaveTrainSpawnerPayload read(PacketByteBuf buf) {
        return new SaveTrainSpawnerPayload(
                buf.readBlockPos(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(cityName);
        buf.writeString(lineId);
        buf.writeString(lineThemeColor);
        buf.writeString(direction);
        buf.writeVarInt(targetTrainCount);
        buf.writeVarInt(flags);
    }

    public boolean redstoneControlled() {
        return (flags & 1) != 0;
    }

    public boolean circularLine() {
        return (flags & 2) != 0;
    }
}
