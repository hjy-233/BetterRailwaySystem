package org.dcstudio.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.dcstudio.network.RequestBaliseAssetSyncPayload;
import org.dcstudio.network.UploadBaliseAssetPayload;

// 统一判断当前连接是否支持服务端功能。
public final class BetterRailwaySystemClientSupport {
    private BetterRailwaySystemClientSupport() {
    }

    public static boolean hasServerMod() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            return true;
        }
        return client.player != null && ClientPlayNetworking.canSend(RequestBaliseAssetSyncPayload.ID);
    }

    public static boolean hasServerAssetSupport() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            return true;
        }
        return client.player != null
                && ClientPlayNetworking.canSend(UploadBaliseAssetPayload.ID)
                && ClientPlayNetworking.canSend(RequestBaliseAssetSyncPayload.ID);
    }

    public static void showServerUnavailableMessage() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("message.betterrailwaysystem.server_missing"), true);
        }
    }
}
