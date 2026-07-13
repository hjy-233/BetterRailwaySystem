package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

import java.util.List;

// 服务端向客户端发送素材库目录清单。
public record BaliseAssetCatalogPayload(
        List<Entry> imageFiles,
        List<Entry> soundFiles
) {
    public static final Identifier ID = BetterRailwaySystem.id("balise_asset_catalog");

    public static BaliseAssetCatalogPayload read(PacketByteBuf buf) {
        return new BaliseAssetCatalogPayload(
                PacketBufHelper.readCatalogEntries(buf),
                PacketBufHelper.readCatalogEntries(buf)
        );
    }

    public void write(PacketByteBuf buf) {
        PacketBufHelper.writeCatalogEntries(buf, imageFiles);
        PacketBufHelper.writeCatalogEntries(buf, soundFiles);
    }

    public record Entry(String fileName, String uploadedBy) {
    }
}
