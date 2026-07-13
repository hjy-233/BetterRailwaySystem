package org.dcstudio.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import org.dcstudio.asset.BaliseAssetType;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.network.BaliseAssetCatalogPayload;
import org.dcstudio.network.BaliseAssetSyncCompletePayload;
import org.dcstudio.network.OpenStopRailEditorPayload;
import org.dcstudio.network.OpenTrainSpawnerEditorPayload;
import org.dcstudio.network.OpenBaliseEditorPayload;
import org.dcstudio.network.SyncBaliseAssetPayload;
import org.dcstudio.network.StationAnnouncementPayload;
import org.dcstudio.renderer.RailwayBaliseScreen;
import org.dcstudio.renderer.StationAnnouncementOverlay;
import org.dcstudio.renderer.StopRailScreen;
import org.dcstudio.renderer.TrainSpawnerScreen;

// 客户端收包后直接打开编辑界面或刷新 HUD。
public final class BetterRailwaySystemClientNetworking {
    private BetterRailwaySystemClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenBaliseEditorPayload.ID, (client, handler, buf, responseSender) ->
                betterrailwaysystem$execute(client, () -> client.setScreen(new RailwayBaliseScreen(OpenBaliseEditorPayload.read(buf))))
        );
        ClientPlayNetworking.registerGlobalReceiver(OpenStopRailEditorPayload.ID, (client, handler, buf, responseSender) ->
                betterrailwaysystem$execute(client, () -> client.setScreen(new StopRailScreen(OpenStopRailEditorPayload.read(buf))))
        );
        ClientPlayNetworking.registerGlobalReceiver(OpenTrainSpawnerEditorPayload.ID, (client, handler, buf, responseSender) ->
                betterrailwaysystem$execute(client, () -> client.setScreen(new TrainSpawnerScreen(OpenTrainSpawnerEditorPayload.read(buf))))
        );
        ClientPlayNetworking.registerGlobalReceiver(StationAnnouncementPayload.ID, (client, handler, buf, responseSender) ->
                betterrailwaysystem$execute(client, () -> StationAnnouncementOverlay.show(StationAnnouncementPayload.read(buf), client.player))
        );
        ClientPlayNetworking.registerGlobalReceiver(BaliseAssetCatalogPayload.ID, (client, handler, buf, responseSender) -> {
            BaliseAssetCatalogPayload payload = BaliseAssetCatalogPayload.read(buf);
            betterrailwaysystem$execute(client, () -> BaliseAssetLibrary.applyServerCatalog(payload.imageFiles(), payload.soundFiles()));
        }
        );
        ClientPlayNetworking.registerGlobalReceiver(SyncBaliseAssetPayload.ID, (client, handler, buf, responseSender) -> {
            SyncBaliseAssetPayload payload = SyncBaliseAssetPayload.read(buf);
            betterrailwaysystem$execute(client, () -> BaliseAssetLibrary.acceptSyncedChunk(
                        BaliseAssetType.fromString(payload.assetType()),
                        payload.fileName(),
                        payload.chunkIndex(),
                        payload.chunkCount(),
                        payload.data()
                ));
        });
        ClientPlayNetworking.registerGlobalReceiver(BaliseAssetSyncCompletePayload.ID, (client, handler, buf, responseSender) ->
                betterrailwaysystem$execute(client, () -> BaliseAssetLibrary.finalizeServerSync(client))
        );
    }

    private static void betterrailwaysystem$execute(MinecraftClient client, Runnable runnable) {
        client.execute(runnable);
    }
}
