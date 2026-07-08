package org.dcstudio.minecart;

import net.minecraft.entity.boss.BossBar;

// 定义线路主题色，供发车器、BossBar 和线路图复用。
public enum LineThemeColor {
    RED("red", 0xE35D5B, BossBar.Color.RED),
    ORANGE("orange", 0xF08B3E, BossBar.Color.YELLOW),
    YELLOW("yellow", 0xE0BE42, BossBar.Color.YELLOW),
    GREEN("green", 0x58B368, BossBar.Color.GREEN),
    CYAN("cyan", 0x3FB8AF, BossBar.Color.BLUE),
    BLUE("blue", 0x3C78D8, BossBar.Color.BLUE),
    PURPLE("purple", 0x8E6AC8, BossBar.Color.PURPLE),
    PINK("pink", 0xD96AA7, BossBar.Color.PINK),
    WHITE("white", 0xF4F4F4, BossBar.Color.WHITE);

    private final String serializedName;
    private final int rgb;
    private final BossBar.Color bossBarColor;

    LineThemeColor(String serializedName, int rgb, BossBar.Color bossBarColor) {
        this.serializedName = serializedName;
        this.rgb = rgb;
        this.bossBarColor = bossBarColor;
    }

    public String serializedName() {
        return serializedName;
    }

    public int rgb() {
        return rgb;
    }

    public BossBar.Color bossBarColor() {
        return bossBarColor;
    }

    public static LineThemeColor fromString(String value) {
        for (LineThemeColor color : values()) {
            if (color.serializedName.equalsIgnoreCase(value)) {
                return color;
            }
        }
        return BLUE;
    }
}
