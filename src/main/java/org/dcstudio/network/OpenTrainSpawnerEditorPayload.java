package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

import java.util.ArrayList;
import java.util.List;

// 服务端打开发车器编辑界面的数据包。
public record OpenTrainSpawnerEditorPayload(
        BlockPos pos,
        String lineId,
        String lineThemeColor,
        String direction,
        int targetTrainCount,
        int spawnIntervalSeconds,
        int flags,
        List<String> cityOptions
) implements CustomPayload {
    public static final Id<OpenTrainSpawnerEditorPayload> ID = new Id<>(BetterRailwaySystem.id("open_train_spawner_editor"));
    public static final PacketCodec<RegistryByteBuf, OpenTrainSpawnerEditorPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                BlockPos.PACKET_CODEC.encode(buf, payload.pos);
                PacketCodecs.STRING.encode(buf, payload.lineId);
                PacketCodecs.STRING.encode(buf, payload.lineThemeColor);
                PacketCodecs.STRING.encode(buf, payload.direction);
                PacketCodecs.VAR_INT.encode(buf, payload.targetTrainCount);
                PacketCodecs.VAR_INT.encode(buf, payload.spawnIntervalSeconds);
                PacketCodecs.VAR_INT.encode(buf, payload.flags);
                PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING).encode(buf, new ArrayList<>(payload.cityOptions));
            },
            buf -> new OpenTrainSpawnerEditorPayload(
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    new ArrayList<>(PacketCodecs.collection(ArrayList::new, PacketCodecs.STRING).decode(buf))
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
