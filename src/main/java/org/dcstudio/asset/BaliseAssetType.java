package org.dcstudio.asset;

import net.minecraft.text.Text;

import java.util.Locale;

// 定义 balise 素材类型及其文件约束。
public enum BaliseAssetType {
    IMAGE("image", ".png", "screen.betterrailwaysystem.image_library"),
    SOUND("sound", ".ogg", "screen.betterrailwaysystem.sound_library");

    private final String serializedName;
    private final String extension;
    private final String titleKey;

    BaliseAssetType(String serializedName, String extension, String titleKey) {
        this.serializedName = serializedName;
        this.extension = extension;
        this.titleKey = titleKey;
    }

    public String serializedName() {
        return serializedName;
    }

    public String extension() {
        return extension;
    }

    public Text dialogTitle() {
        return Text.translatable(titleKey);
    }

    public boolean isAllowed(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(extension);
    }

    public static BaliseAssetType fromString(String value) {
        if (value != null) {
            for (BaliseAssetType assetType : values()) {
                if (assetType.serializedName.equalsIgnoreCase(value)) {
                    return assetType;
                }
            }
        }
        return IMAGE;
    }
}
