package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;

// 服务端打开铁路信标编辑界面的数据包。
public record OpenBaliseEditorPayload(
        BlockPos pos,
        String mode,
        String titleText,
        String subtitleText,
        String currentStation,
        String nextStation,
        String soundId,
        String imageId,
        int imageDurationSeconds,
        boolean keepImageUntilNextBalise,
        boolean updateBossBar,
        double speedLimitBps,
        String triggerDirection
) implements CustomPayload {
    public static final Id<OpenBaliseEditorPayload> ID = new Id<>(BetterRailwaySystem.id("open_balise_editor"));
    public static final PacketCodec<RegistryByteBuf, OpenBaliseEditorPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                BlockPos.PACKET_CODEC.encode(buf, payload.pos);
                PacketCodecs.STRING.encode(buf, payload.mode);
                PacketCodecs.STRING.encode(buf, payload.titleText);
                PacketCodecs.STRING.encode(buf, payload.subtitleText);
                PacketCodecs.STRING.encode(buf, payload.currentStation);
                PacketCodecs.STRING.encode(buf, payload.nextStation);
                PacketCodecs.STRING.encode(buf, payload.soundId);
                PacketCodecs.STRING.encode(buf, payload.imageId);
                PacketCodecs.VAR_INT.encode(buf, payload.imageDurationSeconds);
                PacketCodecs.BOOLEAN.encode(buf, payload.keepImageUntilNextBalise);
                PacketCodecs.BOOLEAN.encode(buf, payload.updateBossBar);
                PacketCodecs.DOUBLE.encode(buf, payload.speedLimitBps);
                PacketCodecs.STRING.encode(buf, payload.triggerDirection);
            },
            buf -> new OpenBaliseEditorPayload(
                    BlockPos.PACKET_CODEC.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.STRING.decode(buf),
                    PacketCodecs.VAR_INT.decode(buf),
                    PacketCodecs.BOOLEAN.decode(buf),
                    PacketCodecs.BOOLEAN.decode(buf),
                    PacketCodecs.DOUBLE.decode(buf),
                    PacketCodecs.STRING.decode(buf)
            )
    );

    public BaliseMode parsedMode() {
        return BaliseMode.fromString(mode);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
