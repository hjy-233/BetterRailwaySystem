package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 客户端请求清理铁路网数据的数据包。
public record ClearRailwayMapPayload(
        String mode,
        String cityName,
        String lineId
) implements CustomPayload {
    public static final Id<ClearRailwayMapPayload> ID = new Id<>(BetterRailwaySystem.id("clear_railway_map"));
    public static final PacketCodec<RegistryByteBuf, ClearRailwayMapPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            ClearRailwayMapPayload::mode,
            PacketCodecs.STRING,
            ClearRailwayMapPayload::cityName,
            PacketCodecs.STRING,
            ClearRailwayMapPayload::lineId,
            ClearRailwayMapPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Mode {
        ALL,
        CITY,
        LINE;

        public static Mode fromString(String value) {
            for (Mode mode : values()) {
                if (mode.name().equalsIgnoreCase(value)) {
                    return mode;
                }
            }
            return ALL;
        }
    }
}
