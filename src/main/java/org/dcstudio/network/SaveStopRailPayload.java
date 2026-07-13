package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 客户端保存停车轨配置时发回服务端的数据包。
public record SaveStopRailPayload(
        BlockPos pos,
        int stopDistance,
        int dwellSeconds,
        String waitMode
) {
    public static final Identifier ID = BetterRailwaySystem.id("save_stop_rail");

    public static SaveStopRailPayload read(PacketByteBuf buf) {
        return new SaveStopRailPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readString());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(stopDistance);
        buf.writeVarInt(dwellSeconds);
        buf.writeString(waitMode);
    }
}
