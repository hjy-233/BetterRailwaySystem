package org.dcstudio.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

// 读取随模组打包的数据文件，失败时由调用方使用默认值兜底。
public final class DataDrivenResource {
    private DataDrivenResource() {
    }

    public static Optional<JsonObject> readObject(String path) {
        try (InputStream stream = DataDrivenResource.class.getResourceAsStream(path)) {
            if (stream == null) {
                BetterRailwaySystem.LOGGER.warn("Missing data-driven resource: {}", path);
                return Optional.empty();
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IllegalStateException | IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to read data-driven resource: {}", path, exception);
            return Optional.empty();
        }
    }

    public static Optional<JsonObject> readObject(ResourceManager manager, Identifier id) {
        Optional<Resource> resource = manager.getResource(id);
        if (resource.isEmpty()) {
            BetterRailwaySystem.LOGGER.warn("Missing data-driven resource: {}", id);
            return Optional.empty();
        }
        try (InputStream stream = resource.get().getInputStream();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IllegalStateException | IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to read data-driven resource: {}", id, exception);
            return Optional.empty();
        }
    }
}
