package org.dcstudio.config;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.dcstudio.BetterRailwaySystem;
import org.dcstudio.minecart.BaliseTypeData;
import org.dcstudio.minecart.LineThemeColorData;
import org.dcstudio.minecart.UiOptionData;

// 让 data/betterrailwaysystem 下的数据文件支持 /reload 和数据包覆盖。
public final class BetterRailwaySystemDataReloadListener implements SimpleSynchronousResourceReloadListener {
    @Override
    public Identifier getFabricId() {
        return BetterRailwaySystem.id("data_driven_reload");
    }

    @Override
    public void reload(ResourceManager manager) {
        BaliseTypeData.reload(manager);
        LineThemeColorData.reload(manager);
        UiOptionData.reload(manager);
        BetterRailwaySystemDataSchema.reload(manager);
        BetterRailwaySystem.LOGGER.info("Reloaded BetterRailwaySystem data-driven definitions");
    }
}
