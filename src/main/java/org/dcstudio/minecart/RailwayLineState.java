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

    private final Map<String, Map<String, SavedLine>> cities = new LinkedHashMap<>();

    public static RailwayLineState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    private static RailwayLineState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RailwayLineState state = new RailwayLineState();
        NbtCompound citiesNbt = nbt.getCompound("Cities");
        for (String cityName : citiesNbt.getKeys()) {
            NbtCompound cityLinesNbt = citiesNbt.getCompound(cityName);
            Map<String, SavedLine> cityLines = new LinkedHashMap<>();
            for (String lineId : cityLinesNbt.getKeys()) {
                NbtList list;
                String lineThemeColor = LineThemeColor.BLUE.serializedName();
                if (cityLinesNbt.contains(lineId, NbtElement.COMPOUND_TYPE)) {
                    NbtCompound lineNbt = cityLinesNbt.getCompound(lineId);
                    list = lineNbt.getList("Stations", NbtElement.STRING_TYPE);
                    if (lineNbt.contains("LineThemeColor")) {
                        lineThemeColor = lineNbt.getString("LineThemeColor");
                    }
                } else {
                    list = cityLinesNbt.getList(lineId, NbtElement.STRING_TYPE);
                }
                List<StationEntry> stations = new ArrayList<>(list.size());
                NbtList positions = cityLinesNbt.contains(lineId, NbtElement.COMPOUND_TYPE)
                        ? cityLinesNbt.getCompound(lineId).getList("StationPositions", NbtElement.LONG_TYPE)
                        : new NbtList();
                for (int index = 0; index < list.size(); index++) {
                    String station = list.getString(index);
                    if (!station.isBlank()) {
                        BlockPos pos = BlockPos.ORIGIN;
                        if (index < positions.size() && positions.get(index) instanceof net.minecraft.nbt.AbstractNbtNumber nbtNumber) {
                            pos = BlockPos.fromLong(nbtNumber.longValue());
                        }
                        stations.add(new StationEntry(station, pos));
                    }
                }
                if (!stations.isEmpty()) {
                    cityLines.put(lineId, new SavedLine(LineThemeColor.fromString(lineThemeColor).serializedName(), stations));
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
        updateLine(cityName, lineId, stations, positions, lineThemeColor);
    }

    public void updateLine(String cityName, String lineId, List<String> stations, List<BlockPos> stationPositions, String lineThemeColor) {
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
        String normalizedThemeColor = LineThemeColor.fromString(lineThemeColor).serializedName();
        Map<String, SavedLine> cityLines = cities.computeIfAbsent(cityName, ignored -> new LinkedHashMap<>());
        SavedLine previous = cityLines.get(lineId);
        if (previous != null && normalized.equals(previous.stations()) && normalizedThemeColor.equals(previous.lineThemeColor())) {
            return;
        }
        cityLines.put(lineId, new SavedLine(normalizedThemeColor, normalized));
        markDirty();
    }

    public List<String> getLine(String cityName, String lineId) {
        Map<String, SavedLine> cityLines = cities.get(cityName);
        if (cityLines == null) {
            return List.of();
        }
        SavedLine stations = cityLines.get(lineId);
        if (stations == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>(stations.stations().size());
        for (StationEntry station : stations.stations()) {
            names.add(station.stationName());
        }
        return List.copyOf(names);
    }

    public List<StationEntry> getLineStations(String cityName, String lineId) {
        Map<String, SavedLine> cityLines = cities.get(cityName);
        if (cityLines == null || !cityLines.containsKey(lineId)) {
            return List.of();
        }
        return List.copyOf(cityLines.get(lineId).stations());
    }

    public String getLineThemeColor(String cityName, String lineId) {
        Map<String, SavedLine> cityLines = cities.get(cityName);
        if (cityLines == null || !cityLines.containsKey(lineId)) {
            return LineThemeColor.BLUE.serializedName();
        }
        return cityLines.get(lineId).lineThemeColor();
    }

    public Map<String, Map<String, SavedLine>> getAllLines() {
        Map<String, Map<String, SavedLine>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, SavedLine>> cityEntry : cities.entrySet()) {
            Map<String, SavedLine> cityCopy = new LinkedHashMap<>();
            for (Map.Entry<String, SavedLine> lineEntry : cityEntry.getValue().entrySet()) {
                cityCopy.put(lineEntry.getKey(), new SavedLine(lineEntry.getValue().lineThemeColor(), List.copyOf(lineEntry.getValue().stations())));
            }
            copy.put(cityEntry.getKey(), cityCopy);
        }
        return copy;
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
        Map<String, SavedLine> cityLines = cities.get(cityName);
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
        for (Map.Entry<String, Map<String, SavedLine>> cityEntry : cities.entrySet()) {
            NbtCompound cityLinesNbt = new NbtCompound();
            for (Map.Entry<String, SavedLine> lineEntry : cityEntry.getValue().entrySet()) {
                NbtCompound lineNbt = new NbtCompound();
                NbtList list = new NbtList();
                NbtList positions = new NbtList();
                for (StationEntry station : lineEntry.getValue().stations()) {
                    list.add(NbtString.of(station.stationName()));
                    positions.add(net.minecraft.nbt.NbtLong.of(station.pos().asLong()));
                }
                lineNbt.put("Stations", list);
                lineNbt.put("StationPositions", positions);
                lineNbt.putString("LineThemeColor", lineEntry.getValue().lineThemeColor());
                cityLinesNbt.put(lineEntry.getKey(), lineNbt);
            }
            citiesNbt.put(cityEntry.getKey(), cityLinesNbt);
        }
        nbt.put("Cities", citiesNbt);
        return nbt;
    }

    public record SavedLine(String lineThemeColor, List<StationEntry> stations) {
    }

    public record StationEntry(String stationName, BlockPos pos) {
    }
}
