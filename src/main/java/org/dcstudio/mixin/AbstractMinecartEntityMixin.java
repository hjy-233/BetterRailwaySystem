package org.dcstudio.mixin;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseMode;
import org.dcstudio.minecart.BetterRailwaySystemAccess;
import org.dcstudio.minecart.LineThemeColor;
import org.dcstudio.minecart.StopRailWaitMode;
import org.dcstudio.minecart.TrainSpawnDirection;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.dcstudio.station.RailwayBaliseBlockEntity;
import org.dcstudio.station.StopRailBlockEntity;
import org.dcstudio.station.TrainCollectorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// 统一处理矿车速度、线路运行、停车、信标和区块加载。
@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin implements BetterRailwaySystemAccess {
    @Unique
    private static final double BETTERMINECART_VANILLA_MAX_RAIL_SPEED = 8.0 / 20.0;
    @Unique
    private static final double BETTERMINECART_MIN_LAUNCH_SPEED = 0.08;
    @Unique
    private static final int BETTERMINECART_MAX_LAUNCH_TICKS = 6;
    @Unique
    private static final Direction[] BETTERMINECART_HORIZONTAL_DIRECTIONS = new Direction[]{
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };
    @Unique
    private static final int[][] BETTERMINECART_RAIL_SEARCH_OFFSETS = new int[][]{
            {0, 0, 0},
            {0, -1, 0},
            {1, 0, 0},
            {-1, 0, 0},
            {0, 0, 1},
            {0, 0, -1},
            {1, -1, 0},
            {-1, -1, 0},
            {0, -1, 1},
            {0, -1, -1},
            {1, 0, 1},
            {1, 0, -1},
            {-1, 0, 1},
            {-1, 0, -1},
            {1, -1, 1},
            {1, -1, -1},
            {-1, -1, 1},
            {-1, -1, -1}
    };
    @Unique
    private final BlockPos.Mutable betterrailwaysystem$searchPos = new BlockPos.Mutable();
    @Unique
    private final BlockPos.Mutable betterrailwaysystem$railPos = new BlockPos.Mutable();
    @Unique
    private final BlockPos.Mutable betterrailwaysystem$otherRailPos = new BlockPos.Mutable();
    @Unique
    private double betterrailwaysystem$previousHorizontalSpeed;
    @Unique
    private long betterrailwaysystem$lastBalisePos = Long.MIN_VALUE;
    @Unique
    private boolean betterrailwaysystem$substeppingRail;
    @Unique
    private boolean betterrailwaysystem$ascendingLaunchArmed;
    @Unique
    private int betterrailwaysystem$launchTicksRemaining;
    @Unique
    private double betterrailwaysystem$launchDirectionX;
    @Unique
    private double betterrailwaysystem$launchDirectionZ;
    @Unique
    private double betterrailwaysystem$activeSpeedLimitBps = -1.0;
    @Unique
    private String betterrailwaysystem$cityName = "";
    @Unique
    private String betterrailwaysystem$lineId = "";
    @Unique
    private String betterrailwaysystem$lineThemeColor = LineThemeColor.BLUE.serializedName();
    @Unique
    private BlockPos betterrailwaysystem$originSpawnerPos;
    @Unique
    private boolean betterrailwaysystem$circularLine;
    @Unique
    private boolean betterrailwaysystem$leftOriginSpawner;
    @Unique
    private boolean betterrailwaysystem$circularLineRecorded;
    @Unique
    private TrainSpawnDirection betterrailwaysystem$lineDirection = TrainSpawnDirection.FORWARD;
    @Unique
    private String betterrailwaysystem$currentStation = "";
    @Unique
    private String betterrailwaysystem$nextStation = "";
    @Unique
    private final List<String> betterrailwaysystem$visitedStations = new ArrayList<>();
    @Unique
    private final List<BlockPos> betterrailwaysystem$visitedStationPositions = new ArrayList<>();
    @Unique
    private BlockPos betterrailwaysystem$pendingStopRailPos;
    @Unique
    private int betterrailwaysystem$stopDwellTicksRemaining;
    @Unique
    private StopRailWaitMode betterrailwaysystem$stopWaitMode = StopRailWaitMode.TIMER;
    @Unique
    private boolean betterrailwaysystem$waitingAtStopRail;
    @Unique
    private BlockPos betterrailwaysystem$lastReleasedStopRailPos;
    @Unique
    private int betterrailwaysystem$stopRailReleaseCooldownTicks;
    @Unique
    private double betterrailwaysystem$lastTravelDirectionX = 1.0;
    @Unique
    private double betterrailwaysystem$lastTravelDirectionZ;
    @Unique
    private double betterrailwaysystem$lastTravelSpeed;
    @Unique
    private double betterrailwaysystem$stopRailReleaseSpeed;
    @Unique
    private int betterrailwaysystem$stopRailBoostTicksRemaining;
    @Unique
    private double betterrailwaysystem$stopRailBoostDirectionX;
    @Unique
    private double betterrailwaysystem$stopRailBoostDirectionZ;
    @Unique
    private int betterrailwaysystem$followRecoveryTicksRemaining;
    @Unique
    private int betterrailwaysystem$bounceRestartTicksRemaining;
    @Unique
    private double betterrailwaysystem$bounceRestartSpeed;
    @Unique
    private int betterrailwaysystem$idleTicks;
    @Unique
    private ServerBossBar betterrailwaysystem$bossBar;
    @Unique
    private int betterrailwaysystem$forcedChunkX = Integer.MIN_VALUE;
    @Unique
    private int betterrailwaysystem$forcedChunkZ = Integer.MIN_VALUE;

    @Shadow
    protected abstract void moveOnRail(BlockPos pos, BlockState state);

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$overrideMaxSpeed(CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(betterrailwaysystem$getCurrentMaxSpeedPerTick());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void betterrailwaysystem$captureSpeed(CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;
        if (betterrailwaysystem$waitingAtStopRail) {
            minecart.setVelocity(0.0, minecart.getVelocity().y, 0.0);
        } else if (betterrailwaysystem$stopRailBoostTicksRemaining > 0) {
            double boostSpeed = Math.min(betterrailwaysystem$getCurrentMaxSpeedPerTick(), Math.max(betterrailwaysystem$stopRailReleaseSpeed, 0.24));
            minecart.setVelocity(
                    betterrailwaysystem$stopRailBoostDirectionX * boostSpeed,
                    minecart.getVelocity().y,
                    betterrailwaysystem$stopRailBoostDirectionZ * boostSpeed
            );
        }
        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        betterrailwaysystem$previousHorizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (betterrailwaysystem$previousHorizontalSpeed > 0.0001) {
            double currentDirectionX = velocityX / betterrailwaysystem$previousHorizontalSpeed;
            double currentDirectionZ = velocityZ / betterrailwaysystem$previousHorizontalSpeed;
            double lastDirectionLengthSquared = betterrailwaysystem$lastTravelDirectionX * betterrailwaysystem$lastTravelDirectionX
                    + betterrailwaysystem$lastTravelDirectionZ * betterrailwaysystem$lastTravelDirectionZ;
            if (lastDirectionLengthSquared > 0.0001) {
                double alignment = currentDirectionX * betterrailwaysystem$lastTravelDirectionX + currentDirectionZ * betterrailwaysystem$lastTravelDirectionZ;
                if (alignment < -0.6 && betterrailwaysystem$lastTravelSpeed >= BETTERMINECART_MIN_LAUNCH_SPEED) {
                    betterrailwaysystem$bounceRestartTicksRemaining = 12;
                    betterrailwaysystem$bounceRestartSpeed = Math.max(betterrailwaysystem$bounceRestartSpeed, betterrailwaysystem$lastTravelSpeed);
                    return;
                }
            }
            betterrailwaysystem$lastTravelDirectionX = currentDirectionX;
            betterrailwaysystem$lastTravelDirectionZ = currentDirectionZ;
            betterrailwaysystem$lastTravelSpeed = betterrailwaysystem$previousHorizontalSpeed;
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void betterrailwaysystem$afterTick(CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;
        if (minecart.getEntityWorld().isClient() || !(minecart.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        betterrailwaysystem$handleIdleRemoval(minecart);
        if (minecart.isRemoved()) {
            return;
        }
        betterrailwaysystem$syncForcedChunks(minecart, serverWorld);
        betterrailwaysystem$syncBossBar(minecart);
        betterrailwaysystem$handleStopRailWait(minecart);
        if (betterrailwaysystem$stopRailReleaseCooldownTicks > 0) {
            betterrailwaysystem$stopRailReleaseCooldownTicks--;
        }
        if (betterrailwaysystem$stopRailBoostTicksRemaining > 0) {
            betterrailwaysystem$stopRailBoostTicksRemaining--;
            if (betterrailwaysystem$getCurrentStopRail(minecart) == null) {
                betterrailwaysystem$stopRailBoostTicksRemaining = 0;
            }
        }

        BlockState railState = betterrailwaysystem$findRailState(minecart);
        if (railState == null) {
            betterrailwaysystem$lastBalisePos = Long.MIN_VALUE;
            return;
        }

        betterrailwaysystem$triggerBaliseIfNeeded(minecart);
        betterrailwaysystem$updateStopRailTarget(minecart);
        betterrailwaysystem$applyStopRailBraking(minecart);
        betterrailwaysystem$applySafeFollowing(minecart);
        betterrailwaysystem$handleBounceRestart(minecart);
        betterrailwaysystem$recordCircularLineIfNeeded(minecart, serverWorld);
        betterrailwaysystem$collectIfNeeded(minecart, serverWorld);
    }

    @Inject(method = "moveOnRail", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$substepRailMovement(BlockPos pos, BlockState state, CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;
        betterrailwaysystem$updateAscendingLaunchState(minecart, state);
        if (betterrailwaysystem$substeppingRail || minecart.getEntityWorld().isClient()) {
            return;
        }

        double velocityX = minecart.getVelocity().x;
        double velocityY = minecart.getVelocity().y;
        double velocityZ = minecart.getVelocity().z;
        double horizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        int substeps = Math.max(1, MathHelper.ceil(horizontalSpeed / BETTERMINECART_VANILLA_MAX_RAIL_SPEED));
        if (substeps <= 1) {
            betterrailwaysystem$applyPoweredRailSmoothing(minecart, state);
            return;
        }

        betterrailwaysystem$substeppingRail = true;
        try {
            double desiredHorizontalSpeed = horizontalSpeed;
            double directionX = horizontalSpeed > 0.0 ? velocityX / horizontalSpeed : betterrailwaysystem$guessTravelDirectionX(minecart);
            double directionZ = horizontalSpeed > 0.0 ? velocityZ / horizontalSpeed : betterrailwaysystem$guessTravelDirectionZ(minecart);
            for (int step = 0; step < substeps; step++) {
                BlockState currentRailState = betterrailwaysystem$findRailState(minecart);
                if (currentRailState == null) {
                    break;
                }
                desiredHorizontalSpeed = betterrailwaysystem$getAdjustedTargetSpeed(desiredHorizontalSpeed, currentRailState);
                double stepHorizontalSpeed = desiredHorizontalSpeed / substeps;
                minecart.setVelocity(directionX * stepHorizontalSpeed, velocityY / substeps, directionZ * stepHorizontalSpeed);
                moveOnRail(betterrailwaysystem$railPos.toImmutable(), currentRailState);

                double movedVelocityX = minecart.getVelocity().x;
                double movedVelocityZ = minecart.getVelocity().z;
                double movedHorizontalSpeed = Math.sqrt(movedVelocityX * movedVelocityX + movedVelocityZ * movedVelocityZ);
                if (movedHorizontalSpeed > 0.0) {
                    directionX = movedVelocityX / movedHorizontalSpeed;
                    directionZ = movedVelocityZ / movedHorizontalSpeed;
                }
            }

            double finalVelocityX = minecart.getVelocity().x;
            double finalVelocityY = minecart.getVelocity().y;
            double finalVelocityZ = minecart.getVelocity().z;
            double finalHorizontalSpeed = Math.sqrt(finalVelocityX * finalVelocityX + finalVelocityZ * finalVelocityZ);
            double targetSpeed = Math.min(desiredHorizontalSpeed, betterrailwaysystem$getCurrentMaxSpeedPerTick());
            if (finalHorizontalSpeed > 0.0 && targetSpeed > 0.0) {
                double scale = targetSpeed / finalHorizontalSpeed;
                minecart.setVelocity(finalVelocityX * scale, finalVelocityY, finalVelocityZ * scale);
            }
        } finally {
            betterrailwaysystem$substeppingRail = false;
        }
        ci.cancel();
    }

    @Inject(method = "moveOffRail", at = @At("HEAD"), cancellable = true)
    private void betterrailwaysystem$preserveAscendingLaunch(CallbackInfo ci) {
        AbstractMinecartEntity minecart = (AbstractMinecartEntity) (Object) this;
        if (minecart.isOnGround()) {
            betterrailwaysystem$launchTicksRemaining = 0;
            betterrailwaysystem$ascendingLaunchArmed = false;
            return;
        }

        double maxSpeed = betterrailwaysystem$getCurrentMaxSpeedPerTick();
        double velocityX = MathHelper.clamp(minecart.getVelocity().x, -maxSpeed, maxSpeed);
        double velocityZ = MathHelper.clamp(minecart.getVelocity().z, -maxSpeed, maxSpeed);
        double horizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (betterrailwaysystem$launchTicksRemaining <= 0) {
            if (!betterrailwaysystem$ascendingLaunchArmed || horizontalSpeed < BETTERMINECART_MIN_LAUNCH_SPEED) {
                return;
            }
            betterrailwaysystem$launchTicksRemaining = betterrailwaysystem$computeLaunchTicks(horizontalSpeed);
            betterrailwaysystem$ascendingLaunchArmed = false;
        }

        if (betterrailwaysystem$launchTicksRemaining <= 0) {
            return;
        }

        double directionX = betterrailwaysystem$launchDirectionX;
        double directionZ = betterrailwaysystem$launchDirectionZ;
        if (horizontalSpeed > 0.0) {
            double currentDirectionX = velocityX / horizontalSpeed;
            double currentDirectionZ = velocityZ / horizontalSpeed;
            if (currentDirectionX * directionX + currentDirectionZ * directionZ > 0.5) {
                directionX = currentDirectionX;
                directionZ = currentDirectionZ;
            }
        }

        minecart.setVelocity(directionX * horizontalSpeed, horizontalSpeed, directionZ * horizontalSpeed);
        minecart.move(MovementType.SELF, minecart.getVelocity());
        if (!minecart.isOnGround()) {
            minecart.setVelocity(minecart.getVelocity().multiply(0.95));
        }

        betterrailwaysystem$launchTicksRemaining--;
        if (minecart.isOnGround()) {
            betterrailwaysystem$launchTicksRemaining = 0;
        }
        ci.cancel();
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void betterrailwaysystem$writeMinecartData(NbtCompound nbt, CallbackInfo ci) {
        nbt.putDouble("BetterRailwaySystemSpeedLimit", betterrailwaysystem$activeSpeedLimitBps);
        nbt.putString("BetterRailwaySystemCityName", betterrailwaysystem$cityName);
        nbt.putString("BetterRailwaySystemLineId", betterrailwaysystem$lineId);
        nbt.putString("BetterRailwaySystemLineThemeColor", betterrailwaysystem$lineThemeColor);
        nbt.putBoolean("BetterRailwaySystemCircularLine", betterrailwaysystem$circularLine);
        nbt.putBoolean("BetterRailwaySystemLeftOriginSpawner", betterrailwaysystem$leftOriginSpawner);
        nbt.putBoolean("BetterRailwaySystemCircularRecorded", betterrailwaysystem$circularLineRecorded);
        nbt.putString("BetterRailwaySystemLineDirection", betterrailwaysystem$lineDirection.serializedName());
        nbt.putString("BetterRailwaySystemCurrentStation", betterrailwaysystem$currentStation);
        nbt.putString("BetterRailwaySystemNextStation", betterrailwaysystem$nextStation);
        nbt.putBoolean("BetterRailwaySystemWaitingStop", betterrailwaysystem$waitingAtStopRail);
        nbt.putInt("BetterRailwaySystemStopDwell", betterrailwaysystem$stopDwellTicksRemaining);
        nbt.putString("BetterRailwaySystemStopWaitMode", betterrailwaysystem$stopWaitMode.serializedName());
        if (betterrailwaysystem$pendingStopRailPos != null) {
            nbt.putLong("BetterRailwaySystemStopPos", betterrailwaysystem$pendingStopRailPos.asLong());
        }
        if (betterrailwaysystem$originSpawnerPos != null) {
            nbt.putLong("BetterRailwaySystemOriginSpawnerPos", betterrailwaysystem$originSpawnerPos.asLong());
        }
        net.minecraft.nbt.NbtList list = new net.minecraft.nbt.NbtList();
        for (String station : betterrailwaysystem$visitedStations) {
            list.add(net.minecraft.nbt.NbtString.of(station));
        }
        nbt.put("BetterRailwaySystemVisitedStations", list);
        net.minecraft.nbt.NbtList posList = new net.minecraft.nbt.NbtList();
        for (BlockPos pos : betterrailwaysystem$visitedStationPositions) {
            posList.add(net.minecraft.nbt.NbtLong.of(pos.asLong()));
        }
        nbt.put("BetterRailwaySystemVisitedStationPositions", posList);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void betterrailwaysystem$readMinecartData(NbtCompound nbt, CallbackInfo ci) {
        betterrailwaysystem$activeSpeedLimitBps = nbt.getDouble("BetterRailwaySystemSpeedLimit", -1.0);
        betterrailwaysystem$cityName = nbt.getString("BetterRailwaySystemCityName", "");
        betterrailwaysystem$lineId = nbt.getString("BetterRailwaySystemLineId", "");
        betterrailwaysystem$lineThemeColor = LineThemeColor.fromString(nbt.getString("BetterRailwaySystemLineThemeColor", "")).serializedName();
        betterrailwaysystem$circularLine = nbt.getBoolean("BetterRailwaySystemCircularLine", false);
        betterrailwaysystem$leftOriginSpawner = nbt.getBoolean("BetterRailwaySystemLeftOriginSpawner", false);
        betterrailwaysystem$circularLineRecorded = nbt.getBoolean("BetterRailwaySystemCircularRecorded", false);
        betterrailwaysystem$lineDirection = TrainSpawnDirection.fromString(nbt.getString("BetterRailwaySystemLineDirection", ""));
        betterrailwaysystem$currentStation = nbt.getString("BetterRailwaySystemCurrentStation", "");
        betterrailwaysystem$nextStation = nbt.getString("BetterRailwaySystemNextStation", "");
        betterrailwaysystem$waitingAtStopRail = nbt.getBoolean("BetterRailwaySystemWaitingStop", false);
        betterrailwaysystem$stopDwellTicksRemaining = nbt.getInt("BetterRailwaySystemStopDwell", 0);
        betterrailwaysystem$stopWaitMode = StopRailWaitMode.fromString(nbt.getString("BetterRailwaySystemStopWaitMode", ""));
        betterrailwaysystem$pendingStopRailPos = nbt.contains("BetterRailwaySystemStopPos") ? BlockPos.fromLong(nbt.getLong("BetterRailwaySystemStopPos", 0L)) : null;
        betterrailwaysystem$originSpawnerPos = nbt.contains("BetterRailwaySystemOriginSpawnerPos") ? BlockPos.fromLong(nbt.getLong("BetterRailwaySystemOriginSpawnerPos", 0L)) : null;
        betterrailwaysystem$visitedStations.clear();
        betterrailwaysystem$visitedStationPositions.clear();
        net.minecraft.nbt.NbtList list = nbt.getListOrEmpty("BetterRailwaySystemVisitedStations");
        for (int index = 0; index < list.size(); index++) {
            String station = list.getString(index, "");
            if (!station.isBlank()) {
                betterrailwaysystem$visitedStations.add(station);
            }
        }
        net.minecraft.nbt.NbtList posList = nbt.getListOrEmpty("BetterRailwaySystemVisitedStationPositions");
        for (int index = 0; index < posList.size(); index++) {
            if (posList.get(index) instanceof net.minecraft.nbt.AbstractNbtNumber nbtNumber) {
                betterrailwaysystem$visitedStationPositions.add(BlockPos.fromLong(nbtNumber.longValue()));
            }
        }
        while (betterrailwaysystem$visitedStationPositions.size() < betterrailwaysystem$visitedStations.size()) {
            betterrailwaysystem$visitedStationPositions.add(BlockPos.ORIGIN);
        }
        while (betterrailwaysystem$visitedStationPositions.size() > betterrailwaysystem$visitedStations.size()) {
            betterrailwaysystem$visitedStationPositions.remove(betterrailwaysystem$visitedStationPositions.size() - 1);
        }
    }

    @Unique
    private void betterrailwaysystem$applyPoweredRailSmoothing(AbstractMinecartEntity minecart, BlockState railState) {
        double adjustedSpeed = betterrailwaysystem$getAdjustedTargetSpeed(betterrailwaysystem$previousHorizontalSpeed, railState);
        double velocityX = minecart.getVelocity().x;
        double velocityY = minecart.getVelocity().y;
        double velocityZ = minecart.getVelocity().z;
        double currentSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (currentSpeed <= 0.0) {
            if (adjustedSpeed == 0.0) {
                minecart.setVelocity(0.0, velocityY, 0.0);
            }
            return;
        }

        if (adjustedSpeed == 0.0) {
            minecart.setVelocity(0.0, velocityY, 0.0);
            return;
        }

        double scale = adjustedSpeed / currentSpeed;
        minecart.setVelocity(velocityX * scale, velocityY, velocityZ * scale);
    }

    @Unique
    private double betterrailwaysystem$getAdjustedTargetSpeed(double baseSpeed, BlockState railState) {
        double currentMaxSpeed = betterrailwaysystem$getCurrentMaxSpeedPerTick();
        if (!(railState.getBlock() instanceof PoweredRailBlock)) {
            return Math.min(baseSpeed, currentMaxSpeed);
        }

        if (railState.get(PoweredRailBlock.POWERED)) {
            return Math.min(currentMaxSpeed, baseSpeed + BetterRailwaySystem.config().accelerationPerTick());
        }
        if (betterrailwaysystem$stopRailBoostTicksRemaining > 0) {
            return Math.min(currentMaxSpeed, Math.max(baseSpeed, Math.max(betterrailwaysystem$stopRailReleaseSpeed, 0.24)));
        }

        return Math.max(0.0, baseSpeed - BetterRailwaySystem.config().decelerationPerTick());
    }

    @Unique
    private double betterrailwaysystem$getCurrentMaxSpeedPerTick() {
        double configMax = BetterRailwaySystem.config().maxSpeedPerTick();
        if (betterrailwaysystem$activeSpeedLimitBps <= 0.0) {
            return configMax;
        }
        return Math.min(configMax, betterrailwaysystem$activeSpeedLimitBps / 20.0);
    }

    @Unique
    private void betterrailwaysystem$applySafeFollowing(AbstractMinecartEntity minecart) {
        if (betterrailwaysystem$waitingAtStopRail) {
            return;
        }
        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        double currentSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        double safeDistance = BetterRailwaySystem.config().safeFollowingDistance;
        double directionX = currentSpeed > 0.0001 ? velocityX / currentSpeed : betterrailwaysystem$lastTravelDirectionX;
        double directionZ = currentSpeed > 0.0001 ? velocityZ / currentSpeed : betterrailwaysystem$lastTravelDirectionZ;
        if (directionX == 0.0 && directionZ == 0.0) {
            directionX = betterrailwaysystem$guessTravelDirectionX(minecart);
            directionZ = betterrailwaysystem$guessTravelDirectionZ(minecart);
        }
        int currentRailX = betterrailwaysystem$railPos.getX();
        int currentRailY = betterrailwaysystem$railPos.getY();
        int currentRailZ = betterrailwaysystem$railPos.getZ();
        double scanDistance = safeDistance + Math.max(4.0, currentSpeed * 40.0);
        double bestGap = Double.MAX_VALUE;
        double frontSpeed = 0.0;
        List<AbstractMinecartEntity> minecarts = minecart.getEntityWorld().getEntitiesByClass(
                AbstractMinecartEntity.class,
                new Box(
                        minecart.getX() - scanDistance,
                        minecart.getY() - 1.0,
                        minecart.getZ() - scanDistance,
                        minecart.getX() + scanDistance,
                        minecart.getY() + 1.0,
                        minecart.getZ() + scanDistance
                ),
                other -> other != minecart
        );
        for (AbstractMinecartEntity other : minecarts) {
            if (!betterrailwaysystem$findRailPos(other, betterrailwaysystem$otherRailPos)) {
                continue;
            }
            Vec3d offset = other.getEntityPos().subtract(minecart.getEntityPos());
            double forwardDistance = betterrailwaysystem$getRailForwardDistance(minecart, other, directionX, directionZ, scanDistance);
            if (forwardDistance <= 0.0 || forwardDistance >= bestGap) {
                continue;
            }
            double otherVelocityX = other.getVelocity().x;
            double otherVelocityZ = other.getVelocity().z;
            double otherSpeed = Math.sqrt(otherVelocityX * otherVelocityX + otherVelocityZ * otherVelocityZ);
            if (otherSpeed > 0.0001) {
                double alignment = directionX * (otherVelocityX / otherSpeed) + directionZ * (otherVelocityZ / otherSpeed);
                if (alignment < 0.6) {
                    continue;
                }
            }
            bestGap = forwardDistance;
            frontSpeed = otherSpeed;
        }
        if (bestGap == Double.MAX_VALUE) {
            if (betterrailwaysystem$followRecoveryTicksRemaining > 0) {
                double recoveredSpeed = Math.min(
                        betterrailwaysystem$getCurrentMaxSpeedPerTick(),
                        Math.max(currentSpeed + BetterRailwaySystem.config().accelerationPerTick(), 0.08)
                );
                minecart.setVelocity(directionX * recoveredSpeed, minecart.getVelocity().y, directionZ * recoveredSpeed);
                betterrailwaysystem$followRecoveryTicksRemaining--;
            }
            return;
        }
        if (currentSpeed <= 0.0001) {
            betterrailwaysystem$followRecoveryTicksRemaining = 20;
            return;
        }
        double deceleration = Math.max(BetterRailwaySystem.config().decelerationPerTick(), 0.001);
        double availableGap = Math.max(0.0, bestGap - safeDistance);
        double requiredGap = Math.max(0.0, currentSpeed * currentSpeed - frontSpeed * frontSpeed) / (2.0 * deceleration);
        if (bestGap <= safeDistance || availableGap <= requiredGap) {
            betterrailwaysystem$followRecoveryTicksRemaining = 20;
            minecart.setVelocity(0.0, minecart.getVelocity().y, 0.0);
            return;
        }
        double brakingSpeed = Math.sqrt(Math.max(0.0, 2.0 * deceleration * availableGap));
        double allowedSpeed = Math.min(currentSpeed, Math.max(frontSpeed, brakingSpeed));
        if (allowedSpeed >= currentSpeed) {
            return;
        }
        betterrailwaysystem$followRecoveryTicksRemaining = 20;
        double scale = allowedSpeed / currentSpeed;
        minecart.setVelocity(velocityX * scale, minecart.getVelocity().y, velocityZ * scale);
    }

    @Unique
    private void betterrailwaysystem$handleBounceRestart(AbstractMinecartEntity minecart) {
        if (betterrailwaysystem$bounceRestartTicksRemaining <= 0 || betterrailwaysystem$waitingAtStopRail) {
            return;
        }
        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        double currentSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (currentSpeed > BETTERMINECART_MIN_LAUNCH_SPEED) {
            betterrailwaysystem$bounceRestartTicksRemaining--;
            return;
        }
        double directionX = betterrailwaysystem$lastTravelDirectionX;
        double directionZ = betterrailwaysystem$lastTravelDirectionZ;
        if (directionX == 0.0 && directionZ == 0.0) {
            directionX = betterrailwaysystem$guessTravelDirectionX(minecart);
            directionZ = betterrailwaysystem$guessTravelDirectionZ(minecart);
        }
        if (directionX == 0.0 && directionZ == 0.0) {
            betterrailwaysystem$bounceRestartTicksRemaining--;
            return;
        }
        double restartSpeed = Math.min(
                betterrailwaysystem$getCurrentMaxSpeedPerTick(),
                Math.max(betterrailwaysystem$bounceRestartSpeed, 0.18)
        );
        minecart.setVelocity(directionX * restartSpeed, minecart.getVelocity().y, directionZ * restartSpeed);
        betterrailwaysystem$stopRailBoostDirectionX = directionX;
        betterrailwaysystem$stopRailBoostDirectionZ = directionZ;
        betterrailwaysystem$stopRailBoostTicksRemaining = Math.max(betterrailwaysystem$stopRailBoostTicksRemaining, 8);
        betterrailwaysystem$bounceRestartTicksRemaining = 0;
        betterrailwaysystem$bounceRestartSpeed = 0.0;
    }

    @Unique
    private double betterrailwaysystem$getRailForwardDistance(AbstractMinecartEntity minecart, AbstractMinecartEntity other, double directionX, double directionZ, double maxDistance) {
        Vec3d offset = other.getEntityPos().subtract(minecart.getEntityPos());
        double straightForward = offset.x * directionX + offset.z * directionZ;
        if (betterrailwaysystem$otherRailPos.getX() == betterrailwaysystem$railPos.getX()
                && betterrailwaysystem$otherRailPos.getY() == betterrailwaysystem$railPos.getY()
                && betterrailwaysystem$otherRailPos.getZ() == betterrailwaysystem$railPos.getZ()) {
            return straightForward;
        }
        BlockState currentRailState = minecart.getEntityWorld().getBlockState(betterrailwaysystem$railPos);
        Direction moveDirection = betterrailwaysystem$getRailTravelDirection(currentRailState, directionX, directionZ);
        if (moveDirection == null) {
            return straightForward;
        }
        BlockPos currentRailPos = betterrailwaysystem$railPos.toImmutable();
        double traveled = 0.0;
        int maxSteps = Math.max(4, MathHelper.ceil(maxDistance) + 4);
        for (int step = 0; step < maxSteps; step++) {
            BlockPos nextRailPos = betterrailwaysystem$getNextRailPos(currentRailPos, currentRailState, moveDirection);
            if (nextRailPos == null) {
                return -1.0;
            }
            traveled += Math.sqrt(currentRailPos.getSquaredDistance(nextRailPos));
            if (traveled > maxDistance + 2.0) {
                return -1.0;
            }
            if (nextRailPos.getX() == betterrailwaysystem$otherRailPos.getX()
                    && nextRailPos.getY() == betterrailwaysystem$otherRailPos.getY()
                    && nextRailPos.getZ() == betterrailwaysystem$otherRailPos.getZ()) {
                return traveled;
            }
            BlockState nextRailState = betterrailwaysystem$getRailStateAt(minecart, nextRailPos);
            if (nextRailState == null) {
                return -1.0;
            }
            Direction nextDirection = betterrailwaysystem$getNextRailTravelDirection(nextRailState, moveDirection);
            if (nextDirection == null) {
                return -1.0;
            }
            currentRailPos = nextRailPos;
            currentRailState = nextRailState;
            moveDirection = nextDirection;
        }
        return -1.0;
    }

    @Unique
    private Direction betterrailwaysystem$getRailTravelDirection(BlockState railState, double directionX, double directionZ) {
        Direction[] connections = betterrailwaysystem$getRailConnections(railState);
        if (connections == null) {
            return null;
        }
        Direction bestDirection = null;
        double bestAlignment = -Double.MAX_VALUE;
        for (Direction connection : connections) {
            double alignment = connection.getOffsetX() * directionX + connection.getOffsetZ() * directionZ;
            if (alignment > bestAlignment) {
                bestAlignment = alignment;
                bestDirection = connection;
            }
        }
        return bestDirection;
    }

    @Unique
    private Direction betterrailwaysystem$getNextRailTravelDirection(BlockState railState, Direction previousMoveDirection) {
        Direction[] connections = betterrailwaysystem$getRailConnections(railState);
        if (connections == null) {
            return null;
        }
        Direction incomingDirection = previousMoveDirection.getOpposite();
        for (Direction connection : connections) {
            if (connection != incomingDirection) {
                return connection;
            }
        }
        return previousMoveDirection;
    }

    @Unique
    private Direction[] betterrailwaysystem$getRailConnections(BlockState railState) {
        if (!(railState.getBlock() instanceof AbstractRailBlock railBlock)) {
            return null;
        }
        RailShape railShape = railState.get(railBlock.getShapeProperty());
        return switch (railShape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> new Direction[]{Direction.EAST, Direction.WEST};
            case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> new Direction[]{Direction.NORTH, Direction.SOUTH};
            case NORTH_EAST -> new Direction[]{Direction.NORTH, Direction.EAST};
            case NORTH_WEST -> new Direction[]{Direction.NORTH, Direction.WEST};
            case SOUTH_EAST -> new Direction[]{Direction.SOUTH, Direction.EAST};
            case SOUTH_WEST -> new Direction[]{Direction.SOUTH, Direction.WEST};
        };
    }

    @Unique
    private BlockPos betterrailwaysystem$getNextRailPos(BlockPos currentRailPos, BlockState railState, Direction moveDirection) {
        if (!(railState.getBlock() instanceof AbstractRailBlock railBlock)) {
            return null;
        }
        RailShape railShape = railState.get(railBlock.getShapeProperty());
        return switch (railShape) {
            case ASCENDING_EAST -> moveDirection == Direction.EAST ? currentRailPos.east().up() : currentRailPos.west();
            case ASCENDING_WEST -> moveDirection == Direction.WEST ? currentRailPos.west().up() : currentRailPos.east();
            case ASCENDING_NORTH -> moveDirection == Direction.NORTH ? currentRailPos.north().up() : currentRailPos.south();
            case ASCENDING_SOUTH -> moveDirection == Direction.SOUTH ? currentRailPos.south().up() : currentRailPos.north();
            default -> currentRailPos.offset(moveDirection);
        };
    }

    @Unique
    private BlockState betterrailwaysystem$getRailStateAt(AbstractMinecartEntity minecart, BlockPos railPos) {
        BlockState state = minecart.getEntityWorld().getBlockState(railPos);
        if (state.isIn(BlockTags.RAILS) || state.getBlock() instanceof AbstractRailBlock) {
            return state;
        }
        BlockState belowState = minecart.getEntityWorld().getBlockState(railPos.down());
        if (belowState.isIn(BlockTags.RAILS) || belowState.getBlock() instanceof AbstractRailBlock) {
            return belowState;
        }
        return null;
    }

    @Unique
    private void betterrailwaysystem$updateAscendingLaunchState(AbstractMinecartEntity minecart, BlockState railState) {
        betterrailwaysystem$launchTicksRemaining = 0;
        Direction uphillDirection = betterrailwaysystem$getAscendingDirection(railState);
        if (uphillDirection == null) {
            betterrailwaysystem$ascendingLaunchArmed = false;
            return;
        }

        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        double horizontalSpeed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (horizontalSpeed < BETTERMINECART_MIN_LAUNCH_SPEED) {
            betterrailwaysystem$ascendingLaunchArmed = false;
            return;
        }

        double directionX = velocityX / horizontalSpeed;
        double directionZ = velocityZ / horizontalSpeed;
        double alignment = directionX * uphillDirection.getOffsetX() + directionZ * uphillDirection.getOffsetZ();
        if (alignment <= 0.25) {
            betterrailwaysystem$ascendingLaunchArmed = false;
            return;
        }

        betterrailwaysystem$ascendingLaunchArmed = true;
        betterrailwaysystem$launchDirectionX = directionX;
        betterrailwaysystem$launchDirectionZ = directionZ;
    }

    @Unique
    private Direction betterrailwaysystem$getAscendingDirection(BlockState railState) {
        if (!(railState.getBlock() instanceof AbstractRailBlock railBlock)) {
            return null;
        }

        RailShape railShape = railState.get(railBlock.getShapeProperty());
        return switch (railShape) {
            case ASCENDING_EAST -> Direction.EAST;
            case ASCENDING_WEST -> Direction.WEST;
            case ASCENDING_NORTH -> Direction.NORTH;
            case ASCENDING_SOUTH -> Direction.SOUTH;
            default -> null;
        };
    }

    @Unique
    private int betterrailwaysystem$computeLaunchTicks(double horizontalSpeed) {
        double speedRatio = horizontalSpeed / BETTERMINECART_VANILLA_MAX_RAIL_SPEED;
        return MathHelper.clamp(MathHelper.ceil(speedRatio * 3.0), 1, BETTERMINECART_MAX_LAUNCH_TICKS);
    }

    @Unique
    private void betterrailwaysystem$triggerBaliseIfNeeded(AbstractMinecartEntity minecart) {
        RailwayBaliseBlockEntity blockEntity = betterrailwaysystem$getTriggeredBlockEntity(minecart, RailwayBaliseBlockEntity.class);
        if (blockEntity == null) {
            betterrailwaysystem$lastBalisePos = Long.MIN_VALUE;
            return;
        }

        if (!betterrailwaysystem$matchesBaliseTriggerDirection(minecart, blockEntity)) {
            return;
        }

        long currentPos = blockEntity.getPos().asLong();
        if (currentPos == betterrailwaysystem$lastBalisePos) {
            return;
        }
        betterrailwaysystem$lastBalisePos = currentPos;

        switch (blockEntity.getMode()) {
            case ARRIVAL -> {
                if (!blockEntity.getCurrentStation().isBlank()) {
                    betterrailwaysystem$currentStation = blockEntity.getCurrentStation();
                }
                if (!blockEntity.getNextStation().isBlank()) {
                    betterrailwaysystem$nextStation = blockEntity.getNextStation();
                }
                if (blockEntity.shouldUpdateBossBar()) {
                    betterrailwaysystem$updateBossBarText();
                }
                betterrailwaysystem$sendBaliseAnnouncement(minecart, blockEntity, defaultText(blockEntity.getTitleText(), "本站"), defaultText(blockEntity.getSubtitleText(), betterrailwaysystem$currentStation));
            }
            case DEPARTURE -> {
                if (!blockEntity.getNextStation().isBlank()) {
                    betterrailwaysystem$nextStation = blockEntity.getNextStation();
                }
                if (!blockEntity.getCurrentStation().isBlank()) {
                    betterrailwaysystem$currentStation = blockEntity.getCurrentStation();
                }
                if (blockEntity.shouldUpdateBossBar()) {
                    betterrailwaysystem$updateBossBarText();
                }
                betterrailwaysystem$sendBaliseAnnouncement(minecart, blockEntity, defaultText(blockEntity.getTitleText(), "下一站"), defaultText(blockEntity.getSubtitleText(), betterrailwaysystem$nextStation));
            }
            case ANNOUNCEMENT -> {
                if (!blockEntity.getCurrentStation().isBlank()) {
                    betterrailwaysystem$currentStation = blockEntity.getCurrentStation();
                }
                if (!blockEntity.getNextStation().isBlank()) {
                    betterrailwaysystem$nextStation = blockEntity.getNextStation();
                }
                if (blockEntity.shouldUpdateBossBar()) {
                    betterrailwaysystem$updateBossBarText();
                }
                betterrailwaysystem$sendBaliseAnnouncement(
                        minecart,
                        blockEntity,
                        defaultText(blockEntity.getTitleText(), "提示"),
                        defaultText(blockEntity.getSubtitleText(), blockEntity.getCurrentStation().isBlank() ? blockEntity.getNextStation() : blockEntity.getCurrentStation())
                );
            }
            case SPEED_LIMIT_START -> {
                betterrailwaysystem$activeSpeedLimitBps = blockEntity.getSpeedLimitBps();
                betterrailwaysystem$sendBaliseAnnouncement(minecart, blockEntity, defaultText(blockEntity.getTitleText(), "限速开始"), defaultText(blockEntity.getSubtitleText(), MathHelper.floor(blockEntity.getSpeedLimitBps()) + " bps"));
            }
            case SPEED_LIMIT_END -> {
                betterrailwaysystem$activeSpeedLimitBps = -1.0;
                betterrailwaysystem$sendBaliseAnnouncement(minecart, blockEntity, defaultText(blockEntity.getTitleText(), "限速结束"), defaultText(blockEntity.getSubtitleText(), "恢复默认速度"));
            }
        }
    }

    @Unique
    private boolean betterrailwaysystem$matchesBaliseTriggerDirection(AbstractMinecartEntity minecart, RailwayBaliseBlockEntity blockEntity) {
        String configuredDirection = blockEntity.getTriggerDirection();
        if (configuredDirection == null || configuredDirection.isBlank()) {
            return true;
        }
        TrainSpawnDirection triggerDirection = TrainSpawnDirection.fromString(configuredDirection);
        if (triggerDirection.isLegacyRelative()) {
            return true;
        }
        BlockState railState = betterrailwaysystem$findRailState(minecart);
        if (railState == null) {
            return true;
        }
        Direction travelDirection = betterrailwaysystem$getRailTravelDirection(
                railState,
                betterrailwaysystem$guessTravelDirectionX(minecart),
                betterrailwaysystem$guessTravelDirectionZ(minecart)
        );
        if (travelDirection == null) {
            return true;
        }
        return switch (triggerDirection) {
            case NORTH -> travelDirection == Direction.NORTH;
            case SOUTH -> travelDirection == Direction.SOUTH;
            case WEST -> travelDirection == Direction.WEST;
            case EAST -> travelDirection == Direction.EAST;
            default -> true;
        };
    }

    @Unique
    private void betterrailwaysystem$sendBaliseAnnouncement(AbstractMinecartEntity minecart, RailwayBaliseBlockEntity blockEntity, String title, String subtitle) {
        List<Entity> passengers = minecart.getPassengerList();
        for (int index = 0; index < passengers.size(); index++) {
            Entity passenger = passengers.get(index);
            if (passenger instanceof ServerPlayerEntity serverPlayer) {
                BetterRailwaySystemNetworking.sendAnnouncement(serverPlayer, title, subtitle, blockEntity);
            }
        }
    }

    @Unique
    private void betterrailwaysystem$updateBossBarText() {
        if (betterrailwaysystem$currentStation.isBlank() && betterrailwaysystem$nextStation.isBlank()) {
            betterrailwaysystem$clearBossBar();
            return;
        }
        LineThemeColor lineTheme = LineThemeColor.fromString(betterrailwaysystem$lineThemeColor);
        if (betterrailwaysystem$bossBar == null) {
            betterrailwaysystem$bossBar = new ServerBossBar(Text.empty(), lineTheme.bossBarColor(), BossBar.Style.PROGRESS);
            betterrailwaysystem$bossBar.setPercent(1.0F);
        }
        String current = betterrailwaysystem$currentStation.isBlank() ? "-" : betterrailwaysystem$currentStation;
        String next = betterrailwaysystem$nextStation.isBlank() ? "-" : betterrailwaysystem$nextStation;
        betterrailwaysystem$bossBar.setColor(lineTheme.bossBarColor());
        betterrailwaysystem$bossBar.setName(Text.translatable("text.betterrailwaysystem.bossbar", current, next).styled(style -> style.withColor(lineTheme.rgb())));
        betterrailwaysystem$bossBar.setVisible(true);
    }

    @Unique
    private void betterrailwaysystem$syncBossBar(AbstractMinecartEntity minecart) {
        if (betterrailwaysystem$bossBar == null) {
            return;
        }
        List<ServerPlayerEntity> currentPlayers = new ArrayList<>();
        for (Entity passenger : minecart.getPassengerList()) {
            if (passenger instanceof ServerPlayerEntity serverPlayer) {
                currentPlayers.add(serverPlayer);
                if (!betterrailwaysystem$bossBar.getPlayers().contains(serverPlayer)) {
                    betterrailwaysystem$bossBar.addPlayer(serverPlayer);
                }
            }
        }
        for (ServerPlayerEntity player : new ArrayList<>(betterrailwaysystem$bossBar.getPlayers())) {
            if (!currentPlayers.contains(player)) {
                betterrailwaysystem$bossBar.removePlayer(player);
            }
        }
        if (currentPlayers.isEmpty()) {
            betterrailwaysystem$bossBar.setVisible(false);
        }
    }

    @Unique
    private void betterrailwaysystem$clearBossBar() {
        if (betterrailwaysystem$bossBar != null) {
            betterrailwaysystem$bossBar.clearPlayers();
            betterrailwaysystem$bossBar = null;
        }
    }

    @Unique
    private void betterrailwaysystem$updateStopRailTarget(AbstractMinecartEntity minecart) {
        if (betterrailwaysystem$waitingAtStopRail) {
            return;
        }
        StopRailBlockEntity stopRail = betterrailwaysystem$getCurrentStopRail(minecart);
        if (stopRail == null) {
            betterrailwaysystem$pendingStopRailPos = null;
            if (betterrailwaysystem$lastReleasedStopRailPos != null) {
                betterrailwaysystem$lastReleasedStopRailPos = null;
                betterrailwaysystem$stopRailReleaseCooldownTicks = 0;
            }
            return;
        }
        if (betterrailwaysystem$shouldIgnoreStopRail(minecart, stopRail.getPos())) {
            betterrailwaysystem$pendingStopRailPos = null;
            return;
        }
        betterrailwaysystem$setPendingStopRail(stopRail.getPos(), stopRail.getDwellSeconds() * 20, stopRail.getWaitMode());
    }

    @Unique
    private void betterrailwaysystem$applyStopRailBraking(AbstractMinecartEntity minecart) {
        if (betterrailwaysystem$waitingAtStopRail) {
            return;
        }
        if (betterrailwaysystem$pendingStopRailPos == null) {
            return;
        }
        if (!(minecart.getEntityWorld().getBlockEntity(betterrailwaysystem$pendingStopRailPos) instanceof StopRailBlockEntity stopRail)) {
            betterrailwaysystem$clearPendingStopRail();
            return;
        }
        StopRailBlockEntity currentStopRail = betterrailwaysystem$getCurrentStopRail(minecart);
        if (currentStopRail == null || !currentStopRail.getPos().equals(betterrailwaysystem$pendingStopRailPos)) {
            betterrailwaysystem$pendingStopRailPos = null;
            return;
        }
        minecart.setVelocity(0.0, minecart.getVelocity().y, 0.0);
        betterrailwaysystem$waitingAtStopRail = true;
        betterrailwaysystem$stopWaitMode = stopRail.getWaitMode();
        betterrailwaysystem$stopDwellTicksRemaining = stopRail.getDwellSeconds() * 20;
        betterrailwaysystem$stopRailReleaseSpeed = Math.min(
                betterrailwaysystem$getCurrentMaxSpeedPerTick(),
                Math.max(betterrailwaysystem$lastTravelSpeed, 0.24)
        );
    }

    @Unique
    private void betterrailwaysystem$handleStopRailWait(AbstractMinecartEntity minecart) {
        if (!betterrailwaysystem$waitingAtStopRail) {
            return;
        }
        minecart.setVelocity(0.0, minecart.getVelocity().y, 0.0);
        if (betterrailwaysystem$pendingStopRailPos == null) {
            betterrailwaysystem$waitingAtStopRail = false;
            return;
        }
        StopRailBlockEntity stopRail = minecart.getEntityWorld().getBlockEntity(betterrailwaysystem$pendingStopRailPos) instanceof StopRailBlockEntity current ? current : null;
        if (stopRail == null) {
            betterrailwaysystem$releaseFromStopRail(minecart);
            return;
        }

        switch (betterrailwaysystem$stopWaitMode) {
            case IMMEDIATE -> betterrailwaysystem$releaseFromStopRail(minecart);
            case TIMER -> {
                if (betterrailwaysystem$stopDwellTicksRemaining > 0) {
                    betterrailwaysystem$stopDwellTicksRemaining--;
                } else {
                    betterrailwaysystem$releaseFromStopRail(minecart);
                }
            }
            case REDSTONE -> {
                if (minecart.getEntityWorld().isReceivingRedstonePower(betterrailwaysystem$pendingStopRailPos)) {
                    betterrailwaysystem$releaseFromStopRail(minecart);
                }
            }
        }
    }

    @Unique
    private void betterrailwaysystem$releaseFromStopRail(AbstractMinecartEntity minecart) {
        betterrailwaysystem$waitingAtStopRail = false;
        betterrailwaysystem$stopDwellTicksRemaining = 0;
        betterrailwaysystem$lastReleasedStopRailPos = betterrailwaysystem$pendingStopRailPos;
        betterrailwaysystem$stopRailReleaseCooldownTicks = 80;
        betterrailwaysystem$pendingStopRailPos = null;
        double directionX = betterrailwaysystem$lastTravelDirectionX;
        double directionZ = betterrailwaysystem$lastTravelDirectionZ;
        if (directionX == 0.0 && directionZ == 0.0) {
            directionX = betterrailwaysystem$guessTravelDirectionX(minecart);
            directionZ = betterrailwaysystem$guessTravelDirectionZ(minecart);
        }
        double launchSpeed = Math.min(
                betterrailwaysystem$getCurrentMaxSpeedPerTick(),
                Math.max(betterrailwaysystem$stopRailReleaseSpeed, 0.24)
        );
        if (directionX != 0.0 || directionZ != 0.0) {
            minecart.setVelocity(directionX * launchSpeed, minecart.getVelocity().y, directionZ * launchSpeed);
            betterrailwaysystem$stopRailBoostDirectionX = directionX;
            betterrailwaysystem$stopRailBoostDirectionZ = directionZ;
            betterrailwaysystem$stopRailBoostTicksRemaining = 20;
        }
        betterrailwaysystem$stopRailReleaseSpeed = 0.0;
    }

    @Unique
    private boolean betterrailwaysystem$shouldIgnoreStopRail(AbstractMinecartEntity minecart, BlockPos pos) {
        if (betterrailwaysystem$lastReleasedStopRailPos == null || !betterrailwaysystem$lastReleasedStopRailPos.equals(pos)) {
            return false;
        }
        return betterrailwaysystem$stopRailReleaseCooldownTicks > 0 || betterrailwaysystem$getCurrentStopRail(minecart) != null;
    }

    @Unique
    private void betterrailwaysystem$collectIfNeeded(AbstractMinecartEntity minecart, ServerWorld serverWorld) {
        TrainCollectorBlockEntity collector = betterrailwaysystem$getTriggeredBlockEntity(minecart, TrainCollectorBlockEntity.class);
        if (collector == null) {
            return;
        }
        betterrailwaysystem$clearBossBar();
        betterrailwaysystem$clearForcedChunks(serverWorld);
        minecart.discard();
    }

    @Unique
    private void betterrailwaysystem$recordCircularLineIfNeeded(AbstractMinecartEntity minecart, ServerWorld serverWorld) {
        if (betterrailwaysystem$circularLineRecorded) {
            return;
        }
        betterrailwaysystem$circularLineRecorded = true;
    }

    @Unique
    private void betterrailwaysystem$handleIdleRemoval(AbstractMinecartEntity minecart) {
        if (minecart.hasPassengers()) {
            betterrailwaysystem$idleTicks = 0;
            return;
        }
        betterrailwaysystem$idleTicks++;
        if (betterrailwaysystem$idleTicks >= BetterRailwaySystem.config().unattendedDespawnSeconds * 20) {
            if (minecart.getEntityWorld() instanceof ServerWorld serverWorld) {
                betterrailwaysystem$clearForcedChunks(serverWorld);
            }
            betterrailwaysystem$clearBossBar();
            minecart.discard();
        }
    }

    @Unique
    private void betterrailwaysystem$syncForcedChunks(AbstractMinecartEntity minecart, ServerWorld world) {
        ChunkPos chunkPos = minecart.getChunkPos();
        if (chunkPos.x == betterrailwaysystem$forcedChunkX && chunkPos.z == betterrailwaysystem$forcedChunkZ) {
            return;
        }
        betterrailwaysystem$clearForcedChunks(world);
        betterrailwaysystem$forcedChunkX = chunkPos.x;
        betterrailwaysystem$forcedChunkZ = chunkPos.z;
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                world.setChunkForced(chunkPos.x + xOffset, chunkPos.z + zOffset, true);
            }
        }
    }

    @Unique
    private void betterrailwaysystem$clearForcedChunks(ServerWorld world) {
        if (betterrailwaysystem$forcedChunkX == Integer.MIN_VALUE) {
            return;
        }
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                world.setChunkForced(betterrailwaysystem$forcedChunkX + xOffset, betterrailwaysystem$forcedChunkZ + zOffset, false);
            }
        }
        betterrailwaysystem$forcedChunkX = Integer.MIN_VALUE;
        betterrailwaysystem$forcedChunkZ = Integer.MIN_VALUE;
    }

    @Unique
    private <T extends BlockEntity> T betterrailwaysystem$getTriggeredBlockEntity(AbstractMinecartEntity minecart, Class<T> type) {
        BlockState railState = betterrailwaysystem$findRailState(minecart);
        if (railState == null) {
            return null;
        }
        int baseX = betterrailwaysystem$railPos.getX();
        int baseY = betterrailwaysystem$railPos.getY();
        int baseZ = betterrailwaysystem$railPos.getZ();
        int[] yOffsets = new int[]{-1, 0, 1};
        for (int index = 0; index < yOffsets.length; index++) {
            BlockEntity blockEntity = minecart.getEntityWorld().getBlockEntity(betterrailwaysystem$searchPos.set(baseX, baseY + yOffsets[index], baseZ));
            if (type.isInstance(blockEntity)) {
                return type.cast(blockEntity);
            }
        }
        return null;
    }

    @Unique
    private StopRailBlockEntity betterrailwaysystem$getCurrentStopRail(AbstractMinecartEntity minecart) {
        BlockState railState = betterrailwaysystem$findRailState(minecart);
        if (railState == null) {
            return null;
        }
        BlockEntity blockEntity = minecart.getEntityWorld().getBlockEntity(betterrailwaysystem$searchPos.set(
                betterrailwaysystem$railPos.getX(),
                betterrailwaysystem$railPos.getY() - 1,
                betterrailwaysystem$railPos.getZ()
        ));
        return blockEntity instanceof StopRailBlockEntity stopRail ? stopRail : null;
    }

    @Unique
    private boolean betterrailwaysystem$findRailPos(AbstractMinecartEntity minecart, BlockPos.Mutable target) {
        BlockPos blockPos = minecart.getBlockPos();
        for (int index = 0; index < BETTERMINECART_RAIL_SEARCH_OFFSETS.length; index++) {
            int[] offset = BETTERMINECART_RAIL_SEARCH_OFFSETS[index];
            target.set(
                    blockPos.getX() + offset[0],
                    blockPos.getY() + offset[1],
                    blockPos.getZ() + offset[2]
            );
            BlockState state = minecart.getEntityWorld().getBlockState(target);
            if (state.isIn(BlockTags.RAILS) || state.getBlock() instanceof AbstractRailBlock) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private BlockEntity betterrailwaysystem$getBlockEntityAt(int x, int y, int z, Class<? extends BlockEntity> type) {
        BlockEntity blockEntity = ((AbstractMinecartEntity) (Object) this).getEntityWorld().getBlockEntity(betterrailwaysystem$searchPos.set(x, y, z));
        return type.isInstance(blockEntity) ? blockEntity : null;
    }

    @Unique
    private BlockState betterrailwaysystem$findRailState(AbstractMinecartEntity minecart) {
        BlockPos blockPos = minecart.getBlockPos();
        for (int index = 0; index < BETTERMINECART_RAIL_SEARCH_OFFSETS.length; index++) {
            int[] offset = BETTERMINECART_RAIL_SEARCH_OFFSETS[index];
            BlockState state = minecart.getEntityWorld().getBlockState(betterrailwaysystem$railPos.set(
                    blockPos.getX() + offset[0],
                    blockPos.getY() + offset[1],
                    blockPos.getZ() + offset[2]
            ));
            if (state.isIn(BlockTags.RAILS) || state.getBlock() instanceof AbstractRailBlock) {
                return state;
            }
        }
        return null;
    }

    @Unique
    private String defaultText(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? (fallback == null ? "" : fallback) : preferred;
    }

    @Unique
    private double betterrailwaysystem$guessTravelDirectionX(AbstractMinecartEntity minecart) {
        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        double speed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (speed > 0.0001) {
            return velocityX / speed;
        }
        Direction direction = minecart.getMovementDirection();
        if (direction != null) {
            return direction.getOffsetX();
        }
        return switch (betterrailwaysystem$lineDirection) {
            case WEST, BACKWARD -> -1.0;
            case EAST, FORWARD -> 1.0;
            default -> 0.0;
        };
    }

    @Unique
    private double betterrailwaysystem$guessTravelDirectionZ(AbstractMinecartEntity minecart) {
        double velocityX = minecart.getVelocity().x;
        double velocityZ = minecart.getVelocity().z;
        double speed = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        if (speed > 0.0001) {
            return velocityZ / speed;
        }
        Direction direction = minecart.getMovementDirection();
        if (direction != null) {
            return direction.getOffsetZ();
        }
        return switch (betterrailwaysystem$lineDirection) {
            case NORTH -> -1.0;
            case SOUTH -> 1.0;
            default -> 0.0;
        };
    }

    @Override
    public double betterrailwaysystem$getActiveSpeedLimitBps() {
        return betterrailwaysystem$activeSpeedLimitBps;
    }

    @Override
    public void betterrailwaysystem$setActiveSpeedLimitBps(double value) {
        betterrailwaysystem$activeSpeedLimitBps = value;
    }

    @Override
    public void betterrailwaysystem$clearActiveSpeedLimitBps() {
        betterrailwaysystem$activeSpeedLimitBps = -1.0;
    }

    @Override
    public String betterrailwaysystem$getLineId() {
        return betterrailwaysystem$lineId;
    }

    @Override
    public String betterrailwaysystem$getCityName() {
        return betterrailwaysystem$cityName;
    }

    @Override
    public void betterrailwaysystem$setCityName(String value) {
        betterrailwaysystem$cityName = value == null ? "" : value;
    }

    @Override
    public String betterrailwaysystem$getLineThemeColor() {
        return betterrailwaysystem$lineThemeColor;
    }

    @Override
    public void betterrailwaysystem$setLineThemeColor(String value) {
        betterrailwaysystem$lineThemeColor = LineThemeColor.fromString(value).serializedName();
        betterrailwaysystem$updateBossBarText();
    }

    @Override
    public BlockPos betterrailwaysystem$getOriginSpawnerPos() {
        return betterrailwaysystem$originSpawnerPos;
    }

    @Override
    public void betterrailwaysystem$setOriginSpawnerPos(BlockPos pos) {
        betterrailwaysystem$originSpawnerPos = pos;
        betterrailwaysystem$leftOriginSpawner = false;
        betterrailwaysystem$circularLineRecorded = false;
    }

    @Override
    public boolean betterrailwaysystem$isCircularLine() {
        return betterrailwaysystem$circularLine;
    }

    @Override
    public void betterrailwaysystem$setCircularLine(boolean value) {
        betterrailwaysystem$circularLine = value;
        betterrailwaysystem$leftOriginSpawner = false;
        betterrailwaysystem$circularLineRecorded = false;
    }

    @Override
    public void betterrailwaysystem$setLineId(String value) {
        betterrailwaysystem$lineId = value == null ? "" : value;
    }

    @Override
    public TrainSpawnDirection betterrailwaysystem$getLineDirection() {
        return betterrailwaysystem$lineDirection;
    }

    @Override
    public void betterrailwaysystem$setLineDirection(TrainSpawnDirection value) {
        betterrailwaysystem$lineDirection = value == null ? TrainSpawnDirection.FORWARD : value;
    }

    @Override
    public String betterrailwaysystem$getCurrentStation() {
        return betterrailwaysystem$currentStation;
    }

    @Override
    public void betterrailwaysystem$setCurrentStation(String value) {
        betterrailwaysystem$currentStation = value == null ? "" : value;
        betterrailwaysystem$updateBossBarText();
    }

    @Override
    public String betterrailwaysystem$getNextStation() {
        return betterrailwaysystem$nextStation;
    }

    @Override
    public void betterrailwaysystem$setNextStation(String value) {
        betterrailwaysystem$nextStation = value == null ? "" : value;
        betterrailwaysystem$updateBossBarText();
    }

    @Override
    public List<String> betterrailwaysystem$getVisitedStations() {
        return List.copyOf(betterrailwaysystem$visitedStations);
    }

    @Override
    public List<BlockPos> betterrailwaysystem$getVisitedStationPositions() {
        return List.copyOf(betterrailwaysystem$visitedStationPositions);
    }

    @Override
    public void betterrailwaysystem$clearVisitedStations() {
        betterrailwaysystem$visitedStations.clear();
        betterrailwaysystem$visitedStationPositions.clear();
    }

    @Override
    public void betterrailwaysystem$appendVisitedStation(String stationName) {
        betterrailwaysystem$appendVisitedStation(stationName, BlockPos.ORIGIN);
    }

    @Override
    public void betterrailwaysystem$appendVisitedStation(String stationName, BlockPos pos) {
        if (stationName == null || stationName.isBlank()) {
            return;
        }
        if (betterrailwaysystem$visitedStations.isEmpty() || !betterrailwaysystem$visitedStations.get(betterrailwaysystem$visitedStations.size() - 1).equals(stationName)) {
            betterrailwaysystem$visitedStations.add(stationName);
            betterrailwaysystem$visitedStationPositions.add(pos == null ? BlockPos.ORIGIN : pos);
        }
    }

    @Override
    public BlockPos betterrailwaysystem$getPendingStopRailPos() {
        return betterrailwaysystem$pendingStopRailPos;
    }

    @Override
    public void betterrailwaysystem$setPendingStopRail(BlockPos pos, int dwellTicks, StopRailWaitMode waitMode) {
        betterrailwaysystem$pendingStopRailPos = pos;
        betterrailwaysystem$stopDwellTicksRemaining = Math.max(0, dwellTicks);
        betterrailwaysystem$stopWaitMode = waitMode == null ? StopRailWaitMode.TIMER : waitMode;
    }

    @Override
    public void betterrailwaysystem$clearPendingStopRail() {
        betterrailwaysystem$pendingStopRailPos = null;
        betterrailwaysystem$stopDwellTicksRemaining = 0;
        betterrailwaysystem$waitingAtStopRail = false;
    }

    @Override
    public int betterrailwaysystem$getStopDwellTicksRemaining() {
        return betterrailwaysystem$stopDwellTicksRemaining;
    }

    @Override
    public void betterrailwaysystem$setStopDwellTicksRemaining(int ticks) {
        betterrailwaysystem$stopDwellTicksRemaining = Math.max(0, ticks);
    }

    @Override
    public StopRailWaitMode betterrailwaysystem$getStopWaitMode() {
        return betterrailwaysystem$stopWaitMode;
    }

    @Override
    public boolean betterrailwaysystem$isWaitingAtStopRail() {
        return betterrailwaysystem$waitingAtStopRail;
    }

    @Override
    public void betterrailwaysystem$setWaitingAtStopRail(boolean value) {
        betterrailwaysystem$waitingAtStopRail = value;
    }
}
