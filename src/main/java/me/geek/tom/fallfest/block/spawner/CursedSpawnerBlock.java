package me.geek.tom.fallfest.block.spawner;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CursedSpawnerBlock extends Block implements BlockEntityProvider {
    public static BooleanProperty ACTIVE = BooleanProperty.of("active");

    public CursedSpawnerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(ACTIVE, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient())
            return ActionResult.CONSUME;

        world.setBlockState(pos, state.with(ACTIVE, !state.get(ACTIVE)));
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof CursedSpawnerBlockEntity) {
            return ((CursedSpawnerBlockEntity) be).onUse(state, world, pos, player, hand, hit);
        }
        return ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new CursedSpawnerBlockEntity();
    }
}
