package org.dcstudio.station;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.BetterRailwaySystemDataSchema;
import org.dcstudio.minecart.MinecartDebugSnapshot;
import org.dcstudio.minecart.StopRailWaitMode;
import org.jetbrains.annotations.Nullable;

// 保存停车轨的停车距离和等待策略。
public final class StopRailBlockEntity extends BlockEntity {
    private int stopDistance = BetterRailwaySystemDataSchema.defaultStopDistance();
    private int dwellSeconds = BetterRailwaySystemDataSchema.defaultStopDwellSeconds();
    private StopRailWaitMode waitMode = BetterRailwaySystemDataSchema.defaultStopWaitMode();
    private MinecartDebugSnapshot lastMinecartDebug = MinecartDebugSnapshot.empty();

    public StopRailBlockEntity(BlockPos pos, BlockState state) {
        super(BetterRailwaySystem.STOP_RAIL_BLOCK_ENTITY, pos, state);
    }

    public int getStopDistance() {
        return stopDistance;
    }

    public int getDwellSeconds() {
        return dwellSeconds;
    }

    public StopRailWaitMode getWaitMode() {
        return waitMode;
    }

    public MinecartDebugSnapshot getLastMinecartDebug() {
        return lastMinecartDebug;
    }

    public void recordLastMinecart(AbstractMinecartEntity minecart) {
        lastMinecartDebug = MinecartDebugRecorder.snapshot(minecart);
        MinecartDebugRecorder.sync(this);
    }

    public void setSettings(int stopDistance, int dwellSeconds, StopRailWaitMode waitMode) {
        this.stopDistance = MathHelper.clamp(stopDistance, 1, 128);
        this.dwellSeconds = MathHelper.clamp(dwellSeconds, 0, 600);
        this.waitMode = waitMode == null ? StopRailWaitMode.TIMER : waitMode;
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt(BetterRailwaySystemDataSchema.VERSION_KEY, BetterRailwaySystemDataSchema.currentVersion());
        nbt.putInt("StopDistance", stopDistance);
        nbt.putInt("DwellSeconds", dwellSeconds);
        nbt.putString("WaitMode", waitMode.serializedName());
        nbt.put("LastMinecartDebug", lastMinecartDebug.toNbt());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        stopDistance = MathHelper.clamp(nbt.contains("StopDistance") ? nbt.getInt("StopDistance") : BetterRailwaySystemDataSchema.defaultStopDistance(), 1, 128);
        dwellSeconds = MathHelper.clamp(nbt.contains("DwellSeconds") ? nbt.getInt("DwellSeconds") : BetterRailwaySystemDataSchema.defaultStopDwellSeconds(), 0, 600);
        waitMode = nbt.contains("WaitMode") ? StopRailWaitMode.fromString(nbt.getString("WaitMode")) : BetterRailwaySystemDataSchema.defaultStopWaitMode();
        lastMinecartDebug = nbt.contains("LastMinecartDebug") ? MinecartDebugSnapshot.fromNbt(nbt.getCompound("LastMinecartDebug")) : MinecartDebugSnapshot.empty();
        if (!nbt.contains(BetterRailwaySystemDataSchema.VERSION_KEY)) {
            markDirty();
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
