package org.dcstudio.config;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.dcstudio.BetterRailwaySystem;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

// 注册 BetterRailwaySystem 的运行时配置指令。
public final class BetterRailwaySystemCommands {
    private BetterRailwaySystemCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(commandRoot("betterrailwaysystem"));
            dispatcher.register(commandRoot("brs"));
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> commandRoot(String name) {
        return literal(name)
                .requires(source -> source.hasPermissionLevel(2))
                .then(literal("debug")
                        .then(literal("on")
                                .executes(context -> setDebugMode(context, true)))
                        .then(literal("off")
                                .executes(context -> setDebugMode(context, false))))
                .then(literal("config")
                        .then(literal("show")
                                .executes(BetterRailwaySystemCommands::showConfig))
                        .then(literal("reload")
                                .executes(BetterRailwaySystemCommands::reloadConfig))
                        .then(literal("set")
                                .then(literal("maxSpeed")
                                        .then(argument("value", DoubleArgumentType.doubleArg(0.01))
                                                .executes(context -> setMaxSpeed(context, DoubleArgumentType.getDouble(context, "value")))))
                                .then(literal("acceleration")
                                        .then(argument("value", DoubleArgumentType.doubleArg(0.01))
                                                .executes(context -> setAcceleration(context, DoubleArgumentType.getDouble(context, "value")))))
                                .then(literal("deceleration")
                                        .then(argument("value", DoubleArgumentType.doubleArg(0.01))
                                                .executes(context -> setDeceleration(context, DoubleArgumentType.getDouble(context, "value")))))
                                .then(literal("safeFollowingDistance")
                                        .then(argument("value", DoubleArgumentType.doubleArg(0.1))
                                                .executes(context -> setSafeFollowingDistance(context, DoubleArgumentType.getDouble(context, "value")))))
                                .then(literal("maxPassengers")
                                        .then(argument("value", IntegerArgumentType.integer(1, 256))
                                                .executes(context -> setMaxPassengers(context, IntegerArgumentType.getInteger(context, "value")))))
                                .then(literal("stopRailApproachDistance")
                                        .then(argument("value", IntegerArgumentType.integer(1, 256))
                                                .executes(context -> setStopRailApproachDistance(context, IntegerArgumentType.getInteger(context, "value")))))
                                .then(literal("unattendedDespawnSeconds")
                                        .then(argument("value", IntegerArgumentType.integer(1, 3600))
                                                .executes(context -> setUnattendedDespawnSeconds(context, IntegerArgumentType.getInteger(context, "value")))))));
    }

    private static int setDebugMode(CommandContext<ServerCommandSource> context, boolean enabled) {
        BetterRailwaySystem.setDebugMode(enabled);
        context.getSource().sendFeedback(() -> Text.translatable(
                enabled ? "command.betterrailwaysystem.debug.enabled" : "command.betterrailwaysystem.debug.disabled"
        ), true);
        return 1;
    }

    private static int showConfig(CommandContext<ServerCommandSource> context) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        context.getSource().sendFeedback(() -> Text.translatable(
                "command.betterrailwaysystem.config.current",
                config.maxSpeed,
                config.acceleration,
                config.deceleration,
                config.safeFollowingDistance,
                config.maxPassengers,
                config.stopRailApproachDistance,
                config.unattendedDespawnSeconds
        ), false);
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        BetterRailwaySystem.reloadConfig();
        return showConfig(context);
    }

    private static int setMaxSpeed(CommandContext<ServerCommandSource> context, double value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.maxSpeed = value;
        return saveAndShow(context, "maxSpeed", value);
    }

    private static int setAcceleration(CommandContext<ServerCommandSource> context, double value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.acceleration = value;
        return saveAndShow(context, "acceleration", value);
    }

    private static int setDeceleration(CommandContext<ServerCommandSource> context, double value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.deceleration = value;
        return saveAndShow(context, "deceleration", value);
    }

    private static int setSafeFollowingDistance(CommandContext<ServerCommandSource> context, double value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.safeFollowingDistance = value;
        return saveAndShow(context, "safeFollowingDistance", value);
    }

    private static int setMaxPassengers(CommandContext<ServerCommandSource> context, int value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.maxPassengers = value;
        return saveAndShow(context, "maxPassengers", value);
    }

    private static int setStopRailApproachDistance(CommandContext<ServerCommandSource> context, int value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.stopRailApproachDistance = value;
        return saveAndShow(context, "stopRailApproachDistance", value);
    }

    private static int setUnattendedDespawnSeconds(CommandContext<ServerCommandSource> context, int value) {
        BetterRailwaySystemConfig config = BetterRailwaySystem.config();
        config.unattendedDespawnSeconds = value;
        return saveAndShow(context, "unattendedDespawnSeconds", value);
    }

    private static int saveAndShow(CommandContext<ServerCommandSource> context, String key, double value) {
        BetterRailwaySystem.saveConfig();
        context.getSource().sendFeedback(() -> Text.translatable(
                "command.betterrailwaysystem.config.updated",
                key,
                value
        ), true);
        return showConfig(context);
    }
}
