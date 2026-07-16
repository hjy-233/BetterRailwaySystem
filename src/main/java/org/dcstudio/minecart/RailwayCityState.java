package org.dcstudio.minecart;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import org.dcstudio.config.BetterRailwaySystemDataSchema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 记录世界中已创建的城市名。
public final class RailwayCityState extends PersistentState {
    public static final String STATE_ID = "betterrailwaysystem_cities";
    public static final Type<RailwayCityState> TYPE = new Type<>(
            RailwayCityState::new,
            RailwayCityState::fromNbt,
            DataFixTypes.SAVED_DATA_MAP_DATA
    );

    private final Set<String> cities = new LinkedHashSet<>();

    public static RailwayCityState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    private static RailwayCityState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RailwayCityState state = new RailwayCityState();
        NbtList list = nbt.getList("Cities", NbtElement.STRING_TYPE);
        for (int index = 0; index < list.size(); index++) {
            String city = list.getString(index);
            if (!city.isBlank()) {
                state.cities.add(city);
            }
        }
        if (!nbt.contains(BetterRailwaySystemDataSchema.VERSION_KEY)) {
            state.markDirty();
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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt(BetterRailwaySystemDataSchema.VERSION_KEY, BetterRailwaySystemDataSchema.currentVersion());
        NbtList list = new NbtList();
        for (String city : cities) {
            list.add(NbtString.of(city));
        }
        nbt.put("Cities", list);
        return nbt;
    }
}
