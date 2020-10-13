package me.geek.tom.fallfest.component.impl;

import me.geek.tom.fallfest.component.SpawnerMobComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;

public class SpawnerPosComponent implements SpawnerMobComponent {

    private BlockPos pos;
    private boolean hasSpawner;

    public SpawnerPosComponent() {
        this(null, false);
    }

    public SpawnerPosComponent(BlockPos pos, boolean hasSpawner) {
        this.pos = pos;
        this.hasSpawner = hasSpawner;
    }

    @Override
    public BlockPos getSpawnerPos() {
        return pos;
    }

    @Override
    public boolean hasSpawner() {
        return hasSpawner;
    }

    @Override
    public void setPos(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void setHasSpawner(boolean hasSpawner) {
        this.hasSpawner = hasSpawner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SpawnerPosComponent that = (SpawnerPosComponent) o;
        return hasSpawner == that.hasSpawner &&
                Objects.equals(pos, that.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos, hasSpawner);
    }

    @Override
    public void readFromNbt(CompoundTag compoundTag) {
        if (compoundTag.contains("DungeonSpawnerPos"))
            this.pos = NbtHelper.toBlockPos(compoundTag.getCompound("DungeonSpawnerPos"));
        this.hasSpawner = compoundTag.getBoolean("HasSpawner");
    }

    @Override
    public void writeToNbt(CompoundTag compoundTag) {
        if (this.pos != null)
            compoundTag.put("DungeonSpawnerPos", NbtHelper.fromBlockPos(this.pos));
        compoundTag.putBoolean("HasSpawner", this.hasSpawner);
    }
}
