package org.dcstudio.minecart;

// 定义发车器给矿车绑定的线路方向。
public enum TrainSpawnDirection {
    FORWARD("forward"),
    BACKWARD("backward"),
    NORTH("north"),
    SOUTH("south"),
    WEST("west"),
    EAST("east");

    private final String serializedName;

    TrainSpawnDirection(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public boolean isLegacyRelative() {
        return this == FORWARD || this == BACKWARD;
    }

    public static TrainSpawnDirection fromString(String value) {
        for (TrainSpawnDirection direction : values()) {
            if (direction.serializedName.equalsIgnoreCase(value)) {
                return direction;
            }
        }
        return FORWARD;
    }
}
