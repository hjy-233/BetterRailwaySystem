package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
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
) implements CustomPayload {
    public static final Id<SaveTrainSpawnerPayload> ID = new Id<>(BetterRailwaySystem.id("save_train_spawner"));
    public static final PacketCodec<RegistryByteBuf, SaveTrainSpawnerPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                BlockPos.PACKET_CODEC.encode(buf, payload.pos);
                PacketCodecs.STRING.encode(buf, payload.cityName);
                PacketCodecs.STRING.encode(buf, payload.lineId);
                PacketCodecs.STRING.encode(buf, payload.lineThemeColor);
                PacketCodecs.STRING.encode(buf, payload.direction);
                PacketCodecs.VAR_INT.encode(buf, payload.targetTrainCount);
                PacketCodecs.VAR_INT.encode(buf, payload.flags);
            },
            buf -> new SaveTrainSpawnerPayload(
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf)
            )
    );

    public boolean redstoneControlled() {
        return (flags & 1) != 0;
    }

    public boolean circularLine() {
        return (flags & 2) != 0;
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
