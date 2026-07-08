package org.dcstudio.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.BetterRailwaySystemConfig;
import org.dcstudio.config.BetterRailwaySystemConfigManager;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.dcstudio.network.ClearRailwayMapPayload;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;

import java.util.List;
import java.util.Optional;

// 提供 Cloth Config 配置界面。
public final class BetterRailwaySystemConfigScreen {
    private BetterRailwaySystemConfigScreen() {
    }

    public static Screen create(Screen parent) {
        BetterRailwaySystemConfig draft = BetterRailwaySystem.config().copy();
        StringListEntry[] cityEntryRef = new StringListEntry[1];
        StringListEntry[] lineEntryRef = new StringListEntry[1];
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("screen.betterrailwaysystem.config.title"))
                .setSavingRunnable(() -> {
                    draft.sanitize();
                    BetterRailwaySystemConfigManager.save(draft);
                    BetterRailwaySystem.reloadConfig();
                });
        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("screen.betterrailwaysystem.config.category.general"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(entryBuilder.startDoubleField(Text.translatable("screen.betterrailwaysystem.config.max_speed"), draft.maxSpeed)
                .setDefaultValue(8.0)
                .setSaveConsumer(value -> draft.maxSpeed = value)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("screen.betterrailwaysystem.config.acceleration"), draft.acceleration)
                .setDefaultValue(0.15)
                .setSaveConsumer(value -> draft.acceleration = value)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("screen.betterrailwaysystem.config.deceleration"), draft.deceleration)
                .setDefaultValue(0.20)
                .setSaveConsumer(value -> draft.deceleration = value)
                .build());
        category.addEntry(entryBuilder.startDoubleField(Text.translatable("screen.betterrailwaysystem.config.safe_following_distance"), draft.safeFollowingDistance)
                .setDefaultValue(5.0)
                .setSaveConsumer(value -> draft.safeFollowingDistance = value)
                .build());
        category.addEntry(entryBuilder.startIntField(Text.translatable("screen.betterrailwaysystem.config.max_passengers"), draft.maxPassengers)
                .setDefaultValue(24)
                .setMin(1)
                .setMax(256)
                .setSaveConsumer(value -> draft.maxPassengers = value)
                .build());
        category.addEntry(entryBuilder.startIntField(Text.translatable("screen.betterrailwaysystem.config.stop_rail_approach_distance"), draft.stopRailApproachDistance)
                .setDefaultValue(30)
                .setMin(1)
                .setMax(256)
                .setSaveConsumer(value -> draft.stopRailApproachDistance = value)
                .build());
        category.addEntry(entryBuilder.startIntField(Text.translatable("screen.betterrailwaysystem.config.unattended_despawn_seconds"), draft.unattendedDespawnSeconds)
                .setDefaultValue(180)
                .setMin(1)
                .setMax(3600)
                .setSaveConsumer(value -> draft.unattendedDespawnSeconds = value)
                .build());

        ConfigCategory mapCategory = builder.getOrCreateCategory(Text.translatable("screen.betterrailwaysystem.config.category.map"));
        mapCategory.addEntry(entryBuilder.startTextDescription(Text.translatable("screen.betterrailwaysystem.map_clear_desc")).build());
        StringListEntry cityEntry = entryBuilder.startStrField(Text.translatable("screen.betterrailwaysystem.city_name"), "")
                .setDefaultValue("")
                .build();
        cityEntryRef[0] = cityEntry;
        mapCategory.addEntry(cityEntry);
        StringListEntry lineEntry = entryBuilder.startStrField(Text.translatable("screen.betterrailwaysystem.line_id"), "")
                .setDefaultValue("")
                .build();
        lineEntryRef[0] = lineEntry;
        mapCategory.addEntry(lineEntry);
        mapCategory.addEntry(new ActionButtonListEntry(
                Text.translatable("screen.betterrailwaysystem.map_clear_all"),
                Text.translatable("screen.betterrailwaysystem.map_clear_all"),
                () -> betterrailwaysystem$clearRailwayMap(ClearRailwayMapPayload.Mode.ALL, "", "")
        ));
        mapCategory.addEntry(new ActionButtonListEntry(
                Text.translatable("screen.betterrailwaysystem.map_clear_city"),
                Text.translatable("screen.betterrailwaysystem.map_clear_city"),
                () -> betterrailwaysystem$clearRailwayMap(
                        ClearRailwayMapPayload.Mode.CITY,
                        cityEntryRef[0] == null ? "" : cityEntryRef[0].getValue().trim(),
                        ""
                )
        ));
        mapCategory.addEntry(new ActionButtonListEntry(
                Text.translatable("screen.betterrailwaysystem.map_clear_line"),
                Text.translatable("screen.betterrailwaysystem.map_clear_line"),
                () -> betterrailwaysystem$clearRailwayMap(
                        ClearRailwayMapPayload.Mode.LINE,
                        cityEntryRef[0] == null ? "" : cityEntryRef[0].getValue().trim(),
                        lineEntryRef[0] == null ? "" : lineEntryRef[0].getValue().trim()
                )
        ));

        return builder.build();
    }

    private static void betterrailwaysystem$clearRailwayMap(ClearRailwayMapPayload.Mode mode, String cityName, String lineId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }
        java.util.UUID playerId = client.player.getUuid();
        ClearRailwayMapPayload payload = new ClearRailwayMapPayload(mode.name(), cityName, lineId);
        MinecraftServer server = client.getServer();
        if (server != null) {
            server.execute(() -> {
                ServerPlayerEntity serverPlayer = server.getPlayerManager().getPlayer(playerId);
                if (serverPlayer != null) {
                    BetterRailwaySystemNetworking.clearRailwayMap(serverPlayer, payload);
                }
            });
            return;
        }
        ClientPlayNetworking.send(payload);
    }

    // 提供可直接点击的操作条目，避免悬浮按钮命中区域异常。
    private static final class ActionButtonListEntry extends AbstractConfigListEntry<Void> {
        private final ButtonWidget button;
        private final List<ButtonWidget> widgets;

        private ActionButtonListEntry(Text fieldName, Text buttonText, Runnable action) {
            super(fieldName, false);
            this.button = ButtonWidget.builder(buttonText, widget -> action.run()).dimensions(0, 0, 120, 20).build();
            this.widgets = List.of(button);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            int textY = y + (entryHeight - 9) / 2;
            context.drawTextWithShadow(textRenderer, getDisplayedFieldName(), x, textY, 0xFFFFFF);
            int buttonWidth = 120;
            int buttonX = x + entryWidth - buttonWidth;
            button.setX(buttonX);
            button.setY(y);
            button.active = MinecraftClient.getInstance().player != null;
            button.render(context, mouseX, mouseY, delta);
        }

        @Override
        public int getItemHeight() {
            return 24;
        }

        @Override
        public boolean isEdited() {
            return false;
        }

        @Override
        public Void getValue() {
            return null;
        }

        @Override
        public Optional<Void> getDefaultValue() {
            return Optional.empty();
        }

        @Override
        public List<? extends Element> children() {
            return widgets;
        }

        @Override
        public List<? extends Selectable> narratables() {
            return widgets;
        }
    }
}
