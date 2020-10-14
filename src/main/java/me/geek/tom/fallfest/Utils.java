package me.geek.tom.fallfest;

import me.geek.tom.fallfest.block.spawner.CursedSpawnerBlockEntity;
import me.geek.tom.fallfest.component.ModComponents;
import me.geek.tom.fallfest.component.SpawnerMobComponent;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class Utils {
    public static <T> T choice(Random rand, List<T> lst) {
        return lst.get(rand.nextInt(lst.size()));
    }

    public static int rand(Random random, int min, int max) {
        return min + random.nextInt(max - min);
    }

//    private static int inRange(Random rand, int min, int max) {
//        int diff = max - min;
//        return (rand.nextInt(diff * 2) - diff);
//    }

    public static void handleEntityDeath(World world, LivingEntity e) {
        if (world.isClient()) return;

        SpawnerMobComponent component = ModComponents.SPAWNER_MOB.get(e);
        if (component.hasSpawner()) {
            BlockEntity be = world.getBlockEntity(component.getSpawnerPos());
            if (be instanceof CursedSpawnerBlockEntity) {
                ((CursedSpawnerBlockEntity) be).onEntityDie(e);
            }
            component.setHasSpawner(false);
            component.setPos(null);
        }
    }
}
