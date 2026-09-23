package org.minestorm.essentials.commands;

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
import org.minestorm.essentials.util.MessageUtil;

/**
 * /msg <player> <message>
 * /reply <message>
 *
 * Also handles social spy for staff.
 */
public class MsgCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    // lastMessageSender[receiver] = sender
    private final Map<UUID, UUID> lastReplyTarget = new HashMap<UUID, UUID>();

    public MsgCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String cmd = command.getName().toLowerCase();

        if (cmd.equals("reply") || cmd.equals("r")) {
            return handleReply(sender, args);
        }
        return handleMsg(sender, args);
    }

    // ============================================================
    //  /msg
    // ============================================================
    private boolean handleMsg(CommandSender sender, String[] args) {

        if (!sender.hasPermission("minestorm.msg")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            messages.send(sender, "invalid-args", "%usage%", "/msg <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            messages.send(sender, "player-not-found", "%player%", args[0]);
            return true;
        }

        if (target.equals(sender)) {
            messages.send(sender, "msg-self");
            return true;
        }

        String message = joinArgs(args, 1);
        if (sender.hasPermission("minestorm.msg.color")
                && messages.setting("allow-msg-colors", true)) {
            message = messages.color(message);
        }

        // Store reply targets
        if (sender instanceof Player) {
            this.lastReplyTarget.put(target.getUniqueId(), ((Player) sender).getUniqueId());
            this.lastReplyTarget.put(((Player) sender).getUniqueId(), target.getUniqueId());
        }

        String senderName = sender.getName();
        String receiverName = target.getName();

        // Send to sender
        messages.send(sender, "msg-sender",
                "%receiver%", receiverName,
                "%message%", message);

        // Send to receiver
        messages.send(target, "msg-receiver",
                "%sender%", senderName,
                "%message%", message);

        // Play sound to receiver
        if (messages.setting("msg-sound", true)) {
            String soundName = messages.setting("msg-sound-name", "ORB_PICKUP");
            double volume = messages.setting("msg-sound-volume", 0.5D);
            double pitch = messages.setting("msg-sound-pitch", 1.5D);
            messages.playSound(target, soundName, volume, pitch);
        }

        // Social spy
        broadcastSocialSpy(sender, target, message);

        return true;
    }

    // ============================================================
    //  /reply
    // ============================================================
    private boolean handleReply(CommandSender sender, String[] args) {

        if (!sender.hasPermission("minestorm.msg")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (!(sender instanceof Player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (args.length < 1) {
            messages.send(sender, "invalid-args", "%usage%", "/reply <message>");
            return true;
        }

        Player player = (Player) sender;
        UUID targetId = this.lastReplyTarget.get(player.getUniqueId());

        if (targetId == null) {
            messages.send(sender, "msg-no-reply");
            return true;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            messages.send(sender, "player-not-found", "%player%", "unknown");
            return true;
        }

        String message = joinArgs(args, 0);
        if (sender.hasPermission("minestorm.msg.color")
                && messages.setting("allow-msg-colors", true)) {
            message = messages.color(message);
        }

        // Refresh reply mapping
        this.lastReplyTarget.put(target.getUniqueId(), player.getUniqueId());
        this.lastReplyTarget.put(player.getUniqueId(), target.getUniqueId());

        String senderName = sender.getName();
        String receiverName = target.getName();

        messages.send(sender, "msg-sender",
                "%receiver%", receiverName,
                "%message%", message);

        messages.send(target, "msg-receiver",
                "%sender%", senderName,
                "%message%", message);

        if (messages.setting("msg-sound", true)) {
            String soundName = messages.setting("msg-sound-name", "ORB_PICKUP");
            double volume = messages.setting("msg-sound-volume", 0.5D);
            double pitch = messages.setting("msg-sound-pitch", 1.5D);
            messages.playSound(target, soundName, volume, pitch);
        }

        broadcastSocialSpy(sender, target, message);

        return true;
    }

    // ============================================================
    //  SOCIAL SPY
    // ============================================================
    private void broadcastSocialSpy(CommandSender sender, Player receiver, String message) {

        if (!messages.setting("social-spy-enabled", true)) return;

        String senderName = sender.getName();
        String receiverName = receiver.getName();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("minestorm.msg.spy")) continue;
            if (online.equals(sender) || online.equals(receiver)) continue;

            messages.send(online, "social-spy",
                    "%sender%", senderName,
                    "%receiver%", receiverName,
                    "%message%", message);
        }
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {

        List<String> out = new ArrayList<String>();
        String cmd = command.getName().toLowerCase();

        if (cmd.equals("reply") || cmd.equals("r")) {
            return out;
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(sender)) continue;
                if (p.getName().toLowerCase().startsWith(partial)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
