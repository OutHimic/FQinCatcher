package cn.craftime.fqincatcher.seed;

import cn.craftime.fqincatcher.store.FqincatcherMapStore;
import kaptainwutax.seedcrackerX.SeedCracker;
import kaptainwutax.seedcrackerX.config.Config;
import kaptainwutax.seedcrackerX.cracker.DataAddedEvent;
import kaptainwutax.seedcrackerX.cracker.HashedSeedData;
import kaptainwutax.seedcrackerX.finder.FinderQueue;
import kaptainwutax.seedcrackerX.finder.ReloadFinders;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

public final class FqincatcherSeedCrackerController {
    private static boolean initialized = false;
    private static boolean active = false;
    private static long lastHashedSeed = Long.MIN_VALUE;

    private FqincatcherSeedCrackerController() {
    }

    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        try {
            new SeedCracker().onInitialize();
        } catch (Throwable ignored) {
        }
        Config.get().render = Config.RenderType.OFF;
        Config.get().debug = false;
        Config.get().active = false;
        initialized = true;
    }

    public static void start() {
        ensureInitialized();
        active = true;
        Config.get().active = true;
        try {
            SeedCracker.get().reset();
        } catch (Throwable ignored) {
        }
        chat("已启动服务器种子破解：请在服务器中跑图/加载结构，结果会自动保存到当前选中地图");
    }

    public static boolean isActive() {
        return active;
    }

    public static void stop() {
        active = false;
        Config.get().active = false;
    }

    public static void tick() {
        if (!active) {
            return;
        }
        try {
            SeedCracker.get().getDataStorage().tick();
        } catch (Throwable ignored) {
        }
    }

    public static void onGameJoin(long hashedSeed) {
        onDimensionSeedUpdate(hashedSeed, false);
    }

    public static void onPlayerRespawn(long hashedSeed) {
        onDimensionSeedUpdate(hashedSeed, true);
    }

    private static void onDimensionSeedUpdate(long hashedSeed, boolean dimensionChange) {
        if (!active) {
            return;
        }
        if (hashedSeed == lastHashedSeed) {
            return;
        }
        lastHashedSeed = hashedSeed;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return;
        }
        DimensionType dimension = client.world.getDimension();
        try {
            ReloadFinders.reloadHeight(dimension.minY(), dimension.minY() + dimension.logicalHeight());
        } catch (Throwable ignored) {
        }
        try {
            SeedCracker.get().getDataStorage().addHashedSeedData(new HashedSeedData(hashedSeed), DataAddedEvent.POKE_BIOMES);
        } catch (Throwable ignored) {
        }
        if (dimensionChange) {
            chat("已获取服务器哈希种子，继续收集结构线索中…");
        }
    }

    public static void onChunkData(ChunkPos chunkPos) {
        if (!active) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return;
        }
        try {
            FinderQueue.get().onChunkData(client.world, chunkPos);
        } catch (Throwable ignored) {
        }
    }

    public static void onCracked(long seed) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        client.execute(() -> {
            var store = FqincatcherMapStore.get();
            var selected = store.selected();
            if (selected.isPresent()) {
                store.setSeed(selected.get().id, seed);
            }
            chat("已破解到服务器种子: " + seed + "（已保存）");
            stop();
        });
    }

    private static void chat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal("[FQinCatcher] " + message), false);
    }
}

