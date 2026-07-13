package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

// 客户端向服务端上传素材文件分块。
public record UploadBaliseAssetPayload(
        String assetType,
        String fileName,
        int chunkIndex,
        int chunkCount,
        byte[] data
) {
    public static final Identifier ID = BetterRailwaySystem.id("upload_balise_asset");

    public static UploadBaliseAssetPayload read(PacketByteBuf buf) {
        return new UploadBaliseAssetPayload(
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
