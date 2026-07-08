package org.dcstudio.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.dcstudio.BetterRailwaySystem;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

// 读取并落盘配置文件，避免引入额外配置依赖。
public final class BetterRailwaySystemConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("betterrailwaysystem.json");

    private BetterRailwaySystemConfigManager() {
    }

    public static BetterRailwaySystemConfig load() {
        BetterRailwaySystemConfig config = new BetterRailwaySystemConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                BetterRailwaySystemConfig loaded = GSON.fromJson(reader, BetterRailwaySystemConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            } catch (IOException exception) {
                BetterRailwaySystem.LOGGER.warn("Failed to read config {}", CONFIG_PATH, exception);
            }
        }
        config.sanitize();
        save(config);
        return config;
    }

    public static void save(BetterRailwaySystemConfig config) {
        config.sanitize();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to save config {}", CONFIG_PATH, exception);
        }
    }
}
