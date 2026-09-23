package org.minestorm.essentials.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.util.MessageUtil;

/**
 * /fly [player]
 * Toggles flight for self or another player.
 */
public class FlyCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public FlyCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player target;
        boolean self;

        if (args.length >= 1) {
            if (!sender.hasPermission("minestorm.fly.others")) {
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

        if (!sender.hasPermission("minestorm.fly")) {
            messages.send(sender, "no-permission");
            return true;
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (!newState) {
            target.setFlying(false);
        }

        String state = newState ? "enabled" : "disabled";

        if (self) {
            messages.send(sender, newState ? "fly-enabled-self" : "fly-disabled-self");
        } else {
            messages.send(sender, newState ? "fly-enabled-other" : "fly-disabled-other",
                    "%player%", target.getName());
            messages.send(target, "fly-notify",
                    "%state%", state,
                    "%by%", sender.getName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && sender.hasPermission("minestorm.fly.others")) {
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
