package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

// 客户端请求服务端广播当前素材库快照。
public record RequestBaliseAssetSyncPayload() {
    public static final RequestBaliseAssetSyncPayload INSTANCE = new RequestBaliseAssetSyncPayload();
    public static final Identifier ID = BetterRailwaySystem.id("request_balise_asset_sync");

    public static RequestBaliseAssetSyncPayload read(PacketByteBuf buf) {
        return INSTANCE;
    }

    public void write(PacketByteBuf buf) {
    }
}
