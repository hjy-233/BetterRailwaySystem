package org.dcstudio.station;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.StopRailWaitMode;
import org.jetbrains.annotations.Nullable;

// 保存停车轨的停车距离和等待策略。
public final class StopRailBlockEntity extends BlockEntity {
    private int stopDistance = 30;
    private int dwellSeconds = 3;
    private StopRailWaitMode waitMode = StopRailWaitMode.TIMER;

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
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putInt("StopDistance", stopDistance);
        view.putInt("DwellSeconds", dwellSeconds);
        view.putString("WaitMode", waitMode.serializedName());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        stopDistance = MathHelper.clamp(view.getInt("StopDistance", 30), 1, 128);
        dwellSeconds = MathHelper.clamp(view.getInt("DwellSeconds", 3), 0, 600);
        waitMode = StopRailWaitMode.fromString(view.getString("WaitMode", ""));
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
