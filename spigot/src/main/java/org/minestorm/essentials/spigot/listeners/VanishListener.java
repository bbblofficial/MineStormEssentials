package org.minestorm.essentials.spigot.listeners;

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
import org.minestorm.essentials.spigot.commands.VanishCommand;

public class VanishListener implements Listener {

    private final JavaPlugin plugin;
    public VanishListener(JavaPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player j = e.getPlayer();
        if (!j.hasPermission("minestorm.vanish.see")) {
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.equals(j)) continue;
                if (VanishCommand.isVanished(o)) j.hidePlayer(o);
            }
        }
        if (VanishCommand.isVanished(j)) {
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.equals(j)) continue;
                if (!o.hasPermission("minestorm.vanish.see")) o.hidePlayer(j);
            }
        }
        if (VanishCommand.isHiding(j)) {
            for (Player o : Bukkit.getOnlinePlayers())
                if (!o.equals(j)) o.hidePlayer(j);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { /* preserve vanish state across reconnect */ }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent e) {
        if (!plugin.getConfig().getBoolean("settings.vanish.hide-from-mobs", true)) return;
        LivingEntity t = e.getTarget();
        if (!(t instanceof Player)) return;
        Player p = (Player) t;
        if (VanishCommand.isVanished(p) || VanishCommand.isHiding(p)) {
            e.setCancelled(true);
            e.setTarget(null);
        }
    }
}
