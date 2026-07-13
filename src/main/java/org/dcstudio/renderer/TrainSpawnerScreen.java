package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.network.OpenTrainSpawnerEditorPayload;
import org.dcstudio.network.SaveTrainSpawnerPayload;
import org.dcstudio.station.TrainSpawnerBlockEntity;

import java.util.ArrayList;
import java.util.List;

// 使用原生 Screen 编辑发车器的城市、线路和发车策略。
public final class TrainSpawnerScreen extends Screen {
    private static final String CREATE_CITY_VALUE = "__create__";
    private static final int PANEL_WIDTH = 396;
    private static final int PANEL_HEIGHT = 308;
    private static final int LABEL_WIDTH = 108;

    private final OpenTrainSpawnerEditorPayload payload;
    private NativeFormWidgets.FormListWidget formList;
    private TextFieldWidget cityField;
    private TextFieldWidget lineIdField;
    private TextFieldWidget targetCountField;
    private CyclingButtonWidget<String> citySelectorButton;
    private CyclingButtonWidget<LineThemeColor> lineColorButton;
    private CyclingButtonWidget<TrainSpawnDirection> directionButton;
    private CheckboxWidget redstoneCheckbox;
    private CheckboxWidget circularCheckbox;
    private List<String> cityOptions = List.of();
    private TrainSpawnDirection fallbackDirection = TrainSpawnDirection.EAST;
    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;

    public TrainSpawnerScreen(OpenTrainSpawnerEditorPayload payload) {
        super(Text.translatable("screen.betterrailwaysystem.train_spawner"));
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

        cityOptions = betterrailwaysystem$buildCityOptions();
        String initialCity = cityOptions.stream().filter(option -> !CREATE_CITY_VALUE.equals(option)).findFirst().orElse("Default");

        cityField = new TextFieldWidget(textRenderer, 0, 0, 110, 20, Text.empty());
        cityField.setMaxLength(32);
        cityField.setText(initialCity);

        lineIdField = new TextFieldWidget(textRenderer, 0, 0, 110, 20, Text.empty());
        lineIdField.setMaxLength(32);
        lineIdField.setText(payload.lineId());

        targetCountField = new TextFieldWidget(textRenderer, 0, 0, 80, 20, Text.empty());
        targetCountField.setText(Integer.toString(payload.targetTrainCount()));
        targetCountField.setMaxLength(2);
        targetCountField.setTextPredicate(value -> value.isEmpty() || betterrailwaysystem$isIntInRange(value, 1, 64));

        citySelectorButton = CyclingButtonWidget.<String>builder(
                        value -> CREATE_CITY_VALUE.equals(value)
                                ? Text.translatable("screen.betterrailwaysystem.city_mode.create")
                : Text.literal(value),
                        initialCity)
                .values(cityOptions)
                .build(0, 0, 120, 20, Text.empty(), (button, value) -> {
                    boolean creating = CREATE_CITY_VALUE.equals(value);
                    cityField.setEditable(creating);
                    cityField.active = creating;
                    if (!creating) {
                        cityField.setText(value);
                    }
                });
        cityField.setEditable(false);
        cityField.active = false;

        lineColorButton = CyclingButtonWidget.<LineThemeColor>builder(
                        color -> Text.translatable("screen.betterrailwaysystem.line_theme_color." + color.serializedName()),
                        LineThemeColor.fromString(payload.lineThemeColor()))
                .values(List.of(LineThemeColor.values()))
                .build(0, 0, 120, 20, Text.empty());

        List<TrainSpawnDirection> directionOptions = betterrailwaysystem$detectDirections(payload);
        if (directionOptions.isEmpty()) {
            directionOptions = new ArrayList<>(List.of(TrainSpawnDirection.fromString(payload.direction())));
        }
        fallbackDirection = directionOptions.getFirst();
        TrainSpawnDirection initialDirection = TrainSpawnDirection.fromString(payload.direction());
        if (!directionOptions.contains(initialDirection)) {
            initialDirection = fallbackDirection;
        }
        directionButton = CyclingButtonWidget.<TrainSpawnDirection>builder(
                        direction -> Text.translatable("screen.betterrailwaysystem.direction." + direction.serializedName()),
                        initialDirection)
                .values(directionOptions)
                .build(0, 0, 120, 20, Text.empty());

        redstoneCheckbox = CheckboxWidget.builder(Text.translatable("screen.betterrailwaysystem.redstone_spawn_enabled"), textRenderer)
                .pos(0, 0)
                .checked(payload.redstoneControlled())
                .maxWidth(contentWidth - 24)
                .build();
        circularCheckbox = CheckboxWidget.builder(Text.translatable("screen.betterrailwaysystem.circular_line"), textRenderer)
                .pos(0, 0)
                .checked(payload.circularLine())
                .maxWidth(contentWidth - 24)
                .build();

        formList = NativeFormWidgets.createFormList(client, contentX, contentY, contentWidth, footerY - contentY - 8, contentWidth - 16);
        formList.setEntries(List.of(
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.city_selector"), citySelectorButton, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.city_name"), cityField, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.line_id"), lineIdField, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.target_train_count"), targetCountField, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.line_theme_color"), lineColorButton, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.direction"), directionButton, LABEL_WIDTH),
                new NativeFormWidgets.FullWidthWidgetEntry(redstoneCheckbox),
                new NativeFormWidgets.FullWidthWidgetEntry(circularCheckbox)
        ));
        addDrawableChild(formList);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> betterrailwaysystem$save())
                .dimensions(panelX + 12, footerY, (panelWidth - 32) / 2, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 20 + (panelWidth - 32) / 2, footerY, (panelWidth - 32) / 2, 20)
                .build());
        setInitialFocus(lineIdField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xE0101010);
        context.drawStrokedRectangle(panelX, panelY, panelWidth, panelHeight, 0xFF8B8B8B);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, panelY + 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    private void betterrailwaysystem$layoutBounds() {
        panelWidth = Math.max(320, Math.min(PANEL_WIDTH, width - 40));
        panelHeight = Math.max(250, Math.min(PANEL_HEIGHT, height - 40));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    private List<String> betterrailwaysystem$buildCityOptions() {
        List<String> options = new ArrayList<>(payload.cityOptions());
        String fallbackCity = options.isEmpty() ? "Default" : options.getFirst();
        if (!options.contains(fallbackCity) && !fallbackCity.isBlank()) {
            options.add(fallbackCity);
        }
        options.add(CREATE_CITY_VALUE);
        return options;
    }

    private void betterrailwaysystem$save() {
        int targetTrainCount = betterrailwaysystem$parseInt(targetCountField.getText(), payload.targetTrainCount(), 1, 64);
        String selectedCity = citySelectorButton.getValue();
        String cityName = CREATE_CITY_VALUE.equals(selectedCity) ? cityField.getText().trim() : selectedCity;
        LineThemeColor lineThemeColor = lineColorButton.getValue() == null ? LineThemeColor.BLUE : lineColorButton.getValue();
        TrainSpawnDirection direction = directionButton.getValue() == null ? fallbackDirection : directionButton.getValue();
        int flags = (redstoneCheckbox.isChecked() ? 1 : 0) | (circularCheckbox.isChecked() ? 2 : 0);
        ClientPlayNetworking.send(new SaveTrainSpawnerPayload(
                payload.pos(),
                cityName,
                lineIdField.getText().trim(),
                lineThemeColor.serializedName(),
                direction.serializedName(),
                targetTrainCount,
                flags
        ));
        close();
    }

    private static boolean betterrailwaysystem$isIntInRange(String value, int min, int max) {
        try {
            int parsed = Integer.parseInt(value);
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

    private static List<TrainSpawnDirection> betterrailwaysystem$detectDirections(OpenTrainSpawnerEditorPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(TrainSpawnerBlockEntity.detectDirections(client.world, payload.pos()));
    }
}
