package org.minestorm.essentials.spigot.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.MineStormSpigot;

/**
 * Sends structured payloads on channel "minestorm:main" back to the proxy.
 */
public final class ProxyMessenger {

    public static final String CHANNEL = MineStormSpigot.CHANNEL;

    private ProxyMessenger() {}

    public static void sendResult(Player replyTo, String token, boolean ok, String message) {
        if (replyTo == null || token == null) return;
        Plugin p = Bukkit.getPluginManager().getPlugin("MineStormEssentials");
        if (p == null) return;
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("RESULT");
        out.writeUTF(token);
        out.writeBoolean(ok);
        out.writeUTF(message == null ? "" : message);
        replyTo.sendPluginMessage((JavaPlugin) p, CHANNEL, out.toByteArray());
    }
}
