package org.dcstudio.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.BetterRailwaySystemConfig;
import org.dcstudio.config.BetterRailwaySystemConfigManager;

// 提供 Cloth Config 配置界面。
public final class BetterRailwaySystemConfigScreen {
    private BetterRailwaySystemConfigScreen() {
    }

    public static Screen create(Screen parent) {
        BetterRailwaySystemConfig draft = BetterRailwaySystem.config().copy();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("screen.betterrailwaysystem.config.title"))
                .setSavingRunnable(() -> {
                    draft.sanitize();
                    BetterRailwaySystemConfigManager.save(draft);
                    BetterRailwaySystem.reloadConfig();
                    BetterRailwaySystemDebugClient.applyConfiguredKey();
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
        category.addEntry(entryBuilder.startKeyCodeField(Text.translatable("screen.betterrailwaysystem.config.debug_toggle_key"), BetterRailwaySystemDebugClient.configuredToggleKey())
                .setDefaultValue(BetterRailwaySystemDebugClient.defaultToggleKey())
                .setKeySaveConsumer(value -> draft.debugToggleKey = value.getTranslationKey())
                .build());

        return builder.build();
    }
}
