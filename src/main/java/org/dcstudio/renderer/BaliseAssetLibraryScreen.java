package org.dcstudio.renderer;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.dcstudio.client.asset.BaliseAssetLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// 使用 LDLib2 浏览、上传、选择 balise 图片和音频素材。
public final class BaliseAssetLibraryScreen extends ModularUIScreen {
    public BaliseAssetLibraryScreen(
            Screen parent,
            BaliseAssetLibrary.AssetType assetType,
            String selectedIdentifier,
            Consumer<String> applySelection
    ) {
        super(betterrailwaysystem$createUi(parent, assetType, selectedIdentifier, applySelection), assetType.dialogTitle());
    }

    private static ModularUI betterrailwaysystem$createUi(
            Screen parent,
            BaliseAssetLibrary.AssetType assetType,
            String selectedIdentifier,
            Consumer<String> applySelection
    ) {
        LibraryUiState state = new LibraryUiState(parent, assetType, selectedIdentifier, applySelection);
        return new ModularUI(UI.of(state.buildRoot()));
    }

    private static <T extends UIElement> T betterrailwaysystem$layout(T element, Consumer<LayoutStyle> consumer) {
        element.layout(consumer);
        return element;
    }

    private static final class LibraryUiState {
        private final Screen parent;
        private final BaliseAssetLibrary.AssetType assetType;
        private final Consumer<String> applySelection;
        private final List<BaliseAssetLibrary.LibraryEntry> entries = new ArrayList<>();
        private final ScrollerView scrollerView = betterrailwaysystem$layout(new ScrollerView(), layout -> {
            layout.flex(1);
            layout.minHeight(0);
            layout.widthPercent(100);
        });
        private final Label selectedValueLabel = betterrailwaysystem$layout(new Label(), layout -> layout.height(18));
        private final Label statusLabel = betterrailwaysystem$layout(new Label(), layout -> layout.height(18));
        private final AssetPreviewElement previewElement;
        private final Button useButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("screen.betterrailwaysystem.use_selected")), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        private String selectedIdentifier;
        private int selectedIndex = -1;
        private int previewImageWidth;
        private int previewImageHeight;

        private LibraryUiState(
                Screen parent,
                BaliseAssetLibrary.AssetType assetType,
                String selectedIdentifier,
                Consumer<String> applySelection
        ) {
            this.parent = parent;
            this.assetType = assetType;
            this.selectedIdentifier = selectedIdentifier == null ? "" : selectedIdentifier;
            this.applySelection = applySelection;
            this.previewElement = new AssetPreviewElement(this);
            this.selectedValueLabel.setText("");
            this.statusLabel.setText("");
            reloadEntries();
        }

        private UIElement buildRoot() {
            useButton.setOnClick(event -> useSelected());

            Button uploadButton = betterrailwaysystem$layout(new Button()
                    .setText(Text.translatable("screen.betterrailwaysystem.upload"))
                    .setOnClick(event -> importAsset()), layout -> {
                layout.height(20);
                layout.flex(1);
            });
            Button clearButton = betterrailwaysystem$layout(new Button()
                    .setText(Text.translatable("screen.betterrailwaysystem.clear_selected"))
                    .setOnClick(event -> clearSelection()), layout -> {
                layout.height(20);
                layout.flex(1);
            });
            Button backButton = betterrailwaysystem$layout(new Button()
                    .setText(Text.translatable("gui.back"))
                    .setOnClick(event -> MinecraftClient.getInstance().setScreen(parent)), layout -> {
                layout.height(20);
                layout.flex(1);
            });

            UIElement body = new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(100);
                        layout.flex(1);
                        layout.minHeight(0);
                        layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW);
                        layout.gapAll(8);
                    })
                    .addChildren(
                            betterrailwaysystem$layout(new UIElement()
                                    .style(style -> style.backgroundTexture(Sprites.BORDER))
                                    .addChild(scrollerView), layout -> {
                                layout.flex(1);
                                layout.minWidth(150);
                                layout.minHeight(0);
                                layout.paddingAll(6);
                            }),
                            betterrailwaysystem$layout(new UIElement()
                                    .style(style -> style.backgroundTexture(Sprites.BORDER))
                                    .addChild(previewElement), layout -> {
                                layout.widthPercent(38);
                                layout.minWidth(110);
                                layout.maxWidth(160);
                                layout.minHeight(0);
                                layout.paddingAll(6);
                            })
                    );

            UIElement footer = new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(100);
                        layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.ROW);
                        layout.gapAll(8);
                    })
                    .addChildren(uploadButton, useButton, clearButton, backButton);

            UIElement panel = new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(72);
                        layout.maxWidth(440);
                        layout.minWidth(280);
                        layout.heightPercent(76);
                        layout.maxHeight(320);
                        layout.minHeight(220);
                        layout.paddingAll(8);
                        layout.gapAll(8);
                        layout.flexDirection(dev.vfyjxf.taffy.style.FlexDirection.COLUMN);
                    })
                    .style(style -> style.backgroundTexture(Sprites.BORDER))
                    .addChildren(
                            betterrailwaysystem$layout(new Label()
                                    .setText(assetType.dialogTitle())
                                    .textStyle(textStyle -> textStyle.fontSize(18)), layout -> layout.height(24)),
                            betterrailwaysystem$layout(new Label()
                                    .setText(Text.translatable("screen.betterrailwaysystem.selected_asset")), layout -> layout.height(16)),
                            selectedValueLabel,
                            body,
                            statusLabel,
                            footer
                    );

            UIElement root = new UIElement()
                    .layout(layout -> {
                        layout.widthPercent(100);
                        layout.heightPercent(100);
                        layout.justifyContent(dev.vfyjxf.taffy.style.AlignContent.CENTER);
                        layout.alignItems(dev.vfyjxf.taffy.style.AlignItems.CENTER);
                    })
                    .addChild(panel);

            refreshList();
            refreshStatus();
            return root;
        }

        private void reloadEntries() {
            entries.clear();
            entries.addAll(BaliseAssetLibrary.list(assetType));
            selectedIndex = -1;
            for (int index = 0; index < entries.size(); index++) {
                if (entries.get(index).identifier().equals(selectedIdentifier)) {
                    selectedIndex = index;
                    break;
                }
            }
            updatePreviewMetadata();
        }

        private void refreshList() {
            scrollerView.clearAllScrollViewChildren();
            for (int index = 0; index < entries.size(); index++) {
                final int entryIndex = index;
                BaliseAssetLibrary.LibraryEntry entry = entries.get(index);
                String prefix = entryIndex == selectedIndex ? "> " : "";
                Button entryButton = betterrailwaysystem$layout(new Button()
                        .setText(Text.literal(prefix + entry.displayName()))
                        .setOnClick(event -> selectEntry(entryIndex)), layout -> {
                    layout.widthPercent(100);
                    layout.height(20);
                });
                if (entryIndex == selectedIndex) {
                    entryButton.style(style -> style.backgroundTexture(new ColorRectTexture(0xFF4C7A3D)));
                }
                scrollerView.addScrollViewChild(entryButton);
            }
            if (entries.isEmpty()) {
                scrollerView.addScrollViewChild(betterrailwaysystem$layout(new Label()
                        .setText(Text.translatable("screen.betterrailwaysystem.no_assets")), layout -> layout.height(20)));
            }
            useButton.setActive(selectedIndex >= 0 && selectedIndex < entries.size());
        }

        private void selectEntry(int entryIndex) {
            if (entryIndex < 0 || entryIndex >= entries.size()) {
                return;
            }
            selectedIndex = entryIndex;
            selectedIdentifier = entries.get(entryIndex).identifier();
            if (assetType == BaliseAssetLibrary.AssetType.SOUND) {
                BaliseAssetLibrary.previewSound(selectedIdentifier);
            }
            updatePreviewMetadata();
            refreshStatus();
            refreshList();
        }

        private void refreshStatus() {
            selectedValueLabel.setText(selectedIdentifier.isBlank() ? "-" : selectedIdentifier);
        }

        private void importAsset() {
            Optional<BaliseAssetLibrary.LibraryEntry> imported = BaliseAssetLibrary.importFromDialog(MinecraftClient.getInstance(), assetType);
            if (imported.isEmpty()) {
                statusLabel.setText(Text.translatable("screen.betterrailwaysystem.upload_skipped"));
                return;
            }
            selectedIdentifier = imported.get().identifier();
            statusLabel.setText(Text.translatable("screen.betterrailwaysystem.upload_reloading"));
            reloadEntries();
            if (assetType == BaliseAssetLibrary.AssetType.SOUND) {
                BaliseAssetLibrary.previewSound(selectedIdentifier);
            }
            refreshStatus();
            refreshList();
        }

        private void useSelected() {
            if (selectedIndex < 0 || selectedIndex >= entries.size()) {
                return;
            }
            applySelection.accept(entries.get(selectedIndex).identifier());
            MinecraftClient.getInstance().setScreen(parent);
        }

        private void clearSelection() {
            applySelection.accept("");
            MinecraftClient.getInstance().setScreen(parent);
        }

        private void updatePreviewMetadata() {
            previewImageWidth = 0;
            previewImageHeight = 0;
            if (assetType != BaliseAssetLibrary.AssetType.IMAGE || selectedIndex < 0 || selectedIndex >= entries.size()) {
                return;
            }
            Identifier identifier = Identifier.tryParse(entries.get(selectedIndex).identifier());
            if (identifier == null) {
                return;
            }
            Optional<net.minecraft.resource.Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
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
    }

    private static final class AssetPreviewElement extends UIElement {
        private final LibraryUiState state;

        private AssetPreviewElement(LibraryUiState state) {
            this.state = state;
            style(style -> style.backgroundTexture(Sprites.BORDER));
        }

        @Override
        public void drawBackgroundAdditional(GUIContext context) {
            super.drawBackgroundAdditional(context);

            int x = Math.round(getContentX());
            int y = Math.round(getContentY());
            int width = Math.max(1, Math.round(getContentWidth()));
            int height = Math.max(1, Math.round(getContentHeight()));

            context.graphics.fill(x, y, x + width, y + height, 0x66000000);

            if (state.selectedIdentifier.isBlank()) {
                betterrailwaysystem$drawCenteredText(context, Text.translatable("screen.betterrailwaysystem.no_assets"), x, y, width, height, 0xFFAAAAAA);
                return;
            }

            if (state.assetType == BaliseAssetLibrary.AssetType.SOUND) {
                betterrailwaysystem$drawCenteredText(context, Text.literal(state.selectedIdentifier), x + 4, y, width - 8, height, 0xFFFFFFFF);
                return;
            }

            Identifier identifier = Identifier.tryParse(state.selectedIdentifier);
            if (identifier == null || state.previewImageWidth <= 0 || state.previewImageHeight <= 0) {
                betterrailwaysystem$drawCenteredText(context, Text.literal(state.selectedIdentifier), x + 4, y, width - 8, height, 0xFFFFFFFF);
                return;
            }

            float scale = Math.min((float) (width - 8) / state.previewImageWidth, (float) (height - 8) / state.previewImageHeight);
            scale = Math.max(scale, 0.01F);
            int drawWidth = Math.max(1, Math.round(state.previewImageWidth * scale));
            int drawHeight = Math.max(1, Math.round(state.previewImageHeight * scale));
            int drawX = x + (width - drawWidth) / 2;
            int drawY = y + (height - drawHeight) / 2;

            context.graphics.drawTexture(
                    identifier,
                    drawX,
                    drawY,
                    drawWidth,
                    drawHeight,
                    0.0F,
                    0.0F,
                    state.previewImageWidth,
                    state.previewImageHeight,
                    state.previewImageWidth,
                    state.previewImageHeight
            );
        }

        private void betterrailwaysystem$drawCenteredText(GUIContext context, Text text, int x, int y, int width, int height, int color) {
            int textWidth = context.mc.textRenderer.getWidth(text);
            int drawX = x + Math.max(0, (width - textWidth) / 2);
            int drawY = y + Math.max(0, (height - 8) / 2);
            if (textWidth > width) {
                String raw = text.getString();
                int maxChars = Math.max(4, width / 6);
                String clipped = raw.length() > maxChars ? raw.substring(0, Math.min(raw.length(), maxChars - 3)) + "..." : raw;
                context.graphics.drawTextWithShadow(context.mc.textRenderer, clipped, x + 4, drawY, color);
                return;
            }
            context.graphics.drawTextWithShadow(context.mc.textRenderer, text, drawX, drawY, color);
        }
    }
}
