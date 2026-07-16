package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 客户端 debug 跟踪矿车时同步服务端玩家碰撞状态。
public record DebugTrackingPayload(boolean tracking) implements CustomPayload {
    public static final Id<DebugTrackingPayload> ID = new Id<>(BetterRailwaySystem.id("debug_tracking"));
    public static final PacketCodec<RegistryByteBuf, DebugTrackingPayload> CODEC = PacketCodec.of(
            (payload, buf) -> PacketCodecs.BOOL.encode(buf, payload.tracking),
            buf -> new DebugTrackingPayload(PacketCodecs.BOOL.decode(buf))
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
