package org.dcstudio.renderer;

import com.lowdragmc.lowdraglib2.gui.holder.ModularUIScreen;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Selector;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Toggle;
import com.lowdragmc.lowdraglib2.gui.ui.style.LayoutStyle;
import com.lowdragmc.lowdraglib2.gui.ui.styletemplate.Sprites;
import com.lowdragmc.lowdraglib2.gui.ui.utils.UIElementProvider;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.network.OpenBaliseEditorPayload;
import org.dcstudio.network.SaveBalisePayload;

import java.util.Arrays;
import java.util.function.Consumer;

// 使用 LDLib2 编辑铁路信标的多类型事件配置。
public final class RailwayBaliseScreen extends ModularUIScreen {
    public RailwayBaliseScreen(OpenBaliseEditorPayload payload) {
        super(betterrailwaysystem$createUi(payload), Text.translatable("screen.betterrailwaysystem.railway_balise"));
    }

    private static ModularUI betterrailwaysystem$createUi(OpenBaliseEditorPayload payload) {
        Selector<BaliseMode> modeSelector = betterrailwaysystem$layout(new Selector<BaliseMode>()
                .setCandidates(Arrays.stream(BaliseMode.values()).toList())
                .setCandidateUIProvider(UIElementProvider.text(value -> Text.translatable("screen.betterrailwaysystem.mode." + (value == null ? BaliseMode.ANNOUNCEMENT.serializedName() : value.serializedName()))))
                .setSelected(payload.parsedMode(), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        TextField titleField = betterrailwaysystem$textField(payload.titleText());
        TextField subtitleField = betterrailwaysystem$textField(payload.subtitleText());
        TextField currentStationField = betterrailwaysystem$textField(payload.currentStation());
        TextField nextStationField = betterrailwaysystem$textField(payload.nextStation());
        TextField soundIdField = betterrailwaysystem$textField(payload.soundId());
        TextField imageIdField = betterrailwaysystem$textField(payload.imageId());
        TextField durationField = betterrailwaysystem$layout(new TextField()
                .setNumbersOnlyInt(1, 60)
                .setText(Integer.toString(payload.imageDurationSeconds()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        TextField speedLimitField = betterrailwaysystem$layout(new TextField()
                .setNumbersOnlyDouble(0.01, 128.0)
                .setText(Double.toString(payload.speedLimitBps()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });

        Toggle keepImageToggle = betterrailwaysystem$layout(new Toggle()
                .setText(Text.translatable("screen.betterrailwaysystem.keep_image_until_next_balise"))
                .setOn(payload.keepImageUntilNextBalise(), false), layout -> layout.height(20));
        Toggle bossBarToggle = betterrailwaysystem$layout(new Toggle()
                .setText(Text.translatable("screen.betterrailwaysystem.update_bossbar"))
                .setOn(payload.updateBossBar(), false), layout -> layout.height(20));

        Button soundLibraryButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("screen.betterrailwaysystem.open_sound_library"))
                .setOnClick(event -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetLibrary.AssetType.SOUND, soundIdField.getText(), soundIdField::setText));
                    }
                }), layout -> {
            layout.width(84);
            layout.height(20);
        });
        Button imageLibraryButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("screen.betterrailwaysystem.open_image_library"))
                .setOnClick(event -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetLibrary.AssetType.IMAGE, imageIdField.getText(), imageIdField::setText));
                    }
                }), layout -> {
            layout.width(84);
            layout.height(20);
        });

        UIElement titleRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.title_text", titleField);
        UIElement subtitleRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.subtitle_text", subtitleField);
        UIElement currentStationRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.current_station", currentStationField);
        UIElement nextStationRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.next_station", nextStationField);
        UIElement soundRow = betterrailwaysystem$labeledButtonRow("screen.betterrailwaysystem.sound_id", soundIdField, soundLibraryButton);
        UIElement imageRow = betterrailwaysystem$labeledButtonRow("screen.betterrailwaysystem.image_id", imageIdField, imageLibraryButton);
        UIElement durationRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.image_duration", durationField);
        UIElement speedLimitRow = betterrailwaysystem$labeledRow("screen.betterrailwaysystem.speed_limit", speedLimitField);

        UIElement rows = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.balise_mode", modeSelector),
                        titleRow,
                        subtitleRow,
                        currentStationRow,
                        nextStationRow,
                        soundRow,
                        imageRow,
                        durationRow,
                        speedLimitRow,
                        keepImageToggle,
                        bossBarToggle
                );

        modeSelector.setOnValueChanged(mode -> betterrailwaysystem$refreshModeRows(
                mode == null ? payload.parsedMode() : mode,
                titleRow,
                subtitleRow,
                currentStationRow,
                nextStationRow,
                soundRow,
                imageRow,
                durationRow,
                speedLimitRow,
                keepImageToggle,
                bossBarToggle
        ));
        betterrailwaysystem$refreshModeRows(
                payload.parsedMode(),
                titleRow,
                subtitleRow,
                currentStationRow,
                nextStationRow,
                soundRow,
                imageRow,
                durationRow,
                speedLimitRow,
                keepImageToggle,
                bossBarToggle
        );

        ScrollerView scrollerView = betterrailwaysystem$layout(new ScrollerView()
                .addScrollViewChild(rows), layout -> {
            layout.widthPercent(100);
            layout.flex(1);
            layout.minHeight(0);
        });

        Button doneButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("gui.done"))
                .setOnClick(event -> {
                    BaliseMode mode = modeSelector.getValue() == null ? payload.parsedMode() : modeSelector.getValue();
                    int durationSeconds = betterrailwaysystem$parseInt(durationField.getText(), payload.imageDurationSeconds(), 1, 60);
                    double speedLimit = betterrailwaysystem$parseDouble(speedLimitField.getText(), payload.speedLimitBps(), 0.01, 128.0);
                    ClientPlayNetworking.send(new SaveBalisePayload(
                            payload.pos(),
                            mode.serializedName(),
                            titleField.getText(),
                            subtitleField.getText(),
                            currentStationField.getText(),
                            nextStationField.getText(),
                            soundIdField.getText(),
                            imageIdField.getText(),
                            durationSeconds,
                            keepImageToggle.isOn(),
                            bossBarToggle.isOn(),
                            speedLimit
                    ));
                    MinecraftClient.getInstance().setScreen(null);
                }), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        Button cancelButton = betterrailwaysystem$layout(new Button()
                .setText(Text.translatable("gui.cancel"))
                .setOnClick(event -> MinecraftClient.getInstance().setScreen(null)), layout -> {
            layout.height(20);
            layout.flex(1);
        });

        UIElement footer = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                })
                .addChildren(doneButton, cancelButton);

        UIElement panel = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(64);
                    layout.maxWidth(380);
                    layout.minWidth(260);
                    layout.heightPercent(76);
                    layout.maxHeight(360);
                    layout.minHeight(240);
                    layout.paddingAll(8);
                    layout.gapAll(8);
                    layout.flexDirection(FlexDirection.COLUMN);
                })
                .style(style -> style.backgroundTexture(Sprites.BORDER))
                .addChildren(
                        new Label()
                                .setText(Text.translatable("screen.betterrailwaysystem.railway_balise"))
                                .textStyle(textStyle -> textStyle.fontSize(18))
                                .layout(layout -> layout.height(24)),
                        scrollerView,
                        footer
                );

        UIElement root = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.justifyContent(AlignContent.CENTER);
                    layout.alignItems(AlignItems.CENTER);
                })
                .addChild(panel);
        return new ModularUI(UI.of(root));
    }

    private static TextField betterrailwaysystem$textField(String value) {
        return betterrailwaysystem$layout(new TextField()
                .setText(value == null ? "" : value, false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
    }

    private static UIElement betterrailwaysystem$labeledRow(String translationKey, UIElement field) {
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(8);
                })
                .addChildren(
                        new Label()
                                .setText(Text.translatable(translationKey))
                                .layout(layout -> {
                                    layout.width(116);
                                    layout.height(20);
                                }),
                        field
                );
    }

    private static UIElement betterrailwaysystem$labeledButtonRow(String translationKey, UIElement field, UIElement button) {
        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(8);
                })
                .addChildren(
                        new Label()
                                .setText(Text.translatable(translationKey))
                                .layout(layout -> {
                                    layout.width(116);
                                    layout.height(20);
                                }),
                        new UIElement()
                                .layout(layout -> {
                                    layout.flexDirection(FlexDirection.ROW);
                                    layout.alignItems(AlignItems.CENTER);
                                    layout.gapAll(6);
                                    layout.flex(1);
                                })
                                .addChildren(field, button)
                );
    }

    private static void betterrailwaysystem$refreshModeRows(
            BaliseMode mode,
            UIElement titleRow,
            UIElement subtitleRow,
            UIElement currentStationRow,
            UIElement nextStationRow,
            UIElement soundRow,
            UIElement imageRow,
            UIElement durationRow,
            UIElement speedLimitRow,
            UIElement keepImageToggle,
            UIElement bossBarToggle
    ) {
        titleRow.setDisplay(betterrailwaysystem$showsTitleField(mode));
        subtitleRow.setDisplay(betterrailwaysystem$showsSubtitleField(mode));
        currentStationRow.setDisplay(betterrailwaysystem$showsStationFields(mode));
        nextStationRow.setDisplay(betterrailwaysystem$showsStationFields(mode));
        soundRow.setDisplay(betterrailwaysystem$showsSoundField(mode));
        imageRow.setDisplay(betterrailwaysystem$showsImageField(mode));
        durationRow.setDisplay(betterrailwaysystem$showsImageDurationField(mode));
        speedLimitRow.setDisplay(betterrailwaysystem$showsSpeedLimitField(mode));
        keepImageToggle.setDisplay(betterrailwaysystem$showsKeepImageField(mode));
        bossBarToggle.setDisplay(betterrailwaysystem$showsBossBarField(mode));
    }

    private static boolean betterrailwaysystem$showsTitleField(BaliseMode mode) {
        return mode == BaliseMode.ANNOUNCEMENT || mode == BaliseMode.SPEED_LIMIT_END;
    }

    private static boolean betterrailwaysystem$showsSubtitleField(BaliseMode mode) {
        return betterrailwaysystem$showsTitleField(mode);
    }

    private static boolean betterrailwaysystem$showsStationFields(BaliseMode mode) {
        return mode == BaliseMode.ARRIVAL || mode == BaliseMode.DEPARTURE || mode == BaliseMode.ANNOUNCEMENT;
    }

    private static boolean betterrailwaysystem$showsSoundField(BaliseMode mode) {
        return mode != BaliseMode.SPEED_LIMIT_START;
    }

    private static boolean betterrailwaysystem$showsImageField(BaliseMode mode) {
        return mode != BaliseMode.SPEED_LIMIT_START;
    }

    private static boolean betterrailwaysystem$showsImageDurationField(BaliseMode mode) {
        return mode != BaliseMode.SPEED_LIMIT_START;
    }

    private static boolean betterrailwaysystem$showsSpeedLimitField(BaliseMode mode) {
        return mode == BaliseMode.SPEED_LIMIT_START;
    }

    private static boolean betterrailwaysystem$showsKeepImageField(BaliseMode mode) {
        return mode != BaliseMode.SPEED_LIMIT_START;
    }

    private static boolean betterrailwaysystem$showsBossBarField(BaliseMode mode) {
        return mode == BaliseMode.ARRIVAL || mode == BaliseMode.DEPARTURE || mode == BaliseMode.ANNOUNCEMENT;
    }

    private static int betterrailwaysystem$parseInt(String value, int fallback, int min, int max) {
        try {
            return MathHelper.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double betterrailwaysystem$parseDouble(String value, double fallback, double min, double max) {
        try {
            return MathHelper.clamp(Double.parseDouble(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static <T extends UIElement> T betterrailwaysystem$layout(T element, Consumer<LayoutStyle> consumer) {
        element.layout(consumer);
        return element;
    }
}
