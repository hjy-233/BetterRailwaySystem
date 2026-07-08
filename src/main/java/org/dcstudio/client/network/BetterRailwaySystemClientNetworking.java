package org.dcstudio.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.dcstudio.network.OpenLineMapPayload;
import org.dcstudio.network.OpenStopRailEditorPayload;
import org.dcstudio.network.OpenTrainSpawnerEditorPayload;
import org.dcstudio.network.OpenBaliseEditorPayload;
import org.dcstudio.network.StationAnnouncementPayload;
import org.dcstudio.renderer.LineMapScreen;
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
        ClientPlayNetworking.registerGlobalReceiver(OpenLineMapPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new LineMapScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(StationAnnouncementPayload.ID, (payload, context) ->
                context.client().execute(() -> StationAnnouncementOverlay.show(payload, context.player()))
        );
    }
}
