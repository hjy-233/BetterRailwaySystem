package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 服务端打开停车轨编辑界面的数据包。
public record OpenStopRailEditorPayload(
        BlockPos pos,
        int stopDistance,
        int dwellSeconds,
        String waitMode
) implements CustomPayload {
    public static final Id<OpenStopRailEditorPayload> ID = new Id<>(BetterRailwaySystem.id("open_stop_rail_editor"));
    public static final PacketCodec<RegistryByteBuf, OpenStopRailEditorPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            OpenStopRailEditorPayload::pos,
            PacketCodecs.VAR_INT,
            OpenStopRailEditorPayload::stopDistance,
            PacketCodecs.VAR_INT,
            OpenStopRailEditorPayload::dwellSeconds,
            PacketCodecs.STRING,
            OpenStopRailEditorPayload::waitMode,
            OpenStopRailEditorPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
