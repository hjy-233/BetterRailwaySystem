package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

import java.util.ArrayList;
import java.util.List;

// 服务端向客户端发送素材库目录清单。
public record BaliseAssetCatalogPayload(
        List<Entry> imageFiles,
        List<Entry> soundFiles
) implements CustomPayload {
    public static final PacketCodec<RegistryByteBuf, Entry> ENTRY_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING,
            Entry::fileName,
            PacketCodecs.STRING,
            Entry::uploadedBy,
            Entry::new
    );
    public static final Id<BaliseAssetCatalogPayload> ID = new Id<>(BetterRailwaySystem.id("balise_asset_catalog"));
    public static final PacketCodec<RegistryByteBuf, BaliseAssetCatalogPayload> CODEC = PacketCodec.of(
            (payload, buf) -> {
                PacketCodecs.collection(ArrayList::new, ENTRY_CODEC).encode(buf, new ArrayList<>(payload.imageFiles));
                PacketCodecs.collection(ArrayList::new, ENTRY_CODEC).encode(buf, new ArrayList<>(payload.soundFiles));
            },
            buf -> new BaliseAssetCatalogPayload(
                    new ArrayList<>(PacketCodecs.collection(ArrayList::new, ENTRY_CODEC).decode(buf)),
                    new ArrayList<>(PacketCodecs.collection(ArrayList::new, ENTRY_CODEC).decode(buf))
            )
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record Entry(String fileName, String uploadedBy) {
    }
}
