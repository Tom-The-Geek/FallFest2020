package me.geek.tom.fallfest.component;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import me.geek.tom.fallfest.component.impl.SpawnerPosComponent;
import net.minecraft.util.math.BlockPos;

public interface SpawnerMobComponent extends ComponentV3 {
    BlockPos getSpawnerPos();
    boolean hasSpawner();

    void setPos(BlockPos pos);
    void setHasSpawner(boolean hasSpawner);
}
