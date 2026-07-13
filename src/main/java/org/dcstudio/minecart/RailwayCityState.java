package org.dcstudio.minecart;

import com.mojang.serialization.Codec;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 记录世界中已创建的城市名。
public final class RailwayCityState extends PersistentState {
    public static final String STATE_ID = "betterrailwaysystem_cities";
    private static final Codec<RailwayCityState> CODEC = Codec.STRING.listOf()
            .fieldOf("Cities")
            .xmap(RailwayCityState::fromCities, RailwayCityState::getCities)
            .codec();
    public static final PersistentStateType<RailwayCityState> TYPE = new PersistentStateType<>(
            STATE_ID,
            RailwayCityState::new,
            CODEC,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final Set<String> cities = new LinkedHashSet<>();

    public static RailwayCityState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE);
    }

    private static RailwayCityState fromCities(List<String> cities) {
        RailwayCityState state = new RailwayCityState();
        for (String city : cities) {
            if (!city.isBlank()) {
                state.cities.add(city);
            }
        }
        return state;
    }

    public void addCity(String cityName) {
        if (cityName == null || cityName.isBlank()) {
            return;
        }
        if (cities.add(cityName)) {
            markDirty();
        }
    }

    public List<String> getCities() {
        return List.copyOf(cities);
    }
}
