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
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SpawnerProfileManager extends JsonDataLoader implements IdentifiableResourceReloadListener {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final Identifier EMPTY_PROFILE = new Identifier("empty");
    public static final SpawnerProfile EMPTY = new SpawnerProfile(Collections.emptyList(), Items.AIR);

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

            try {
                Dynamic<JsonElement> dynamic = new Dynamic<>(jsonOps, entry.getValue());
                SpawnerProfile profile = SpawnerProfile.CODEC.decode(dynamic).getOrThrow(false, LOGGER::error).getFirst();

                profiles.put(entry.getKey(), profile);
            } catch (RuntimeException e) {
                LOGGER.warn("Profile: " + entry.getKey() + " failed to load:", e);
            }
        }

        // Validate each profile
        List<Identifier> invalid = new ArrayList<>();
        for (Map.Entry<Identifier, SpawnerProfile> entry : profiles.entrySet()) {
            if (!validate(entry.getKey(), entry.getValue())) {
                LOGGER.warn("Profile: " + entry.getKey() + " failed validation! Dropping...");
                invalid.add(entry.getKey());
            }
        }
        invalid.forEach(profiles::remove);

        // Special profile that does nothing.
        profiles.put(EMPTY_PROFILE, EMPTY);

        SpawnerProfileManager.profiles = ImmutableMap.copyOf(profiles);
        LOGGER.info("Loaded {} spawner profiles!", profiles.size());
    }

    private boolean validate(Identifier id, SpawnerProfile profile) {
        boolean ok = true;
        for (SpawnerProfile.SpawnerWave wave : profile.getWaves()) {
            if (wave.entities.size() == 0) {
                LOGGER.warn("Spawner profile: " + id + " does not have any entities specified for a wave!");
                ok = false;
            }
            if (wave.minMobCount <= 0 || wave.maxMobCount < wave.minMobCount) {
                LOGGER.warn("There is an issue (min/max is less than 0, max is less than min) with the mob counts in: " + id + "!");
            }
        }
        return ok;
    }

    public static class SpawnerProfile {
        public static final Codec<SpawnerProfile> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                        SpawnerWave.CODEC.listOf().fieldOf("waves").forGetter(SpawnerProfile::getWaves),
                        Registry.ITEM.fieldOf("reward").forGetter(SpawnerProfile::getReward)
                ).apply(instance, SpawnerProfile::new)
        );

        private final List<SpawnerWave> waves;
        private final Item reward;

        public SpawnerProfile(List<SpawnerWave> waves, Item reward) {
            this.waves = waves;
            this.reward = reward;
        }

        public List<SpawnerWave> getWaves() {
            return waves;
        }

        public Item getReward() {
            return reward;
        }

        @Override
        public String toString() {
            return "SpawnerProfile{" +
                    "waves=" + waves +
                    ", reward=" + reward +
                    '}';
        }

        public static class SpawnerWave {
            public static final Codec<SpawnerWave> CODEC = RecordCodecBuilder.create(
                    instance -> instance.group(
                            Registry.ENTITY_TYPE.listOf().fieldOf("entities").forGetter(SpawnerWave::getEntities),
                            Codec.INT.fieldOf("minMobCount").forGetter(SpawnerWave::getMinMobCount),
                            Codec.INT.fieldOf("maxMobCount").forGetter(SpawnerWave::getMaxMobCount)
                    ).apply(instance, SpawnerWave::new)
            );

            private final List<EntityType<?>> entities;
            private final int minMobCount;
            private final int maxMobCount;

            public SpawnerWave(List<EntityType<?>> entities, int minMobCount, int maxMobCount) {
                this.entities = entities;
                this.minMobCount = minMobCount;
                this.maxMobCount = maxMobCount;
            }

            public List<EntityType<?>> getEntities() {
                return entities;
            }

            public int getMinMobCount() {
                return minMobCount;
            }

            public int getMaxMobCount() {
                return maxMobCount;
            }

            @Override
            public String toString() {
                return "SpawnerWave{" +
                        "entities=" + entities +
                        ", minMobCount=" + minMobCount +
                        ", maxMobCount=" + maxMobCount +
                        '}';
            }
        }
    }
}
