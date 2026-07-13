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
import net.minecraft.util.Util;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.asset.BaliseAssetType;
import org.dcstudio.network.BaliseAssetCatalogPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final Path STAGING_ROOT = FabricLoader.getInstance().getGameDir().resolve("betterrailwaysystem_upload_staging");
    private static final Path STAGING_IMAGE_DIR = STAGING_ROOT.resolve("images");
    private static final Path STAGING_SOUND_DIR = STAGING_ROOT.resolve("sounds");
    private static final Map<String, PendingAsset> PENDING_SYNC = new HashMap<>();
    private static final Map<String, String> IMAGE_UPLOADERS = new HashMap<>();
    private static final Map<String, String> SOUND_UPLOADERS = new HashMap<>();
    private static int libraryRevision;

    private BaliseAssetLibrary() {
    }

    public static void initialize() {
        try {
            ensurePackStructure();
            ensureStagingStructure();
            rewriteSoundsJson();
        } catch (IOException ignored) {
        }
    }

    public static List<LibraryEntry> list(BaliseAssetType assetType) {
        try {
            ensurePackStructure();
            List<LibraryEntry> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(assetDirectory(assetType))) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> assetType.isAllowed(path.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .forEach(path -> entries.add(new LibraryEntry(
                                displayName(path),
                                identifierFor(assetType, path),
                                path,
                                uploaderFor(assetType, path.getFileName().toString())
                        )));
            }
            return entries;
        } catch (IOException ignored) {
            return List.of();
        }
    }

    public static Path directory(BaliseAssetType assetType) {
        try {
            ensurePackStructure();
        } catch (IOException ignored) {
        }
        return assetDirectory(assetType);
    }

    public static Path stagingDirectory(BaliseAssetType assetType) {
        try {
            ensureStagingStructure();
        } catch (IOException ignored) {
        }
        return stagingAssetDirectory(assetType);
    }

    public static boolean openStagingDirectory(BaliseAssetType assetType) {
        Path directory = stagingDirectory(assetType);
        if (!Files.exists(directory)) {
            return false;
        }
        try {
            Util.getOperatingSystem().open(directory.toUri());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static List<UploadEntry> listUploadEntries(BaliseAssetType assetType) {
        try {
            ensureStagingStructure();
            List<UploadEntry> entries = new ArrayList<>();
            try (Stream<Path> stream = Files.list(stagingAssetDirectory(assetType))) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> assetType.isAllowed(path.getFileName().toString()))
                        .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                        .forEach(path -> {
                            try {
                                entries.add(new UploadEntry(assetType, path.getFileName().toString(), Files.readAllBytes(path)));
                            } catch (IOException exception) {
                                BetterRailwaySystem.LOGGER.warn("Failed to read staged balise asset {}", path, exception);
                            }
                        });
            }
            return entries;
        } catch (IOException ignored) {
            return List.of();
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

    public static void applyServerCatalog(List<BaliseAssetCatalogPayload.Entry> imageFiles, List<BaliseAssetCatalogPayload.Entry> soundFiles) {
        try {
            ensurePackStructure();
            deleteFilesNotInCatalog(BaliseAssetType.IMAGE, imageFiles.stream().map(BaliseAssetCatalogPayload.Entry::fileName).toList());
            deleteFilesNotInCatalog(BaliseAssetType.SOUND, soundFiles.stream().map(BaliseAssetCatalogPayload.Entry::fileName).toList());
            refreshUploaderMap(BaliseAssetType.IMAGE, imageFiles);
            refreshUploaderMap(BaliseAssetType.SOUND, soundFiles);
            PENDING_SYNC.clear();
        } catch (IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to apply balise asset catalog", exception);
        }
    }

    public static void acceptSyncedChunk(BaliseAssetType assetType, String fileName, int chunkIndex, int chunkCount, byte[] data) {
        String normalizedFileName = sanitizeFileName(fileName, assetType);
        if (normalizedFileName == null) {
            return;
        }
        String key = assetType.serializedName() + ":" + normalizedFileName;
        PendingAsset pendingAsset = PENDING_SYNC.computeIfAbsent(key, ignored -> new PendingAsset(assetType, normalizedFileName, chunkCount));
        if (pendingAsset.chunkCount() != Math.max(1, chunkCount)) {
            pendingAsset = new PendingAsset(assetType, normalizedFileName, chunkCount);
            PENDING_SYNC.put(key, pendingAsset);
        }
        if (chunkIndex < 0 || chunkIndex >= pendingAsset.chunkCount()) {
            return;
        }
        pendingAsset.storeChunk(chunkIndex, data);
        if (!pendingAsset.isComplete()) {
            return;
        }
        try {
            Files.createDirectories(directory(assetType));
            Files.write(directory(assetType).resolve(normalizedFileName), pendingAsset.joinBytes());
        } catch (IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to write synced balise asset {}", normalizedFileName, exception);
        } finally {
            PENDING_SYNC.remove(key);
        }
    }

    public static boolean finalizeServerSync(MinecraftClient client) {
        try {
            ensurePackStructure();
            rewriteSoundsJson();
            enablePackAndReload(client);
            libraryRevision++;
            return true;
        } catch (IOException | RuntimeException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to finalize synced balise assets", exception);
            return false;
        }
    }

    public static int libraryRevision() {
        return libraryRevision;
    }

    private static void ensurePackStructure() throws IOException {
        Files.createDirectories(IMAGE_DIR);
        Files.createDirectories(SOUND_DIR);
        writePackMetadata();
    }

    private static void ensureStagingStructure() throws IOException {
        Files.createDirectories(STAGING_IMAGE_DIR);
        Files.createDirectories(STAGING_SOUND_DIR);
    }

    private static void writePackMetadata() throws IOException {
        Map<String, Object> packSection = new LinkedHashMap<>();
        packSection.put("pack_format", SharedConstants.getGameVersion().packVersion(ResourceType.CLIENT_RESOURCES));
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
                    .filter(path -> BaliseAssetType.SOUND.isAllowed(path.getFileName().toString()))
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

    private static void deleteFilesNotInCatalog(BaliseAssetType assetType, List<String> expectedFiles) throws IOException {
        Set<String> allowed = Set.copyOf(expectedFiles.stream()
                .map(fileName -> sanitizeFileName(fileName, assetType))
                .filter(java.util.Objects::nonNull)
                .toList());
        try (Stream<Path> stream = Files.list(directory(assetType))) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !allowed.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException exception) {
                            BetterRailwaySystem.LOGGER.warn("Failed to delete stale balise asset {}", path, exception);
                        }
                    });
        }
    }

    private static void refreshUploaderMap(BaliseAssetType assetType, List<BaliseAssetCatalogPayload.Entry> entries) {
        Map<String, String> uploaders = uploaderMap(assetType);
        uploaders.clear();
        for (BaliseAssetCatalogPayload.Entry entry : entries) {
            String fileName = sanitizeFileName(entry.fileName(), assetType);
            if (fileName != null) {
                uploaders.put(fileName, entry.uploadedBy());
            }
        }
    }

    private static String uploaderFor(BaliseAssetType assetType, String fileName) {
        return uploaderMap(assetType).getOrDefault(fileName, "");
    }

    private static Map<String, String> uploaderMap(BaliseAssetType assetType) {
        return assetType == BaliseAssetType.SOUND ? SOUND_UPLOADERS : IMAGE_UPLOADERS;
    }

    private static String sanitizeFileName(String rawFileName, BaliseAssetType assetType) {
        if (rawFileName == null || rawFileName.isBlank()) {
            return null;
        }
        String fileName = rawFileName.replace('\\', '/');
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        if (fileName.isBlank() || fileName.contains("..") || !assetType.isAllowed(fileName)) {
            return null;
        }
        return fileName;
    }

    public record LibraryEntry(
            String displayName,
            String identifier,
            Path filePath,
            String uploadedBy
        ) {
    }

    public record UploadEntry(
            BaliseAssetType assetType,
            String fileName,
            byte[] data
    ) {
    }

    private static final class PendingAsset {
        private final BaliseAssetType assetType;
        private final String fileName;
        private final byte[][] chunks;

        private PendingAsset(BaliseAssetType assetType, String fileName, int chunkCount) {
            this.assetType = assetType;
            this.fileName = fileName;
            this.chunks = new byte[Math.max(1, chunkCount)][];
        }

        private int chunkCount() {
            return chunks.length;
        }

        private void storeChunk(int chunkIndex, byte[] data) {
            chunks[chunkIndex] = data == null ? new byte[0] : data.clone();
        }

        private boolean isComplete() {
            for (byte[] chunk : chunks) {
                if (chunk == null) {
                    return false;
                }
            }
            return true;
        }

        private byte[] joinBytes() throws IOException {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            for (byte[] chunk : chunks) {
                outputStream.write(chunk);
            }
            return outputStream.toByteArray();
        }
    }

    private static Path assetDirectory(BaliseAssetType assetType) {
        return assetType == BaliseAssetType.SOUND ? SOUND_DIR : IMAGE_DIR;
    }

    private static Path stagingAssetDirectory(BaliseAssetType assetType) {
        return assetType == BaliseAssetType.SOUND ? STAGING_SOUND_DIR : STAGING_IMAGE_DIR;
    }

    private static String identifierFor(BaliseAssetType assetType, Path path) {
        return assetType == BaliseAssetType.IMAGE
                ? Identifier.of(NAMESPACE, "textures/balise/" + path.getFileName()).toString()
                : Identifier.of(NAMESPACE, soundEventPath(path)).toString();
    }

}
