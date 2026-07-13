package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

// 矿车经过铁路信标后发给乘客的广播数据包。
public record StationAnnouncementPayload(
        String titleText,
        String subtitleText,
        String soundId,
        String imageId,
        int imageDurationSeconds,
        boolean keepImageUntilNextBalise
) {
    public static final Identifier ID = BetterRailwaySystem.id("station_announcement");

    public static StationAnnouncementPayload read(PacketByteBuf buf) {
        return new StationAnnouncementPayload(
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt(),
                buf.readBoolean()
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(titleText);
        buf.writeString(subtitleText);
        buf.writeString(soundId);
        buf.writeString(imageId);
        buf.writeVarInt(imageDurationSeconds);
        buf.writeBoolean(keepImageUntilNextBalise);
    }
}
