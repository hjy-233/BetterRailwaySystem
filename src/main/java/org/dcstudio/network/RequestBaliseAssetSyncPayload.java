package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 客户端请求服务端广播当前素材库快照。
public record RequestBaliseAssetSyncPayload() implements CustomPayload {
    public static final RequestBaliseAssetSyncPayload INSTANCE = new RequestBaliseAssetSyncPayload();
    public static final Id<RequestBaliseAssetSyncPayload> ID = new Id<>(BetterRailwaySystem.id("request_balise_asset_sync"));
    public static final PacketCodec<RegistryByteBuf, RequestBaliseAssetSyncPayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
