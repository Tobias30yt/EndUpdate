package de.midnitesmp.endupdate;

import java.util.random.RandomGenerator;

public final class MithrylRules {
    private MithrylRules() {}
    public static boolean outerIsland(int x, int z, int radius) { return (long)x*x + (long)z*z >= (long)radius*radius; }
    public static int fortuneDrops(int level, RandomGenerator random) {
        return level <= 0 ? 1 : 1 + Math.max(0, random.nextInt(Math.min(level, 255) + 2) - 1);
    }
    public static int speedAmplifier(int pieces) { return pieces == 0 ? -1 : pieces >= 4 ? 1 : 0; }
    public static int encode(int x, int y, int z, int minY) { return ((y - minY) << 8) | ((z & 15) << 4) | (x & 15); }
}
