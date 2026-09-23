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
 * /flyspeed <1-10> [player]
 * Values:
 *  1  = 0.1
 *  5  = 0.5  (default)
 *  10 = 1.0
 */
public class FlySpeedCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public FlySpeedCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            messages.send(sender, "invalid-args", "%usage%", "/" + label + " <1-10> [player]");
            return true;
        }

        int speed;
        try {
            speed = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            messages.send(sender, "flyspeed-invalid");
            return true;
        }

        if (speed < 1 || speed > 10) {
            messages.send(sender, "flyspeed-invalid");
            return true;
        }

        Player target;
        boolean self;

        if (args.length >= 2) {
            if (!sender.hasPermission("minestorm.flyspeed.others")) {
                messages.send(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messages.send(sender, "player-not-found", "%player%", args[1]);
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

        if (!sender.hasPermission("minestorm.flyspeed")) {
            messages.send(sender, "no-permission");
            return true;
        }

        // In 1.8: setFlySpeed is capped at 1.0
        float value = speed / 10.0F;
        if (value > 1.0F) value = 1.0F;

        target.setFlySpeed(value);

        if (self) {
            messages.send(sender, "flyspeed-set-self", "%speed%", String.valueOf(speed));
        } else {
            messages.send(sender, "flyspeed-set-other",
                    "%player%", target.getName(),
                    "%speed%", String.valueOf(speed));
            messages.send(target, "flyspeed-notify",
                    "%speed%", String.valueOf(speed),
                    "%by%", sender.getName());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();

        if (args.length == 1) {
            String partial = args[0];
            for (int i = 1; i <= 10; i++) {
                String s = String.valueOf(i);
                if (s.startsWith(partial)) out.add(s);
            }
        } else if (args.length == 2 && sender.hasPermission("minestorm.flyspeed.others")) {
            String partial = args[1].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    out.add(p.getName());
                }
            }
        }

        return out;
    }
}
