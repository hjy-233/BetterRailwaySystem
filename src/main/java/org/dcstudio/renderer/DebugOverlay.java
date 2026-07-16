package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.client.DebugMinecartTracker;
import org.dcstudio.minecart.MinecartDebugSnapshot;
import org.dcstudio.station.RailwayBaliseBlockEntity;
import org.dcstudio.station.StopRailBlockEntity;
import org.dcstudio.station.TrainCollectorBlockEntity;
import org.dcstudio.station.TrainSpawnerBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Debug 模式下显示准心目标的铁路与矿车运行信息。
public final class DebugOverlay {
    private static final int X = 8;
    private static final int Y = 8;
    private static final int PADDING = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_LINES = 32;
    private static final int BACKGROUND_COLOR = 0xB0000000;
    private static final int TEXT_COLOR = 0xFFE8E8E8;
    private static final int TITLE_COLOR = 0xFFFFD36A;

    private DebugOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(DebugOverlay::render);
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        if (!BetterRailwaySystem.isDebugMode()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.crosshairTarget == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        HitResult target = client.crosshairTarget;
        AbstractMinecartEntity trackedMinecart = DebugMinecartTracker.getTrackedMinecart();
        if (trackedMinecart != null && !trackedMinecart.isRemoved() && trackedMinecart.getWorld() == client.world) {
            addMinecartLines(lines, trackedMinecart, true);
        }

        if (target instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof AbstractMinecartEntity minecart && minecart != trackedMinecart) {
            addMinecartLines(lines, minecart, false);
        } else if (target instanceof BlockHitResult blockHit) {
            if (!lines.isEmpty()) {
                lines.add("");
            }
            BlockState state = client.world.getBlockState(blockHit.getBlockPos());
            BlockEntity blockEntity = client.world.getBlockEntity(blockHit.getBlockPos());
            addBlockLines(lines, state, blockEntity);
        }

        if (!lines.isEmpty()) {
            renderLines(context, client.textRenderer, lines);
        }
    }

    private static void addMinecartLines(List<String> lines, AbstractMinecartEntity minecart, boolean tracked) {
        if (!lines.isEmpty()) {
            lines.add("");
        }
            lines.add(tr("debug.betterrailwaysystem.title"));
            lines.add(tr("debug.betterrailwaysystem.target", tracked ? tr("debug.betterrailwaysystem.target.tracked_minecart") : tr("debug.betterrailwaysystem.target.minecart")));
            lines.add(tr("debug.betterrailwaysystem.tracking", localizedBoolean(tracked || DebugMinecartTracker.isTracking(minecart))));
            lines.addAll(MinecartDebugSnapshot.from(minecart).lines(""));
    }

    private static void addBlockLines(List<String> lines, BlockState state, BlockEntity blockEntity) {
        lines.add(tr("debug.betterrailwaysystem.title"));
        lines.add(tr("debug.betterrailwaysystem.target", blockName(state)));
        addRailLines(lines, state);

        if (blockEntity instanceof RailwayBaliseBlockEntity balise) {
            lines.add(tr("debug.betterrailwaysystem.balise_mode", balise.getMode().serializedName()));
            lines.add(tr("debug.betterrailwaysystem.trigger_direction", displayAny(balise.getTriggerDirection())));
            lines.add(tr("debug.betterrailwaysystem.current_station", display(balise.getCurrentStation())));
            lines.add(tr("debug.betterrailwaysystem.next_station", display(balise.getNextStation())));
            lines.add(tr("debug.betterrailwaysystem.title_text", display(balise.getTitleText())));
            lines.add(tr("debug.betterrailwaysystem.subtitle_text", display(balise.getSubtitleText())));
            lines.add(tr("debug.betterrailwaysystem.sound", display(balise.getSoundId())));
            lines.add(tr("debug.betterrailwaysystem.image", display(balise.getImageId())));
            lines.add(tr("debug.betterrailwaysystem.image_duration", balise.getImageDurationSeconds()));
            lines.add(tr("debug.betterrailwaysystem.keep_image", localizedBoolean(balise.shouldKeepImageUntilNextBalise())));
            lines.add(tr("debug.betterrailwaysystem.bossbar", localizedBoolean(balise.shouldUpdateBossBar())));
            lines.add(tr("debug.betterrailwaysystem.block_speed_limit", format(balise.getSpeedLimitBps())));
            addLastMinecart(lines, balise.getLastMinecartDebug());
        } else if (blockEntity instanceof StopRailBlockEntity stopRail) {
            lines.add(tr("debug.betterrailwaysystem.stop_distance", stopRail.getStopDistance()));
            lines.add(tr("debug.betterrailwaysystem.dwell", stopRail.getDwellSeconds()));
            lines.add(tr("debug.betterrailwaysystem.wait_mode", stopRail.getWaitMode().serializedName()));
            addLastMinecart(lines, stopRail.getLastMinecartDebug());
        } else if (blockEntity instanceof TrainSpawnerBlockEntity spawner) {
            lines.add(tr("debug.betterrailwaysystem.city", display(spawner.getCityName())));
            lines.add(tr("debug.betterrailwaysystem.line_id", display(spawner.getLineId())));
            lines.add(tr("debug.betterrailwaysystem.direction", spawner.getDirection().serializedName()));
            lines.add(tr("debug.betterrailwaysystem.theme_color", display(spawner.getLineThemeColor())));
            if (spawner.isCircularLine()) {
                lines.add(tr("debug.betterrailwaysystem.target_train_count", spawner.getTargetTrainCount()));
            } else {
                lines.add(tr("debug.betterrailwaysystem.spawn_interval", spawner.getSpawnIntervalSeconds()));
            }
            lines.add(tr("debug.betterrailwaysystem.redstone_controlled", localizedBoolean(spawner.isRedstoneControlled())));
            lines.add(tr("debug.betterrailwaysystem.circular_line", localizedBoolean(spawner.isCircularLine())));
            addLastMinecart(lines, spawner.getLastMinecartDebug());
        } else if (blockEntity instanceof TrainCollectorBlockEntity collector) {
            lines.add(tr("debug.betterrailwaysystem.collector", localizedBoolean(true)));
            addLastMinecart(lines, collector.getLastMinecartDebug());
        }
    }

    private static void addRailLines(List<String> lines, BlockState state) {
        if (!state.isIn(BlockTags.RAILS)) {
            return;
        }
        lines.add(tr("debug.betterrailwaysystem.on_rail_block", localizedBoolean(true)));
        if (state.getBlock() instanceof AbstractRailBlock railBlock) {
            RailShape shape = state.get(railBlock.getShapeProperty());
            lines.add(tr("debug.betterrailwaysystem.rail_shape", shape.asString()));
        }
    }

    private static void addLastMinecart(List<String> lines, MinecartDebugSnapshot snapshot) {
        lines.add(tr("debug.betterrailwaysystem.last_minecart"));
        lines.addAll(snapshot.lines("  "));
    }

    private static void renderLines(DrawContext context, TextRenderer textRenderer, List<String> lines) {
        int visibleLines = Math.min(lines.size(), MAX_LINES);
        int width = 0;
        for (int index = 0; index < visibleLines; index++) {
            width = Math.max(width, textRenderer.getWidth(lines.get(index)));
        }
        int right = X + width + PADDING * 2;
        int bottom = Y + visibleLines * LINE_HEIGHT + PADDING * 2;
        context.fill(X, Y, right, bottom, BACKGROUND_COLOR);
        for (int index = 0; index < visibleLines; index++) {
            int color = index == 0 ? TITLE_COLOR : TEXT_COLOR;
            context.drawText(textRenderer, lines.get(index), X + PADDING, Y + PADDING + index * LINE_HEIGHT, color, true);
        }
    }

    private static String blockName(BlockState state) {
        return Registries.BLOCK.getId(state.getBlock()).toString();
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String displayAny(String value) {
        return value == null || value.isBlank() ? tr("debug.betterrailwaysystem.any") : value;
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String localizedBoolean(boolean value) {
        return tr(value ? "debug.betterrailwaysystem.boolean.true" : "debug.betterrailwaysystem.boolean.false");
    }

    private static String tr(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }
}
