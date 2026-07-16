package org.dcstudio.minecart;

import com.google.gson.JsonObject;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.resource.ResourceManager;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.DataDrivenResource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// 从数据文件读取线路主题色的 RGB 和 BossBar 颜色映射。
public final class LineThemeColorData {
    private static final String RESOURCE_PATH = "/data/betterrailwaysystem/line_theme_colors.json";
    private static volatile Map<String, ColorEntry> colors = loadClasspath();

    private LineThemeColorData() {
    }

    public static int rgb(String serializedName, int fallback) {
        ColorEntry entry = colors.get(serializedName);
        return entry == null ? fallback : entry.rgb;
    }

    public static BossBar.Color bossBarColor(String serializedName, BossBar.Color fallback) {
        ColorEntry entry = colors.get(serializedName);
        return entry == null ? fallback : entry.bossBarColor;
    }

    public static void reload(ResourceManager manager) {
        colors = DataDrivenResource.readObject(manager, BetterRailwaySystem.id("line_theme_colors.json"))
                .map(LineThemeColorData::parse)
                .orElseGet(Map::of);
    }

    private static Map<String, ColorEntry> loadClasspath() {
        return DataDrivenResource.readObject(RESOURCE_PATH)
                .map(LineThemeColorData::parse)
                .orElseGet(Map::of);
    }

    private static Map<String, ColorEntry> parse(JsonObject root) {
        Map<String, ColorEntry> colors = new HashMap<>();
        JsonObject rawColors = root.getAsJsonObject("colors");
        if (rawColors == null) {
            return Map.of();
        }
        for (Map.Entry<String, com.google.gson.JsonElement> rawEntry : rawColors.entrySet()) {
            if (!rawEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject color = rawEntry.getValue().getAsJsonObject();
            int rgb = parseRgb(color.has("rgb") ? color.get("rgb").getAsString() : "");
            BossBar.Color bossBarColor = parseBossBarColor(color.has("bossbar") ? color.get("bossbar").getAsString() : "");
            colors.put(rawEntry.getKey(), new ColorEntry(rgb, bossBarColor));
        }
        return Map.copyOf(colors);
    }

    private static int parseRgb(String value) {
        String normalized = value.startsWith("#") ? value.substring(1) : value;
        try {
            return Integer.parseInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0x3C78D8;
        }
    }

    private static BossBar.Color parseBossBarColor(String value) {
        try {
            return BossBar.Color.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return BossBar.Color.BLUE;
        }
    }

    private record ColorEntry(int rgb, BossBar.Color bossBarColor) {
    }
}
