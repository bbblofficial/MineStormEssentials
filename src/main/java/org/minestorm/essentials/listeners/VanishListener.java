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
 *  - On join: if the player was in the 5-second "hiding" window, hide them
 *    from everyone again so the countdown can finish.
 *  - On join: if a player joins while another is vanished, nothing special
 *    is needed because vanish is now cosmetic (action bar only) — but we
 *    still leave the hook so future behaviour can be added.
 *  - On quit: nothing to do, state is kept in memory.
 *  - On mob target: cancel targeting of players in the "hiding" window
 *    (the 5 seconds after unvanish), so mobs don't attack them while
 *    they're temporarily invisible.
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

        // If this player was in the 5-second hiding window when they
        // disconnected, re-apply the hide so the countdown can finish.
        if (VanishCommand.isHiding(joined)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(joined)) continue;
                online.hidePlayer(joined);
            }
        }

        // Vanished players remain visible now (cosmetic vanish only).
        // If you later want to re-hide vanished players on join,
        // add that logic here.
    }

    // ============================================================
    //  QUIT
    // ============================================================
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Vanish state is kept in memory so it survives a reconnect.
        // Remove the line below if you want vanish to reset on logout.
        // VanishCommand.getVanished().remove(event.getPlayer().getUniqueId());
    }

    // ============================================================
    //  MOB TARGETING
    //  While a player is in the 5-second "hiding" window after
    //  unvanishing, stop mobs from targeting them.
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {

        if (!plugin.getConfig().getBoolean("settings.vanish.hide-from-mobs", true)) {
            return;
        }

        LivingEntity target = event.getTarget();
        if (!(target instanceof Player)) return;

        Player player = (Player) target;

        // Only cancel targeting while the player is in the hide window
        // (i.e. right after unvanishing).
        if (VanishCommand.isHiding(player)) {
            event.setCancelled(true);
            event.setTarget(null);
        }
    }
}