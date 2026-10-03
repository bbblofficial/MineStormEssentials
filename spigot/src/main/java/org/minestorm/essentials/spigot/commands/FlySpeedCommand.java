package org.minestorm.essentials.spigot.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.MessageUtil;

public class FlySpeedCommand implements CommandExecutor, TabCompleter {

    private final MessageUtil messages;
    public FlySpeedCommand(JavaPlugin p, MessageUtil m) { this.messages = m; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) { messages.send(sender, "invalid-args", "%usage%", "/" + label + " <1-10> [player]"); return true; }
        int speed;
        try { speed = Integer.parseInt(args[0]); }
        catch (NumberFormatException e) { messages.send(sender, "flyspeed-invalid"); return true; }
        if (speed < 1 || speed > 10) { messages.send(sender, "flyspeed-invalid"); return true; }

        Player target; boolean self;
        if (args.length >= 2) {
            if (!messages.can(sender, "minestorm.flyspeed.others")) { messages.send(sender, "no-permission"); return true; }
            target = Bukkit.getPlayer(args[1]);
            if (target == null) { messages.send(sender, "player-not-found", "%player%", args[1]); return true; }
            self = sender.equals(target);
        } else {
            if (!(sender instanceof Player)) { messages.send(sender, "player-only"); return true; }
            target = (Player) sender; self = true;
        }
        if (!messages.can(sender, "minestorm.flyspeed")) { messages.send(sender, "no-permission"); return true; }

        float value = speed / 10.0F;
        if (value > 1.0F) value = 1.0F;
        target.setFlySpeed(value);

        if (self) messages.send(sender, "flyspeed-set-self", "%speed%", String.valueOf(speed));
        else {
            messages.send(sender, "flyspeed-set-other", "%player%", target.getName(), "%speed%", String.valueOf(speed));
            messages.send(target, "flyspeed-notify", "%speed%", String.valueOf(speed), "%by%", sender.getName());
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (int i = 1; i <= 10; i++) {
                String v = String.valueOf(i);
                if (v.startsWith(args[0])) out.add(v);
            }
        } else if (args.length == 2 && messages.can(s, "minestorm.flyspeed.others")) {
            String p = args[1].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers())
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        }
        return out;
    }
}
