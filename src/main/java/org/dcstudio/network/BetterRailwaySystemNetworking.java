package org.dcstudio.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.asset.ServerBaliseAssetLibrary;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.minecart.RailwayCityState;
import org.dcstudio.station.RailwayBaliseBlockEntity;
import org.dcstudio.station.StopRailBlockEntity;
import org.dcstudio.station.TrainSpawnerBlockEntity;

import java.util.List;

// 集中注册服务端网络，并处理铁路配置界面保存。
public final class BetterRailwaySystemNetworking {
    private BetterRailwaySystemNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SaveBalisePayload.ID, (server, player, handler, buf, responseSender) ->
                server.execute(() -> saveBalise(player, SaveBalisePayload.read(buf)))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveStopRailPayload.ID, (server, player, handler, buf, responseSender) ->
                server.execute(() -> saveStopRail(player, SaveStopRailPayload.read(buf)))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveTrainSpawnerPayload.ID, (server, player, handler, buf, responseSender) ->
                server.execute(() -> saveTrainSpawner(player, SaveTrainSpawnerPayload.read(buf)))
        );
        ServerPlayNetworking.registerGlobalReceiver(UploadBaliseAssetPayload.ID, (server, player, handler, buf, responseSender) ->
                server.execute(() -> uploadBaliseAsset(player, UploadBaliseAssetPayload.read(buf)))
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestBaliseAssetSyncPayload.ID, (server, player, handler, buf, responseSender) ->
                server.execute(() -> requestBaliseAssetSync(player))
        );
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ServerBaliseAssetLibrary.syncToPlayer(handler.player))
        );
    }

    public static void openEditor(ServerPlayerEntity player, RailwayBaliseBlockEntity blockEntity) {
        send(player, OpenBaliseEditorPayload.ID, new OpenBaliseEditorPayload(
                blockEntity.getPos(),
                blockEntity.getMode().serializedName(),
                blockEntity.getTitleText(),
                blockEntity.getSubtitleText(),
                blockEntity.getCurrentStation(),
                blockEntity.getNextStation(),
                blockEntity.getSoundId(),
                blockEntity.getImageId(),
                blockEntity.getImageDurationSeconds(),
                blockEntity.shouldKeepImageUntilNextBalise(),
                blockEntity.shouldUpdateBossBar(),
                blockEntity.getSpeedLimitBps()
        ));
    }

    public static void sendAnnouncement(ServerPlayerEntity player, RailwayBaliseBlockEntity blockEntity) {
        sendAnnouncement(player, blockEntity.getTitleText(), blockEntity.getSubtitleText(), blockEntity);
    }

    public static void sendAnnouncement(ServerPlayerEntity player, String titleText, String subtitleText, RailwayBaliseBlockEntity blockEntity) {
        send(player, StationAnnouncementPayload.ID, new StationAnnouncementPayload(
                titleText,
                subtitleText,
                blockEntity.getSoundId(),
                blockEntity.getImageId(),
                blockEntity.getImageDurationSeconds(),
                blockEntity.shouldKeepImageUntilNextBalise()
        ));
    }

    public static void openStopRailEditor(ServerPlayerEntity player, StopRailBlockEntity blockEntity) {
        send(player, OpenStopRailEditorPayload.ID, new OpenStopRailEditorPayload(
                blockEntity.getPos(),
                blockEntity.getStopDistance(),
                blockEntity.getDwellSeconds(),
                blockEntity.getWaitMode().serializedName()
        ));
    }

    public static void openTrainSpawnerEditor(ServerPlayerEntity player, TrainSpawnerBlockEntity blockEntity) {
        List<String> cityOptions = new java.util.ArrayList<>();
        cityOptions.add(blockEntity.getCityName());
        if (player.getWorld() instanceof ServerWorld serverWorld) {
            for (String city : RailwayCityState.get(serverWorld).getCities()) {
                if (!city.equals(blockEntity.getCityName())) {
                    cityOptions.add(city);
                }
            }
        }
        send(player, OpenTrainSpawnerEditorPayload.ID, new OpenTrainSpawnerEditorPayload(
                blockEntity.getPos(),
                blockEntity.getLineId(),
                blockEntity.getLineThemeColor(),
                blockEntity.getDirection().serializedName(),
                blockEntity.getTargetTrainCount(),
                (blockEntity.isRedstoneControlled() ? 1 : 0) | (blockEntity.isCircularLine() ? 2 : 0),
                cityOptions
        ));
    }

    private static void saveBalise(ServerPlayerEntity player, SaveBalisePayload payload) {
        RailwayBaliseBlockEntity blockEntity = getNearbyBlockEntity(player, payload.pos(), RailwayBaliseBlockEntity.class);
        if (blockEntity != null) {
            blockEntity.setSettings(
                    payload.parsedMode(),
                    payload.titleText(),
                    payload.subtitleText(),
                    payload.currentStation(),
                    payload.nextStation(),
                    payload.soundId(),
                    payload.imageId(),
                    payload.imageDurationSeconds(),
                    payload.keepImageUntilNextBalise(),
                    payload.updateBossBar(),
                    payload.speedLimitBps()
            );
        }
    }

    private static void saveStopRail(ServerPlayerEntity player, SaveStopRailPayload payload) {
        StopRailBlockEntity blockEntity = getNearbyBlockEntity(player, payload.pos(), StopRailBlockEntity.class);
        if (blockEntity != null) {
            blockEntity.setSettings(payload.stopDistance(), payload.dwellSeconds(), StopRailWaitMode.fromString(payload.waitMode()));
        }
    }

    private static void saveTrainSpawner(ServerPlayerEntity player, SaveTrainSpawnerPayload payload) {
        TrainSpawnerBlockEntity blockEntity = getNearbyBlockEntity(player, payload.pos(), TrainSpawnerBlockEntity.class);
        if (blockEntity != null) {
            blockEntity.setSettings(payload.cityName(), payload.lineId(), payload.lineThemeColor(), TrainSpawnDirection.fromString(payload.direction()), payload.targetTrainCount(), payload.redstoneControlled(), payload.circularLine());
        }
    }

    private static <T extends BlockEntity> T getNearbyBlockEntity(ServerPlayerEntity player, BlockPos pos, Class<T> type) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return null;
        }
        BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    private static void uploadBaliseAsset(ServerPlayerEntity player, UploadBaliseAssetPayload payload) {
        if (!betterrailwaysystem$canModifyServerAssets(player)) {
            player.sendMessage(Text.translatable("screen.betterrailwaysystem.asset_upload_no_permission"), false);
            return;
        }
        boolean stored = ServerBaliseAssetLibrary.acceptUploadChunk(player, payload);
        if (!stored) {
            player.sendMessage(Text.translatable("screen.betterrailwaysystem.asset_upload_failed"), false);
        }
    }

    private static void requestBaliseAssetSync(ServerPlayerEntity player) {
        ServerBaliseAssetLibrary.syncAllPlayers(player.getServer());
        player.sendMessage(Text.translatable("screen.betterrailwaysystem.asset_sync_broadcast"), false);
    }

    private static boolean betterrailwaysystem$canModifyServerAssets(ServerPlayerEntity player) {
        return player.getServer() == null || !player.getServer().isDedicated() || player.hasPermissionLevel(2);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, OpenBaliseEditorPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, StationAnnouncementPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, OpenStopRailEditorPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, OpenTrainSpawnerEditorPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, BaliseAssetCatalogPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, SyncBaliseAssetPayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }

    public static void send(ServerPlayerEntity player, net.minecraft.util.Identifier channel, BaliseAssetSyncCompletePayload payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        payload.write(buf);
        ServerPlayNetworking.send(player, channel, buf);
    }
}
