package org.dcstudio.station;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.dcstudio.BetterRailwaySystem;

// 作为收车终点的标记方块实体。
public final class TrainCollectorBlockEntity extends BlockEntity {
    public TrainCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(BetterRailwaySystem.TRAIN_COLLECTOR_BLOCK_ENTITY, pos, state);
    }
}
