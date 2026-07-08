package org.dcstudio.minecart;

// 定义停车轨的等待模式。
public enum StopRailWaitMode {
    IMMEDIATE("immediate"),
    TIMER("timer"),
    REDSTONE("redstone");

    private final String serializedName;

    StopRailWaitMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static StopRailWaitMode fromString(String value) {
        for (StopRailWaitMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return TIMER;
    }
}
