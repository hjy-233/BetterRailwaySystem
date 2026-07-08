package org.dcstudio.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

// 对接 Mod Menu 的配置入口。
public final class BetterRailwaySystemModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BetterRailwaySystemConfigScreen::create;
    }
}
