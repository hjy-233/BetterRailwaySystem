package org.dcstudio.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.dcstudio.BetterRailwaySystem;
import org.lwjgl.glfw.GLFW;

// 客户端调试模式快捷键，占位开关供后续诊断 UI 使用。
public final class BetterRailwaySystemDebugClient {
    private static KeyBinding toggleKeyBinding;
    private static boolean configuredKeyPressed;

    private BetterRailwaySystemDebugClient() {
    }

    public static void register() {
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.betterrailwaysystem.toggle_debug",
                GLFW.GLFW_KEY_UNKNOWN,
                "category.betterrailwaysystem"
        ));
        applyConfiguredKey();
        ClientTickEvents.END_CLIENT_TICK.register(BetterRailwaySystemDebugClient::tick);
    }

    public static InputUtil.Key configuredToggleKey() {
        return InputUtil.fromTranslationKey(BetterRailwaySystem.config().debugToggleKey);
    }

    public static InputUtil.Key defaultToggleKey() {
        return InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_UNKNOWN);
    }

    public static void applyConfiguredKey() {
        if (toggleKeyBinding == null) {
            return;
        }
        toggleKeyBinding.setBoundKey(configuredToggleKey());
        configuredKeyPressed = false;
        KeyBinding.updateKeysByCode();
    }

    private static void tick(MinecraftClient client) {
        boolean pressed = false;
        while (toggleKeyBinding != null && toggleKeyBinding.wasPressed()) {
            pressed = true;
        }
        pressed = pressed || consumeConfiguredRawPress(client);
        if (pressed) {
            boolean enabled = !BetterRailwaySystem.isDebugMode();
            BetterRailwaySystem.setDebugMode(enabled);
            if (client.player != null) {
                client.player.sendMessage(Text.translatable(
                        enabled ? "command.betterrailwaysystem.debug.enabled" : "command.betterrailwaysystem.debug.disabled"
                ), true);
            }
        }
    }

    private static boolean consumeConfiguredRawPress(MinecraftClient client) {
        if (client.currentScreen != null) {
            configuredKeyPressed = false;
            return false;
        }
        InputUtil.Key key = configuredToggleKey();
        if (key.getCode() == GLFW.GLFW_KEY_UNKNOWN) {
            configuredKeyPressed = false;
            return false;
        }

        long handle = client.getWindow().getHandle();
        boolean pressedNow = switch (key.getCategory()) {
            case KEYSYM, SCANCODE -> InputUtil.isKeyPressed(handle, key.getCode());
            case MOUSE -> GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
        };
        boolean consumed = pressedNow && !configuredKeyPressed;
        configuredKeyPressed = pressedNow;
        return consumed;
    }
}
