package org.dcstudio.station;

import net.minecraft.block.Block;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BetterRailwaySystemAccess;
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.RailwayCityState;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// 负责定时或红石控制生成带线路信息的矿车。
public final class TrainSpawnerBlockEntity extends BlockEntity {
    private String cityName = "Default";
    private String lineId = "L1";
    private String lineThemeColor = LineThemeColor.BLUE.serializedName();
    private TrainSpawnDirection direction = TrainSpawnDirection.FORWARD;
    private int intervalSeconds = 60;
    private boolean redstoneControlled;
    private boolean circularLine;
    private int cooldownTicks;
    private boolean wasPowered;
    private static final double SPAWN_SPEED = 0.24;

    public TrainSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(BetterRailwaySystem.TRAIN_SPAWNER_BLOCK_ENTITY, pos, state);
        cooldownTicks = intervalSeconds * 20;
    }

    public static void tick(ServerWorld world, BlockPos pos, BlockState state, TrainSpawnerBlockEntity blockEntity) {
        ChunkPos chunkPos = new ChunkPos(pos);
        world.setChunkForced(chunkPos.x, chunkPos.z, true);
        boolean powered = world.isReceivingRedstonePower(pos);
        if (blockEntity.redstoneControlled) {
            if (powered && !blockEntity.wasPowered) {
                blockEntity.spawnMinecart(world);
            }
            blockEntity.wasPowered = powered;
            return;
        }

        if (blockEntity.cooldownTicks > 0) {
            blockEntity.cooldownTicks--;
        }
        if (blockEntity.cooldownTicks <= 0) {
            blockEntity.spawnMinecart(world);
            blockEntity.cooldownTicks = Math.max(20, blockEntity.intervalSeconds * 20);
        }
    }

    public String getLineId() {
        return lineId;
    }

    public String getCityName() {
        return cityName;
    }

    public TrainSpawnDirection getDirection() {
        return direction;
    }

    public String getLineThemeColor() {
        return lineThemeColor;
    }

    public int getIntervalSeconds() {
        return intervalSeconds;
    }

    public boolean isRedstoneControlled() {
        return redstoneControlled;
    }

    public boolean isCircularLine() {
        return circularLine;
    }

    public void setSettings(String cityName, String lineId, String lineThemeColor, TrainSpawnDirection direction, int intervalSeconds, boolean redstoneControlled, boolean circularLine) {
        this.cityName = sanitizeText(cityName, 32, "Default");
        this.lineId = sanitizeText(lineId, 32, "L1");
        this.lineThemeColor = LineThemeColor.fromString(lineThemeColor).serializedName();
        this.direction = direction == null ? TrainSpawnDirection.FORWARD : direction;
        this.intervalSeconds = MathHelper.clamp(intervalSeconds, 1, 3600);
        this.redstoneControlled = redstoneControlled;
        this.circularLine = circularLine;
        this.cooldownTicks = Math.max(20, this.intervalSeconds * 20);
        if (world instanceof ServerWorld serverWorld) {
            RailwayCityState.get(serverWorld).addCity(this.cityName);
        }
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putString("CityName", cityName);
        nbt.putString("LineId", lineId);
        nbt.putString("LineThemeColor", lineThemeColor);
        nbt.putString("Direction", direction.serializedName());
        nbt.putInt("IntervalSeconds", intervalSeconds);
        nbt.putBoolean("RedstoneControlled", redstoneControlled);
        nbt.putBoolean("CircularLine", circularLine);
        nbt.putInt("CooldownTicks", cooldownTicks);
        nbt.putBoolean("WasPowered", wasPowered);
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        cityName = sanitizeText(nbt.getString("CityName"), 32, "Default");
        lineId = sanitizeText(nbt.getString("LineId"), 32, "L1");
        lineThemeColor = LineThemeColor.fromString(nbt.getString("LineThemeColor")).serializedName();
        direction = TrainSpawnDirection.fromString(nbt.getString("Direction"));
        intervalSeconds = MathHelper.clamp(nbt.getInt("IntervalSeconds"), 1, 3600);
        redstoneControlled = nbt.getBoolean("RedstoneControlled");
        circularLine = nbt.getBoolean("CircularLine");
        cooldownTicks = nbt.contains("CooldownTicks") ? nbt.getInt("CooldownTicks") : intervalSeconds * 20;
        wasPowered = nbt.getBoolean("WasPowered");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    private void spawnMinecart(ServerWorld world) {
        BlockPos railPos = pos.up();
        if (!world.getBlockState(railPos).isIn(net.minecraft.registry.tag.BlockTags.RAILS)) {
            railPos = pos;
            if (!world.getBlockState(railPos).isIn(net.minecraft.registry.tag.BlockTags.RAILS)) {
                return;
            }
        }

        List<MinecartEntity> nearbyMinecarts = world.getEntitiesByClass(
                MinecartEntity.class,
                new Box(railPos).expand(1.0),
                minecart -> true
        );
        if (!nearbyMinecarts.isEmpty()) {
            return;
        }

        MinecartEntity minecart = new MinecartEntity(world, railPos.getX() + 0.5, railPos.getY() + 0.0625, railPos.getZ() + 0.5);
        TrainSpawnDirection resolvedDirection = resolveDirection(world.getBlockState(railPos), direction);
        if (minecart instanceof BetterRailwaySystemAccess access) {
            access.betterrailwaysystem$setCityName(cityName);
            access.betterrailwaysystem$setLineId(lineId);
            access.betterrailwaysystem$setLineThemeColor(lineThemeColor);
            access.betterrailwaysystem$setOriginSpawnerPos(pos.toImmutable());
            access.betterrailwaysystem$setCircularLine(circularLine);
            access.betterrailwaysystem$setLineDirection(resolvedDirection);
            access.betterrailwaysystem$clearVisitedStations();
            access.betterrailwaysystem$setCurrentStation("");
            access.betterrailwaysystem$setNextStation("");
        }
        applySpawnVelocity(minecart, resolvedDirection);
        world.spawnEntity(minecart);
    }

    private void applySpawnVelocity(MinecartEntity minecart, TrainSpawnDirection resolvedDirection) {
        double velocityX = 0.0;
        double velocityZ = 0.0;
        switch (resolvedDirection) {
            case EAST -> velocityX = SPAWN_SPEED;
            case WEST -> velocityX = -SPAWN_SPEED;
            case SOUTH -> velocityZ = SPAWN_SPEED;
            case NORTH -> velocityZ = -SPAWN_SPEED;
            default -> velocityX = SPAWN_SPEED;
        }
        if (velocityX == 0.0 && velocityZ == 0.0) {
            velocityX = SPAWN_SPEED;
        }
        minecart.setVelocity(velocityX, 0.0, velocityZ);
    }

    public static List<TrainSpawnDirection> detectDirections(World world, BlockPos originPos) {
        BlockPos railPos = originPos.up();
        BlockState railState = world.getBlockState(railPos);
        if (!railState.isIn(net.minecraft.registry.tag.BlockTags.RAILS)) {
            railPos = originPos;
            railState = world.getBlockState(railPos);
        }
        return getAvailableDirections(railState);
    }

    public static List<TrainSpawnDirection> getAvailableDirections(BlockState railState) {
        if (!(railState.getBlock() instanceof AbstractRailBlock railBlock)) {
            return List.of();
        }
        RailShape shape = railState.get(railBlock.getShapeProperty());
        List<TrainSpawnDirection> directions = new ArrayList<>(2);
        switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> {
                directions.add(TrainSpawnDirection.EAST);
                directions.add(TrainSpawnDirection.WEST);
            }
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> {
                directions.add(TrainSpawnDirection.SOUTH);
                directions.add(TrainSpawnDirection.NORTH);
            }
            case NORTH_EAST -> {
                directions.add(TrainSpawnDirection.NORTH);
                directions.add(TrainSpawnDirection.EAST);
            }
            case NORTH_WEST -> {
                directions.add(TrainSpawnDirection.NORTH);
                directions.add(TrainSpawnDirection.WEST);
            }
            case SOUTH_EAST -> {
                directions.add(TrainSpawnDirection.SOUTH);
                directions.add(TrainSpawnDirection.EAST);
            }
            case SOUTH_WEST -> {
                directions.add(TrainSpawnDirection.SOUTH);
                directions.add(TrainSpawnDirection.WEST);
            }
        }
        return directions;
    }

    private TrainSpawnDirection resolveDirection(BlockState railState, TrainSpawnDirection preferredDirection) {
        List<TrainSpawnDirection> availableDirections = getAvailableDirections(railState);
        if (availableDirections.isEmpty()) {
            return preferredDirection.isLegacyRelative() ? TrainSpawnDirection.EAST : preferredDirection;
        }
        if (availableDirections.contains(preferredDirection)) {
            return preferredDirection;
        }
        if (preferredDirection == TrainSpawnDirection.BACKWARD && availableDirections.size() > 1) {
            return availableDirections.get(1);
        }
        return availableDirections.getFirst();
    }

    private String sanitizeText(String value, int maxLength, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isBlank()) {
            return fallback;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
