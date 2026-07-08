package org.dcstudio.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

// 作为列车运行终点的回收方块。
public final class TrainCollectorBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<TrainCollectorBlock> CODEC = createCodec(TrainCollectorBlock::new);

    public TrainCollectorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrainCollectorBlockEntity(pos, state);
    }
}
