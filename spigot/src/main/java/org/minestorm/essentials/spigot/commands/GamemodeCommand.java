package org.minestorm.essentials.spigot.commands;

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
import org.minestorm.essentials.spigot.util.MessageUtil;

public class GamemodeCommand implements CommandExecutor, TabCompleter {

    private final MessageUtil messages;
    public GamemodeCommand(JavaPlugin p, MessageUtil m) { this.messages = m; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GameMode mode = modeFromLabel(label);
        if (mode == null) { messages.sendRaw(sender, "&cUnknown gamemode."); return true; }

        Player target; boolean self;
        if (args.length >= 1) {
            if (!messages.can(sender, "minestorm.gamemode.others")) { messages.send(sender, "no-permission"); return true; }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { messages.send(sender, "player-not-found", "%player%", args[0]); return true; }
            self = sender.equals(target);
        } else {
            if (!(sender instanceof Player)) { messages.send(sender, "player-only"); return true; }
            target = (Player) sender; self = true;
        }
        if (!messages.can(sender, "minestorm.gamemode")) { messages.send(sender, "no-permission"); return true; }

        target.setGameMode(mode);
        String modeName = mode.name();
        if (self) messages.send(sender, "gamemode-changed-self", "%mode%", modeName);
        else {
            messages.send(sender, "gamemode-changed-other", "%player%", target.getName(), "%mode%", modeName);
            messages.send(target, "gamemode-notify", "%mode%", modeName, "%by%", sender.getName());
        }
        return true;
    }

    private GameMode modeFromLabel(String l) {
        l = l.toLowerCase();
        if (l.equals("gmc")) return GameMode.CREATIVE;
        if (l.equals("gms")) return GameMode.SURVIVAL;
        if (l.equals("gmsp")) return GameMode.SPECTATOR;
        if (l.equals("gma")) return GameMode.ADVENTURE;
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1 && messages.can(s, "minestorm.gamemode.others")) {
            String p = args[0].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers())
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        }
        return out;
    }
}
