package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;

// 客户端保存铁路信标内容时发回服务端的数据包。
public record SaveBalisePayload(
        BlockPos pos,
        String mode,
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
    public static final Identifier ID = BetterRailwaySystem.id("save_balise");

    public static SaveBalisePayload read(PacketByteBuf buf) {
        return new SaveBalisePayload(
                buf.readBlockPos(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readString(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readString()
        );
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeString(mode);
        buf.writeString(titleText);
        buf.writeString(subtitleText);
        buf.writeString(currentStation);
        buf.writeString(nextStation);
        buf.writeString(soundId);
        buf.writeString(imageId);
        buf.writeVarInt(imageDurationSeconds);
        buf.writeBoolean(keepImageUntilNextBalise);
        buf.writeBoolean(updateBossBar);
        buf.writeDouble(speedLimitBps);
        buf.writeString(triggerDirection);
    }

    public BaliseMode parsedMode() {
        return BaliseMode.fromString(mode);
    }
}
