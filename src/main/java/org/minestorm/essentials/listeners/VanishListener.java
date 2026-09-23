package org.minestorm.essentials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.commands.VanishCommand;

/**
 * Ensures vanished players stay hidden when players join.
 */
public class VanishListener implements Listener {

    private final JavaPlugin plugin;

    public VanishListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        // Hide all currently vanished players from the joining player
        // (unless they have see permission)
        if (!joined.hasPermission("minestorm.vanish.see")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                if (VanishCommand.isVanished(online)) {
                    joined.hidePlayer(online);
                }
            }
        }

        // If the joining player is vanished, hide them from everyone
        // who can't see vanished players
        if (VanishCommand.isVanished(joined)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                if (online.hasPermission("minestorm.vanish.see")) continue;
                online.hidePlayer(joined);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Keep vanish state in memory; will be reapplied on next join.
        // Optionally remove from set if you want vanish to reset on logout:
        // VanishCommand.getVanished().remove(event.getPlayer().getUniqueId());
    }
}
