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
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.network.OpenStopRailEditorPayload;
import org.dcstudio.network.SaveStopRailPayload;

import java.util.Arrays;
import java.util.function.Consumer;

// 使用 LDLib2 编辑停车轨的停车距离和等待模式。
public final class StopRailScreen extends ModularUIScreen {
    public StopRailScreen(OpenStopRailEditorPayload payload) {
        super(betterrailwaysystem$createUi(payload), Text.translatable("screen.betterrailwaysystem.stop_rail"));
    }

    private static ModularUI betterrailwaysystem$createUi(OpenStopRailEditorPayload payload) {
        TextField stopDistanceField = betterrailwaysystem$layout(new TextField()
                .setNumbersOnlyInt(1, 128)
                .setText(Integer.toString(payload.stopDistance()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        TextField dwellSecondsField = betterrailwaysystem$layout(new TextField()
                .setNumbersOnlyInt(0, 600)
                .setText(Integer.toString(payload.dwellSeconds()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        Selector<StopRailWaitMode> waitModeSelector = betterrailwaysystem$layout(new Selector<StopRailWaitMode>()
                .setCandidates(Arrays.stream(StopRailWaitMode.values()).toList())
                .setCandidateUIProvider(UIElementProvider.text(value -> Text.translatable("screen.betterrailwaysystem.wait_mode." + (value == null ? StopRailWaitMode.TIMER.serializedName() : value.serializedName()))))
                .setSelected(StopRailWaitMode.fromString(payload.waitMode()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });

        UIElement rows = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.stop_distance", stopDistanceField),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.dwell_seconds", dwellSecondsField),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.wait_mode", waitModeSelector)
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
                    int stopDistance = betterrailwaysystem$parseInt(stopDistanceField.getText(), payload.stopDistance(), 1, 128);
                    int dwellSeconds = betterrailwaysystem$parseInt(dwellSecondsField.getText(), payload.dwellSeconds(), 0, 600);
                    StopRailWaitMode waitMode = waitModeSelector.getValue() == null ? StopRailWaitMode.TIMER : waitModeSelector.getValue();
                    ClientPlayNetworking.send(new SaveStopRailPayload(payload.pos(), stopDistance, dwellSeconds, waitMode.serializedName()));
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
                    layout.widthPercent(42);
                    layout.maxWidth(280);
                    layout.minWidth(220);
                    layout.heightPercent(42);
                    layout.maxHeight(210);
                    layout.minHeight(170);
                    layout.paddingAll(8);
                    layout.gapAll(8);
                    layout.flexDirection(FlexDirection.COLUMN);
                })
                .style(style -> style.backgroundTexture(Sprites.BORDER))
                .addChildren(
                        new Label()
                                .setText(Text.translatable("screen.betterrailwaysystem.stop_rail"))
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
                                    layout.width(92);
                                    layout.height(20);
                                }),
                        field
                );
    }

    private static int betterrailwaysystem$parseInt(String value, int fallback, int min, int max) {
        try {
            return MathHelper.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static <T extends UIElement> T betterrailwaysystem$layout(T element, Consumer<LayoutStyle> consumer) {
        element.layout(consumer);
        return element;
    }
}
