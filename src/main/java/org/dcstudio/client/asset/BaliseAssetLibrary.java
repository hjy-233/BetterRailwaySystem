package org.dcstudio.client.asset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.resource.ResourceType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

// 管理 balise 图片和语音素材库，并生成本地资源包。
public final class BaliseAssetLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String PACK_NAME = "betterrailwaysystem_library";
    private static final String PACK_ID = "file/" + PACK_NAME;
    private static final String NAMESPACE = "betterrailwaysystem_library";
    private static final Path PACK_ROOT = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(PACK_NAME);
    private static final Path ASSET_ROOT = PACK_ROOT.resolve("assets").resolve(NAMESPACE);
    private static final Path IMAGE_DIR = ASSET_ROOT.resolve("textures").resolve("balise");
    private static final Path SOUND_DIR = ASSET_ROOT.resolve("sounds").resolve("balise");
    private static final Path PACK_METADATA_PATH = PACK_ROOT.resolve("pack.mcmeta");
    private static final Path SOUNDS_JSON_PATH = ASSET_ROOT.resolve("sounds.json");

    private BaliseAssetLibrary() {
    }

    public static void initialize() {
        try {
            ensurePackStructure();
            rewriteSoundsJson();
        } catch (IOException ignored) {
        }
    }

    public static List<LibraryEntry> list(AssetType assetType) {
        try {
            ensurePackStructure();
            List<LibraryEntry> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(assetType.directory())) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> assetType.isAllowed(path.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .forEach(path -> entries.add(new LibraryEntry(
                                displayName(path),
                                assetType.identifierFor(path),
                                path
                        )));
            }
            return entries;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public static Optional<LibraryEntry> importFromDialog(MinecraftClient client, AssetType assetType) {
        Path selectedPath = chooseFile(assetType.dialogTitle().getString());
        if (selectedPath == null) {
            return Optional.empty();
        }

        try {
            ensurePackStructure();
            String fileName = selectedPath.getFileName().toString();
            if (!assetType.isAllowed(fileName)) {
                return Optional.empty();
            }

            String sanitizedFileName = sanitizeFileName(fileName, assetType.extension());
            Path targetPath = assetType.directory().resolve(sanitizedFileName);
            Files.copy(selectedPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            if (assetType == AssetType.SOUND) {
                rewriteSoundsJson();
            }
            enablePackAndReload(client);
            return Optional.of(new LibraryEntry(displayName(targetPath), assetType.identifierFor(targetPath), targetPath));
        } catch (IOException ignored) {
            return Optional.empty();
        }
    }

    public static void previewSound(String soundId) {
        if (soundId == null || soundId.isBlank()) {
            return;
        }
        Identifier identifier = Identifier.tryParse(soundId);
        if (identifier == null) {
            return;
        }
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(identifier), 1.0F));
    }

    private static void ensurePackStructure() throws IOException {
        Files.createDirectories(IMAGE_DIR);
        Files.createDirectories(SOUND_DIR);
        writePackMetadata();
    }

    private static void writePackMetadata() throws IOException {
        Map<String, Object> packSection = new LinkedHashMap<>();
        packSection.put("pack_format", SharedConstants.getGameVersion().getResourceVersion(ResourceType.CLIENT_RESOURCES));
        packSection.put("description", "BetterRailwaySystem asset library");

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("pack", packSection);

        try (Writer writer = Files.newBufferedWriter(PACK_METADATA_PATH)) {
            GSON.toJson(root, writer);
        }
    }

    private static void rewriteSoundsJson() throws IOException {
        Map<String, Object> sounds = new LinkedHashMap<>();
        try (Stream<Path> stream = Files.list(SOUND_DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> AssetType.SOUND.isAllowed(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(path -> {
                        String eventPath = soundEventPath(path);
                        Map<String, Object> soundEntry = new LinkedHashMap<>();
                        soundEntry.put("sounds", List.of(NAMESPACE + ":balise/" + baseName(path)));
                        sounds.put(eventPath, soundEntry);
                    });
        }

        try (Writer writer = Files.newBufferedWriter(SOUNDS_JSON_PATH)) {
            GSON.toJson(sounds, writer);
        }
    }

    private static void enablePackAndReload(MinecraftClient client) {
        client.getResourcePackManager().scanPacks();
        if (!client.options.resourcePacks.contains(PACK_ID)) {
            client.options.resourcePacks.add(PACK_ID);
            client.options.write();
        }
        client.options.addResourcePackProfilesToManager(client.getResourcePackManager());
        client.reloadResources();
    }

    private static Path chooseFile(String title) {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!osName.contains("mac")) {
            return null;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                "/usr/bin/osascript",
                "-e",
                "set selectedFile to choose file with prompt \"" + escapeAppleScript(title) + "\"",
                "-e",
                "POSIX path of selectedFile"
        );
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            String output;
            try (InputStream inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
            int exitCode = process.waitFor();
            if (exitCode != 0 || output.isBlank()) {
                return null;
            }
            return Path.of(output);
        } catch (IOException | InterruptedException ignored) {
            if (ignored instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private static String sanitizeFileName(String fileName, String extension) {
        String baseName = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex >= 0) {
            baseName = fileName.substring(0, dotIndex);
        }
        String sanitizedBase = baseName.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_\\-]+", "_");
        while (sanitizedBase.contains("__")) {
            sanitizedBase = sanitizedBase.replace("__", "_");
        }
        sanitizedBase = sanitizedBase.replaceAll("^_+|_+$", "");
        if (sanitizedBase.isEmpty()) {
            sanitizedBase = "asset";
        }
        return sanitizedBase + extension;
    }

    private static String displayName(Path path) {
        return baseName(path);
    }

    private static String baseName(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private static String soundEventPath(Path path) {
        return "balise." + baseName(path);
    }

    private static String escapeAppleScript(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record LibraryEntry(
            String displayName,
            String identifier,
            Path filePath
    ) {
    }

    public enum AssetType {
        IMAGE(".png", IMAGE_DIR, "screen.betterrailwaysystem.image_library"),
        SOUND(".ogg", SOUND_DIR, "screen.betterrailwaysystem.sound_library");

        private final String extension;
        private final Path directory;
        private final String titleKey;

        AssetType(String extension, Path directory, String titleKey) {
            this.extension = extension;
            this.directory = directory;
            this.titleKey = titleKey;
        }

        public String extension() {
            return extension;
        }

        public Path directory() {
            return directory;
        }

        public Text dialogTitle() {
            return Text.translatable(titleKey);
        }

        public boolean isAllowed(String fileName) {
            return fileName.toLowerCase(Locale.ROOT).endsWith(extension);
        }

        public String identifierFor(Path path) {
            return this == IMAGE
                    ? Identifier.of(NAMESPACE, "textures/balise/" + path.getFileName()).toString()
                    : Identifier.of(NAMESPACE, soundEventPath(path)).toString();
        }
    }
}
