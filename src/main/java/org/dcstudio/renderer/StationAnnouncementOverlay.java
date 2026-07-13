package org.dcstudio.renderer;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.dcstudio.network.StationAnnouncementPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

// 在乘客 HUD 上显示报站图片，并只在本地播放语音。
public final class StationAnnouncementOverlay {
    private static final int IMAGE_TOP_MARGIN = 8;
    private static final int IMAGE_SIDE_MARGIN = 12;
    private static final float MAX_IMAGE_WIDTH_RATIO = 0.6F;
    private static final float MAX_IMAGE_HEIGHT_RATIO = 0.16F;
    private static long visibleUntilTick;
    private static boolean announcementVisible;
    private static boolean keepImageUntilNextBalise;
    private static Identifier currentImageId;
    private static NativeImageBackedTexture currentImageTexture;
    private static int currentImageWidth;
    private static int currentImageHeight;

    private StationAnnouncementOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(StationAnnouncementOverlay::render);
    }

    public static void show(StationAnnouncementPayload payload, ClientPlayerEntity player) {
        long durationTicks = Math.max(1L, (long) payload.imageDurationSeconds() * 20L);
        visibleUntilTick = player.age + durationTicks;
        announcementVisible = true;
        keepImageUntilNextBalise = payload.keepImageUntilNextBalise();
        currentImageId = payload.imageId() == null || payload.imageId().isBlank() ? null : betterrailwaysystem$resolveImage(payload.imageId());
        MinecraftClient client = MinecraftClient.getInstance();
        client.inGameHud.setTitleTicks(0, (int) durationTicks, 0);
        client.inGameHud.setTitle(Text.literal(payload.titleText()));
        client.inGameHud.setSubtitle(Text.literal(payload.subtitleText()));

        Identifier soundIdentifier = payload.soundId() == null || payload.soundId().isBlank() ? null : Identifier.tryParse(payload.soundId());
        if (soundIdentifier != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(net.minecraft.sound.SoundEvent.of(soundIdentifier), 1.0F));
        }
    }

    private static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            betterrailwaysystem$clearAnnouncement(client);
            return;
        }
        if (!announcementVisible) {
            return;
        }
        if (keepImageUntilNextBalise && !(client.player.getVehicle() instanceof AbstractMinecartEntity)) {
            betterrailwaysystem$clearAnnouncement();
            return;
        }
        if (!keepImageUntilNextBalise && client.player.age >= visibleUntilTick) {
            betterrailwaysystem$clearAnnouncement(client);
            return;
        }

        if (currentImageId != null && currentImageWidth > 0 && currentImageHeight > 0) {
            int maxWidth = Math.max(1, Math.min(
                    context.getScaledWindowWidth() - IMAGE_SIDE_MARGIN * 2,
                    Math.round(context.getScaledWindowWidth() * MAX_IMAGE_WIDTH_RATIO)
            ));
            int maxHeight = Math.max(1, Math.round(context.getScaledWindowHeight() * MAX_IMAGE_HEIGHT_RATIO));
            float scale = Math.min((float) maxWidth / currentImageWidth, (float) maxHeight / currentImageHeight);
            int drawWidth = Math.max(1, Math.round(currentImageWidth * scale));
            int drawHeight = Math.max(1, Math.round(currentImageHeight * scale));
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    currentImageId,
                    (context.getScaledWindowWidth() - drawWidth) / 2,
                    IMAGE_TOP_MARGIN,
                    drawWidth,
                    drawHeight,
                    0,
                    0,
                    currentImageWidth,
                    currentImageHeight,
                    currentImageWidth,
                    currentImageHeight
            );
        }
    }

    private static Identifier betterrailwaysystem$resolveImage(String imageId) {
        betterrailwaysystem$clearCurrentImageTexture();
        Identifier identifier = Identifier.tryParse(imageId);
        if (identifier == null) {
            return null;
        }

        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(identifier);
        if (resource.isEmpty()) {
            return null;
        }

        try (InputStream inputStream = resource.get().getInputStream()) {
            NativeImage image = NativeImage.read(inputStream);
            currentImageWidth = image.getWidth();
            currentImageHeight = image.getHeight();
            if (currentImageWidth <= 0 || currentImageHeight <= 0) {
                image.close();
                currentImageWidth = 0;
                currentImageHeight = 0;
                return null;
            }

            currentImageTexture = new NativeImageBackedTexture(image);
            currentImageTexture.setFilter(true, false);
            currentImageId = Identifier.of("betterrailwaysystem", "station_image_dynamic");
            MinecraftClient.getInstance().getTextureManager().registerTexture(currentImageId, currentImageTexture);
            return currentImageId;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void betterrailwaysystem$clearAnnouncement(MinecraftClient client) {
        announcementVisible = false;
        keepImageUntilNextBalise = false;
        visibleUntilTick = 0L;
        betterrailwaysystem$clearCurrentImageTexture();
        client.inGameHud.clearTitle();
    }

    private static void betterrailwaysystem$clearAnnouncement() {
        announcementVisible = false;
        keepImageUntilNextBalise = false;
        visibleUntilTick = 0L;
        betterrailwaysystem$clearCurrentImageTexture();
    }

    private static void betterrailwaysystem$clearCurrentImageTexture() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (currentImageId != null) {
            client.getTextureManager().destroyTexture(currentImageId);
            currentImageId = null;
        }
        if (currentImageTexture != null) {
            currentImageTexture.close();
            currentImageTexture = null;
        }
        currentImageId = null;
        currentImageWidth = 0;
        currentImageHeight = 0;
    }
}
