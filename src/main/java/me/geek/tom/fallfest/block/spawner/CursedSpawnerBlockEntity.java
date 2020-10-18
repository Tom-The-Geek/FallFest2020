package me.geek.tom.fallfest.block.spawner;

import me.geek.tom.fallfest.FallFest;
import me.geek.tom.fallfest.Registration;
import me.geek.tom.fallfest.resources.SpawnerProfileManager;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

import java.util.Optional;

public class CursedSpawnerBlockEntity extends BlockEntity implements Tickable, BlockEntityClientSerializable {
    private Identifier spawnerProfile;

    private CursedSpawnerController controller;

    public CursedSpawnerBlockEntity() {
        super(Registration.CURSED_SPAWNER_BLOCK_ENTITY);
        spawnerProfile = SpawnerProfileManager.EMPTY_PROFILE;
    }

    private CompoundTag write(CompoundTag tag) {
        tag.putString("Profile", spawnerProfile.toString());

        if (controller != null)
            tag.put("Controller", controller.toTag(new CompoundTag()));

        return tag;
    }

    private void read(CompoundTag tag) {
        if (tag.contains("Profile"))
            spawnerProfile = new Identifier(tag.getString("Profile"));

        if (tag.contains("Controller")) {
            if (controller == null) {
                createController(null);
                if (controller != null) {
                    controller.fromTag(tag.getCompound("Controller"));
                }
            }
        }
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        read(tag);
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        return super.toTag(write(tag));
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!state.get(CursedSpawnerBlock.ACTIVE)) {
            if (world.getDifficulty() == Difficulty.PEACEFUL) {
                player.sendMessage(new LiteralText("World is peaceful, you cannot start the spawner!")
                        .styled(s -> s.withColor(Formatting.RED)), false);
                return ActionResult.CONSUME;
            }

            createController(player);
            markDirty();
            sync();
        }

        return ActionResult.SUCCESS;
    }

    private void createController(PlayerEntity player) {
        SpawnerProfileManager.SpawnerProfile profile = SpawnerProfileManager.getProfiles().get(this.spawnerProfile);
//        FallFest.LOGGER.info(profile);
        if (profile == null) {
            if (player != null)
                player.sendMessage(new LiteralText("No profile found: " + spawnerProfile), false);
            else
                FallFest.LOGGER.warn("Failed to locate profile: " + spawnerProfile);
            return;
        }

        controller = new CursedSpawnerController(profile, player, this.pos, this.world, () -> this.world, this::markDirty, this::sync);
    }

    @Override
    public void fromClientTag(CompoundTag tag) {
        this.read(tag);
    }

    @Override
    public CompoundTag toClientTag(CompoundTag tag) {
        return this.write(tag);
    }

    public void onEntityDie(LivingEntity entity) {
        if (this.controller != null)
            this.controller.onEntityDie(entity);
    }

    @Override
    public void tick() {
        if (this.controller != null)
            this.controller.tick();
    }

    public void spawnerComplete() {
        this.controller = null;
        markDirty();
        sync();
    }

    public Optional<CursedSpawnerController> getController() {
        return Optional.ofNullable(this.controller);
    }
}
