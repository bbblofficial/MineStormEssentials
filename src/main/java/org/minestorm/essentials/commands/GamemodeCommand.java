package org.minestorm.essentials.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.util.MessageUtil;

/**
 * Handles /gmc, /gms, /gmsp, /gma
 */
public class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public GamemodeCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        GameMode mode = modeFromLabel(label);
        if (mode == null) {
            messages.sendRaw(sender, "&cUnknown gamemode command.");
            return true;
        }

        Player target;
        boolean self;

        if (args.length >= 1) {
            if (!sender.hasPermission("minestorm.gamemode.others")) {
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

        if (!sender.hasPermission("minestorm.gamemode")) {
            messages.send(sender, "no-permission");
            return true;
        }

        target.setGameMode(mode);

        String modeName = mode.name();

        if (self) {
            messages.send(sender, "gamemode-changed-self", "%mode%", modeName);
        } else {
            messages.send(sender, "gamemode-changed-other",
                    "%player%", target.getName(),
                    "%mode%", modeName);
            messages.send(target, "gamemode-notify",
                    "%mode%", modeName,
                    "%by%", sender.getName());
        }

        return true;
    }

    private GameMode modeFromLabel(String label) {
        String l = label.toLowerCase();
        if (l.equals("gmc") || l.equals("gamemodecreative") || l.equals("creative"))
            return GameMode.CREATIVE;
        if (l.equals("gms") || l.equals("gamemodesurvival") || l.equals("survival"))
            return GameMode.SURVIVAL;
        if (l.equals("gmsp") || l.equals("gamemodespectator") || l.equals("spectator"))
            return GameMode.SPECTATOR;
        if (l.equals("gma") || l.equals("gamemodeadventure") || l.equals("adventure"))
            return GameMode.ADVENTURE;
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && sender.hasPermission("minestorm.gamemode.others")) {
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
