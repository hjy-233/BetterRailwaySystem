package org.dcstudio.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
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
        PayloadTypeRegistry.playS2C().register(OpenBaliseEditorPayload.ID, OpenBaliseEditorPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StationAnnouncementPayload.ID, StationAnnouncementPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveBalisePayload.ID, SaveBalisePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenStopRailEditorPayload.ID, OpenStopRailEditorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveStopRailPayload.ID, SaveStopRailPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenTrainSpawnerEditorPayload.ID, OpenTrainSpawnerEditorPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SaveTrainSpawnerPayload.ID, SaveTrainSpawnerPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UploadBaliseAssetPayload.ID, UploadBaliseAssetPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestBaliseAssetSyncPayload.ID, RequestBaliseAssetSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BaliseAssetCatalogPayload.ID, BaliseAssetCatalogPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncBaliseAssetPayload.ID, SyncBaliseAssetPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(BaliseAssetSyncCompletePayload.ID, BaliseAssetSyncCompletePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SaveBalisePayload.ID, (payload, context) ->
                context.server().execute(() -> saveBalise(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveStopRailPayload.ID, (payload, context) ->
                context.server().execute(() -> saveStopRail(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveTrainSpawnerPayload.ID, (payload, context) ->
                context.server().execute(() -> saveTrainSpawner(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(UploadBaliseAssetPayload.ID, (payload, context) ->
                context.server().execute(() -> uploadBaliseAsset(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestBaliseAssetSyncPayload.ID, (payload, context) ->
                context.server().execute(() -> requestBaliseAssetSync(context.player()))
        );
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                server.execute(() -> ServerBaliseAssetLibrary.syncToPlayer(handler.player))
        );
    }

    public static void openEditor(ServerPlayerEntity player, RailwayBaliseBlockEntity blockEntity) {
        ServerPlayNetworking.send(player, new OpenBaliseEditorPayload(
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
        ServerPlayNetworking.send(player, new StationAnnouncementPayload(
                titleText,
                subtitleText,
                blockEntity.getSoundId(),
                blockEntity.getImageId(),
                blockEntity.getImageDurationSeconds(),
                blockEntity.shouldKeepImageUntilNextBalise()
        ));
    }

    public static void openStopRailEditor(ServerPlayerEntity player, StopRailBlockEntity blockEntity) {
        ServerPlayNetworking.send(player, new OpenStopRailEditorPayload(
                blockEntity.getPos(),
                blockEntity.getStopDistance(),
                blockEntity.getDwellSeconds(),
                blockEntity.getWaitMode().serializedName()
        ));
    }

    public static void openTrainSpawnerEditor(ServerPlayerEntity player, TrainSpawnerBlockEntity blockEntity) {
        List<String> cityOptions = new java.util.ArrayList<>();
        cityOptions.add(blockEntity.getCityName());
        if (player.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (String city : RailwayCityState.get(serverWorld).getCities()) {
                if (!city.equals(blockEntity.getCityName())) {
                    cityOptions.add(city);
                }
            }
        }
        ServerPlayNetworking.send(player, new OpenTrainSpawnerEditorPayload(
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
        BlockEntity blockEntity = player.getEntityWorld().getBlockEntity(pos);
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
        ServerBaliseAssetLibrary.syncAllPlayers(player.getEntityWorld().getServer());
        player.sendMessage(Text.translatable("screen.betterrailwaysystem.asset_sync_broadcast"), false);
    }

    private static boolean betterrailwaysystem$canModifyServerAssets(ServerPlayerEntity player) {
        return !player.getEntityWorld().getServer().isDedicated()
                || player.getCommandSource().getPermissions().hasPermission(new Permission.Level(PermissionLevel.GAMEMASTERS));
    }
}
