package org.dcstudio.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.minecart.BetterRailwaySystemAccess;
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.RailwayCityState;
import org.dcstudio.minecart.RailwayLineState;
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
        PayloadTypeRegistry.playS2C().register(OpenLineMapPayload.ID, OpenLineMapPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RequestLineMapPayload.ID, RequestLineMapPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClearRailwayMapPayload.ID, ClearRailwayMapPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SaveBalisePayload.ID, (payload, context) ->
                context.server().execute(() -> saveBalise(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveStopRailPayload.ID, (payload, context) ->
                context.server().execute(() -> saveStopRail(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(SaveTrainSpawnerPayload.ID, (payload, context) ->
                context.server().execute(() -> saveTrainSpawner(context.player(), payload))
        );
        ServerPlayNetworking.registerGlobalReceiver(RequestLineMapPayload.ID, (payload, context) ->
                context.server().execute(() -> sendLineMap(context.player()))
        );
        ServerPlayNetworking.registerGlobalReceiver(ClearRailwayMapPayload.ID, (payload, context) ->
                context.server().execute(() -> clearRailwayMap(context.player(), payload))
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
        if (player.getWorld() instanceof ServerWorld serverWorld) {
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
                blockEntity.getIntervalSeconds(),
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
            blockEntity.setSettings(payload.cityName(), payload.lineId(), payload.lineThemeColor(), TrainSpawnDirection.fromString(payload.direction()), payload.intervalSeconds(), payload.redstoneControlled(), payload.circularLine());
        }
    }

    private static <T extends BlockEntity> T getNearbyBlockEntity(ServerPlayerEntity player, BlockPos pos, Class<T> type) {
        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return null;
        }
        BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
        return type.isInstance(blockEntity) ? type.cast(blockEntity) : null;
    }

    public static void clearRailwayMap(ServerPlayerEntity player, ClearRailwayMapPayload payload) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }
        boolean requiresPermission = player.getServer() != null && player.getServer().isDedicated();
        if (requiresPermission && !player.hasPermissionLevel(2)) {
            player.sendMessage(Text.translatable("screen.betterrailwaysystem.map_clear_no_permission"), false);
            return;
        }
        RailwayLineState lineState = RailwayLineState.get(serverWorld);
        boolean changed = switch (ClearRailwayMapPayload.Mode.fromString(payload.mode())) {
            case ALL -> lineState.clearAll();
            case CITY -> lineState.clearCity(payload.cityName());
            case LINE -> lineState.clearLine(payload.cityName(), payload.lineId());
        };
        if (!changed) {
            player.sendMessage(Text.translatable("screen.betterrailwaysystem.map_clear_failed"), false);
            return;
        }
        betterrailwaysystem$clearMinecartRouteCaches(serverWorld, payload);
        Text feedback = switch (ClearRailwayMapPayload.Mode.fromString(payload.mode())) {
            case ALL -> Text.translatable("screen.betterrailwaysystem.map_clear_all_done");
            case CITY -> Text.translatable("screen.betterrailwaysystem.map_clear_city_done", payload.cityName());
            case LINE -> Text.translatable("screen.betterrailwaysystem.map_clear_line_done", payload.cityName(), payload.lineId());
        };
        player.sendMessage(feedback, false);
    }

    private static void betterrailwaysystem$clearMinecartRouteCaches(ServerWorld world, ClearRailwayMapPayload payload) {
        ClearRailwayMapPayload.Mode mode = ClearRailwayMapPayload.Mode.fromString(payload.mode());
        String targetCity = payload.cityName();
        String targetLine = payload.lineId();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof AbstractMinecartEntity) || !(entity instanceof BetterRailwaySystemAccess access)) {
                continue;
            }
            if (!betterrailwaysystem$matchesClearTarget(access, mode, targetCity, targetLine)) {
                continue;
            }
            access.betterrailwaysystem$clearVisitedStations();
            access.betterrailwaysystem$setCurrentStation("");
            access.betterrailwaysystem$setNextStation("");
            if (access.betterrailwaysystem$isCircularLine()) {
                access.betterrailwaysystem$setCircularLine(true);
                BlockPos originSpawnerPos = access.betterrailwaysystem$getOriginSpawnerPos();
                if (originSpawnerPos != null) {
                    access.betterrailwaysystem$setOriginSpawnerPos(originSpawnerPos);
                }
            }
        }
    }

    private static boolean betterrailwaysystem$matchesClearTarget(BetterRailwaySystemAccess access, ClearRailwayMapPayload.Mode mode, String cityName, String lineId) {
        return switch (mode) {
            case ALL -> true;
            case CITY -> cityName != null && !cityName.isBlank() && cityName.equals(access.betterrailwaysystem$getCityName());
            case LINE -> cityName != null
                    && !cityName.isBlank()
                    && lineId != null
                    && !lineId.isBlank()
                    && cityName.equals(access.betterrailwaysystem$getCityName())
                    && lineId.equals(access.betterrailwaysystem$getLineId());
        };
    }

    private static void sendLineMap(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (player.getVehicle() instanceof AbstractMinecartEntity minecart && minecart instanceof BetterRailwaySystemAccess access) {
            String lineId = access.betterrailwaysystem$getLineId();
            if (lineId.isBlank()) {
                return;
            }
            String cityName = access.betterrailwaysystem$getCityName();
            List<String> stations = RailwayLineState.get(serverWorld).getLine(cityName, lineId);
            List<org.dcstudio.minecart.RailwayLineState.StationEntry> stationEntries = RailwayLineState.get(serverWorld).getLineStations(cityName, lineId);
            String lineThemeColor = RailwayLineState.get(serverWorld).getLineThemeColor(cityName, lineId);
            if (stations.isEmpty() || stationEntries.isEmpty()) {
                stations = access.betterrailwaysystem$getVisitedStations();
                List<net.minecraft.util.math.BlockPos> positions = access.betterrailwaysystem$getVisitedStationPositions();
                java.util.ArrayList<OpenLineMapPayload.StationEntry> fallbackEntries = new java.util.ArrayList<>(stations.size());
                for (int index = 0; index < stations.size(); index++) {
                    net.minecraft.util.math.BlockPos pos = index < positions.size() ? positions.get(index) : net.minecraft.util.math.BlockPos.ORIGIN;
                    fallbackEntries.add(new OpenLineMapPayload.StationEntry(stations.get(index), pos));
                }
                stationEntries = fallbackEntries.stream()
                        .map(entry -> new org.dcstudio.minecart.RailwayLineState.StationEntry(entry.stationName(), entry.pos()))
                        .toList();
                lineThemeColor = access.betterrailwaysystem$getLineThemeColor();
            }
            String displayLineId = cityName.isBlank() ? lineId : cityName + " / " + lineId;
            int color = LineThemeColor.fromString(lineThemeColor).rgb();
            List<OpenLineMapPayload.LineEntry> lines = List.of(new OpenLineMapPayload.LineEntry(
                    cityName,
                    lineId,
                    color,
                    stationEntries.stream()
                            .map(station -> new OpenLineMapPayload.StationEntry(station.stationName(), station.pos()))
                            .toList()
            ));
            ServerPlayNetworking.send(player, new OpenLineMapPayload(false, displayLineId, access.betterrailwaysystem$getCurrentStation(), color, lines));
            return;
        }

        List<OpenLineMapPayload.LineEntry> lineEntries = new java.util.ArrayList<>();
        RailwayLineState.get(serverWorld).getAllLines().forEach((cityName, cityLines) -> {
            cityLines.forEach((lineId, line) -> {
                int color = LineThemeColor.fromString(line.lineThemeColor()).rgb();
                lineEntries.add(new OpenLineMapPayload.LineEntry(
                        cityName,
                        lineId,
                        color,
                        line.stations().stream()
                                .map(station -> new OpenLineMapPayload.StationEntry(station.stationName(), station.pos()))
                                .toList()
                ));
            });
        });
        ServerPlayNetworking.send(player, new OpenLineMapPayload(true, "", "", 0xFFD966, lineEntries));
    }
}
