package org.minestorm.essentials.spigot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.MessageUtil;

public class MsgCommand implements CommandExecutor, TabCompleter {

    private final MessageUtil messages;
    private final Map<UUID, UUID> lastReplyTarget = new HashMap<>();

    public MsgCommand(JavaPlugin p, MessageUtil m) { this.messages = m; }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] args) {
        String n = c.getName().toLowerCase();
        if (n.equals("reply") || n.equals("r")) return reply(s, args);
        return msg(s, args);
    }

    private boolean msg(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestorm.msg")) { messages.send(sender, "no-permission"); return true; }
        if (args.length < 2) { messages.send(sender, "invalid-args", "%usage%", "/msg <player> <message>"); return true; }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) { messages.send(sender, "player-not-found", "%player%", args[0]); return true; }
        if (target.equals(sender)) { messages.send(sender, "msg-self"); return true; }

        String message = join(args, 1);
        if (sender.hasPermission("minestorm.msg.color") && messages.setting("allow-msg-colors", true))
            message = messages.color(message);

        if (sender instanceof Player) {
            lastReplyTarget.put(target.getUniqueId(), ((Player) sender).getUniqueId());
            lastReplyTarget.put(((Player) sender).getUniqueId(), target.getUniqueId());
        }
        deliver(sender, target, message);
        return true;
    }

    private boolean reply(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestorm.msg")) { messages.send(sender, "no-permission"); return true; }
        if (!(sender instanceof Player)) { messages.send(sender, "player-only"); return true; }
        if (args.length < 1) { messages.send(sender, "invalid-args", "%usage%", "/reply <message>"); return true; }

        Player player = (Player) sender;
        UUID tid = lastReplyTarget.get(player.getUniqueId());
        if (tid == null) { messages.send(sender, "msg-no-reply"); return true; }
        Player target = Bukkit.getPlayer(tid);
        if (target == null) { messages.send(sender, "player-not-found", "%player%", "unknown"); return true; }

        String message = join(args, 0);
        if (sender.hasPermission("minestorm.msg.color") && messages.setting("allow-msg-colors", true))
            message = messages.color(message);

        lastReplyTarget.put(target.getUniqueId(), player.getUniqueId());
        lastReplyTarget.put(player.getUniqueId(), target.getUniqueId());

        deliver(sender, target, message);
        return true;
    }

    private void deliver(CommandSender sender, Player target, String message) {
        messages.send(sender, "msg-sender", "%receiver%", target.getName(), "%message%", message);
        messages.send(target, "msg-receiver", "%sender%", sender.getName(), "%message%", message);

        if (messages.setting("msg-sound", true)) {
            messages.playSound(target, messages.setting("msg-sound-name", "ORB_PICKUP"),
                    messages.setting("msg-sound-volume", 0.5D),
                    messages.setting("msg-sound-pitch", 1.5D));
        }
        if (messages.setting("social-spy-enabled", true)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("minestorm.msg.spy")) continue;
                if (online.equals(sender) || online.equals(target)) continue;
                messages.send(online, "social-spy",
                        "%sender%", sender.getName(), "%receiver%", target.getName(), "%message%", message);
            }
        }
    }

    private String join(String[] a, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < a.length; i++) { if (sb.length() > 0) sb.append(' '); sb.append(a[i]); }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        String n = c.getName().toLowerCase();
        if (n.equals("reply") || n.equals("r")) return out;
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.equals(s)) continue;
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
            }
        }
        return out;
    }
}
