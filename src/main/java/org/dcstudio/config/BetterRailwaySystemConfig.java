package org.dcstudio.config;

// 集中保存 Better Railway System 的可调参数。
public final class BetterRailwaySystemConfig {
    public static final String DEFAULT_DEBUG_TOGGLE_KEY = "key.keyboard.unknown";

    public double maxSpeed = 8.0;
    public double acceleration = 0.15;
    public double deceleration = 0.20;
    public double safeFollowingDistance = 5.0;
    public int maxPassengers = 24;
    public int stopRailApproachDistance = 30;
    public int unattendedDespawnSeconds = 180;
    public String debugToggleKey = DEFAULT_DEBUG_TOGGLE_KEY;

    public double maxSpeedPerTick() {
        return clampPositive(maxSpeed) / 20.0;
    }

    public double accelerationPerTick() {
        return clampPositive(acceleration) / 20.0;
    }

    public double decelerationPerTick() {
        return clampPositive(deceleration) / 20.0;
    }

    public void sanitize() {
        maxSpeed = clampPositive(maxSpeed);
        acceleration = clampPositive(acceleration);
        deceleration = clampPositive(deceleration);
        safeFollowingDistance = clampPositive(safeFollowingDistance);
        maxPassengers = Math.max(1, maxPassengers);
        stopRailApproachDistance = Math.max(1, stopRailApproachDistance);
        unattendedDespawnSeconds = Math.max(1, unattendedDespawnSeconds);
        if (debugToggleKey == null || debugToggleKey.isBlank()) {
            debugToggleKey = DEFAULT_DEBUG_TOGGLE_KEY;
        }
    }

    public BetterRailwaySystemConfig copy() {
        BetterRailwaySystemConfig copy = new BetterRailwaySystemConfig();
        copy.maxSpeed = maxSpeed;
        copy.acceleration = acceleration;
        copy.deceleration = deceleration;
        copy.safeFollowingDistance = safeFollowingDistance;
        copy.maxPassengers = maxPassengers;
        copy.stopRailApproachDistance = stopRailApproachDistance;
        copy.unattendedDespawnSeconds = unattendedDespawnSeconds;
        copy.debugToggleKey = debugToggleKey;
        return copy;
    }

    private double clampPositive(double value) {
        return value > 0.0 ? value : 0.01;
    }
}
