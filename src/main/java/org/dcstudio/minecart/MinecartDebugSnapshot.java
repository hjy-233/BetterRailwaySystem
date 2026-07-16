package org.dcstudio.minecart;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.dcstudio.BetterRailwaySystem;

import java.util.ArrayList;
import java.util.List;

// 保存矿车调试信息，供 HUD 和方块实体记录最近经过的矿车。
public record MinecartDebugSnapshot(
        double speedBps,
        double targetMaxSpeedBps,
        double activeSpeedLimitBps,
        boolean onRail,
        String railType,
        String cityName,
        String lineId,
        String direction,
        String currentStation,
        String nextStation,
        String stopState,
        double safeFollowingTargetDistance,
        double safeFollowingActualDistance
) {
    private static final MinecartDebugSnapshot EMPTY = new MinecartDebugSnapshot(
            0.0,
            0.0,
            -1.0,
            false,
            "-",
            "-",
            "-",
            "-",
            "-",
            "-",
            "-",
            0.0,
            -1.0
    );

    public static MinecartDebugSnapshot empty() {
        return EMPTY;
    }

    public static MinecartDebugSnapshot from(AbstractMinecartEntity minecart) {
        Vec3d velocity = minecart.getVelocity();
        double speedBps = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z) * 20.0;
        double activeLimit = -1.0;
        String city = "-";
        String line = "-";
        String direction = "-";
        String currentStation = "-";
        String nextStation = "-";
        String stopState = "-";

        if (minecart instanceof BetterRailwaySystemAccess access) {
            activeLimit = access.betterrailwaysystem$getActiveSpeedLimitBps();
            city = display(access.betterrailwaysystem$getCityName());
            line = display(access.betterrailwaysystem$getLineId());
            direction = access.betterrailwaysystem$getLineDirection().serializedName();
            currentStation = display(access.betterrailwaysystem$getCurrentStation());
            nextStation = display(access.betterrailwaysystem$getNextStation());
            stopState = stopState(access);
        }

        RailDebug rail = railDebug(minecart.getWorld(), minecart.getBlockPos());
        double safeTarget = BetterRailwaySystem.config().safeFollowingDistance;
        double followingDistance = followingDistance(minecart);
        double targetMaxSpeed = activeLimit > 0.0 ? Math.min(BetterRailwaySystem.config().maxSpeed, activeLimit) : BetterRailwaySystem.config().maxSpeed;
        return new MinecartDebugSnapshot(
                speedBps,
                targetMaxSpeed,
                activeLimit,
                rail.onRail,
                rail.name,
                city,
                line,
                direction,
                currentStation,
                nextStation,
                stopState,
                safeTarget,
                followingDistance
        );
    }

    public static MinecartDebugSnapshot fromNbt(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) {
            return EMPTY;
        }
        return new MinecartDebugSnapshot(
                nbt.getDouble("SpeedBps"),
                nbt.getDouble("TargetMaxSpeedBps"),
                nbt.getDouble("ActiveSpeedLimitBps"),
                nbt.getBoolean("OnRail"),
                display(nbt.getString("RailType")),
                display(nbt.getString("CityName")),
                display(nbt.getString("LineId")),
                display(nbt.getString("Direction")),
                display(nbt.getString("CurrentStation")),
                display(nbt.getString("NextStation")),
                display(nbt.getString("StopState")),
                nbt.getDouble("SafeFollowingTargetDistance"),
                nbt.contains("SafeFollowingActualDistance") ? nbt.getDouble("SafeFollowingActualDistance") : -1.0
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putDouble("SpeedBps", speedBps);
        nbt.putDouble("TargetMaxSpeedBps", targetMaxSpeedBps);
        nbt.putDouble("ActiveSpeedLimitBps", activeSpeedLimitBps);
        nbt.putBoolean("OnRail", onRail);
        nbt.putString("RailType", railType);
        nbt.putString("CityName", cityName);
        nbt.putString("LineId", lineId);
        nbt.putString("Direction", direction);
        nbt.putString("CurrentStation", currentStation);
        nbt.putString("NextStation", nextStation);
        nbt.putString("StopState", stopState);
        nbt.putDouble("SafeFollowingTargetDistance", safeFollowingTargetDistance);
        nbt.putDouble("SafeFollowingActualDistance", safeFollowingActualDistance);
        return nbt;
    }

    public List<String> lines(String prefix) {
        List<String> lines = new ArrayList<>();
        lines.add(prefix + translatable("debug.betterrailwaysystem.speed", format(speedBps)));
        lines.add(prefix + translatable("debug.betterrailwaysystem.target_max_speed", format(targetMaxSpeedBps)));
        lines.add(prefix + translatable("debug.betterrailwaysystem.speed_limit", activeSpeedLimitBps > 0.0 ? format(activeSpeedLimitBps) + " bps" : translatable("debug.betterrailwaysystem.none")));
        lines.add(prefix + translatable("debug.betterrailwaysystem.on_rail", translatableBoolean(onRail)));
        lines.add(prefix + translatable("debug.betterrailwaysystem.rail", railType));
        lines.add(prefix + translatable("debug.betterrailwaysystem.line", cityName, lineId, direction));
        lines.add(prefix + translatable("debug.betterrailwaysystem.station", currentStation, nextStation));
        lines.add(prefix + translatable("debug.betterrailwaysystem.stop_rail", stopState));
        lines.add(prefix + translatable(
                "debug.betterrailwaysystem.following",
                format(safeFollowingTargetDistance),
                safeFollowingActualDistance >= 0.0 ? format(safeFollowingActualDistance) : "-"
        ));
        return lines;
    }

    private static String stopState(BetterRailwaySystemAccess access) {
        if (access.betterrailwaysystem$isWaitingAtStopRail()) {
            return "waiting " + access.betterrailwaysystem$getStopWaitMode().serializedName() + " " + (access.betterrailwaysystem$getStopDwellTicksRemaining() / 20.0) + "s";
        }
        if (access.betterrailwaysystem$isDepartingFromStopRail()) {
            return "departing";
        }
        if (access.betterrailwaysystem$getPendingStopRailPos() != null) {
            return "braking " + access.betterrailwaysystem$getStopWaitMode().serializedName();
        }
        return "running";
    }

    private static RailDebug railDebug(World world, BlockPos pos) {
        BlockPos[] candidates = new BlockPos[]{pos, pos.down(), pos.up()};
        for (BlockPos candidate : candidates) {
            BlockState state = world.getBlockState(candidate);
            if (!state.isIn(BlockTags.RAILS)) {
                continue;
            }
            String name = Registries.BLOCK.getId(state.getBlock()).toString();
            if (state.getBlock() instanceof AbstractRailBlock railBlock) {
                RailShape shape = state.get(railBlock.getShapeProperty());
                name += " " + shape.asString();
            }
            return new RailDebug(true, name);
        }
        return new RailDebug(false, "-");
    }

    private static double followingDistance(AbstractMinecartEntity minecart) {
        Vec3d velocity = minecart.getVelocity();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double directionX = speed > 0.0001 ? velocity.x / speed : fallbackDirectionX(minecart);
        double directionZ = speed > 0.0001 ? velocity.z / speed : fallbackDirectionZ(minecart);
        if (directionX == 0.0 && directionZ == 0.0) {
            return -1.0;
        }

        double maxDistance = Math.max(BetterRailwaySystem.config().safeFollowingDistance + 16.0, 32.0);
        double best = Double.MAX_VALUE;
        for (AbstractMinecartEntity other : minecart.getWorld().getEntitiesByClass(AbstractMinecartEntity.class, new Box(minecart.getBlockPos()).expand(maxDistance), other -> other != minecart)) {
            if (!sameLine(minecart, other)) {
                continue;
            }
            Vec3d delta = other.getPos().subtract(minecart.getPos());
            double along = delta.x * directionX + delta.z * directionZ;
            if (along <= 0.0) {
                continue;
            }
            double lateralX = delta.x - directionX * along;
            double lateralZ = delta.z - directionZ * along;
            if (lateralX * lateralX + lateralZ * lateralZ > 4.0) {
                continue;
            }
            best = Math.min(best, along);
        }
        return best == Double.MAX_VALUE ? -1.0 : best;
    }

    private static boolean sameLine(AbstractMinecartEntity minecart, AbstractMinecartEntity other) {
        if (!(minecart instanceof BetterRailwaySystemAccess first) || !(other instanceof BetterRailwaySystemAccess second)) {
            return true;
        }
        return first.betterrailwaysystem$getCityName().equals(second.betterrailwaysystem$getCityName())
                && first.betterrailwaysystem$getLineId().equals(second.betterrailwaysystem$getLineId())
                && first.betterrailwaysystem$getLineDirection() == second.betterrailwaysystem$getLineDirection();
    }

    private static double fallbackDirectionX(AbstractMinecartEntity minecart) {
        if (minecart instanceof BetterRailwaySystemAccess access) {
            return switch (access.betterrailwaysystem$getLineDirection()) {
                case EAST -> 1.0;
                case WEST -> -1.0;
                default -> 0.0;
            };
        }
        return 0.0;
    }

    private static double fallbackDirectionZ(AbstractMinecartEntity minecart) {
        if (minecart instanceof BetterRailwaySystemAccess access) {
            return switch (access.betterrailwaysystem$getLineDirection()) {
                case SOUTH -> 1.0;
                case NORTH -> -1.0;
                default -> 0.0;
            };
        }
        return 0.0;
    }

    private static String display(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String translatable(String key, Object... args) {
        return Text.translatable(key, args).getString();
    }

    private static String translatableBoolean(boolean value) {
        return translatable(value ? "debug.betterrailwaysystem.boolean.true" : "debug.betterrailwaysystem.boolean.false");
    }

    private record RailDebug(boolean onRail, String name) {
    }
}
