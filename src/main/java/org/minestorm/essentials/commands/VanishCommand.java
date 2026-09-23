package org.minestorm.essentials.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.util.MessageUtil;

/**
 * /vanish [player]
 * Toggles vanish mode. Vanished players are hidden from players
 * without the minestorm.vanish.see permission.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    public static final String META_VANISHED = "minestorm.vanished";

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    private static final Set<UUID> vanished = new HashSet<UUID>();

    public VanishCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!messages.setting("vanish.enabled", true)) {
            messages.sendRaw(sender, "&cVanish is disabled in the config.");
            return true;
        }

        Player target;
        boolean self;

        if (args.length >= 1) {
            if (!sender.hasPermission("minestorm.vanish.others")) {
                messages.send(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messages.send(sender, "player-not-found", "%player%", args[0]);
                return true;
            }
            self = sender.equals(target);
        } else {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            self = true;
        }

        if (!sender.hasPermission("minestorm.vanish")) {
            messages.send(sender, "no-permission");
            return true;
        }

        boolean newState = !vanished.contains(target.getUniqueId());
        setVanished(this.plugin, target, newState);

        String state = newState ? "enabled" : "disabled";

        if (self) {
            messages.send(sender, newState ? "vanish-enabled-self" : "vanish-disabled-self");
        } else {
            messages.send(sender, newState ? "vanish-enabled-other" : "vanish-disabled-other",
                    "%player%", target.getName());
            messages.send(target, "vanish-notify",
                    "%state%", state,
                    "%by%", sender.getName());
        }

        return true;
    }

    /**
     * Apply or remove vanish state for a player.
     * Uses metadata instead of setSilent() (which doesn't exist in 1.8.8).
     */
    public static void setVanished(JavaPlugin plugin, Player player, boolean state) {
        if (state) {
            vanished.add(player.getUniqueId());

            // Set metadata so other listeners can detect vanish state
            player.setMetadata(META_VANISHED,
                    new FixedMetadataValue(plugin, Boolean.TRUE));

            // Hide from players who can't see vanished players
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                if (online.hasPermission("minestorm.vanish.see")) continue;
                online.hidePlayer(player);
            }
        } else {
            vanished.remove(player.getUniqueId());

            player.removeMetadata(META_VANISHED, plugin);

            // Show to everyone
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                online.showPlayer(player);
            }
        }
    }

    public static boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public static Set<UUID> getVanished() {
        return vanished;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && sender.hasPermission("minestorm.vanish.others")) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}