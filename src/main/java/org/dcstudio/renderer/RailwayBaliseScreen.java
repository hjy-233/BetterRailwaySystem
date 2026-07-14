package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.asset.BaliseAssetType;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.network.OpenBaliseEditorPayload;
import org.dcstudio.network.SaveBalisePayload;
import org.dcstudio.station.TrainSpawnerBlockEntity;

import java.util.ArrayList;
import java.util.List;

// 使用原生 Screen 编辑铁路信标的多类型事件配置。
public final class RailwayBaliseScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 336;
    private static final int LABEL_WIDTH = 118;
    private static final int LIBRARY_BUTTON_WIDTH = 76;

    private final OpenBaliseEditorPayload payload;
    private NativeFormWidgets.FormListWidget formList;
    private CyclingButtonWidget<BaliseMode> modeButton;
    private TextFieldWidget titleField;
    private TextFieldWidget subtitleField;
    private TextFieldWidget currentStationField;
    private TextFieldWidget nextStationField;
    private TextFieldWidget soundIdField;
    private TextFieldWidget imageIdField;
    private TextFieldWidget durationField;
    private TextFieldWidget speedLimitField;
    private CyclingButtonWidget<TrainSpawnDirection> triggerDirectionButton;
    private CheckboxWidget keepImageCheckbox;
    private CheckboxWidget bossBarCheckbox;
    private TrainSpawnDirection fallbackTriggerDirection = TrainSpawnDirection.EAST;
    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;

    public RailwayBaliseScreen(OpenBaliseEditorPayload payload) {
        super(Text.translatable("screen.betterrailwaysystem.railway_balise"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        super.init();
        betterrailwaysystem$layoutBounds();
        int contentX = panelX + 12;
        int contentY = panelY + 34;
        int contentWidth = panelWidth - 24;
        int footerY = panelY + panelHeight - 26;

        BaliseMode initialMode = payload.parsedMode();

        modeButton = CyclingButtonWidget.<BaliseMode>builder(mode ->
                        Text.translatable("screen.betterrailwaysystem.mode." + mode.serializedName()))
                .values(List.of(BaliseMode.values()))
                .initially(initialMode)
                .build(0, 0, 130, 20, Text.empty(), (button, value) -> betterrailwaysystem$refreshRows());

        titleField = betterrailwaysystem$textField(payload.titleText(), 64);
        subtitleField = betterrailwaysystem$textField(payload.subtitleText(), 96);
        currentStationField = betterrailwaysystem$textField(payload.currentStation(), 64);
        nextStationField = betterrailwaysystem$textField(payload.nextStation(), 64);
        soundIdField = betterrailwaysystem$textField(payload.soundId(), 128);
        imageIdField = betterrailwaysystem$textField(payload.imageId(), 128);

        durationField = new TextFieldWidget(textRenderer, 0, 0, 70, 20, Text.empty());
        durationField.setText(Integer.toString(payload.imageDurationSeconds()));
        durationField.setTextPredicate(value -> value.isEmpty() || betterrailwaysystem$isIntInRange(value, 1, 60));

        speedLimitField = new TextFieldWidget(textRenderer, 0, 0, 90, 20, Text.empty());
        speedLimitField.setText(Double.toString(payload.speedLimitBps()));
        speedLimitField.setTextPredicate(value -> value.isEmpty() || betterrailwaysystem$isDoubleInRange(value, 0.01, 128.0));

        List<TrainSpawnDirection> triggerDirectionOptions = new ArrayList<>();
        triggerDirectionOptions.add(TrainSpawnDirection.FORWARD);
        triggerDirectionOptions.addAll(betterrailwaysystem$detectDirections(payload));
        if (triggerDirectionOptions.size() == 1) {
            TrainSpawnDirection savedDirection = TrainSpawnDirection.fromString(payload.triggerDirection());
            if (!savedDirection.isLegacyRelative()) {
                triggerDirectionOptions.add(savedDirection);
            }
        }
        fallbackTriggerDirection = triggerDirectionOptions.get(0);
        TrainSpawnDirection initialTriggerDirection = TrainSpawnDirection.fromString(payload.triggerDirection());
        if (payload.triggerDirection().isBlank() || !triggerDirectionOptions.contains(initialTriggerDirection)) {
            initialTriggerDirection = fallbackTriggerDirection;
        }
        triggerDirectionButton = CyclingButtonWidget.<TrainSpawnDirection>builder(direction ->
                        direction.isLegacyRelative()
                                ? Text.translatable("screen.betterrailwaysystem.direction.any")
                                : Text.translatable("screen.betterrailwaysystem.direction." + direction.serializedName()))
                .values(triggerDirectionOptions)
                .initially(initialTriggerDirection)
                .build(0, 0, 120, 20, Text.empty(), (button, value) -> betterrailwaysystem$refreshRows());

        keepImageCheckbox = CheckboxWidget.builder(Text.translatable("screen.betterrailwaysystem.keep_image_until_next_balise"), textRenderer)
                .pos(0, 0)
                .checked(payload.keepImageUntilNextBalise())
                .build();
        bossBarCheckbox = CheckboxWidget.builder(Text.translatable("screen.betterrailwaysystem.update_bossbar"), textRenderer)
                .pos(0, 0)
                .checked(payload.updateBossBar())
                .build();

        ButtonWidget soundLibraryButton = ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.open_sound_library"), button -> {
                    if (client != null && client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetType.SOUND, soundIdField.getText(), soundIdField::setText));
                    }
                })
                .dimensions(0, 0, LIBRARY_BUTTON_WIDTH, 20)
                .build();
        ButtonWidget imageLibraryButton = ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.open_image_library"), button -> {
                    if (client != null && client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetType.IMAGE, imageIdField.getText(), imageIdField::setText));
                    }
                })
                .dimensions(0, 0, LIBRARY_BUTTON_WIDTH, 20)
                .build();

        formList = NativeFormWidgets.createFormList(client, contentX, contentY, contentWidth, footerY - contentY - 8, contentWidth - 16);
        addDrawableChild(formList);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> betterrailwaysystem$save())
                .dimensions(panelX + 12, footerY, (panelWidth - 32) / 2, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 20 + (panelWidth - 32) / 2, footerY, (panelWidth - 32) / 2, 20)
                .build());

        betterrailwaysystem$refreshRows(soundLibraryButton, imageLibraryButton);
        setInitialFocus(modeButton);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0101010);
        context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void betterrailwaysystem$layoutBounds() {
        panelWidth = Math.max(340, Math.min(PANEL_WIDTH, width - 40));
        panelHeight = Math.max(260, Math.min(PANEL_HEIGHT, height - 40));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    private void betterrailwaysystem$refreshRows() {
        ButtonWidget soundLibraryButton = ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.open_sound_library"), button -> {
                    if (client != null && client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetType.SOUND, soundIdField.getText(), soundIdField::setText));
                    }
                })
                .dimensions(0, 0, LIBRARY_BUTTON_WIDTH, 20)
                .build();
        ButtonWidget imageLibraryButton = ButtonWidget.builder(Text.translatable("screen.betterrailwaysystem.open_image_library"), button -> {
                    if (client != null && client.currentScreen != null) {
                        client.setScreen(new BaliseAssetLibraryScreen(client.currentScreen, BaliseAssetType.IMAGE, imageIdField.getText(), imageIdField::setText));
                    }
                })
                .dimensions(0, 0, LIBRARY_BUTTON_WIDTH, 20)
                .build();
        betterrailwaysystem$refreshRows(soundLibraryButton, imageLibraryButton);
    }

    private void betterrailwaysystem$refreshRows(ButtonWidget soundLibraryButton, ButtonWidget imageLibraryButton) {
        BaliseMode mode = modeButton.getValue() == null ? payload.parsedMode() : modeButton.getValue();
        List<NativeFormWidgets.RowEntry> rows = new ArrayList<>();
        rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.balise_mode"), modeButton, LABEL_WIDTH));
        rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.trigger_direction"), triggerDirectionButton, LABEL_WIDTH));
        rows.add(new NativeFormWidgets.HintEntry(betterrailwaysystem$triggerDirectionHint()));
        if (betterrailwaysystem$showsTitleField(mode)) {
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.title_text"), titleField, LABEL_WIDTH));
        }
        if (betterrailwaysystem$showsSubtitleField(mode)) {
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.subtitle_text"), subtitleField, LABEL_WIDTH));
        }
        if (betterrailwaysystem$showsStationFields(mode)) {
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.current_station"), currentStationField, LABEL_WIDTH));
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.next_station"), nextStationField, LABEL_WIDTH));
        }
        if (betterrailwaysystem$showsSoundField(mode)) {
            rows.add(new NativeFormWidgets.LabeledDualWidgetEntry(Text.translatable("screen.betterrailwaysystem.sound_id"), soundIdField, soundLibraryButton, LABEL_WIDTH, LIBRARY_BUTTON_WIDTH));
        }
        if (betterrailwaysystem$showsImageField(mode)) {
            rows.add(new NativeFormWidgets.LabeledDualWidgetEntry(Text.translatable("screen.betterrailwaysystem.image_id"), imageIdField, imageLibraryButton, LABEL_WIDTH, LIBRARY_BUTTON_WIDTH));
        }
        if (betterrailwaysystem$showsImageDurationField(mode)) {
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.image_duration"), durationField, LABEL_WIDTH));
        }
        if (betterrailwaysystem$showsSpeedLimitField(mode)) {
            rows.add(new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.speed_limit"), speedLimitField, LABEL_WIDTH));
        }
        if (betterrailwaysystem$showsKeepImageField(mode)) {
            rows.add(new NativeFormWidgets.FullWidthWidgetEntry(keepImageCheckbox));
        }
        if (betterrailwaysystem$showsBossBarField(mode)) {
            rows.add(new NativeFormWidgets.FullWidthWidgetEntry(bossBarCheckbox));
        }
        formList.setEntries(rows);
    }

    private void betterrailwaysystem$save() {
        BaliseMode mode = modeButton.getValue() == null ? payload.parsedMode() : modeButton.getValue();
        int durationSeconds = betterrailwaysystem$parseInt(durationField.getText(), payload.imageDurationSeconds(), 1, 60);
        double speedLimit = betterrailwaysystem$parseDouble(speedLimitField.getText(), payload.speedLimitBps(), 0.01, 128.0);
        TrainSpawnDirection triggerDirection = triggerDirectionButton.getValue() == null ? fallbackTriggerDirection : triggerDirectionButton.getValue();
        String triggerDirectionValue = triggerDirection.isLegacyRelative() ? "" : triggerDirection.serializedName();
        SaveBalisePayload savePayload = new SaveBalisePayload(
                payload.pos(),
                mode.serializedName(),
                titleField.getText(),
                subtitleField.getText(),
                currentStationField.getText(),
                nextStationField.getText(),
                soundIdField.getText(),
                imageIdField.getText(),
                durationSeconds,
                keepImageCheckbox.isChecked(),
                bossBarCheckbox.isChecked(),
                speedLimit,
                triggerDirectionValue
        );
        net.minecraft.network.PacketByteBuf buf = PacketByteBufs.create();
        savePayload.write(buf);
        ClientPlayNetworking.send(SaveBalisePayload.ID, buf);
        close();
    }

    private TextFieldWidget betterrailwaysystem$textField(String value, int maxLength) {
        TextFieldWidget textField = new TextFieldWidget(textRenderer, 0, 0, 120, 20, Text.empty());
        textField.setMaxLength(maxLength);
        textField.setText(value == null ? "" : value);
        return textField;
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

    private Text betterrailwaysystem$triggerDirectionHint() {
        TrainSpawnDirection direction = triggerDirectionButton.getValue();
        if (direction == null || direction.isLegacyRelative()) {
            return Text.translatable("screen.betterrailwaysystem.trigger_direction_hint.any");
        }
        return Text.translatable(
                "screen.betterrailwaysystem.trigger_direction_hint.direction",
                Text.translatable("screen.betterrailwaysystem.direction." + direction.serializedName())
        );
    }

    private static boolean betterrailwaysystem$isIntInRange(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= min && parsed <= max;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static boolean betterrailwaysystem$isDoubleInRange(String value, double min, double max) {
        try {
            double parsed = Double.parseDouble(value);
            return parsed >= min && parsed <= max;
        } catch (NumberFormatException ignored) {
            return false;
        }
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

    private static List<TrainSpawnDirection> betterrailwaysystem$detectDirections(OpenBaliseEditorPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(TrainSpawnerBlockEntity.detectDirections(client.world, payload.pos()));
    }
}
