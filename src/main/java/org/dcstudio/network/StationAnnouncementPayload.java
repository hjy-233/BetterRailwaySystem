package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 矿车经过铁路信标后发给乘客的广播数据包。
public record StationAnnouncementPayload(
        String titleText,
        String subtitleText,
        String soundId,
        String imageId,
        int imageDurationSeconds,
        boolean keepImageUntilNextBalise
) implements CustomPayload {
    public static final Id<StationAnnouncementPayload> ID = new Id<>(BetterRailwaySystem.id("station_announcement"));
    public static final PacketCodec<RegistryByteBuf, StationAnnouncementPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            StationAnnouncementPayload::titleText,
            PacketCodecs.STRING,
            StationAnnouncementPayload::subtitleText,
            PacketCodecs.STRING,
            StationAnnouncementPayload::soundId,
            PacketCodecs.STRING,
            StationAnnouncementPayload::imageId,
            PacketCodecs.VAR_INT,
            StationAnnouncementPayload::imageDurationSeconds,
            PacketCodecs.BOOL,
            StationAnnouncementPayload::keepImageUntilNextBalise,
            StationAnnouncementPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
