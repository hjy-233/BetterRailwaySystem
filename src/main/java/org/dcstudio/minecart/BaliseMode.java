package org.dcstudio.minecart;

// 定义铁路信标的事件类型。
public enum BaliseMode {
    ARRIVAL("arrival"),
    DEPARTURE("departure"),
    ANNOUNCEMENT("announcement"),
    SPEED_LIMIT_START("speed_limit_start"),
    SPEED_LIMIT_END("speed_limit_end");

    private final String serializedName;

    BaliseMode(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public static BaliseMode fromString(String value) {
        for (BaliseMode mode : values()) {
            if (mode.serializedName.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return ARRIVAL;
    }
}
