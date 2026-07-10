package org.dcstudio.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.dcstudio.BetterRailwaySystem;

// 服务端通知客户端素材快照发送完成。
public record BaliseAssetSyncCompletePayload() implements CustomPayload {
    public static final BaliseAssetSyncCompletePayload INSTANCE = new BaliseAssetSyncCompletePayload();
    public static final Id<BaliseAssetSyncCompletePayload> ID = new Id<>(BetterRailwaySystem.id("balise_asset_sync_complete"));
    public static final PacketCodec<RegistryByteBuf, BaliseAssetSyncCompletePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
