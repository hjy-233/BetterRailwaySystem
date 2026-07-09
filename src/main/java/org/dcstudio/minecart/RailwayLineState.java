package org.dcstudio.minecart;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import org.dcstudio.minecart.TrainSpawnDirection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 记录世界中的城市线路图谱。
public final class RailwayLineState extends PersistentState {
    public static final String STATE_ID = "betterrailwaysystem_railway_map";
    public static final Type<RailwayLineState> TYPE = new Type<>(
            RailwayLineState::new,
            RailwayLineState::fromNbt,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final Map<String, Map<String, Map<String, SavedLine>>> cities = new LinkedHashMap<>();

    public static RailwayLineState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    private static RailwayLineState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RailwayLineState state = new RailwayLineState();
        NbtCompound citiesNbt = nbt.getCompound("Cities");
        for (String cityName : citiesNbt.getKeys()) {
            NbtCompound cityLinesNbt = citiesNbt.getCompound(cityName);
            Map<String, Map<String, SavedLine>> cityLines = new LinkedHashMap<>();
            for (String lineId : cityLinesNbt.getKeys()) {
                Map<String, SavedLine> directionLines = new LinkedHashMap<>();
                if (cityLinesNbt.contains(lineId, NbtElement.COMPOUND_TYPE)) {
                    NbtCompound lineNbt = cityLinesNbt.getCompound(lineId);
                    if (lineNbt.contains("Stations", NbtElement.LIST_TYPE)) {
                        SavedLine savedLine = betterrailwaysystem$readSavedLine(lineNbt);
                        if (savedLine != null) {
                            directionLines.put(TrainSpawnDirection.FORWARD.serializedName(), savedLine);
                        }
                    } else {
                        for (String direction : lineNbt.getKeys()) {
                            if (!lineNbt.contains(direction, NbtElement.COMPOUND_TYPE)) {
                                continue;
                            }
                            SavedLine savedLine = betterrailwaysystem$readSavedLine(lineNbt.getCompound(direction));
                            if (savedLine != null) {
                                directionLines.put(TrainSpawnDirection.fromString(direction).serializedName(), savedLine);
                            }
                        }
                    }
                } else if (cityLinesNbt.contains(lineId, NbtElement.LIST_TYPE)) {
                    NbtList list = cityLinesNbt.getList(lineId, NbtElement.STRING_TYPE);
                    SavedLine savedLine = betterrailwaysystem$readLegacyList(list, LineThemeColor.BLUE.serializedName());
                    if (savedLine != null) {
                        directionLines.put(TrainSpawnDirection.FORWARD.serializedName(), savedLine);
                    }
                }
                if (!directionLines.isEmpty()) {
                    cityLines.put(lineId, directionLines);
                }
            }
            if (!cityLines.isEmpty()) {
                state.cities.put(cityName, cityLines);
            }
        }
        return state;
    }

    public void updateLine(String cityName, String lineId, List<String> stations, String lineThemeColor) {
        List<BlockPos> positions = new ArrayList<>(stations.size());
        for (int index = 0; index < stations.size(); index++) {
            positions.add(BlockPos.ORIGIN);
        }
        updateLine(cityName, lineId, TrainSpawnDirection.FORWARD.serializedName(), stations, positions, lineThemeColor);
    }

    public void updateLine(String cityName, String lineId, List<String> stations, List<BlockPos> stationPositions, String lineThemeColor) {
        updateLine(cityName, lineId, TrainSpawnDirection.FORWARD.serializedName(), stations, stationPositions, lineThemeColor);
    }

    public void updateLine(String cityName, String lineId, String direction, List<String> stations, List<BlockPos> stationPositions, String lineThemeColor) {
        if (cityName == null || cityName.isBlank() || lineId == null || lineId.isBlank() || stations.isEmpty()) {
            return;
        }
        List<StationEntry> normalized = new ArrayList<>(stations.size());
        for (int index = 0; index < stations.size(); index++) {
            String station = stations.get(index);
            if (station != null && !station.isBlank()) {
                BlockPos pos = index < stationPositions.size() && stationPositions.get(index) != null ? stationPositions.get(index) : BlockPos.ORIGIN;
                if (normalized.isEmpty() || !normalized.get(normalized.size() - 1).stationName().equals(station)) {
                    normalized.add(new StationEntry(station, pos));
                }
            }
        }
        if (normalized.isEmpty()) {
            return;
        }
        String normalizedDirection = TrainSpawnDirection.fromString(direction).serializedName();
        String normalizedThemeColor = LineThemeColor.fromString(lineThemeColor).serializedName();
        Map<String, Map<String, SavedLine>> cityLines = cities.computeIfAbsent(cityName, ignored -> new LinkedHashMap<>());
        Map<String, SavedLine> directionLines = cityLines.computeIfAbsent(lineId, ignored -> new LinkedHashMap<>());
        SavedLine previous = directionLines.get(normalizedDirection);
        if (previous != null && normalized.equals(previous.stations()) && normalizedThemeColor.equals(previous.lineThemeColor())) {
            return;
        }
        directionLines.put(normalizedDirection, new SavedLine(normalizedThemeColor, normalized));
        markDirty();
    }

    public List<String> getLine(String cityName, String lineId, String direction) {
        SavedLine stations = betterrailwaysystem$getSavedLine(cityName, lineId, direction);
        if (stations == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(stations.stations().size());
        for (StationEntry station : stations.stations()) {
            names.add(station.stationName());
        }
        return List.copyOf(names);
    }

    public List<StationEntry> getLineStations(String cityName, String lineId, String direction) {
        SavedLine savedLine = betterrailwaysystem$getSavedLine(cityName, lineId, direction);
        if (savedLine == null) {
            return List.of();
        }
        return List.copyOf(savedLine.stations());
    }

    public String getLineThemeColor(String cityName, String lineId, String direction) {
        SavedLine savedLine = betterrailwaysystem$getSavedLine(cityName, lineId, direction);
        if (savedLine == null) {
            return LineThemeColor.BLUE.serializedName();
        }
        return savedLine.lineThemeColor();
    }

    public List<SavedLineEntry> getAllLineEntries() {
        List<SavedLineEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Map<String, SavedLine>>> cityEntry : cities.entrySet()) {
            for (Map.Entry<String, Map<String, SavedLine>> lineEntry : cityEntry.getValue().entrySet()) {
                for (Map.Entry<String, SavedLine> directionEntry : lineEntry.getValue().entrySet()) {
                    entries.add(new SavedLineEntry(
                            cityEntry.getKey(),
                            lineEntry.getKey(),
                            directionEntry.getKey(),
                            directionEntry.getValue().lineThemeColor(),
                            List.copyOf(directionEntry.getValue().stations())
                    ));
                }
            }
        }
        return List.copyOf(entries);
    }

    public boolean clearAll() {
        if (cities.isEmpty()) {
            return false;
        }
        cities.clear();
        markDirty();
        return true;
    }

    public boolean clearCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return false;
        }
        if (cities.remove(cityName) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean clearLine(String cityName, String lineId) {
        if (cityName == null || cityName.isBlank() || lineId == null || lineId.isBlank()) {
            return false;
        }
        Map<String, Map<String, SavedLine>> cityLines = cities.get(cityName);
        if (cityLines == null || cityLines.remove(lineId) == null) {
            return false;
        }
        if (cityLines.isEmpty()) {
            cities.remove(cityName);
        }
        markDirty();
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound citiesNbt = new NbtCompound();
        for (Map.Entry<String, Map<String, Map<String, SavedLine>>> cityEntry : cities.entrySet()) {
            NbtCompound cityLinesNbt = new NbtCompound();
            for (Map.Entry<String, Map<String, SavedLine>> lineEntry : cityEntry.getValue().entrySet()) {
                NbtCompound lineDirectionsNbt = new NbtCompound();
                for (Map.Entry<String, SavedLine> directionEntry : lineEntry.getValue().entrySet()) {
                    NbtCompound lineNbt = new NbtCompound();
                    NbtList list = new NbtList();
                    NbtList positions = new NbtList();
                    for (StationEntry station : directionEntry.getValue().stations()) {
                        list.add(NbtString.of(station.stationName()));
                        positions.add(net.minecraft.nbt.NbtLong.of(station.pos().asLong()));
                    }
                    lineNbt.put("Stations", list);
                    lineNbt.put("StationPositions", positions);
                    lineNbt.putString("LineThemeColor", directionEntry.getValue().lineThemeColor());
                    lineDirectionsNbt.put(directionEntry.getKey(), lineNbt);
                }
                cityLinesNbt.put(lineEntry.getKey(), lineDirectionsNbt);
            }
            citiesNbt.put(cityEntry.getKey(), cityLinesNbt);
        }
        nbt.put("Cities", citiesNbt);
        return nbt;
    }

    private static SavedLine betterrailwaysystem$readSavedLine(NbtCompound lineNbt) {
        NbtList list = lineNbt.getList("Stations", NbtElement.STRING_TYPE);
        String lineThemeColor = lineNbt.contains("LineThemeColor")
                ? lineNbt.getString("LineThemeColor")
                : LineThemeColor.BLUE.serializedName();
        NbtList positions = lineNbt.getList("StationPositions", NbtElement.LONG_TYPE);
        List<StationEntry> stations = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            String station = list.getString(index);
            if (station.isBlank()) {
                continue;
            }
            BlockPos pos = BlockPos.ORIGIN;
            if (index < positions.size() && positions.get(index) instanceof net.minecraft.nbt.AbstractNbtNumber nbtNumber) {
                pos = BlockPos.fromLong(nbtNumber.longValue());
            }
            stations.add(new StationEntry(station, pos));
        }
        if (stations.isEmpty()) {
            return null;
        }
        return new SavedLine(LineThemeColor.fromString(lineThemeColor).serializedName(), stations);
    }

    private static SavedLine betterrailwaysystem$readLegacyList(NbtList list, String lineThemeColor) {
        List<StationEntry> stations = new ArrayList<>(list.size());
        for (int index = 0; index < list.size(); index++) {
            String station = list.getString(index);
            if (!station.isBlank()) {
                stations.add(new StationEntry(station, BlockPos.ORIGIN));
            }
        }
        if (stations.isEmpty()) {
            return null;
        }
        return new SavedLine(LineThemeColor.fromString(lineThemeColor).serializedName(), stations);
    }

    private SavedLine betterrailwaysystem$getSavedLine(String cityName, String lineId, String direction) {
        Map<String, Map<String, SavedLine>> cityLines = cities.get(cityName);
        if (cityLines == null) {
            return null;
        }
        Map<String, SavedLine> directionLines = cityLines.get(lineId);
        if (directionLines == null) {
            return null;
        }
        String normalizedDirection = TrainSpawnDirection.fromString(direction).serializedName();
        SavedLine savedLine = directionLines.get(normalizedDirection);
        if (savedLine != null) {
            return savedLine;
        }
        return directionLines.get(TrainSpawnDirection.FORWARD.serializedName());
    }

    public record SavedLine(String lineThemeColor, List<StationEntry> stations) {
    }

    public record SavedLineEntry(
            String cityName,
            String lineId,
            String direction,
            String lineThemeColor,
            List<StationEntry> stations
    ) {
    }

    public record StationEntry(String stationName, BlockPos pos) {
    }
}
