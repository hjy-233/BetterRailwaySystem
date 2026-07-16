package org.dcstudio.station;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.config.BetterRailwaySystemDataSchema;
import org.dcstudio.minecart.MinecartDebugSnapshot;
import org.jetbrains.annotations.Nullable;

// 作为收车终点的标记方块实体。
public final class TrainCollectorBlockEntity extends BlockEntity {
    private MinecartDebugSnapshot lastMinecartDebug = MinecartDebugSnapshot.empty();

    public TrainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BetterRailwaySystem.TRAIN_COLLECTOR_BLOCK_ENTITY, pos, state);
    }

    public MinecartDebugSnapshot getLastMinecartDebug() {
        return lastMinecartDebug;
    }

    public void recordLastMinecart(AbstractMinecartEntity minecart) {
        lastMinecartDebug = MinecartDebugRecorder.snapshot(minecart);
        MinecartDebugRecorder.sync(this);
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt(BetterRailwaySystemDataSchema.VERSION_KEY, BetterRailwaySystemDataSchema.currentVersion());
        nbt.put("LastMinecartDebug", lastMinecartDebug.toNbt());
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        lastMinecartDebug = nbt.contains("LastMinecartDebug") ? MinecartDebugSnapshot.fromNbt(nbt.getCompound("LastMinecartDebug")) : MinecartDebugSnapshot.empty();
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
