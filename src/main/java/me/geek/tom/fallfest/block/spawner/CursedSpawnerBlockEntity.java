package me.geek.tom.fallfest.block.spawner;

import me.geek.tom.fallfest.FallFest;
import me.geek.tom.fallfest.Registration;
import me.geek.tom.fallfest.resources.SpawnerProfileManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.LiteralText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CursedSpawnerBlockEntity extends BlockEntity {
    private Identifier spawnerProfile;

    public CursedSpawnerBlockEntity() {
        super(Registration.CURSED_SPAWNER_BLOCK_ENTITY);
        spawnerProfile = SpawnerProfileManager.EMPTY_PROFILE;
    }

    @Override
    public void fromTag(BlockState state, CompoundTag tag) {
        super.fromTag(state, tag);
        if (tag.contains("Profile"))
            spawnerProfile = new Identifier(tag.getString("Profile"));
    }

    @Override
    public CompoundTag toTag(CompoundTag tag) {
        tag.putString("Profile", spawnerProfile.toString());
        return super.toTag(tag);
    }

    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        SpawnerProfileManager.SpawnerProfile profile = SpawnerProfileManager.getProfiles().get(this.spawnerProfile);
        if (profile == null) {
            player.sendMessage(new LiteralText("No profile found: " + spawnerProfile), false);
        } else {
            FallFest.LOGGER.info(profile);
        }

        return ActionResult.SUCCESS;
    }
}
