package me.geek.tom.fallfest.block.spawner;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import me.geek.tom.fallfest.Utils;
import me.geek.tom.fallfest.component.ModComponents;
import me.geek.tom.fallfest.component.SpawnerMobComponent;
import me.geek.tom.fallfest.resources.SpawnerProfileManager;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Controls the mob spawning of the cursed spawner and manages the state (waves, etc)
 */
public class CursedSpawnerController {

    // TODO: Move these to be data-driven too.
    private static final int WAVE_CHARGE_TIME = 100;
    private static final int WAVE_SPAWN_TIME = 60;
    private static final int SPAWN_RADIUS = 4;

    private SpawnerProfileManager.SpawnerProfile profile;
    private final BlockPos spawnerPos;
    private final World world;
    private final Runnable markDirty;
    private final Runnable syncClient;

    private List<UUID> currentWaveMobs;

    private int timer;

    private Map<BlockPos, EntityType<?>> potentialSpawnLocations;
    private int currentWave;
    private WaveState currentState;

    private final ServerBossBar bar;

    public CursedSpawnerController(SpawnerProfileManager.SpawnerProfile profile, PlayerEntity player, BlockPos spawnerPos, World world, Runnable markDirty, Runnable syncClient) {
        this.profile = profile;
        this.spawnerPos = spawnerPos;
        this.world = world;
        this.markDirty = markDirty;
        this.syncClient = syncClient;

        this.currentWave = 0;
        this.timer = 0;

        this.potentialSpawnLocations = new HashMap<>();
        this.currentState = WaveState.CHARGING;
        this.currentWaveMobs = new ArrayList<>();
        bar = new ServerBossBar(new LiteralText("Cursed Spawner"), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
        bar.setPercent(0.0f);
        bar.addPlayer(((ServerPlayerEntity) player));
    }

    public void tick() {
        switch (this.currentState) {
            case CHARGING:
                tickCharging();
                break;
            case SPAWNING:
                tickSpawning();
                break;
            case IN_PROGRESS:
                break;
        }
    }

    private void tickSpawning() {
        timer++;
        if (timer >= WAVE_SPAWN_TIME) {
            updateState(WaveState.IN_PROGRESS);
        } else {
            bar.setPercent(timer / (float) WAVE_SPAWN_TIME);
        }
    }

    private void tickCharging() {
        timer++;
        if (timer >= WAVE_CHARGE_TIME) {
            updateState(WaveState.SPAWNING);
        } else {
            bar.setPercent(timer / (float)WAVE_CHARGE_TIME);
        }
    }

    private void updateState(WaveState state) {
        timer = 0;
        switch (state) {
            case CHARGING:
                beginCharging();
                break;
            case SPAWNING:
                beginSpawning();
                break;
            case IN_PROGRESS:
                beginWave();
                break;
        }

        markDirty.run();
        syncClient.run();
    }

    private void beginWave() {
        List<UUID> spawnedMobs = spawnMobs();
        this.currentWaveMobs.clear();
        this.currentWaveMobs.addAll(spawnedMobs);
        updateBarText();
        bar.setPercent(1.0f);
    }

    private List<UUID> spawnMobs() {
        List<UUID> uuids = new ArrayList<>();
        for (Map.Entry<BlockPos, EntityType<?>> entry : this.potentialSpawnLocations.entrySet()) {
            BlockPos pos = entry.getKey();
            if (canSpawnEntity(entry.getValue(), this.world, pos)) {

                Entity entity = entry.getValue().create(this.world);
                assert entity != null;

                entity.updatePositionAndAngles(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f,
                        entity.yaw, entity.pitch);
                this.world.spawnEntity(entity);
                uuids.add(entity.getUuid());

                SpawnerMobComponent spawnerMobComponent = ModComponents.SPAWNER_MOB.get(entity);
                spawnerMobComponent.setPos(this.spawnerPos);
                spawnerMobComponent.setHasSpawner(true);
            } else {
                // TODO: Implement retry logic when mobs cannot be spawned.
            }
        }
        return uuids;
    }

    private void updateBarText() {
        bar.setName(new LiteralText("Cursed Spawner - "
                + StringUtils.capitalize(this.currentState.name().toLowerCase())
                + " - Wave " + currentWave + "/" + this.profile.getWaves().size()));
    }

    private void beginCharging() {

    }

    private void beginSpawning() {
        SpawnerProfileManager.SpawnerProfile.SpawnerWave currentWave = this.profile.getWaves().get(this.currentWave);
        Map<BlockPos, EntityType<?>> spawnPositions = findValidSpawnPositions(currentWave);
        List<BlockPos> locations = new ArrayList<>(spawnPositions.keySet());
        int amount = Utils.rand(world.getRandom(), currentWave.getBaseMobCount(), currentWave.getMobCountVariation());
        Collections.shuffle(locations, this.world.random);
        List<BlockPos> finalLocations = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            finalLocations.add(locations.remove(this.world.random.nextInt(locations.size())));
        }
        potentialSpawnLocations.clear();
        for (BlockPos pos : finalLocations) {
            potentialSpawnLocations.put(pos, spawnPositions.get(pos));
        }
        markDirty.run();
        syncClient.run();
    }

    private Map<BlockPos, EntityType<?>> findValidSpawnPositions(SpawnerProfileManager.SpawnerProfile.SpawnerWave spawnerWave) {
        Map<BlockPos, EntityType<?>> ret = new HashMap<>();
        Random rand = world.random;
        for (int x = spawnerPos.getX() - SPAWN_RADIUS; x <= spawnerPos.getX() + SPAWN_RADIUS; x++) {
            for (int y = spawnerPos.getY(); y < spawnerPos.getY() + 3; y++) {
                for (int z = spawnerPos.getZ() - SPAWN_RADIUS; z <= spawnerPos.getZ() + SPAWN_RADIUS; z++) {

                    BlockPos pos = new BlockPos(x, y, z);
                    EntityType<?> entityType = Utils.choice(rand, spawnerWave.getEntities());
                    if (canSpawnEntity(entityType, world, pos)) {
                        ret.put(pos, entityType);
                    }
                }
            }
        }
        return ret;
    }

    private static boolean canSpawnEntity(EntityType<?> entityType, World world, BlockPos pos) {
        return world.isSpaceEmpty(entityType.createSimpleBoundingBox(pos.getX(), pos.getY(), pos.getZ()))
                && SpawnRestriction.canSpawn(entityType, (ServerWorldAccess) world, SpawnReason.SPAWNER, pos, world.getRandom());
    }

    public CompoundTag toTag(CompoundTag tag) {
        NbtOps nbtOps = NbtOps.INSTANCE;
        DataResult<Tag> result = SpawnerProfileManager.SpawnerProfile.CODEC.encodeStart(nbtOps, this.profile);
        Tag profileTag = result.getOrThrow(false, System.err::println);
        ListTag mobs = new ListTag();
        this.currentWaveMobs.stream().map(NbtHelper::fromUuid).forEach(mobs::add);

        tag.put("Profile", profileTag);
        tag.put("WaveMobs", mobs);
        tag.putInt("Timer", timer);
        tag.put("PotentialSpawnLocations", storeMap(this.potentialSpawnLocations));
        tag.putInt("CurrentWave", this.currentWave);
        tag.putString("WaveState", this.currentState.name());

        return tag;
    }

    public void fromTag(CompoundTag tag) {
        NbtOps nbtOps = NbtOps.INSTANCE;
        if (tag.contains("Profile")) {
            this.profile = SpawnerProfileManager.SpawnerProfile.CODEC.decode(new Dynamic<>(nbtOps, tag.getCompound("Profile")))
                    .getOrThrow(false, System.out::println)
                    .getFirst();
        }
        if (tag.contains("WaveMobs")) {
            this.currentWaveMobs = tag.getList("WaveMobs", 11).stream()
                    .map(NbtHelper::toUuid).collect(Collectors.toList());
        }
        if (tag.contains("Timer")) {
            this.timer = tag.getInt("Timer");
        }
        if (tag.contains("PotentialSpawnLocations")) {
            this.potentialSpawnLocations = readMap(tag.getList("PotentialSpawnLocations", 10));
        }
        if (tag.contains("CurrentWave")) {
            this.currentWave = tag.getInt("CurrentWave");
        }
        if (tag.contains("WaveState")) {
            this.currentState = WaveState.valueOf(tag.getString("WaveState"));
        }
    }

    private Map<BlockPos, EntityType<?>> readMap(ListTag tag) {
        Map<BlockPos, EntityType<?>> ret = new HashMap<>();
        tag.forEach(t -> {
            CompoundTag tg = ((CompoundTag) t);
            ret.put(NbtHelper.toBlockPos(tg.getCompound("Pos")),
                    Registry.ENTITY_TYPE.get(new Identifier(tg.getString("EntityType"))));
        });
        return ret;
    }

    private static ListTag storeMap(Map<BlockPos, EntityType<?>> mp) {
        ListTag tag = new ListTag();
        mp.entrySet().stream().map(e -> {
            CompoundTag t = new CompoundTag();
            t.put("Pos", NbtHelper.fromBlockPos(e.getKey()));
            t.putString("EntityType", Registry.ENTITY_TYPE.getId(e.getValue()).toString());
            return t;
        }).forEach(tag::add);
        return tag;
    }

    public void onEntityDie(LivingEntity entity) {
        UUID uuid = entity.getUuid();
        if (this.currentWaveMobs.contains(uuid)) {
            this.currentWaveMobs.remove(uuid);

            if (this.currentWaveMobs.size() == 0) {
                startNextWaveOrFinish();
            }
        }
    }

    private void startNextWaveOrFinish() {
        if (currentWave <= profile.getWaves().size()) {
            currentWave++;
            this.updateState(WaveState.CHARGING);
        } else {
            endSpawner();
        }
    }

    private void endSpawner() {
        this.world.createExplosion(null,
                this.spawnerPos.getX(), this.spawnerPos.getY(), this.spawnerPos.getZ(),
                5.0f, Explosion.DestructionType.NONE);
        this.world.setBlockState(this.spawnerPos, this.world.getBlockState(this.spawnerPos).with(CursedSpawnerBlock.ACTIVE, false));
    }

    public enum WaveState {
        CHARGING, SPAWNING, IN_PROGRESS
    }
}
