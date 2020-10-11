package me.geek.tom.fallfest.structure;

import me.geek.tom.fallfest.FallFest;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolBasedGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.StructurePoolFeatureConfig;

import java.util.Arrays;
import java.util.List;

public class DungeonStructure extends StructureFeature<DefaultFeatureConfig> {

    public static final List<Biome.Category> BLACKLIST = Arrays.asList(
            Biome.Category.NETHER,
            Biome.Category.BEACH,
            Biome.Category.ICY,
            Biome.Category.MUSHROOM,
            Biome.Category.OCEAN,
            Biome.Category.THEEND,
            Biome.Category.SWAMP,
            Biome.Category.RIVER,
            Biome.Category.NONE
    );

    private static final Identifier START_POOL = new Identifier(FallFest.MOD_ID, "dungeon_start");
    protected final double probability;

    public DungeonStructure(double probability) {
        super(DefaultFeatureConfig.CODEC);
        this.probability = probability;
    }

    @Override
    protected boolean shouldStartAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long worldSeed, ChunkRandom random, int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, DefaultFeatureConfig config) {
        StructureConfig structureConfig = chunkGenerator.getStructuresConfig().getForType(this);
        if(structureConfig != null) {
            random.setCarverSeed(worldSeed + structureConfig.getSalt(), chunkX, chunkZ);
            double d = (probability / 10000D);
            return random.nextDouble() < d;
        }
        return false;
    }

    @Override
    public StructureStartFactory<DefaultFeatureConfig> getStructureStartFactory() {
        return Start::new;
    }

    public static class Start extends StructureStart<DefaultFeatureConfig> {
        public Start(StructureFeature<DefaultFeatureConfig> feature, int chunkX, int chunkZ, BlockBox box, int references, long seed) {
            super(feature, chunkX, chunkZ, box, references, seed);
        }

        @Override
        public void init(DynamicRegistryManager registryManager, ChunkGenerator chunkGenerator, StructureManager manager, int chunkX, int chunkZ, Biome biome, DefaultFeatureConfig config) {
            BlockPos pos = new BlockPos(chunkX * 16, 0, chunkZ * 16);
            // Thanks TelepathicGrunt for helping with this part!
            StructurePoolBasedGenerator.method_30419(registryManager,
                    new StructurePoolFeatureConfig(() -> getStartPool(registryManager), 10),
                    PoolStructurePiece::new, chunkGenerator, manager, pos, this.children, random, true, true);
            this.setBoundingBoxFromChildren();
        }

        private StructurePool getStartPool(DynamicRegistryManager manager) {
            return manager.get(Registry.TEMPLATE_POOL_WORLDGEN).get(START_POOL);
        }
    }
}
