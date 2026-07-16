package org.dcstudio.client;

import net.fabricmc.api.ClientModInitializer;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.client.network.BetterRailwaySystemClientNetworking;
import org.dcstudio.renderer.DebugOverlay;
import org.dcstudio.renderer.StationAnnouncementOverlay;

public class BetterRailwaySystemClient implements ClientModInitializer {
    // 客户端入口，负责注册 HUD 和客户端收包。
    @Override
    public void onInitializeClient() {
        BaliseAssetLibrary.initialize();
        BetterRailwaySystemDebugClient.register();
        DebugMinecartTracker.register();
        BetterRailwaySystemClientNetworking.register();
        DebugOverlay.register();
        StationAnnouncementOverlay.register();
    }
}
