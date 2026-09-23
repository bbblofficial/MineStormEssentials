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
 * Vanish listener.
 *
 * Responsibilities:
 *  - On join: hide already-vanished players from the newcomer
 *             (unless the newcomer has minestorm.vanish.see).
 *  - On join: if the newcomer is themselves vanished, hide them
 *             from everyone who cannot see vanished players.
 *  - On join: if the newcomer was in the 5s hide window when they
 *             disconnected, re-apply the hide.
 *  - On mob target: cancel targeting during the 5s hide window.
 */
public class VanishListener implements Listener {

    private final JavaPlugin plugin;

    public VanishListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    //  JOIN
    // ============================================================
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        // 1) Hide all currently-vanished players from the newcomer,
        //    unless the newcomer has the see permission.
        if (!joined.hasPermission("minestorm.vanish.see")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                if (VanishCommand.isVanished(online)) {
                    joined.hidePlayer(online);
                }
            }
        }

        // 2) If the newcomer is themselves vanished, hide them from
        //    everyone who cannot see vanished players.
        if (VanishCommand.isVanished(joined)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                if (online.hasPermission("minestorm.vanish.see")) continue;
                online.hidePlayer(joined);
            }
        }

        // 3) If the newcomer was in the 5-second hide window when they
        //    disconnected, re-apply the hide.
        if (VanishCommand.isHiding(joined)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                online.hidePlayer(joined);
            }
        }
    }

    // ============================================================
    //  QUIT
    // ============================================================
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // State is kept in memory across reconnects.
        // Uncomment the line below if you want vanish to reset on logout:
        // VanishCommand.getVanished().remove(event.getPlayer().getUniqueId());
    }

    // ============================================================
    //  MOB TARGETING
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {

        if (!plugin.getConfig().getBoolean("settings.vanish.hide-from-mobs", true)) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (!(target instanceof Player)) return;

        Player player = (Player) target;

        if (VanishCommand.isVanished(player) || VanishCommand.isHiding(player)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }
}