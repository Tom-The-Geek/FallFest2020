package me.geek.tom.fallfest;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.geek.tom.fallfest.dump.DumpCommand;
import me.geek.tom.fallfest.mixin.StructuresConfigAccessor;
import me.geek.tom.fallfest.resources.SpawnerProfileManager;
import me.geek.tom.fallfest.structure.DungeonStructure;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.FlatChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FallFest implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public static final String MOD_ID = "ttg_fallfest";
    public static final String MOD_NAME = "FallFest 2020";

    private static final DungeonStructure DUNGEON_STRUCTURE = new DungeonStructure(40);
    public static final ConfiguredStructureFeature<DefaultFeatureConfig, ? extends StructureFeature<DefaultFeatureConfig>> CONFIGURED_DUNGEON =
            DUNGEON_STRUCTURE.configure(DefaultFeatureConfig.INSTANCE);

    public static final Map<StructureFeature<?>, StructureConfig> MOD_STRUCTURES = new HashMap<>();

    public static Identifier modIdentifier(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        log(Level.INFO, "Initializing");

//        RegistryIdDumper.dumpRegistry(
//                FabricLoader.getInstance().getGameDir().resolve("registries").toFile(),
//                BuiltinRegistries.STRUCTURE_POOL
//        );

        LOGGER.info(FlatChunkGenerator.CODEC);
        FabricStructureBuilder.create(modIdentifier("dungeon"), DUNGEON_STRUCTURE)
                .step(GenerationStep.Feature.UNDERGROUND_STRUCTURES)
                .defaultConfig(new StructureConfig(1, 0, 433729))
                .superflatFeature(DefaultFeatureConfig.INSTANCE)
                .register();

        MOD_STRUCTURES.putAll(StructuresConfig.DEFAULT_STRUCTURES);
        MOD_STRUCTURES.keySet().removeIf(key -> !key.getName().contains(MOD_ID));

        MutableRegistry<ConfiguredStructureFeature<?, ?>> registry = (MutableRegistry<ConfiguredStructureFeature<?, ?>>) BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE;

        Registry.register(registry, FallFest.modIdentifier("dungeon"), CONFIGURED_DUNGEON);

        ServerWorldEvents.LOAD.register(this::addStructures);
        Registration.init(); // Trigger classload that runs the static initializer and registers everything.
        DumpCommand.init();

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SpawnerProfileManager());
    }

    private void addStructures(MinecraftServer server, ServerWorld world) {
        // From: https://github.com/TelepathicGrunt/RepurposedStructures-Fabric/blob/master/src/main/java/com/telepathicgrunt/repurposedstructures/RSAddFeaturesAndStructures.java
        Map<StructureFeature<?>, StructureConfig> tempMap = new HashMap<>(world.getChunkManager().getChunkGenerator()
                .getStructuresConfig().getStructures());
        tempMap.putAll(MOD_STRUCTURES);
        ((StructuresConfigAccessor)world.getChunkManager().getChunkGenerator().getStructuresConfig())
                .setStructures(tempMap);
    }

    // ================================================================================
    // Adapted from: https://gist.github.com/CorgiTaco/3eb2d9128a1ec41bd5d5846d17994851
    public static void addStructureToBiome(Biome biome, ConfiguredStructureFeature<?, ?> configuredStructure) {
        convertImmutableStructureFeatures(biome);
        List<Supplier<ConfiguredStructureFeature<?, ?>>> biomeStructures = biome.getGenerationSettings().structureFeatures;
        biomeStructures.add(() -> configuredStructure);
    }

    //Swap the list to mutable in order for us to add our features with ease.
    private static void convertImmutableStructureFeatures(Biome biome) {
        if (biome.getGenerationSettings().structureFeatures instanceof ImmutableList) {
            biome.getGenerationSettings().structureFeatures = new ArrayList<>(biome.getGenerationSettings().structureFeatures);
        }
    }
    // ================================================================================

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }

}