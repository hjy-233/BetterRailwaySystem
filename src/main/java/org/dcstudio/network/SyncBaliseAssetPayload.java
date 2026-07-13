package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

// 服务端向客户端下发素材文件分块。
public record SyncBaliseAssetPayload(
        String assetType,
        String fileName,
        int chunkIndex,
        int chunkCount,
        byte[] data
) {
    public static final Identifier ID = BetterRailwaySystem.id("sync_balise_asset");

    public static SyncBaliseAssetPayload read(PacketByteBuf buf) {
        return new SyncBaliseAssetPayload(
                buf.readString(),
                buf.readString(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readByteArray()
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeString(assetType);
        buf.writeString(fileName);
        buf.writeVarInt(chunkIndex);
        buf.writeVarInt(chunkCount);
        buf.writeByteArray(data);
    }
}
