package org.dcstudio.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.jetbrains.annotations.Nullable;

// 定时或红石发车的矿车生成方块。
public final class TrainSpawnerBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<TrainSpawnerBlock> CODEC = createCodec(TrainSpawnerBlock::new);

    public TrainSpawnerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrainSpawnerBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof TrainSpawnerBlockEntity blockEntity && player instanceof ServerPlayerEntity serverPlayer) {
            BetterRailwaySystemNetworking.openTrainSpawnerEditor(serverPlayer, blockEntity);
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) {
            return null;
        }
        return validateTicker(type, BetterRailwaySystem.TRAIN_SPAWNER_BLOCK_ENTITY, (serverWorld, pos, currentState, blockEntity) ->
                TrainSpawnerBlockEntity.tick((ServerWorld) serverWorld, pos, currentState, blockEntity)
        );
    }
}
