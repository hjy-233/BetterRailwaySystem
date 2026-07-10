package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 客户端向服务端上传素材文件分块。
public record UploadBaliseAssetPayload(
        String assetType,
        String fileName,
        int chunkIndex,
        int chunkCount,
        byte[] data
) implements CustomPayload {
    public static final Id<UploadBaliseAssetPayload> ID = new Id<>(BetterRailwaySystem.id("upload_balise_asset"));
    public static final PacketCodec<RegistryByteBuf, UploadBaliseAssetPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            UploadBaliseAssetPayload::assetType,
            PacketCodecs.STRING,
            UploadBaliseAssetPayload::fileName,
            PacketCodecs.VAR_INT,
            UploadBaliseAssetPayload::chunkIndex,
            PacketCodecs.VAR_INT,
            UploadBaliseAssetPayload::chunkCount,
            PacketCodecs.BYTE_ARRAY,
            UploadBaliseAssetPayload::data,
            UploadBaliseAssetPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
