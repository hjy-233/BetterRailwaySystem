package org.dcstudio.minecart;

import net.minecraft.util.math.BlockPos;

import java.util.List;

// 暴露矿车运行时增强状态，供方块和网络逻辑复用。
public interface BetterRailwaySystemAccess {
    double betterrailwaysystem$getActiveSpeedLimitBps();

    void betterrailwaysystem$setActiveSpeedLimitBps(double value);

    void betterrailwaysystem$clearActiveSpeedLimitBps();

    String betterrailwaysystem$getLineId();

    void betterrailwaysystem$setLineId(String value);

    String betterrailwaysystem$getCityName();

    void betterrailwaysystem$setCityName(String value);

    String betterrailwaysystem$getLineThemeColor();

    void betterrailwaysystem$setLineThemeColor(String value);

    BlockPos betterrailwaysystem$getOriginSpawnerPos();

    void betterrailwaysystem$setOriginSpawnerPos(BlockPos pos);

    boolean betterrailwaysystem$isCircularLine();

    void betterrailwaysystem$setCircularLine(boolean value);

    TrainSpawnDirection betterrailwaysystem$getLineDirection();

    void betterrailwaysystem$setLineDirection(TrainSpawnDirection value);

    String betterrailwaysystem$getCurrentStation();

    void betterrailwaysystem$setCurrentStation(String value);

    String betterrailwaysystem$getNextStation();

    void betterrailwaysystem$setNextStation(String value);

    List<String> betterrailwaysystem$getVisitedStations();

    List<BlockPos> betterrailwaysystem$getVisitedStationPositions();

    void betterrailwaysystem$clearVisitedStations();

    void betterrailwaysystem$appendVisitedStation(String stationName);

    void betterrailwaysystem$appendVisitedStation(String stationName, BlockPos pos);

    BlockPos betterrailwaysystem$getPendingStopRailPos();

    void betterrailwaysystem$setPendingStopRail(BlockPos pos, int dwellTicks, StopRailWaitMode waitMode);

    void betterrailwaysystem$clearPendingStopRail();

    int betterrailwaysystem$getStopDwellTicksRemaining();

    void betterrailwaysystem$setStopDwellTicksRemaining(int ticks);

    StopRailWaitMode betterrailwaysystem$getStopWaitMode();

    boolean betterrailwaysystem$isWaitingAtStopRail();

    boolean betterrailwaysystem$isDepartingFromStopRail();

    void betterrailwaysystem$setWaitingAtStopRail(boolean value);
}
