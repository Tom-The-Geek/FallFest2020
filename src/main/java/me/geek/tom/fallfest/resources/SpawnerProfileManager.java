package me.geek.tom.fallfest.resources;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.geek.tom.fallfest.FallFest;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpawnerProfileManager extends JsonDataLoader implements IdentifiableResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Identifier EMPTY_PROFILE = new Identifier("empty");
    public static final SpawnerProfile EMPTY = new SpawnerProfile(Collections.emptyList());

    private static Map<Identifier, SpawnerProfile> profiles = ImmutableMap.of();

    public SpawnerProfileManager() {
        super(FallFest.GSON, "spawner_profiles");
    }

    @Override
    public Identifier getFabricId() {
        return FallFest.modIdentifier("spawner_profiles");
    }

    public static Map<Identifier, SpawnerProfile> getProfiles() {
        return profiles;
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        LOGGER.info("Loading spawner profiles...");
        JsonOps jsonOps = JsonOps.INSTANCE;
        Map<Identifier, SpawnerProfile> profiles = new HashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry :loader.entrySet()) {
            if (entry.getKey().equals(EMPTY_PROFILE)) {
                LOGGER.warn("Datapack tried to override spawner profile: {}! This is not allowed, skipping!", EMPTY_PROFILE);
            }

            Dynamic<JsonElement> dynamic = new Dynamic<>(jsonOps, entry.getValue());
            SpawnerProfile profile = SpawnerProfile.CODEC.decode(dynamic).getOrThrow(false, LOGGER::error).getFirst();

            // TODO: Validate profile (entity type, etc)
            profiles.put(entry.getKey(), profile);
        }

        // Special profile that does nothing.
        profiles.put(EMPTY_PROFILE, EMPTY);

        SpawnerProfileManager.profiles = ImmutableMap.copyOf(profiles);
        LOGGER.info("Loaded {} spawner profiles!", profiles.size());
    }

    public static class SpawnerProfile {
        public static final Codec<SpawnerProfile> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        SpawnerWave.CODEC.listOf().fieldOf("waves").forGetter(SpawnerProfile::getWaves)
                ).apply(instance, SpawnerProfile::new)
        );

        private final List<SpawnerWave> waves;

        public SpawnerProfile(List<SpawnerWave> waves) {
            this.waves = waves;
        }

        public List<SpawnerWave> getWaves() {
            return waves;
        }

        @Override
        public String toString() {
            return "SpawnerProfile{" +
                    "waves=" + waves +
                    '}';
        }

        public static class SpawnerWave {
            public static final Codec<SpawnerWave> CODEC = RecordCodecBuilder.create(
                    instance -> instance.group(
                            Registry.ENTITY_TYPE.listOf().fieldOf("entities").forGetter(SpawnerWave::getEntities),
                            Codec.INT.fieldOf("baseMobCount").forGetter(SpawnerWave::getBaseMobCount),
                            Codec.INT.fieldOf("mobCountVariation").forGetter(SpawnerWave::getMobCountVariation)
                    ).apply(instance, SpawnerWave::new)
            );

            private final List<EntityType<?>> entities;
            private final int baseMobCount;
            private final int mobCountVariation;

            public SpawnerWave(List<EntityType<?>> entities, int baseMobCount, int mobCountVariation) {
                this.entities = entities;
                this.baseMobCount = baseMobCount;
                this.mobCountVariation = mobCountVariation;
            }

            public List<EntityType<?>> getEntities() {
                return entities;
            }

            public int getBaseMobCount() {
                return baseMobCount;
            }

            public int getMobCountVariation() {
                return mobCountVariation;
            }

            @Override
            public String toString() {
                return "SpawnerWave{" +
                        "entities=" + entities +
                        ", baseMobCount=" + baseMobCount +
                        ", mobCountVariation=" + mobCountVariation +
                        '}';
            }
        }
    }
}
