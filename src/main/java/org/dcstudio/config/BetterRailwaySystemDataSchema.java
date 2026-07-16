package org.dcstudio.config;

import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.minecart.TrainSpawnDirection;

// 定义世界数据版本和旧存档迁移时使用的默认值。
public final class BetterRailwaySystemDataSchema {
    public static final String VERSION_KEY = "BetterRailwaySystemDataVersion";

    private static final String RESOURCE_PATH = "/data/betterrailwaysystem/world_data_schema.json";
    private static volatile Schema schema = loadClasspath();

    private BetterRailwaySystemDataSchema() {
    }

    public static int currentVersion() {
        return schema.version;
    }

    public static BaliseMode defaultBaliseMode() {
        return BaliseMode.fromString(schema.balise.mode);
    }

    public static int defaultBaliseImageDurationSeconds() {
        return schema.balise.imageDurationSeconds;
    }

    public static boolean defaultBaliseUpdateBossBar() {
        return schema.balise.updateBossBar;
    }

    public static double defaultBaliseSpeedLimitBps() {
        return schema.balise.speedLimitBps;
    }

    public static int defaultStopDistance() {
        return schema.stopRail.stopDistance;
    }

    public static int defaultStopDwellSeconds() {
        return schema.stopRail.dwellSeconds;
    }

    public static StopRailWaitMode defaultStopWaitMode() {
        return StopRailWaitMode.fromString(schema.stopRail.waitMode);
    }

    public static String defaultCityName() {
        return schema.trainSpawner.cityName;
    }

    public static String defaultLineId() {
        return schema.trainSpawner.lineId;
    }

    public static String defaultLineThemeColor() {
        return LineThemeColor.fromString(schema.trainSpawner.lineThemeColor).serializedName();
    }

    public static TrainSpawnDirection defaultTrainSpawnDirection() {
        return TrainSpawnDirection.fromString(schema.trainSpawner.direction);
    }

    public static int defaultTargetTrainCount() {
        return schema.trainSpawner.targetTrainCount;
    }

    public static int defaultSpawnerIntervalSeconds() {
        return schema.trainSpawner.spawnIntervalSeconds;
    }

    public static int defaultSpawnerCooldownTicks() {
        return defaultSpawnerIntervalSeconds() * 20;
    }

    public static String defaultMinecartLineThemeColor() {
        return defaultLineThemeColor();
    }

    public static void reload(ResourceManager manager) {
        schema = DataDrivenResource.readObject(manager, BetterRailwaySystem.id("world_data_schema.json"))
                .map(BetterRailwaySystemDataSchema::parse)
                .orElseGet(BetterRailwaySystemDataSchema::fallbackSchema);
    }

    private static Schema loadClasspath() {
        return DataDrivenResource.readObject(RESOURCE_PATH)
                .map(BetterRailwaySystemDataSchema::parse)
                .orElseGet(BetterRailwaySystemDataSchema::fallbackSchema);
    }

    private static Schema parse(JsonObject root) {
        Schema fallback = fallbackSchema();
        return new Schema(
                readInt(root, "version", fallback.version),
                readBalise(root.getAsJsonObject("balise"), fallback.balise),
                readStopRail(root.getAsJsonObject("stop_rail"), fallback.stopRail),
                readTrainSpawner(root.getAsJsonObject("train_spawner"), fallback.trainSpawner)
        );
    }

    private static BaliseDefaults readBalise(JsonObject json, BaliseDefaults fallback) {
        if (json == null) {
            return fallback;
        }
        return new BaliseDefaults(
                readString(json, "mode", fallback.mode),
                readInt(json, "image_duration_seconds", fallback.imageDurationSeconds),
                readBoolean(json, "update_bossbar", fallback.updateBossBar),
                readDouble(json, "speed_limit_bps", fallback.speedLimitBps)
        );
    }

    private static StopRailDefaults readStopRail(JsonObject json, StopRailDefaults fallback) {
        if (json == null) {
            return fallback;
        }
        return new StopRailDefaults(
                readInt(json, "stop_distance", fallback.stopDistance),
                readInt(json, "dwell_seconds", fallback.dwellSeconds),
                readString(json, "wait_mode", fallback.waitMode)
        );
    }

    private static TrainSpawnerDefaults readTrainSpawner(JsonObject json, TrainSpawnerDefaults fallback) {
        if (json == null) {
            return fallback;
        }
        return new TrainSpawnerDefaults(
                readString(json, "city_name", fallback.cityName),
                readString(json, "line_id", fallback.lineId),
                readString(json, "line_theme_color", fallback.lineThemeColor),
                readString(json, "direction", fallback.direction),
                readInt(json, "target_train_count", fallback.targetTrainCount),
                readInt(json, "spawn_interval_seconds", readInt(json, "cooldown_ticks", fallback.spawnIntervalSeconds * 20) / 20)
        );
    }

    private static String readString(JsonObject json, String key, String fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        String value = json.get(key).getAsString();
        return value.isBlank() ? fallback : value;
    }

    private static int readInt(JsonObject json, String key, int fallback) {
        try {
            return json.has(key) ? json.get(key).getAsInt() : fallback;
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return fallback;
        }
    }

    private static double readDouble(JsonObject json, String key, double fallback) {
        try {
            return json.has(key) ? json.get(key).getAsDouble() : fallback;
        } catch (NumberFormatException | UnsupportedOperationException ignored) {
            return fallback;
        }
    }

    private static boolean readBoolean(JsonObject json, String key, boolean fallback) {
        try {
            return json.has(key) ? json.get(key).getAsBoolean() : fallback;
        } catch (UnsupportedOperationException ignored) {
            return fallback;
        }
    }

    private static Schema fallbackSchema() {
        return new Schema(
                1,
                new BaliseDefaults("arrival", 5, true, 4.0),
                new StopRailDefaults(30, 3, "timer"),
                new TrainSpawnerDefaults("Default", "L1", "blue", "forward", 1, 60)
        );
    }

    private record Schema(int version, BaliseDefaults balise, StopRailDefaults stopRail, TrainSpawnerDefaults trainSpawner) {
    }

    private record BaliseDefaults(String mode, int imageDurationSeconds, boolean updateBossBar, double speedLimitBps) {
    }

    private record StopRailDefaults(int stopDistance, int dwellSeconds, String waitMode) {
    }

    private record TrainSpawnerDefaults(String cityName, String lineId, String lineThemeColor, String direction, int targetTrainCount, int spawnIntervalSeconds) {
    }
}
