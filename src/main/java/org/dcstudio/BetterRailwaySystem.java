package org.dcstudio;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.resource.ResourceType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.dcstudio.asset.ServerBaliseAssetLibrary;
import org.dcstudio.config.BetterRailwaySystemConfig;
import org.dcstudio.config.BetterRailwaySystemCommands;
import org.dcstudio.config.BetterRailwaySystemConfigManager;
import org.dcstudio.config.BetterRailwaySystemDataReloadListener;
import org.dcstudio.network.BetterRailwaySystemNetworking;
import org.dcstudio.station.RailwayBaliseBlock;
import org.dcstudio.station.RailwayBaliseBlockEntity;
import org.dcstudio.station.StopRailBlock;
import org.dcstudio.station.StopRailBlockEntity;
import org.dcstudio.station.TrainCollectorBlock;
import org.dcstudio.station.TrainCollectorBlockEntity;
import org.dcstudio.station.TrainSpawnerBlock;
import org.dcstudio.station.TrainSpawnerBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetterRailwaySystem implements ModInitializer {
    // 模组总入口，负责注册配置、网络和方块。
    public static final String MOD_ID = "betterrailwaysystem";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Block RAILWAY_BALISE = Registry.register(
            Registries.BLOCK,
            id("railway_balise"),
            new RailwayBaliseBlock(AbstractBlock.Settings.create().strength(1.5F))
    );
    public static final Item RAILWAY_BALISE_ITEM = Registry.register(
            Registries.ITEM,
            id("railway_balise"),
            new BlockItem(RAILWAY_BALISE, new Item.Settings())
    );
    public static final BlockEntityType<RailwayBaliseBlockEntity> RAILWAY_BALISE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("railway_balise"),
            BlockEntityType.Builder.create(RailwayBaliseBlockEntity::new, RAILWAY_BALISE).build(null)
    );
    public static final Block STOP_RAIL = Registry.register(
            Registries.BLOCK,
            id("stop_rail"),
            new StopRailBlock(AbstractBlock.Settings.create().strength(0.7F))
    );
    public static final Item STOP_RAIL_ITEM = Registry.register(
            Registries.ITEM,
            id("stop_rail"),
            new BlockItem(STOP_RAIL, new Item.Settings())
    );
    public static final BlockEntityType<StopRailBlockEntity> STOP_RAIL_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("stop_rail"),
            BlockEntityType.Builder.create(StopRailBlockEntity::new, STOP_RAIL).build(null)
    );
    public static final Block TRAIN_SPAWNER = Registry.register(
            Registries.BLOCK,
            id("train_spawner"),
            new TrainSpawnerBlock(AbstractBlock.Settings.create().strength(2.0F))
    );
    public static final Item TRAIN_SPAWNER_ITEM = Registry.register(
            Registries.ITEM,
            id("train_spawner"),
            new BlockItem(TRAIN_SPAWNER, new Item.Settings())
    );
    public static final BlockEntityType<TrainSpawnerBlockEntity> TRAIN_SPAWNER_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("train_spawner"),
            BlockEntityType.Builder.create(TrainSpawnerBlockEntity::new, TRAIN_SPAWNER).build(null)
    );
    public static final Block TRAIN_COLLECTOR = Registry.register(
            Registries.BLOCK,
            id("train_collector"),
            new TrainCollectorBlock(AbstractBlock.Settings.create().strength(2.0F))
    );
    public static final Item TRAIN_COLLECTOR_ITEM = Registry.register(
            Registries.ITEM,
            id("train_collector"),
            new BlockItem(TRAIN_COLLECTOR, new Item.Settings())
    );
    public static final BlockEntityType<TrainCollectorBlockEntity> TRAIN_COLLECTOR_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            id("train_collector"),
            BlockEntityType.Builder.create(TrainCollectorBlockEntity::new, TRAIN_COLLECTOR).build(null)
    );

    private static BetterRailwaySystemConfig config;
    private static boolean debugMode;

    @Override
    public void onInitialize() {
        config = BetterRailwaySystemConfigManager.load();
        ServerBaliseAssetLibrary.initialize();
        registerItemGroups();
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new BetterRailwaySystemDataReloadListener());
        BetterRailwaySystemCommands.register();
        BetterRailwaySystemNetworking.register();
    }

    private static void registerItemGroups() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
            entries.prepend(TRAIN_COLLECTOR_ITEM);
            entries.prepend(TRAIN_SPAWNER_ITEM);
            entries.prepend(STOP_RAIL_ITEM);
            entries.prepend(RAILWAY_BALISE_ITEM);
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static BetterRailwaySystemConfig config() {
        return config;
    }

    public static void saveConfig() {
        BetterRailwaySystemConfigManager.save(config);
    }

    public static void reloadConfig() {
        config = BetterRailwaySystemConfigManager.load();
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean value) {
        debugMode = value;
    }
}
