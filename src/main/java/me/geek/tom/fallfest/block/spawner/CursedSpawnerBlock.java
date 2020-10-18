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
        if (hand == Hand.OFF_HAND) return ActionResult.CONSUME;
        if (state.get(ACTIVE)) return ActionResult.CONSUME;

        if (world.isClient()) return ActionResult.CONSUME;

        world.setBlockState(pos, state.with(ACTIVE, true));
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
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        super.onBreak(world, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CursedSpawnerBlockEntity) {
                ((CursedSpawnerBlockEntity) be).getController().ifPresent(CursedSpawnerController::onRemoved);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockEntity createBlockEntity(BlockView world) {
        return new CursedSpawnerBlockEntity();
    }
}
