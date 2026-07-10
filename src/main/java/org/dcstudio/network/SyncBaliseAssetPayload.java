package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 服务端向客户端下发素材文件分块。
public record SyncBaliseAssetPayload(
        String assetType,
        String fileName,
        int chunkIndex,
        int chunkCount,
        byte[] data
) implements CustomPayload {
    public static final Id<SyncBaliseAssetPayload> ID = new Id<>(BetterRailwaySystem.id("sync_balise_asset"));
    public static final PacketCodec<RegistryByteBuf, SyncBaliseAssetPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            SyncBaliseAssetPayload::assetType,
            PacketCodecs.STRING,
            SyncBaliseAssetPayload::fileName,
            PacketCodecs.VAR_INT,
            SyncBaliseAssetPayload::chunkIndex,
            PacketCodecs.VAR_INT,
            SyncBaliseAssetPayload::chunkCount,
            PacketCodecs.BYTE_ARRAY,
            SyncBaliseAssetPayload::data,
            SyncBaliseAssetPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
