package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.network.OpenStopRailEditorPayload;
import org.dcstudio.network.SaveStopRailPayload;

import java.util.List;

// 使用原生 Screen 编辑停车轨的停车距离和等待模式。
public final class StopRailScreen extends Screen {
    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 196;
    private static final int LABEL_WIDTH = 96;

    private final OpenStopRailEditorPayload payload;
    private NativeFormWidgets.FormListWidget formList;
    private TextFieldWidget stopDistanceField;
    private TextFieldWidget dwellSecondsField;
    private CyclingButtonWidget<StopRailWaitMode> waitModeButton;
    private int panelWidth;
    private int panelHeight;
    private int panelX;
    private int panelY;

    public StopRailScreen(OpenStopRailEditorPayload payload) {
        super(Text.translatable("screen.betterrailwaysystem.stop_rail"));
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

        stopDistanceField = new TextFieldWidget(textRenderer, 0, 0, 80, 20, Text.empty());
        stopDistanceField.setText(Integer.toString(payload.stopDistance()));
        stopDistanceField.setMaxLength(4);
        stopDistanceField.setTextPredicate(value -> value.isEmpty() || betterrailwaysystem$isIntInRange(value, 1, 128));

        dwellSecondsField = new TextFieldWidget(textRenderer, 0, 0, 80, 20, Text.empty());
        dwellSecondsField.setText(Integer.toString(payload.dwellSeconds()));
        dwellSecondsField.setMaxLength(4);
        dwellSecondsField.setTextPredicate(value -> value.isEmpty() || betterrailwaysystem$isIntInRange(value, 0, 600));

        waitModeButton = CyclingButtonWidget.<StopRailWaitMode>builder(mode ->
                        Text.translatable("screen.betterrailwaysystem.wait_mode." + mode.serializedName()))
                .values(List.of(StopRailWaitMode.values()))
                .initially(StopRailWaitMode.fromString(payload.waitMode()))
                .build(0, 0, 100, 20, Text.empty());

        formList = NativeFormWidgets.createFormList(client, contentX, contentY, contentWidth, footerY - contentY - 8, contentWidth - 16);
        formList.setEntries(List.of(
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.stop_distance"), stopDistanceField, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.dwell_seconds"), dwellSecondsField, LABEL_WIDTH),
                new NativeFormWidgets.LabeledWidgetEntry(Text.translatable("screen.betterrailwaysystem.wait_mode"), waitModeButton, LABEL_WIDTH)
        ));
        addDrawableChild(formList);

        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> betterrailwaysystem$save())
                .dimensions(panelX + 12, footerY, (panelWidth - 32) / 2, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 20 + (panelWidth - 32) / 2, footerY, (panelWidth - 32) / 2, 20)
                .build());
        setInitialFocus(stopDistanceField);
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
        panelWidth = Math.max(280, Math.min(PANEL_WIDTH, width - 40));
        panelHeight = Math.max(176, Math.min(PANEL_HEIGHT, height - 40));
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
    }

    private void betterrailwaysystem$save() {
        int stopDistance = betterrailwaysystem$parseInt(stopDistanceField.getText(), payload.stopDistance(), 1, 128);
        int dwellSeconds = betterrailwaysystem$parseInt(dwellSecondsField.getText(), payload.dwellSeconds(), 0, 600);
        StopRailWaitMode waitMode = waitModeButton.getValue();
        ClientPlayNetworking.send(new SaveStopRailPayload(payload.pos(), stopDistance, dwellSeconds, waitMode.serializedName()));
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
}
