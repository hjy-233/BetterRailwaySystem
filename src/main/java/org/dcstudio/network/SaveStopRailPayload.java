package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 客户端保存停车轨配置时发回服务端的数据包。
public record SaveStopRailPayload(
        BlockPos pos,
        int stopDistance,
        int dwellSeconds,
        String waitMode
) implements CustomPayload {
    public static final Id<SaveStopRailPayload> ID = new Id<>(BetterRailwaySystem.id("save_stop_rail"));
    public static final PacketCodec<RegistryByteBuf, SaveStopRailPayload> CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            SaveStopRailPayload::pos,
            PacketCodecs.VAR_INT,
            SaveStopRailPayload::stopDistance,
            PacketCodecs.VAR_INT,
            SaveStopRailPayload::dwellSeconds,
            PacketCodecs.STRING,
            SaveStopRailPayload::waitMode,
            SaveStopRailPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
