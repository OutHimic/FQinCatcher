package cn.craftime.fqincatcher.copy;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.PalettedContainer;
import net.minecraft.world.chunk.ReadableContainer;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Map;

public final class FqincatcherChunkNbtWriter {
    private static final Codec<ReadableContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.createReadableContainerCodec(
            Block.STATE_IDS,
            BlockState.CODEC,
            PalettedContainer.PaletteProvider.BLOCK_STATE,
            Blocks.AIR.getDefaultState()
    );

    private FqincatcherChunkNbtWriter() {
    }

    public static NbtCompound write(WorldChunk chunk, RegistryWrapper.WrapperLookup registries, Registry<Biome> biomeRegistry) {
        ChunkPos pos = chunk.getPos();

        NbtCompound root = NbtHelper.putDataVersion(new NbtCompound());
        root.putInt("xPos", pos.x);
        root.putInt("yPos", chunk.getBottomY() >> 4);
        root.putInt("zPos", pos.z);
        root.putLong("LastUpdate", 0L);
        root.putLong("InhabitedTime", 0L);

        ChunkStatus status = chunk.getStatus();
        root.putString("Status", status == null ? "full" : status.getId());

        NbtList sectionsList = new NbtList();
        ChunkSection[] sections = chunk.getSectionArray();
        int bottomSectionY = chunk.getBottomY() >> 4;

        Codec<ReadableContainer<RegistryEntry<Biome>>> biomeCodec = createBiomeCodec(biomeRegistry);

        for (int i = 0; i < sections.length; i++) {
            ChunkSection section = sections[i];
            if (section == null || section.isEmpty()) {
                continue;
            }

            int sectionY = bottomSectionY + i;
            NbtCompound sectionTag = new NbtCompound();
            sectionTag.putByte("Y", (byte) sectionY);

            NbtElement blockStates = BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getBlockStateContainer()).getOrThrow();
            NbtElement biomes = biomeCodec.encodeStart(NbtOps.INSTANCE, section.getBiomeContainer()).getOrThrow();
            sectionTag.put("block_states", blockStates);
            sectionTag.put("biomes", biomes);

            sectionsList.add(sectionTag);
        }
        root.put("sections", sectionsList);
        root.putBoolean("isLightOn", true);

        NbtCompound heightmaps = new NbtCompound();
        for (Map.Entry<Heightmap.Type, Heightmap> e : chunk.getHeightmaps()) {
            heightmaps.putLongArray(e.getKey().getName(), e.getValue().asLongArray());
        }
        root.put("Heightmaps", heightmaps);

        NbtList blockEntities = new NbtList();
        for (BlockPos bePos : chunk.getBlockEntities().keySet()) {
            NbtCompound be = chunk.getPackedBlockEntityNbt(bePos, registries);
            if (be != null) {
                blockEntities.add(be);
            }
        }
        root.put("block_entities", blockEntities);

        return root;
    }

    private static Codec<ReadableContainer<RegistryEntry<Biome>>> createBiomeCodec(Registry<Biome> biomeRegistry) {
        RegistryEntry<Biome> fallback = biomeRegistry.getEntry(BiomeKeys.PLAINS).orElseThrow();
        return PalettedContainer.createReadableContainerCodec(
                biomeRegistry.getIndexedEntries(),
                biomeRegistry.getEntryCodec(),
                PalettedContainer.PaletteProvider.BIOME,
                fallback
        );
    }
}

