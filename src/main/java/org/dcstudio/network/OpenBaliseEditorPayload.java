package org.dcstudio.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;

// 服务端打开铁路信标编辑界面的数据包。
public record OpenBaliseEditorPayload(
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
    public static final Identifier ID = BetterRailwaySystem.id("open_balise_editor");

    public static OpenBaliseEditorPayload read(PacketByteBuf buf) {
        return new OpenBaliseEditorPayload(
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
