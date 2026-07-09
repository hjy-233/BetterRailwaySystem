package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

import java.util.ArrayList;
import java.util.List;

// 服务端返回当前线路图的数据包。
public record OpenLineMapPayload(
        boolean worldMap,
        String title,
        String currentStation,
        int titleColor,
        List<LineEntry> lines
) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, StationEntry> STATION_ENTRY_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            StationEntry::stationName,
            PacketCodecs.VAR_INT,
            StationEntry::x,
            PacketCodecs.VAR_INT,
            StationEntry::y,
            PacketCodecs.VAR_INT,
            StationEntry::z,
            StationEntry::new
    );
    public static final PacketCodec<RegistryByteBuf, LineEntry> LINE_ENTRY_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            LineEntry::cityName,
            PacketCodecs.STRING,
            LineEntry::lineId,
            PacketCodecs.STRING,
            LineEntry::direction,
            PacketCodecs.VAR_INT,
            LineEntry::lineColor,
            PacketCodecs.collection(ArrayList::new, STATION_ENTRY_CODEC),
            LineEntry::stations,
            LineEntry::new
    );
    public static final Id<OpenLineMapPayload> ID = new Id<>(BetterRailwaySystem.id("open_line_map"));
    public static final PacketCodec<RegistryByteBuf, OpenLineMapPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOL,
            OpenLineMapPayload::worldMap,
            PacketCodecs.STRING,
            OpenLineMapPayload::title,
            PacketCodecs.STRING,
            OpenLineMapPayload::currentStation,
            PacketCodecs.VAR_INT,
            OpenLineMapPayload::titleColor,
            PacketCodecs.collection(ArrayList::new, LINE_ENTRY_CODEC),
            OpenLineMapPayload::lines,
            OpenLineMapPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record LineEntry(
            String cityName,
            String lineId,
            String direction,
            int lineColor,
            List<StationEntry> stations
    ) {
    }

    public record StationEntry(
            String stationName,
            int x,
            int y,
            int z
    ) {
        public StationEntry(String stationName, BlockPos pos) {
            this(stationName, pos.getX(), pos.getY(), pos.getZ());
        }

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }
}
