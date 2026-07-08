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
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.network.OpenTrainSpawnerEditorPayload;
import org.dcstudio.network.SaveTrainSpawnerPayload;
import org.dcstudio.station.TrainSpawnerBlockEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

// 使用 LDLib2 编辑发车器的线路编号和发车策略。
public final class TrainSpawnerScreen extends ModularUIScreen {
    private static final String CREATE_CITY_VALUE = "__create__";

    public TrainSpawnerScreen(OpenTrainSpawnerEditorPayload payload) {
        super(betterrailwaysystem$createUi(payload), Text.translatable("screen.betterrailwaysystem.train_spawner"));
    }

    private static ModularUI betterrailwaysystem$createUi(OpenTrainSpawnerEditorPayload payload) {
        List<String> cityOptions = new ArrayList<>(payload.cityOptions());
        String defaultCity = payload.cityOptions().isEmpty() ? "Default" : payload.cityOptions().getFirst();
        if (!cityOptions.contains(defaultCity) && !defaultCity.isBlank()) {
            cityOptions.add(defaultCity);
        }
        cityOptions.add(CREATE_CITY_VALUE);

        TextField cityField = betterrailwaysystem$layout(new TextField()
                .setText(defaultCity, false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        TextField lineIdField = betterrailwaysystem$layout(new TextField()
                .setText(payload.lineId(), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        TextField intervalField = betterrailwaysystem$layout(new TextField()
                .setNumbersOnlyInt(1, 3600)
                .setText(Integer.toString(payload.intervalSeconds()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });

        List<TrainSpawnDirection> directionOptions = betterrailwaysystem$detectDirections(payload);
        TrainSpawnDirection initialDirection = TrainSpawnDirection.fromString(payload.direction());
        if (directionOptions.isEmpty()) {
            directionOptions = new ArrayList<>(List.of(initialDirection.isLegacyRelative() ? TrainSpawnDirection.EAST : initialDirection));
        }
        if (!directionOptions.contains(initialDirection)) {
            initialDirection = directionOptions.getFirst();
        }
        TrainSpawnDirection resolvedInitialDirection = initialDirection;

        Selector<String> citySelector = betterrailwaysystem$layout(new Selector<String>()
                .setCandidates(cityOptions)
                .setCandidateUIProvider(UIElementProvider.text(value -> CREATE_CITY_VALUE.equals(value) ? Text.translatable("screen.betterrailwaysystem.city_mode.create") : Text.literal(value == null ? "" : value)))
                .setSelected(cityOptions.contains(defaultCity) ? defaultCity : CREATE_CITY_VALUE, false)
                .setOnValueChanged(value -> {
                    boolean creating = CREATE_CITY_VALUE.equals(value);
                    cityField.setActive(creating);
                    if (!creating && value != null) {
                        cityField.setText(value, false);
                    }
                }), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        cityField.setActive(CREATE_CITY_VALUE.equals(citySelector.getValue()));

        Selector<LineThemeColor> colorSelector = betterrailwaysystem$layout(new Selector<LineThemeColor>()
                .setCandidates(Arrays.stream(LineThemeColor.values()).toList())
                .setCandidateUIProvider(UIElementProvider.text(value -> Text.translatable("screen.betterrailwaysystem.line_theme_color." + (value == null ? LineThemeColor.BLUE.serializedName() : value.serializedName()))))
                .setSelected(LineThemeColor.fromString(payload.lineThemeColor()), false), layout -> {
            layout.height(20);
            layout.flex(1);
        });
        Selector<TrainSpawnDirection> directionSelector = betterrailwaysystem$layout(new Selector<TrainSpawnDirection>()
                .setCandidates(directionOptions)
                .setCandidateUIProvider(UIElementProvider.text(value -> Text.translatable("screen.betterrailwaysystem.direction." + (value == null ? resolvedInitialDirection.serializedName() : value.serializedName()))))
                .setSelected(resolvedInitialDirection, false), layout -> {
            layout.height(20);
            layout.flex(1);
        });

        Toggle redstoneToggle = betterrailwaysystem$layout(new Toggle()
                .setText(Text.translatable("screen.betterrailwaysystem.redstone_spawn_enabled"))
                .setOn(payload.redstoneControlled(), false), layout -> layout.height(20));
        Toggle circularToggle = betterrailwaysystem$layout(new Toggle()
                .setText(Text.translatable("screen.betterrailwaysystem.circular_line"))
                .setOn(payload.circularLine(), false), layout -> layout.height(20));

        UIElement rows = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(6);
                })
                .addChildren(
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.city_selector", citySelector),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.city_name", cityField),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.line_id", lineIdField),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.interval_seconds", intervalField),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.line_theme_color", colorSelector),
                        betterrailwaysystem$labeledRow("screen.betterrailwaysystem.direction", directionSelector),
                        redstoneToggle,
                        circularToggle
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
                    int intervalSeconds = betterrailwaysystem$parseInt(intervalField.getText(), payload.intervalSeconds(), 1, 3600);
                    String selectedCity = citySelector.getValue();
                    String cityName = CREATE_CITY_VALUE.equals(selectedCity) ? cityField.getText().trim() : (selectedCity == null ? cityField.getText().trim() : selectedCity);
                    LineThemeColor lineThemeColor = colorSelector.getValue() == null ? LineThemeColor.BLUE : colorSelector.getValue();
                    TrainSpawnDirection direction = directionSelector.getValue() == null ? resolvedInitialDirection : directionSelector.getValue();
                    ClientPlayNetworking.send(new SaveTrainSpawnerPayload(
                            payload.pos(),
                            cityName,
                            lineIdField.getText().trim(),
                            lineThemeColor.serializedName(),
                            direction.serializedName(),
                            intervalSeconds,
                            (redstoneToggle.isOn() ? 1 : 0) | (circularToggle.isOn() ? 2 : 0)
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
                    layout.widthPercent(56);
                    layout.maxWidth(340);
                    layout.minWidth(250);
                    layout.heightPercent(68);
                    layout.maxHeight(320);
                    layout.minHeight(230);
                    layout.paddingAll(8);
                    layout.gapAll(8);
                    layout.flexDirection(FlexDirection.COLUMN);
                })
                .style(style -> style.backgroundTexture(Sprites.BORDER))
                .addChildren(
                        new Label()
                                .setText(Text.translatable("screen.betterrailwaysystem.train_spawner"))
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
                                    layout.width(106);
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

    private static List<TrainSpawnDirection> betterrailwaysystem$detectDirections(OpenTrainSpawnerEditorPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(TrainSpawnerBlockEntity.detectDirections(client.world, payload.pos()));
    }

    private static <T extends UIElement> T betterrailwaysystem$layout(T element, Consumer<LayoutStyle> consumer) {
        element.layout(consumer);
        return element;
    }
}
