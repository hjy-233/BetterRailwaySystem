package org.dcstudio.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.dcstudio.network.RequestLineMapPayload;
import org.dcstudio.client.asset.BaliseAssetLibrary;
import org.dcstudio.client.network.BetterRailwaySystemClientNetworking;
import org.dcstudio.renderer.StationAnnouncementOverlay;
import org.lwjgl.glfw.GLFW;

public class BetterRailwaySystemClient implements ClientModInitializer {
    // 客户端入口，负责注册 HUD 和客户端收包。
    private static KeyBinding openLineMapKey;

    @Override
    public void onInitializeClient() {
        BaliseAssetLibrary.initialize();
        BetterRailwaySystemClientNetworking.register();
        StationAnnouncementOverlay.register();
        openLineMapKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.betterrailwaysystem.open_line_map",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_GRAVE_ACCENT,
                "category.betterrailwaysystem"
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openLineMapKey.wasPressed()) {
                if (client.player != null) {
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(RequestLineMapPayload.INSTANCE);
                }
            }
        });
    }
}
