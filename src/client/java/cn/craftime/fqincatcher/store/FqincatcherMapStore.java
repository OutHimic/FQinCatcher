package cn.craftime.fqincatcher.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class FqincatcherMapStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "fqincatcher_maps.json";
    private static FqincatcherMapStore INSTANCE;

    private final Path filePath;
    private FqincatcherMapState state;

    private FqincatcherMapStore(Path filePath, FqincatcherMapState state) {
        this.filePath = filePath;
        this.state = state;
    }

    public static synchronized FqincatcherMapStore get() {
        if (INSTANCE != null) {
            return INSTANCE;
        }
        Path filePath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
        FqincatcherMapState state = new FqincatcherMapState();
        if (Files.isRegularFile(filePath)) {
            try (Reader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                FqincatcherMapState loaded = GSON.fromJson(reader, FqincatcherMapState.class);
                if (loaded != null && loaded.maps != null) {
                    state = loaded;
                }
            } catch (Exception ignored) {
                state = new FqincatcherMapState();
            }
        }
        INSTANCE = new FqincatcherMapStore(filePath, state);
        INSTANCE.state.maps.sort(Comparator.comparingLong(m -> -m.updatedAtEpochMs));
        return INSTANCE;
    }

    public synchronized List<FqincatcherMapProfile> list() {
        return List.copyOf(this.state.maps);
    }

    public synchronized Optional<FqincatcherMapProfile> selected() {
        String selectedId = this.state.selectedMapId;
        if (selectedId == null || selectedId.isBlank()) {
            return Optional.empty();
        }
        return this.state.maps.stream().filter(m -> Objects.equals(m.id, selectedId)).findFirst();
    }

    public synchronized void select(String mapId) {
        this.state.selectedMapId = mapId;
        touch(mapId);
        save();
    }

    public synchronized FqincatcherMapProfile create(String name) {
        long now = Instant.now().toEpochMilli();
        FqincatcherMapProfile map = new FqincatcherMapProfile();
        map.id = UUID.randomUUID().toString();
        map.name = name == null || name.isBlank() ? "新地图" : name;
        map.worldDirName = "FQinCatcher_" + now + "_" + map.id.substring(0, 8);
        map.createdAtEpochMs = now;
        map.updatedAtEpochMs = now;
        this.state.maps.add(0, map);
        this.state.selectedMapId = map.id;
        save();
        return map;
    }

    public synchronized boolean delete(String mapId) {
        boolean removed = this.state.maps.removeIf(m -> Objects.equals(m.id, mapId));
        if (removed && Objects.equals(this.state.selectedMapId, mapId)) {
            this.state.selectedMapId = this.state.maps.isEmpty() ? null : this.state.maps.get(0).id;
        }
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized boolean deleteWorldFolderIfExists(String mapId) {
        Path worldRoot = null;
        for (FqincatcherMapProfile map : this.state.maps) {
            if (!Objects.equals(map.id, mapId)) continue;
            worldRoot = resolveSavesDir().resolve(map.worldDirName);
            break;
        }
        if (worldRoot == null || !Files.exists(worldRoot)) {
            return false;
        }
        try (var stream = Files.walk(worldRoot)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public synchronized void setSeed(String mapId, Long seed) {
        for (FqincatcherMapProfile map : this.state.maps) {
            if (!Objects.equals(map.id, mapId)) continue;
            map.seed = seed;
            touch(mapId);
            save();
            return;
        }
    }

    public synchronized void setServerAddress(String mapId, String serverAddress) {
        for (FqincatcherMapProfile map : this.state.maps) {
            if (!Objects.equals(map.id, mapId)) continue;
            map.serverAddress = serverAddress;
            touch(mapId);
            save();
            return;
        }
    }

    public synchronized Path resolveWorldRoot(String mapId) {
        for (FqincatcherMapProfile map : this.state.maps) {
            if (!Objects.equals(map.id, mapId)) continue;
            return resolveSavesDir().resolve(map.worldDirName);
        }
        throw new IllegalArgumentException("地图不存在: " + mapId);
    }

    private void touch(String mapId) {
        long now = Instant.now().toEpochMilli();
        for (FqincatcherMapProfile map : this.state.maps) {
            if (!Objects.equals(map.id, mapId)) continue;
            map.updatedAtEpochMs = now;
            break;
        }
        this.state.maps.sort(Comparator.comparingLong(m -> -m.updatedAtEpochMs));
    }

    private void save() {
        try {
            Files.createDirectories(this.filePath.getParent());
            try (Writer writer = Files.newBufferedWriter(this.filePath, StandardCharsets.UTF_8)) {
                GSON.toJson(this.state, writer);
            }
        } catch (IOException ignored) {
        }
    }

    private static Path resolveSavesDir() {
        MinecraftClient client = MinecraftClient.getInstance();
        Path gameDir = client == null ? FabricLoader.getInstance().getGameDir() : client.runDirectory.toPath();
        return gameDir.resolve("saves");
    }
}

