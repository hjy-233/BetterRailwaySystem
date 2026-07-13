package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.dcstudio.asset.BaliseAssetType;
import org.dcstudio.client.BetterRailwaySystemClientSupport;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.network.RequestBaliseAssetSyncPayload;
import org.dcstudio.network.UploadBaliseAssetPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// 使用原生 Screen 浏览、上传、选择 balise 图片和音频素材。
public final class BaliseAssetLibraryScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 290;
    private static final int CHUNK_SIZE = 32 * 1024;

    private final Screen parent;
    private final BaliseAssetType assetType;
    private final Consumer<String> applySelection;
    private final List<BaliseAssetLibrary.LibraryEntry> entries = new ArrayList<>();
    private String selectedIdentifier;
    private AssetListWidget listWidget;
    private ButtonWidget useButton;
    private ButtonWidget clearButton;
    private Text statusText = Text.empty();
    private String selectedUploader = "";
    private int observedLibraryRevision = -1;
    private int previewImageWidth;
    private int previewImageHeight;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    public BaliseAssetLibraryScreen(Screen parent, BaliseAssetType assetType, String selectedIdentifier, Consumer<String> applySelection) {
        super(assetType.dialogTitle());
        this.parent = parent;
        this.assetType = assetType;
        this.selectedIdentifier = selectedIdentifier == null ? "" : selectedIdentifier;
        this.applySelection = applySelection;
    }

    @Override
    protected void init() {
        super.init();
        betterrailwaysystem$layoutBounds();
        int footerY = panelY + panelHeight - 26;
        int listX = panelX + 10;
        int listY = panelY + 54;
        int listWidth = 170;
        int listHeight = footerY - listY - 28;

        reloadEntries();

        listWidget = new AssetListWidget(client, listX, listY, listWidth, listHeight);
        addDrawableChild(listWidget);

        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.open_folder"), button -> betterrailwaysystem$openFolder())
                .dimensions(listX, footerY, 82, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.refresh_assets"), button -> betterrailwaysystem$refreshAssets())
                .dimensions(listX + 88, footerY, 82, 20)
                .build());
        useButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.use_selected"), button -> useSelected())
                .dimensions(panelX + panelWidth - 274, footerY, 82, 20)
                .build());
        clearButton = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.clear_selected"), button -> clearSelection())
                .dimensions(panelX + panelWidth - 186, footerY, 82, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.back"), button -> close())
                .dimensions(panelX + panelWidth - 98, footerY, 82, 20)
                .build());

        betterrailwaysystem$refreshButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        betterrailwaysystem$refreshEntriesIfNeeded();
        super.renderBackground(context, mouseX, mouseY, delta);
        int previewX = panelX + 190;
        int previewY = panelY + 54;
        int previewWidth = panelWidth - 202;
        int previewHeight = panelHeight - 102;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0101010);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.betterrailwaysystem.selected_asset"), panelX + 10, panelY + 32, 0xD0D0D0);
        context.drawTextWithShadow(textRenderer, selectedIdentifier.isBlank() ? "-" : selectedIdentifier, panelX + 96, panelY + 32, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.betterrailwaysystem.asset_uploader", selectedUploader.isBlank() ? "-" : selectedUploader), panelX + 10, panelY + 42, 0xC0C0C0);
        context.drawTextWithShadow(textRenderer, statusText, panelX + 10, panelY + panelHeight - 44, 0xAAAAAA);

        context.fill(previewX, previewY, previewX + previewWidth, previewY + previewHeight, 0x66000000);
        context.drawBorder(previewX, previewY, previewWidth, previewHeight, 0xFF5A5A5A);
        betterrailwaysystem$renderPreview(context, previewX, previewY, previewWidth, previewHeight);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void reloadEntries() {
        entries.clear();
        entries.addAll(BaliseAssetLibrary.list(assetType));
        observedLibraryRevision = BaliseAssetLibrary.libraryRevision();
        previewImageWidth = 0;
        previewImageHeight = 0;
        selectedUploader = entries.stream()
                .filter(entry -> entry.identifier().equals(selectedIdentifier))
                .map(BaliseAssetLibrary.LibraryEntry::uploadedBy)
                .findFirst()
                .orElse("");
        betterrailwaysystem$loadPreviewMetadata();
    }

    private void selectIdentifier(String identifier) {
        selectedIdentifier = identifier == null ? "" : identifier;
        if (assetType == BaliseAssetType.SOUND && !selectedIdentifier.isBlank()) {
            BaliseAssetLibrary.previewSound(selectedIdentifier);
        }
        selectedUploader = entries.stream()
                .filter(entry -> entry.identifier().equals(selectedIdentifier))
                .map(BaliseAssetLibrary.LibraryEntry::uploadedBy)
                .findFirst()
                .orElse("");
        betterrailwaysystem$loadPreviewMetadata();
        betterrailwaysystem$refreshButtons();
    }

    private void betterrailwaysystem$openFolder() {
        if (BaliseAssetLibrary.openStagingDirectory(assetType)) {
            statusText = Text.translatable("screen.betterrailwaysystem.folder_opened");
        } else {
            statusText = Text.translatable("screen.betterrailwaysystem.folder_open_failed");
        }
    }

    private void betterrailwaysystem$refreshAssets() {
        if (client == null) {
            statusText = Text.translatable("screen.betterrailwaysystem.refresh_failed");
            return;
        }
        if (!BetterRailwaySystemClientSupport.hasServerAssetSupport()) {
            BetterRailwaySystemClientSupport.showServerUnavailableMessage();
            statusText = Text.translatable("message.betterrailwaysystem.server_missing");
            return;
        }
        if (!betterrailwaysystem$uploadStagedAssets()) {
            statusText = Text.translatable("screen.betterrailwaysystem.refresh_failed");
            return;
        }
        net.minecraft.network.PacketByteBuf requestBuf = PacketByteBufs.create();
        RequestBaliseAssetSyncPayload.INSTANCE.write(requestBuf);
        ClientPlayNetworking.send(RequestBaliseAssetSyncPayload.ID, requestBuf);
        statusText = Text.translatable("screen.betterrailwaysystem.sync_requested");
    }

    private void useSelected() {
        if (selectedIdentifier.isBlank()) {
            return;
        }
        applySelection.accept(selectedIdentifier);
        close();
    }

    private void clearSelection() {
        applySelection.accept("");
        selectedUploader = "";
        close();
    }

    private void betterrailwaysystem$refreshButtons() {
        boolean hasSelection = !selectedIdentifier.isBlank();
        if (useButton != null) {
            useButton.active = hasSelection;
        }
        if (clearButton != null) {
            clearButton.active = true;
        }
    }

    private void betterrailwaysystem$layoutBounds() {
        panelWidth = Math.max(320, Math.min(PANEL_WIDTH, width - 40));
        panelHeight = Math.max(220, Math.min(PANEL_HEIGHT, height - 40));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    private void betterrailwaysystem$loadPreviewMetadata() {
        previewImageWidth = 0;
        previewImageHeight = 0;
        if (assetType != BaliseAssetType.IMAGE || selectedIdentifier.isBlank()) {
            return;
        }
        Identifier identifier = Identifier.tryParse(selectedIdentifier);
        if (identifier == null || client == null) {
            return;
        }
        Optional<net.minecraft.resource.Resource> resource = client.getResourceManager().getResource(identifier);
        if (resource.isEmpty()) {
            return;
        }
        try (InputStream inputStream = resource.get().getInputStream(); NativeImage image = NativeImage.read(inputStream)) {
            previewImageWidth = image.getWidth();
            previewImageHeight = image.getHeight();
        } catch (IOException ignored) {
            previewImageWidth = 0;
            previewImageHeight = 0;
        }
    }

    private void betterrailwaysystem$renderPreview(DrawContext context, int x, int y, int width, int height) {
        if (selectedIdentifier.isBlank()) {
            context.drawCenteredTextWithShadow(textRenderer, Text.translatable("screen.betterrailwaysystem.no_assets"), x + width / 2, y + height / 2 - 4, 0xAAAAAA);
            return;
        }

        if (assetType == BaliseAssetType.SOUND) {
            betterrailwaysystem$drawWrappedCentered(context, selectedIdentifier, x + 8, y + 8, width - 16, height - 16, 0xFFFFFF);
            return;
        }

        Identifier identifier = Identifier.tryParse(selectedIdentifier);
        if (identifier == null || previewImageWidth <= 0 || previewImageHeight <= 0) {
            betterrailwaysystem$drawWrappedCentered(context, selectedIdentifier, x + 8, y + 8, width - 16, height - 16, 0xFFFFFF);
            return;
        }

        float scale = Math.min((float) (width - 8) / previewImageWidth, (float) (height - 8) / previewImageHeight);
        scale = Math.max(scale, 0.01F);
        int drawWidth = Math.max(1, Math.round(previewImageWidth * scale));
        int drawHeight = Math.max(1, Math.round(previewImageHeight * scale));
        int drawX = x + (width - drawWidth) / 2;
        int drawY = y + (height - drawHeight) / 2;
        context.drawTexture(identifier, drawX, drawY, drawWidth, drawHeight, 0.0F, 0.0F, previewImageWidth, previewImageHeight, previewImageWidth, previewImageHeight);
    }

    private void betterrailwaysystem$drawWrappedCentered(DrawContext context, String raw, int x, int y, int width, int height, int color) {
        List<String> lines = betterrailwaysystem$wrapPlainText(raw, Math.max(20, width));
        int totalHeight = lines.size() * 10;
        int drawY = y + Math.max(0, (height - totalHeight) / 2);
        for (String line : lines) {
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(line), x + width / 2, drawY, color);
            drawY += 10;
        }
    }

    private List<String> betterrailwaysystem$wrapPlainText(String raw, int width) {
        List<String> lines = new ArrayList<>();
        if (raw.isBlank()) {
            lines.add("");
            return lines;
        }
        String remaining = raw;
        while (!remaining.isEmpty()) {
            int bestLength = remaining.length();
            while (bestLength > 1 && textRenderer.getWidth(remaining.substring(0, bestLength)) > width) {
                bestLength--;
            }
            lines.add(remaining.substring(0, bestLength));
            remaining = remaining.substring(bestLength);
        }
        return lines;
    }

    private boolean betterrailwaysystem$uploadStagedAssets() {
        for (BaliseAssetLibrary.UploadEntry uploadEntry : BaliseAssetLibrary.listUploadEntries(assetType)) {
            int chunkCount = Math.max(1, (int) Math.ceil(uploadEntry.data().length / (double) CHUNK_SIZE));
            for (int chunkIndex = 0; chunkIndex < chunkCount; chunkIndex++) {
                int start = chunkIndex * CHUNK_SIZE;
                int end = Math.min(uploadEntry.data().length, start + CHUNK_SIZE);
                byte[] chunk = java.util.Arrays.copyOfRange(uploadEntry.data(), start, end);
                UploadBaliseAssetPayload payload = new UploadBaliseAssetPayload(
                        uploadEntry.assetType().serializedName(),
                        uploadEntry.fileName(),
                        chunkIndex,
                        chunkCount,
                        chunk
                );
                net.minecraft.network.PacketByteBuf uploadBuf = PacketByteBufs.create();
                payload.write(uploadBuf);
                ClientPlayNetworking.send(UploadBaliseAssetPayload.ID, uploadBuf);
            }
        }
        return true;
    }

    private void betterrailwaysystem$refreshEntriesIfNeeded() {
        int currentRevision = BaliseAssetLibrary.libraryRevision();
        if (observedLibraryRevision == currentRevision || listWidget == null) {
            return;
        }
        reloadEntries();
        if (entries.stream().noneMatch(entry -> entry.identifier().equals(selectedIdentifier))) {
            selectedIdentifier = "";
        }
        listWidget.reload();
        betterrailwaysystem$refreshButtons();
        statusText = Text.translatable("screen.betterrailwaysystem.refresh_done");
    }

    private final class AssetListWidget extends AlwaysSelectedEntryListWidget<AssetEntry> {
        private final int left;

        private AssetListWidget(MinecraftClient client, int left, int top, int width, int height) {
            super(client, width, height, top, 24);
            this.left = left;
            setX(left);
            reload();
        }

        @Override
        public int getRowWidth() {
            return width - 10;
        }

        protected int getScrollbarX() {
            return left + width - 6;
        }

        @Override
        public int getRowLeft() {
            return left;
        }

        protected void drawMenuListBackground(DrawContext context) {
        }

        protected void drawHeaderAndFooterSeparators(DrawContext context) {
        }

        protected void renderDecorations(DrawContext context, int mouseX, int mouseY) {
        }

        private void reload() {
            clearEntries();
            if (entries.isEmpty()) {
                addEntry(new AssetEntry(null));
                return;
            }
            for (BaliseAssetLibrary.LibraryEntry entry : entries) {
                addEntry(new AssetEntry(entry));
            }
        }
    }

    private final class AssetEntry extends AlwaysSelectedEntryListWidget.Entry<AssetEntry> {
        private final BaliseAssetLibrary.LibraryEntry entry;

        private AssetEntry(BaliseAssetLibrary.LibraryEntry entry) {
            this.entry = entry;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int color = entry == null ? 0xAAAAAA : (entry.identifier().equals(selectedIdentifier) ? 0xFF4C7A3D : 0xFF2C2C2C);
            context.fill(x, y + 2, x + entryWidth - 4, y + entryHeight - 2, color);
            context.drawBorder(x, y + 2, entryWidth - 4, entryHeight - 4, 0xFF6A6A6A);
            Text label = entry == null ? Text.translatable("screen.betterrailwaysystem.no_assets") : Text.literal(entry.displayName());
            context.drawTextWithShadow(textRenderer, label, x + 6, y + 9, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (entry == null) {
                return false;
            }
            selectIdentifier(entry.identifier());
            return true;
        }

        @Override
        public Text getNarration() {
            return entry == null ? Text.translatable("screen.betterrailwaysystem.no_assets") : Text.literal(entry.displayName());
        }
    }
}
