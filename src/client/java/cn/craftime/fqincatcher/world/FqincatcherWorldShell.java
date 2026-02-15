package cn.craftime.fqincatcher.world;

import cn.craftime.fqincatcher.store.FqincatcherMapProfile;
import cn.craftime.fqincatcher.store.FqincatcherMapStore;
import com.mojang.serialization.Lifecycle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.text.Text;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.LevelProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class FqincatcherWorldShell {
    private FqincatcherWorldShell() {
    }

    public static void generateForSelected() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        FqincatcherMapStore store = FqincatcherMapStore.get();
        Optional<FqincatcherMapProfile> selected = store.selected();
        if (selected.isEmpty()) {
            chat("请先选中一个地图");
            return;
        }
        FqincatcherMapProfile map = selected.get();
        if (map.seed == null) {
            chat("请先填写并保存服务器种子，再生成地图");
            return;
        }

        Path worldRoot = store.resolveWorldRoot(map.id);
        try {
            Files.createDirectories(worldRoot);
        } catch (IOException ignored) {
        }

        LevelInfo levelInfo = new LevelInfo(
                map.name == null ? "FQinCatcher 世界" : map.name,
                GameMode.SURVIVAL,
                false,
                Difficulty.NORMAL,
                true,
                new GameRules(),
                DataConfiguration.SAFE_MODE
        );
        GeneratorOptions generatorOptions = new GeneratorOptions(map.seed, true, false);
        LevelProperties properties = new LevelProperties(levelInfo, generatorOptions, LevelProperties.SpecialProperty.NONE, Lifecycle.stable());
        properties.setSpawnPos(client.player.getBlockPos(), client.player.getYaw());
        properties.setInitialized(true);

        NbtCompound nbt = properties.cloneWorldNbt(client.world.getRegistryManager(), null);
        try {
            NbtIo.writeCompressed(nbt, worldRoot.resolve("level.dat"));
            chat("已生成本地世界：单人模式打开后会按种子生成地形；如需复制服务器建筑请开始复制");
        } catch (IOException e) {
            chat("生成失败：无法写入 level.dat");
        }
    }

    private static void chat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        client.player.sendMessage(Text.literal("[FQinCatcher] " + message), false);
    }
}

