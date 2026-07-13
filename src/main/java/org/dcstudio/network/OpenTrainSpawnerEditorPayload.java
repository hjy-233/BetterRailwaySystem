package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

import java.util.List;

// 服务端打开发车器编辑界面的数据包。
public record OpenTrainSpawnerEditorPayload(
        BlockPos pos,
        String lineId,
        String lineThemeColor,
        String direction,
        int targetTrainCount,
        int flags,
        List<String> cityOptions
) {
    public static final Identifier ID = BetterRailwaySystem.id("open_train_spawner_editor");

    public static OpenTrainSpawnerEditorPayload read(PacketByteBuf buf) {
        return new OpenTrainSpawnerEditorPayload(
                buf.readBlockPos(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt(),
                buf.readVarInt(),
                PacketBufHelper.readStringList(buf)
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(lineId);
        buf.writeString(lineThemeColor);
        buf.writeString(direction);
        buf.writeVarInt(targetTrainCount);
        buf.writeVarInt(flags);
        PacketBufHelper.writeStringList(buf, cityOptions);
    }

    public boolean redstoneControlled() {
        return (flags & 1) != 0;
    }

    public boolean circularLine() {
        return (flags & 2) != 0;
    }
}
