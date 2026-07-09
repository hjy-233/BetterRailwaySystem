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

import java.awt.Desktop;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public static Path directory(AssetType assetType) {
        try {
            ensurePackStructure();
        } catch (IOException ignored) {
        }
        return assetType.directory();
    }

    public static boolean openDirectory(AssetType assetType) {
        Path directory = directory(assetType);
        if (!Files.exists(directory)) {
            return false;
        }
        if (!Desktop.isDesktopSupported()) {
            return false;
        }
        try {
            Desktop.getDesktop().open(directory.toFile());
            return true;
        } catch (IOException | UnsupportedOperationException ignored) {
            return false;
        }
    }

    public static boolean reloadLibrary(MinecraftClient client, AssetType assetType) {
        try {
            ensurePackStructure();
            if (assetType == AssetType.SOUND) {
                rewriteSoundsJson();
            }
            enablePackAndReload(client);
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
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
