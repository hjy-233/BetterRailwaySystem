package org.dcstudio.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        ClientPlayNetworking.registerGlobalReceiver(OpenBaliseEditorPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new RailwayBaliseScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(OpenStopRailEditorPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new StopRailScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(OpenTrainSpawnerEditorPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new TrainSpawnerScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(StationAnnouncementPayload.ID, (payload, context) ->
                context.client().execute(() -> StationAnnouncementOverlay.show(payload, context.player()))
        );
        ClientPlayNetworking.registerGlobalReceiver(BaliseAssetCatalogPayload.ID, (payload, context) ->
                context.client().execute(() -> BaliseAssetLibrary.applyServerCatalog(payload.imageFiles(), payload.soundFiles()))
        );
        ClientPlayNetworking.registerGlobalReceiver(SyncBaliseAssetPayload.ID, (payload, context) ->
                context.client().execute(() -> BaliseAssetLibrary.acceptSyncedChunk(
                        BaliseAssetType.fromString(payload.assetType()),
                        payload.fileName(),
                        payload.chunkIndex(),
                        payload.chunkCount(),
                        payload.data()
                ))
        );
        ClientPlayNetworking.registerGlobalReceiver(BaliseAssetSyncCompletePayload.ID, (payload, context) ->
                context.client().execute(() -> BaliseAssetLibrary.finalizeServerSync(context.client()))
        );
    }
}
