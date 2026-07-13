package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;

// 旧版网络协议的公共读写辅助。
public final class PacketBufHelper {
    private PacketBufHelper() {
    }

    public static void writeStringList(PacketByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeString(value);
        }
    }

    public static List<String> readStringList(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            values.add(buf.readString());
        }
        return values;
    }

    public static void writeCatalogEntries(PacketByteBuf buf, List<BaliseAssetCatalogPayload.Entry> entries) {
        buf.writeVarInt(entries.size());
        for (BaliseAssetCatalogPayload.Entry entry : entries) {
            buf.writeString(entry.fileName());
            buf.writeString(entry.uploadedBy());
        }
    }

    public static List<BaliseAssetCatalogPayload.Entry> readCatalogEntries(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<BaliseAssetCatalogPayload.Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new BaliseAssetCatalogPayload.Entry(buf.readString(), buf.readString()));
        }
        return entries;
    }
}
