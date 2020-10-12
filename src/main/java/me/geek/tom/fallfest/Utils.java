package me.geek.tom.fallfest;

import java.util.List;
import java.util.Random;

public class Utils {
    public static <T> T choice(Random rand, List<T> lst) {
        return lst.get(rand.nextInt(lst.size()));
    }

    public static int rand(Random random, int baseCount, int variation) {
        return baseCount + (inRange(random, baseCount - variation, baseCount + variation));
    }

    private static int inRange(Random rand, int min, int max) {
        int diff = max - min;
        return (rand.nextInt(diff * 2) - diff);
    }
}
