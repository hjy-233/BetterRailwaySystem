package org.dcstudio.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.network.DebugTrackingPayload;

// Debug 模式下用左键选择矿车，并让视角与玩家位置跟随目标。
public final class DebugMinecartTracker {
    private static final double CAMERA_DISTANCE = 2.35;
    private static final double FOCUS_HEIGHT = 0.45;
    private static AbstractMinecartEntity trackedMinecart;
    private static boolean previousNoClip;
    private static boolean hasPreviousNoClip;

    private DebugMinecartTracker() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DebugMinecartTracker::tick);
    }

    public static boolean handleAttack(MinecraftClient client) {
        if (!BetterRailwaySystem.isDebugMode() || client.currentScreen != null) {
            return false;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof AbstractMinecartEntity minecart)) {
            return false;
        }

        if (trackedMinecart == minecart) {
            stop(client, true);
        } else {
            if (client.player != null && !hasPreviousNoClip) {
                previousNoClip = client.player.noClip;
                hasPreviousNoClip = true;
            }
            trackedMinecart = minecart;
            if (client.player != null) {
                client.player.noClip = true;
                client.setCameraEntity(client.player);
            }
            sendTrackingState(true);
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("debug.betterrailwaysystem.tracking.started"), true);
            }
        }
        return true;
    }

    public static boolean isTracking(Entity entity) {
        return trackedMinecart == entity;
    }

    public static AbstractMinecartEntity getTrackedMinecart() {
        return trackedMinecart;
    }

    private static void tick(MinecraftClient client) {
        if (trackedMinecart == null) {
            return;
        }
        if (!BetterRailwaySystem.isDebugMode() || client.player == null || client.world == null || trackedMinecart.isRemoved() || trackedMinecart.getWorld() != client.world) {
            stop(client, false);
            return;
        }
        if (client.options.sneakKey.isPressed()) {
            stop(client, true);
            return;
        }

        if (client.getCameraEntity() != client.player) {
            client.setCameraEntity(client.player);
        }
        client.player.noClip = true;
        sendTrackingState(true);
        followWithPlayer(client, trackedMinecart);
    }

    private static void followWithPlayer(MinecraftClient client, AbstractMinecartEntity minecart) {
        if (client.player == null || client.player.networkHandler == null) {
            return;
        }
        double yawRadians = Math.toRadians(client.player.getYaw());
        double pitchRadians = Math.toRadians(client.player.getPitch());
        double horizontal = Math.cos(pitchRadians);
        double directionX = -Math.sin(yawRadians) * horizontal;
        double directionY = -Math.sin(pitchRadians);
        double directionZ = Math.cos(yawRadians) * horizontal;
        double focusX = minecart.getX();
        double focusY = minecart.getY() + FOCUS_HEIGHT;
        double focusZ = minecart.getZ();
        double eyeHeight = client.player.getEyeY() - client.player.getY();
        double eyeX = focusX - directionX * CAMERA_DISTANCE;
        double eyeY = focusY - directionY * CAMERA_DISTANCE;
        double eyeZ = focusZ - directionZ * CAMERA_DISTANCE;
        double x = eyeX;
        double y = eyeY - eyeHeight;
        double z = eyeZ;
        float yaw = betterrailwaysystem$yawToward(eyeX, eyeZ, focusX, focusZ);
        float pitch = betterrailwaysystem$pitchToward(eyeX, eyeY, eyeZ, focusX, focusY, focusZ);
        client.player.setVelocity(0.0, 0.0, 0.0);
        client.player.setPosition(x, y, z);
        client.player.setYaw(yaw);
        client.player.setPitch(pitch);
        client.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, false));
    }

    private static float betterrailwaysystem$yawToward(double fromX, double fromZ, double toX, double toZ) {
        double deltaX = toX - fromX;
        double deltaZ = toZ - fromZ;
        return (float) Math.toDegrees(Math.atan2(-deltaX, deltaZ));
    }

    private static float betterrailwaysystem$pitchToward(double fromX, double fromY, double fromZ, double toX, double toY, double toZ) {
        double deltaX = toX - fromX;
        double deltaY = toY - fromY;
        double deltaZ = toZ - fromZ;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        return (float) Math.toDegrees(Math.atan2(-deltaY, horizontalDistance));
    }

    private static void stop(MinecraftClient client, boolean notify) {
        trackedMinecart = null;
        sendTrackingState(false);
        if (client.player != null) {
            if (hasPreviousNoClip) {
                client.player.noClip = previousNoClip;
                hasPreviousNoClip = false;
            }
            client.setCameraEntity(client.player);
            if (notify) {
                client.player.sendMessage(Text.translatable("debug.betterrailwaysystem.tracking.stopped"), true);
            }
        } else {
            hasPreviousNoClip = false;
        }
    }

    private static void sendTrackingState(boolean tracking) {
        if (ClientPlayNetworking.canSend(DebugTrackingPayload.ID)) {
            ClientPlayNetworking.send(new DebugTrackingPayload(tracking));
        }
    }
}
