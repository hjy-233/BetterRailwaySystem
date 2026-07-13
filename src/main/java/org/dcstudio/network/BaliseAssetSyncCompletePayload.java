package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

// 服务端通知客户端素材快照发送完成。
public record BaliseAssetSyncCompletePayload() {
    public static final BaliseAssetSyncCompletePayload INSTANCE = new BaliseAssetSyncCompletePayload();
    public static final Identifier ID = BetterRailwaySystem.id("balise_asset_sync_complete");

    public static BaliseAssetSyncCompletePayload read(PacketByteBuf buf) {
        return INSTANCE;
    }

    public void write(PacketByteBuf buf) {
    }
}
