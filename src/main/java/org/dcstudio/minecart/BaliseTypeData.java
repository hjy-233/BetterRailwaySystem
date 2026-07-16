package org.dcstudio.minecart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.DataDrivenResource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 从数据文件定义铁路信标类型的界面字段和显示顺序。
public final class BaliseTypeData {
    public static final String FIELD_TRIGGER_DIRECTION = "trigger_direction";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUBTITLE = "subtitle";
    public static final String FIELD_CURRENT_STATION = "current_station";
    public static final String FIELD_NEXT_STATION = "next_station";
    public static final String FIELD_SOUND = "sound";
    public static final String FIELD_IMAGE = "image";
    public static final String FIELD_IMAGE_DURATION = "image_duration";
    public static final String FIELD_KEEP_IMAGE = "keep_image";
    public static final String FIELD_BOSS_BAR = "bossbar";
    public static final String FIELD_SPEED_LIMIT = "speed_limit";

    private static final String RESOURCE_PATH = "/data/betterrailwaysystem/balise_types.json";
    private static volatile Data data = loadClasspath();

    private BaliseTypeData() {
    }

    public static List<BaliseMode> modes() {
        return data.modes;
    }

    public static boolean shows(BaliseMode mode, String field) {
        return data.fields.getOrDefault(mode, Set.of()).contains(field);
    }

    public static void reload(ResourceManager manager) {
        data = DataDrivenResource.readObject(manager, BetterRailwaySystem.id("balise_types.json"))
                .map(BaliseTypeData::parse)
                .orElseGet(BaliseTypeData::fallbackData);
    }

    private static Data loadClasspath() {
        return DataDrivenResource.readObject(RESOURCE_PATH)
                .map(BaliseTypeData::parse)
                .orElseGet(BaliseTypeData::fallbackData);
    }

    private static Data parse(JsonObject root) {
        Map<BaliseMode, Set<String>> fields = fallbackFields();
        List<BaliseMode> modes = new ArrayList<>(List.of(BaliseMode.values()));
        JsonArray order = root.getAsJsonArray("order");
        if (order != null) {
            List<BaliseMode> parsedOrder = parseModeOrder(order);
            if (!parsedOrder.isEmpty()) {
                modes.clear();
                modes.addAll(parsedOrder);
            }
        }

        JsonObject types = root.getAsJsonObject("types");
        if (types != null) {
            for (BaliseMode mode : BaliseMode.values()) {
                JsonObject type = types.getAsJsonObject(mode.serializedName());
                if (type == null) {
                    continue;
                }
                JsonArray rawFields = type.getAsJsonArray("fields");
                if (rawFields != null) {
                    fields.put(mode, parseFieldSet(rawFields));
                }
            }
        }
        return new Data(List.copyOf(modes), Map.copyOf(fields));
    }

    private static Data fallbackData() {
        return new Data(List.of(BaliseMode.values()), Map.copyOf(fallbackFields()));
    }

    private static List<BaliseMode> parseModeOrder(JsonArray order) {
        List<BaliseMode> modes = new ArrayList<>();
        for (JsonElement element : order) {
            BaliseMode mode = BaliseMode.fromString(element.getAsString());
            if (!modes.contains(mode)) {
                modes.add(mode);
            }
        }
        return modes;
    }

    private static Set<String> parseFieldSet(JsonArray rawFields) {
        Set<String> fields = new HashSet<>();
        for (JsonElement element : rawFields) {
            fields.add(element.getAsString());
        }
        return Set.copyOf(fields);
    }

    private static Map<BaliseMode, Set<String>> fallbackFields() {
        Map<BaliseMode, Set<String>> fields = new EnumMap<>(BaliseMode.class);
        fields.put(BaliseMode.ARRIVAL, Set.of(FIELD_TRIGGER_DIRECTION, FIELD_CURRENT_STATION, FIELD_NEXT_STATION, FIELD_BOSS_BAR));
        fields.put(BaliseMode.DEPARTURE, Set.of(FIELD_TRIGGER_DIRECTION, FIELD_CURRENT_STATION, FIELD_NEXT_STATION, FIELD_BOSS_BAR));
        fields.put(BaliseMode.ANNOUNCEMENT, Set.of(
                FIELD_TRIGGER_DIRECTION,
                FIELD_TITLE,
                FIELD_SUBTITLE,
                FIELD_CURRENT_STATION,
                FIELD_NEXT_STATION,
                FIELD_SOUND,
                FIELD_IMAGE,
                FIELD_IMAGE_DURATION,
                FIELD_KEEP_IMAGE,
                FIELD_BOSS_BAR
        ));
        fields.put(BaliseMode.SPEED_LIMIT_START, Set.of(FIELD_TRIGGER_DIRECTION, FIELD_SPEED_LIMIT));
        fields.put(BaliseMode.SPEED_LIMIT_END, Set.of(
                FIELD_TRIGGER_DIRECTION,
                FIELD_TITLE,
                FIELD_SUBTITLE,
                FIELD_SOUND,
                FIELD_IMAGE,
                FIELD_IMAGE_DURATION,
                FIELD_KEEP_IMAGE
        ));
        return fields;
    }

    private record Data(List<BaliseMode> modes, Map<BaliseMode, Set<String>> fields) {
    }
}
