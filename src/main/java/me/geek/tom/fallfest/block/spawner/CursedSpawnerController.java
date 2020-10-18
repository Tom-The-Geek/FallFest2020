package me.geek.tom.fallfest.block.spawner;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.Unpooled;
import me.geek.tom.fallfest.FallFest;
import me.geek.tom.fallfest.Utils;
import me.geek.tom.fallfest.component.ModComponents;
import me.geek.tom.fallfest.component.SpawnerMobComponent;
import me.geek.tom.fallfest.resources.SpawnerProfileManager;
import net.fabricmc.fabric.api.network.ServerSidePacketRegistry;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Controls the mob spawning of the cursed spawner and manages the state (waves, etc)
 */
public class CursedSpawnerController {

    // TODO: Move these to be data-driven too.
    private static final int WAVE_CHARGE_TIME = 60;
    private static final int WAVE_SPAWN_TIME = 20;
    private static final int SPAWN_RADIUS = 4;

    private SpawnerProfileManager.SpawnerProfile profile;
    private final BlockPos spawnerPos;
    private final Supplier<World> worldSupplier;
    private World world;
    private final Runnable markDirty;
    private final Runnable syncClient;

    private int waveMobCount;
    private List<UUID> currentWaveMobs;

    private int updateTimer;
    private int timer;

    private Map<BlockPos, EntityType<?>> potentialSpawnLocations;
    private int currentWave;
    private WaveState currentState;

    private final ServerBossBar bar;

    public CursedSpawnerController(SpawnerProfileManager.SpawnerProfile profile, PlayerEntity player,
                                   BlockPos spawnerPos, World world, Supplier<World> worldSupplier,
                                   Runnable markDirty, Runnable syncClient) {
        this.profile = profile;
        this.spawnerPos = spawnerPos;
        this.world = world;
        this.worldSupplier = worldSupplier;
        this.markDirty = markDirty;
        this.syncClient = syncClient;

        this.currentWave = 0;
        this.timer = 0;
        this.updateTimer = 0;

        this.potentialSpawnLocations = new HashMap<>();
        this.currentState = WaveState.CHARGING;
        this.currentWaveMobs = new ArrayList<>();
        bar = new ServerBossBar(new LiteralText("Cursed Spawner"), BossBar.Color.GREEN, BossBar.Style.PROGRESS);
        updateBarText();
        bar.setPercent(0.0f);
        if (player != null)
            bar.addPlayer(((ServerPlayerEntity) player));
        this.sendEffect(0, this.spawnerPos);
    }

    public void tick() {
        World world = getWorld();
        if (world == null || world.isClient()) return;
        updateTimer %= 20;
        if (updateTimer == 0) {
            updateBarPlayers();
        }

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

        this.currentState = state;
        this.updateBarText();

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
        World world = getWorld();
        List<UUID> uuids = new ArrayList<>();
        for (Map.Entry<BlockPos, EntityType<?>> entry : this.potentialSpawnLocations.entrySet()) {
            BlockPos pos = entry.getKey();
            if (canSpawnEntity(entry.getValue(), world, pos)) {

                Entity entity = entry.getValue()
                        .spawn((ServerWorld) world, null, null, null, pos, SpawnReason.SPAWNER,
                                true, false);
                assert entity != null;

                uuids.add(entity.getUuid());

                SpawnerMobComponent spawnerMobComponent = ModComponents.SPAWNER_MOB.get(entity);
                spawnerMobComponent.setPos(this.spawnerPos);
                spawnerMobComponent.setHasSpawner(true);
                ModComponents.SPAWNER_MOB.sync(entity);

                sendEffect(2, pos);
            } else {
                // TODO: Implement retry logic when mobs cannot be spawned.
            }
        }
        this.waveMobCount = uuids.size();
        return uuids;
    }

    private void updateBarText() {
        if (this.currentState == WaveState.IN_PROGRESS) {
            bar.setName(new TranslatableText("cursedspawner.bar.message.inprogress",
                    String.valueOf(this.currentWaveMobs.size()), String.valueOf(this.waveMobCount),
                    String.valueOf(currentWave), String.valueOf(this.profile.getWaves().size())));
        } else {
            bar.setName(new TranslatableText("cursedspawner.bar.message",
                    StringUtils.capitalize(this.currentState.name().toLowerCase()),
                    String.valueOf(currentWave + 1), String.valueOf(this.profile.getWaves().size())));
        }
    }

    private void beginCharging() {
        sendEffect(0, this.spawnerPos);
    }

    private void sendEffect(int type, BlockPos pos) {
        World world = this.getWorld();
        if (world == null || world.isClient()) return;

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeInt(type);
        PlayerStream.watching(world, this.spawnerPos).forEach(player ->
                ServerSidePacketRegistry.INSTANCE.sendToPlayer(player, FallFest.SPAWNER_EFFECT_PACKET_ID, buf));
    }

    private void beginSpawning() {
        World world = getWorld();
        SpawnerProfileManager.SpawnerProfile.SpawnerWave currentWave = this.profile.getWaves().get(this.currentWave);
        Map<BlockPos, EntityType<?>> spawnPositions = findValidSpawnPositions(currentWave);
        List<BlockPos> locations = new ArrayList<>(spawnPositions.keySet());
        int amount = Utils.rand(world.getRandom(), currentWave.getMinMobCount(), currentWave.getMaxMobCount());
        Collections.shuffle(locations, world.random);
        List<BlockPos> finalLocations = new ArrayList<>();
        for (int i = 0; i < Math.min(amount, locations.size()); i++) {
            finalLocations.add(locations.remove(world.random.nextInt(locations.size())));
        }
        potentialSpawnLocations.clear();
        for (BlockPos pos : finalLocations) {
            potentialSpawnLocations.put(pos, spawnPositions.get(pos));
        }
        markDirty.run();
        syncClient.run();
        sendEffect(1, this.spawnerPos);
    }

    private Map<BlockPos, EntityType<?>> findValidSpawnPositions(SpawnerProfileManager.SpawnerProfile.SpawnerWave spawnerWave) {
        World world = getWorld();
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
        return world.isSpaceEmpty(entityType.createSimpleBoundingBox(pos.getX(), pos.getY(), pos.getZ()));
    }

    public CompoundTag toTag(CompoundTag tag) {
        NbtOps nbtOps = NbtOps.INSTANCE;
        DataResult<Tag> result = SpawnerProfileManager.SpawnerProfile.CODEC.encodeStart(nbtOps, this.profile);
        Tag profileTag = result.getOrThrow(false, System.err::println);
        ListTag mobs = new ListTag();
        this.currentWaveMobs.stream().map(NbtHelper::fromUuid).forEach(mobs::add);

        tag.put("Profile", profileTag);
        tag.putInt("WaveMobCount", this.waveMobCount);
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
        if (tag.contains("WaveMobCount")) {
            this.waveMobCount = tag.getInt("WaveMobCount");
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
            updateBarText();
            bar.setPercent((float)this.currentWaveMobs.size() / this.waveMobCount);

            if (this.currentWaveMobs.size() == 0) {
                startNextWaveOrFinish();
            }
        }
    }

    private void startNextWaveOrFinish() {
        if (currentWave < profile.getWaves().size() - 1) {
            currentWave++;
            this.updateState(WaveState.CHARGING);
        } else {
            endSpawner();
        }
    }

    private void endSpawner() {
        World world = getWorld();
        assert world != null;

        BlockEntity be = world.getBlockEntity(this.spawnerPos);
        this.dropStack(new ItemStack(profile.getReward(), 1));

        world.setBlockState(this.spawnerPos, world.getBlockState(this.spawnerPos).with(CursedSpawnerBlock.ACTIVE, false));
        this.bar.clearPlayers();
        if (be instanceof CursedSpawnerBlockEntity) {
            ((CursedSpawnerBlockEntity) be).spawnerComplete();
        }
        sendEffect(3, this.spawnerPos);
    }

    private void dropStack(ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
                // offset by one on the y to spawn the drops above
                this.world, this.spawnerPos.getX(), this.spawnerPos.getY() + 1, this.spawnerPos.getZ(), stack);
        itemEntity.setToDefaultPickupDelay();
        this.world.spawnEntity(itemEntity);
    }

    public void updateBarPlayers() {
        World world = getWorld();
        BlockPos start = this.spawnerPos.add(-64, -this.spawnerPos.getY(), -64);
        BlockPos end = this.spawnerPos.add(64, 255 - this.spawnerPos.getY(), 64);
        this.bar.clearPlayers();
        for (PlayerEntity player : world.getEntitiesByType(EntityType.PLAYER, new Box(start, end), __ -> true)) {
            this.bar.addPlayer(((ServerPlayerEntity) player));
        }
    }

    // Retry every time until the world is available. I don't like this code either.
    private World getWorld() {
        if (world == null) {
            world = worldSupplier.get();
        }
        return world;
    }

    public void onRemoved() {
        this.bar.clearPlayers();
    }

    public enum WaveState {
        CHARGING, SPAWNING, IN_PROGRESS
    }
}
