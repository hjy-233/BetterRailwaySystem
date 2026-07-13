package org.dcstudio.asset;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.network.BaliseAssetCatalogPayload;
import org.dcstudio.network.BaliseAssetSyncCompletePayload;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.dcstudio.network.SyncBaliseAssetPayload;
import org.dcstudio.network.UploadBaliseAssetPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

// 管理服务端素材库存储、上传分块重组与向客户端分发。
public final class ServerBaliseAssetLibrary {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int CHUNK_SIZE = 32 * 1024;
    private static final Path ROOT = FabricLoader.getInstance().getGameDir().resolve("betterrailwaysystem_server_assets");
    private static final Path IMAGE_DIR = ROOT.resolve("images");
    private static final Path SOUND_DIR = ROOT.resolve("sounds");
    private static final Path MANIFEST_PATH = ROOT.resolve("manifest.json");
    private static final Map<UUID, Map<String, PendingUpload>> PENDING_UPLOADS = new HashMap<>();
    private static final Map<String, String> IMAGE_UPLOADERS = new HashMap<>();
    private static final Map<String, String> SOUND_UPLOADERS = new HashMap<>();

    private ServerBaliseAssetLibrary() {
    }

    public static void initialize() {
        try {
            Files.createDirectories(IMAGE_DIR);
            Files.createDirectories(SOUND_DIR);
            loadManifest();
        } catch (IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to initialize server balise asset library", exception);
        }
    }

    public static boolean acceptUploadChunk(ServerPlayerEntity player, UploadBaliseAssetPayload payload) {
        BaliseAssetType assetType = BaliseAssetType.fromString(payload.assetType());
        String fileName = sanitizeFileName(payload.fileName(), assetType);
        if (fileName == null) {
            return false;
        }
        int chunkCount = Math.max(1, payload.chunkCount());
        int chunkIndex = payload.chunkIndex();
        if (chunkIndex < 0 || chunkIndex >= chunkCount) {
            return false;
        }
        Map<String, PendingUpload> uploads = PENDING_UPLOADS.computeIfAbsent(player.getUuid(), ignored -> new HashMap<>());
        String key = assetType.serializedName() + ":" + fileName;
        PendingUpload upload = uploads.computeIfAbsent(key, ignored -> new PendingUpload(assetType, fileName, chunkCount));
        if (upload.chunkCount() != chunkCount) {
            uploads.put(key, upload = new PendingUpload(assetType, fileName, chunkCount));
        }
        upload.storeChunk(chunkIndex, payload.data());
        if (!upload.isComplete()) {
            return true;
        }
        try {
            Files.createDirectories(directory(assetType));
            Files.write(directory(assetType).resolve(fileName), upload.joinBytes());
            uploaderMap(assetType).put(fileName, player.getName().getString());
            saveManifest();
            uploads.remove(key);
            if (uploads.isEmpty()) {
                PENDING_UPLOADS.remove(player.getUuid());
            }
            return true;
        } catch (IOException exception) {
            BetterRailwaySystem.LOGGER.warn("Failed to store uploaded balise asset {}", fileName, exception);
            uploads.remove(key);
            if (uploads.isEmpty()) {
                PENDING_UPLOADS.remove(player.getUuid());
            }
            return false;
        }
    }

    public static void syncAllPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            syncToPlayer(player);
        }
    }

    public static void syncToPlayer(ServerPlayerEntity player) {
        initialize();
        List<BaliseAssetCatalogPayload.Entry> imageFiles = listEntries(BaliseAssetType.IMAGE);
        List<BaliseAssetCatalogPayload.Entry> soundFiles = listEntries(BaliseAssetType.SOUND);
        BetterRailwaySystemNetworking.send(player, BaliseAssetCatalogPayload.ID, new BaliseAssetCatalogPayload(imageFiles, soundFiles));
        sendFiles(player, BaliseAssetType.IMAGE, imageFiles);
        sendFiles(player, BaliseAssetType.SOUND, soundFiles);
        BetterRailwaySystemNetworking.send(player, BaliseAssetSyncCompletePayload.ID, BaliseAssetSyncCompletePayload.INSTANCE);
    }

    private static void sendFiles(ServerPlayerEntity player, BaliseAssetType assetType, List<BaliseAssetCatalogPayload.Entry> fileEntries) {
        for (BaliseAssetCatalogPayload.Entry entry : fileEntries) {
            String fileName = entry.fileName();
            Path path = directory(assetType).resolve(fileName);
            try {
                byte[] data = Files.readAllBytes(path);
                int chunkCount = Math.max(1, (int) Math.ceil(data.length / (double) CHUNK_SIZE));
                for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                    int start = chunkIndex * CHUNK_SIZE;
                    int end = Math.min(data.length, start + CHUNK_SIZE);
                    byte[] chunk = java.util.Arrays.copyOfRange(data, start, end);
                    BetterRailwaySystemNetworking.send(player, SyncBaliseAssetPayload.ID, new SyncBaliseAssetPayload(
                            assetType.serializedName(),
                            fileName,
                            chunkIndex,
                            chunkCount,
                            chunk
                    ));
                }
            } catch (IOException exception) {
                BetterRailwaySystem.LOGGER.warn("Failed to send balise asset {}", fileName, exception);
            }
        }
    }

    private static List<BaliseAssetCatalogPayload.Entry> listEntries(BaliseAssetType assetType) {
        try (Stream<Path> stream = Files.list(directory(assetType))) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> sanitizeFileName(path.getFileName().toString(), assetType))
                    .filter(java.util.Objects::nonNull)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .map(fileName -> new BaliseAssetCatalogPayload.Entry(fileName, uploaderMap(assetType).getOrDefault(fileName, "")))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static Path directory(BaliseAssetType assetType) {
        return assetType == BaliseAssetType.SOUND ? SOUND_DIR : IMAGE_DIR;
    }

    private static String sanitizeFileName(String rawFileName, BaliseAssetType assetType) {
        if (rawFileName == null || rawFileName.isBlank()) {
            return null;
        }
        String fileName = rawFileName.replace('\\', '/');
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }
        if (fileName.isBlank() || !assetType.isAllowed(fileName) || fileName.contains("..")) {
            return null;
        }
        return fileName;
    }

    private static Map<String, String> uploaderMap(BaliseAssetType assetType) {
        return assetType == BaliseAssetType.SOUND ? SOUND_UPLOADERS : IMAGE_UPLOADERS;
    }

    private static void loadManifest() throws IOException {
        IMAGE_UPLOADERS.clear();
        SOUND_UPLOADERS.clear();
        if (!Files.exists(MANIFEST_PATH)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(MANIFEST_PATH)) {
            ManifestData manifestData = GSON.fromJson(reader, ManifestData.class);
            if (manifestData == null) {
                return;
            }
            if (manifestData.images != null) {
                IMAGE_UPLOADERS.putAll(manifestData.images);
            }
            if (manifestData.sounds != null) {
                SOUND_UPLOADERS.putAll(manifestData.sounds);
            }
        }
    }

    private static void saveManifest() throws IOException {
        ManifestData manifestData = new ManifestData();
        manifestData.images.putAll(IMAGE_UPLOADERS);
        manifestData.sounds.putAll(SOUND_UPLOADERS);
        try (Writer writer = Files.newBufferedWriter(MANIFEST_PATH)) {
            GSON.toJson(manifestData, writer);
        }
    }

    private static final class ManifestData {
        private final Map<String, String> images = new HashMap<>();
        private final Map<String, String> sounds = new HashMap<>();
    }

    private static final class PendingUpload {
        private final BaliseAssetType assetType;
        private final String fileName;
        private final byte[][] chunks;

        private PendingUpload(BaliseAssetType assetType, String fileName, int chunkCount) {
            this.assetType = assetType;
            this.fileName = fileName;
            this.chunks = new byte[chunkCount][];
        }

        private int chunkCount() {
            return chunks.length;
        }

        private void storeChunk(int index, byte[] data) {
            chunks[index] = data == null ? new byte[0] : data.clone();
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
}
