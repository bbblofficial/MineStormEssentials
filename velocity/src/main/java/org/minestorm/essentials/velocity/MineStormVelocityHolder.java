package org.minestorm.essentials.velocity;

public final class MineStormVelocityHolder {
    private static MineStormVelocity instance;
    private MineStormVelocityHolder() {}
    public static void set(MineStormVelocity plugin) { instance = plugin; }
    public static MineStormVelocity get() { return instance; }
}
