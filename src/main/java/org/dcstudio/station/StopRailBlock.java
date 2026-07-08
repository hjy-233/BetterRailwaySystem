package org.dcstudio.station;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.jetbrains.annotations.Nullable;

// 放在铁轨下方的停车控制方块。
public final class StopRailBlock extends BlockWithEntity implements BlockEntityProvider {
    public static final MapCodec<StopRailBlock> CODEC = createCodec(StopRailBlock::new);

    public StopRailBlock(Settings settings) {
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
        return new StopRailBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof StopRailBlockEntity blockEntity && player instanceof ServerPlayerEntity serverPlayer) {
            BetterRailwaySystemNetworking.openStopRailEditor(serverPlayer, blockEntity);
        }
        return ActionResult.SUCCESS;
    }
}
