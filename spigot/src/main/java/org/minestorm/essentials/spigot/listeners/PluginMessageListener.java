package org.minestorm.essentials.spigot.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.ProxyMessenger;

public class PluginMessageListener implements org.bukkit.plugin.messaging.PluginMessageListener {

    private final JavaPlugin plugin;
    public PluginMessageListener(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public void onPluginMessageReceived(String channel, Player receiver, byte[] message) {
        if (!channel.equals(org.minestorm.essentials.spigot.MineStormSpigot.CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String sub;
        try { sub = in.readUTF(); } catch (Exception ex) { return; }

        if (!sub.equals("EXEC")) return;

        final String token = in.readUTF();
        final String senderName = in.readUTF();
        final String commandLine = in.readUTF();

        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override public void run() {
                Player sender = Bukkit.getPlayerExact(senderName);
                if (sender == null) {
                    ProxyMessenger.sendResult(receiver, token, false, "§cPlayer not on this server.");
                    return;
                }
                boolean ok = sender.performCommand(commandLine);
                ProxyMessenger.sendResult(receiver, token, ok, null);
            }
        });
    }
}
