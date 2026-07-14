package org.dcstudio.station;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.jetbrains.annotations.Nullable;

// 保存铁路信标的触发类型和广播内容。
public final class RailwayBaliseBlockEntity extends BlockEntity {
    private BaliseMode mode = BaliseMode.ARRIVAL;
    private String titleText = "";
    private String subtitleText = "";
    private String currentStation = "";
    private String nextStation = "";
    private String soundId = "";
    private String imageId = "";
    private int imageDurationSeconds = 5;
    private boolean keepImageUntilNextBalise;
    private boolean updateBossBar = true;
    private double speedLimitBps = 4.0;
    private String triggerDirection = "";

    public RailwayBaliseBlockEntity(BlockPos pos, BlockState state) {
        super(BetterRailwaySystem.RAILWAY_BALISE_BLOCK_ENTITY, pos, state);
    }

    public BaliseMode getMode() {
        return mode;
    }

    public String getTitleText() {
        return titleText;
    }

    public String getSubtitleText() {
        return subtitleText;
    }

    public String getCurrentStation() {
        return currentStation;
    }

    public String getNextStation() {
        return nextStation;
    }

    public String getSoundId() {
        return soundId;
    }

    public String getImageId() {
        return imageId;
    }

    public int getImageDurationSeconds() {
        return imageDurationSeconds;
    }

    public boolean shouldKeepImageUntilNextBalise() {
        return keepImageUntilNextBalise;
    }

    public boolean shouldUpdateBossBar() {
        return updateBossBar;
    }

    public double getSpeedLimitBps() {
        return speedLimitBps;
    }

    public String getTriggerDirection() {
        return triggerDirection;
    }

    public void setSettings(
            BaliseMode mode,
            String titleText,
            String subtitleText,
            String currentStation,
            String nextStation,
            String soundId,
            String imageId,
            int imageDurationSeconds,
            boolean keepImageUntilNextBalise,
            boolean updateBossBar,
            double speedLimitBps,
            String triggerDirection
    ) {
        this.mode = mode == null ? BaliseMode.ARRIVAL : mode;
        this.titleText = sanitizeText(titleText, 64);
        this.subtitleText = sanitizeText(subtitleText, 96);
        this.currentStation = sanitizeText(currentStation, 64);
        this.nextStation = sanitizeText(nextStation, 64);
        this.soundId = sanitizeText(soundId, 128);
        this.imageId = sanitizeText(imageId, 128);
        this.imageDurationSeconds = MathHelper.clamp(imageDurationSeconds, 1, 60);
        this.keepImageUntilNextBalise = keepImageUntilNextBalise;
        this.updateBossBar = updateBossBar;
        this.speedLimitBps = Math.max(0.01, speedLimitBps);
        this.triggerDirection = sanitizeDirection(triggerDirection);
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("Mode", mode.serializedName());
        nbt.putString("TitleText", titleText);
        nbt.putString("SubtitleText", subtitleText);
        nbt.putString("CurrentStation", currentStation);
        nbt.putString("NextStation", nextStation);
        nbt.putString("SoundId", soundId);
        nbt.putString("ImageId", imageId);
        nbt.putInt("ImageDurationSeconds", imageDurationSeconds);
        nbt.putBoolean("KeepImageUntilNextBalise", keepImageUntilNextBalise);
        nbt.putBoolean("UpdateBossBar", updateBossBar);
        nbt.putDouble("SpeedLimitBps", speedLimitBps);
        nbt.putString("TriggerDirection", triggerDirection);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        mode = BaliseMode.fromString(nbt.getString("Mode"));
        titleText = sanitizeText(nbt.getString("TitleText"), 64);
        subtitleText = sanitizeText(nbt.getString("SubtitleText"), 96);
        currentStation = sanitizeText(nbt.getString("CurrentStation"), 64);
        nextStation = sanitizeText(nbt.getString("NextStation"), 64);
        if (currentStation.isBlank() && nbt.contains("StationName")) {
            currentStation = sanitizeText(nbt.getString("StationName"), 64);
        }
        soundId = sanitizeText(nbt.getString("SoundId"), 128);
        imageId = sanitizeText(nbt.getString("ImageId"), 128);
        int savedDuration = nbt.contains("ImageDurationSeconds") ? nbt.getInt("ImageDurationSeconds") : 5;
        imageDurationSeconds = MathHelper.clamp(savedDuration, 1, 60);
        keepImageUntilNextBalise = nbt.getBoolean("KeepImageUntilNextBalise");
        updateBossBar = !nbt.contains("UpdateBossBar") || nbt.getBoolean("UpdateBossBar");
        speedLimitBps = nbt.contains("SpeedLimitBps") ? Math.max(0.01, nbt.getDouble("SpeedLimitBps")) : 4.0;
        triggerDirection = nbt.contains("TriggerDirection") ? sanitizeDirection(nbt.getString("TriggerDirection")) : "";
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    private String sanitizeText(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String sanitizeDirection(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        TrainSpawnDirection direction = TrainSpawnDirection.fromString(value);
        return direction.isLegacyRelative() ? "" : direction.serializedName();
    }
}
