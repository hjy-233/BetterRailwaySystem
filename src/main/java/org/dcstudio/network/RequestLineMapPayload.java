package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 客户端请求当前矿车线路图的数据包。
public record RequestLineMapPayload() implements CustomPayload {
    public static final RequestLineMapPayload INSTANCE = new RequestLineMapPayload();
    public static final Id<RequestLineMapPayload> ID = new Id<>(BetterRailwaySystem.id("request_line_map"));
    public static final PacketCodec<RegistryByteBuf, RequestLineMapPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
