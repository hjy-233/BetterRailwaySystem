package org.dcstudio.minecart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resource.ResourceManager;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.DataDrivenResource;

import java.util.ArrayList;
import java.util.List;

// 从数据文件定义原生配置界面的选项顺序。
public final class UiOptionData {
    private static final String RESOURCE_PATH = "/data/betterrailwaysystem/ui_options.json";
    private static volatile Data data = loadClasspath();

    private UiOptionData() {
    }

    public static List<StopRailWaitMode> stopRailWaitModes() {
        return data.stopRailWaitModes;
    }

    public static List<LineThemeColor> lineThemeColors() {
        return data.lineThemeColors;
    }

    public static void reload(ResourceManager manager) {
        data = DataDrivenResource.readObject(manager, BetterRailwaySystem.id("ui_options.json"))
                .map(UiOptionData::parse)
                .orElseGet(UiOptionData::fallbackData);
    }

    private static Data loadClasspath() {
        return DataDrivenResource.readObject(RESOURCE_PATH)
                .map(UiOptionData::parse)
                .orElseGet(UiOptionData::fallbackData);
    }

    private static Data parse(JsonObject root) {
        List<StopRailWaitMode> waitModes = new ArrayList<>(List.of(StopRailWaitMode.values()));
        List<LineThemeColor> lineThemeColors = new ArrayList<>(List.of(LineThemeColor.values()));
        JsonArray rawWaitModes = root.getAsJsonArray("stop_rail_wait_modes");
        if (rawWaitModes != null) {
            List<StopRailWaitMode> parsed = parseStopRailWaitModes(rawWaitModes);
            if (!parsed.isEmpty()) {
                waitModes.clear();
                waitModes.addAll(parsed);
            }
        }

        JsonArray rawColors = root.getAsJsonArray("line_theme_colors");
        if (rawColors != null) {
            List<LineThemeColor> parsed = parseLineThemeColors(rawColors);
            if (!parsed.isEmpty()) {
                lineThemeColors.clear();
                lineThemeColors.addAll(parsed);
            }
        }
        return new Data(List.copyOf(waitModes), List.copyOf(lineThemeColors));
    }

    private static Data fallbackData() {
        return new Data(List.of(StopRailWaitMode.values()), List.of(LineThemeColor.values()));
    }

    private static List<StopRailWaitMode> parseStopRailWaitModes(JsonArray rawModes) {
        List<StopRailWaitMode> modes = new ArrayList<>();
        for (JsonElement element : rawModes) {
            StopRailWaitMode mode = StopRailWaitMode.fromString(element.getAsString());
            if (!modes.contains(mode)) {
                modes.add(mode);
            }
        }
        return modes;
    }

    private static List<LineThemeColor> parseLineThemeColors(JsonArray rawColors) {
        List<LineThemeColor> colors = new ArrayList<>();
        for (JsonElement element : rawColors) {
            LineThemeColor color = LineThemeColor.fromString(element.getAsString());
            if (!colors.contains(color)) {
                colors.add(color);
            }
        }
        return colors;
    }

    private record Data(List<StopRailWaitMode> stopRailWaitModes, List<LineThemeColor> lineThemeColors) {
    }
}
