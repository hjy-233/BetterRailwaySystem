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

// 道旁应答器方块，右键后打开客户端编辑界面。
public final class RailwayBaliseBlock extends BlockWithEntity {
    public static final MapCodec<RailwayBaliseBlock> CODEC = createCodec(RailwayBaliseBlock::new);

    public RailwayBaliseBlock(Settings settings) {
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
        return new RailwayBaliseBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof RailwayBaliseBlockEntity blockEntity && player instanceof ServerPlayerEntity serverPlayer) {
            BetterRailwaySystemNetworking.openEditor(serverPlayer, blockEntity);
        }
        return ActionResult.SUCCESS;
    }
}
