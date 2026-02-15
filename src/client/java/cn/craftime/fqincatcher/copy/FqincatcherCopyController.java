package cn.craftime.fqincatcher.copy;

import cn.craftime.fqincatcher.store.FqincatcherMapProfile;
import cn.craftime.fqincatcher.store.FqincatcherMapStore;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.storage.RegionFile;
import net.minecraft.world.storage.StorageKey;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class FqincatcherCopyController {
    private static boolean active = false;
    private static String activeMapId;
    private static Path activeWorldRoot;
    private static String activeWorldName;

    private static final LongSet dirty = new LongOpenHashSet();
    private static final LongSet pending = new LongOpenHashSet();
    private static final RegionFileCache regionCache = new RegionFileCache();
    private static final RegionFileCache entityCache = new RegionFileCache();

    private FqincatcherCopyController() {
    }

    public static boolean isActive() {
        return active;
    }

    public static void start() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }

        var store = FqincatcherMapStore.get();
        var selected = store.selected();
        if (selected.isEmpty()) {
            chat("请先选中一个地图");
            return;
        }

        FqincatcherMapProfile map = selected.get();
        activeMapId = map.id;
        activeWorldRoot = store.resolveWorldRoot(map.id);
        activeWorldName = map.worldDirName;

        ServerInfo server = client.getCurrentServerEntry();
        if (server != null) {
            store.setServerAddress(map.id, server.address);
        }

        try {
            Files.createDirectories(activeWorldRoot);
        } catch (IOException ignored) {
        }

        active = true;
        chat("开始复制：只会保存你已加载到客户端的区块");
    }

    public static void pause() {
        if (!active) {
            chat("当前未在复制");
            return;
        }
        flushAll();
        writeEntitiesSnapshot();
        regionCache.closeAll();
        entityCache.closeAll();
        active = false;
        chat("已暂停复制并写入磁盘");
    }

    public static void tick() {
        if (!active) {
            return;
        }
        int budget = 2;
        while (budget-- > 0) {
            long key = pollDirty();
            if (key == Long.MIN_VALUE) {
                return;
            }
            int x = ChunkPos.getPackedX(key);
            int z = ChunkPos.getPackedZ(key);
            writeChunk(new ChunkPos(x, z));
        }
    }

    public static void markDirty(ChunkPos pos) {
        if (!active || pos == null) {
            return;
        }
        long key = pos.toLong();
        if (dirty.add(key)) {
            pending.add(key);
        }
    }

    public static void markDirty(ChunkSectionPos sectionPos) {
        if (sectionPos == null) return;
        markDirty(new ChunkPos(sectionPos.getSectionX(), sectionPos.getSectionZ()));
    }

    private static long pollDirty() {
        if (pending.isEmpty()) {
            return Long.MIN_VALUE;
        }
        long key = pending.iterator().nextLong();
        pending.remove(key);
        dirty.remove(key);
        return key;
    }

    private static void flushAll() {
        int safety = 20000;
        while (!pending.isEmpty() && safety-- > 0) {
            long key = pollDirty();
            if (key == Long.MIN_VALUE) {
                break;
            }
            int x = ChunkPos.getPackedX(key);
            int z = ChunkPos.getPackedZ(key);
            writeChunk(new ChunkPos(x, z));
        }
    }

    private static void writeChunk(ChunkPos pos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client == null || client.world == null || pos == null) {
            return;
        }

        WorldChunk chunk;
        try {
            chunk = client.world.getChunkManager().getChunk(pos.x, pos.z, ChunkStatus.FULL, false);
        } catch (Throwable t) {
            return;
        }
        if (chunk == null) {
            return;
        }

        Path dimDir = DimensionType.getSaveDirectory(client.world.getRegistryKey(), activeWorldRoot);
        Path regionDir = dimDir.resolve("region");
        Path externalDir = regionDir.resolve("c");
        try {
            Files.createDirectories(externalDir);
        } catch (IOException ignored) {
        }

        RegistryWrapper.WrapperLookup registries = client.world.getRegistryManager();
        Registry<?> biomeRegistry = client.world.getRegistryManager().get(RegistryKeys.BIOME);
        @SuppressWarnings("unchecked")
        Registry<net.minecraft.world.biome.Biome> biomeRegistryTyped = (Registry<net.minecraft.world.biome.Biome>) biomeRegistry;

        NbtCompound chunkNbt = FqincatcherChunkNbtWriter.write(chunk, registries, biomeRegistryTyped);
        StorageKey key = new StorageKey(activeWorldName, client.world.getRegistryKey(), "region");
        writeRegionNbt(regionCache, key, regionDir, externalDir, pos, chunkNbt);
    }

    private static void writeEntitiesSnapshot() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!active || client == null || client.world == null) {
            return;
        }

        Path dimDir = DimensionType.getSaveDirectory(client.world.getRegistryKey(), activeWorldRoot);
        Path entitiesDir = dimDir.resolve("entities");
        Path externalDir = entitiesDir.resolve("c");
        try {
            Files.createDirectories(externalDir);
        } catch (IOException ignored) {
        }

        Map<Long, NbtList> byChunk = new HashMap<>();
        for (var entity : client.world.getEntities()) {
            if (entity == null || entity.isPlayer()) {
                continue;
            }
            ChunkPos chunkPos = new ChunkPos(entity.getBlockPos());
            long key = chunkPos.toLong();
            NbtList list = byChunk.computeIfAbsent(key, k -> new NbtList());
            NbtCompound nbt = new NbtCompound();
            entity.writeNbt(nbt);
            list.add(nbt);
        }

        for (var entry : byChunk.entrySet()) {
            long keyLong = entry.getKey();
            ChunkPos pos = new ChunkPos(ChunkPos.getPackedX(keyLong), ChunkPos.getPackedZ(keyLong));

            NbtCompound root = NbtHelper.putDataVersion(new NbtCompound());
            root.putIntArray("Position", new int[]{pos.x, pos.z});
            root.put("Entities", entry.getValue());

            StorageKey key = new StorageKey(activeWorldName, client.world.getRegistryKey(), "entities");
            writeRegionNbt(entityCache, key, entitiesDir, externalDir, pos, root);
        }
    }

    private static void writeRegionNbt(RegionFileCache cache, StorageKey key, Path dir, Path externalDir, ChunkPos pos, NbtCompound nbt) {
        try {
            RegionFile region = cache.get(key, dir, externalDir, pos);
            try (DataOutputStream out = region.getChunkOutputStream(pos)) {
                NbtIo.writeCompound(nbt, out);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void chat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal("[FQinCatcher] " + message), false);
    }

    private static final class RegionFileCache {
        private final Map<Path, RegionFile> open = new HashMap<>();

        RegionFile get(StorageKey key, Path dir, Path externalDir, ChunkPos pos) throws IOException {
            int regionX = pos.getRegionX();
            int regionZ = pos.getRegionZ();
            Path file = dir.resolve("r." + regionX + "." + regionZ + ".mca");
            RegionFile existing = open.get(file);
            if (existing != null) {
                return existing;
            }
            Files.createDirectories(dir);
            Files.createDirectories(externalDir);
            RegionFile created = new RegionFile(key, file, externalDir, false);
            open.put(file, created);
            return created;
        }

        void closeAll() {
            for (RegionFile file : open.values()) {
                try {
                    file.close();
                } catch (IOException ignored) {
                }
            }
            open.clear();
        }
    }
}

