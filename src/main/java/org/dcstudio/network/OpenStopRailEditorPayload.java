package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 服务端打开停车轨编辑界面的数据包。
public record OpenStopRailEditorPayload(
        BlockPos pos,
        int stopDistance,
        int dwellSeconds,
        String waitMode
) {
    public static final Identifier ID = BetterRailwaySystem.id("open_stop_rail_editor");

    public static OpenStopRailEditorPayload read(PacketByteBuf buf) {
        return new OpenStopRailEditorPayload(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readString());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(stopDistance);
        buf.writeVarInt(dwellSeconds);
        buf.writeString(waitMode);
    }
}
