package org.minestorm.essentials.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.commands.VanishCommand;

/**
 * Ensures vanished players stay hidden when players join,
 * and prevents mobs from targeting vanished players.
 */
public class VanishListener implements Listener {

    private final JavaPlugin plugin;

    public VanishListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        if (!joined.hasPermission("minestorm.vanish.see")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                if (VanishCommand.isVanished(online)) {
                    joined.hidePlayer(online);
                }
            }
        }

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
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {

        if (!plugin.getConfig().getBoolean("settings.vanish.hide-from-mobs", true)) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (!(target instanceof Player)) return;

        Player player = (Player) target;
        if (VanishCommand.isVanished(player)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }
}