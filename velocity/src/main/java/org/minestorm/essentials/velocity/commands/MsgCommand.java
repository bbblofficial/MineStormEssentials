package org.minestorm.essentials.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.minestorm.essentials.velocity.MineStormVelocity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class MsgCommand implements SimpleCommand {

    public static final Map<UUID, UUID> REPLIES = new HashMap<>();
    private final MineStormVelocity plugin;

    public MsgCommand(MineStormVelocity plugin) { this.plugin = plugin; }

    @Override
    public void execute(Invocation inv) {
        var src = inv.source();
        String[] args = inv.arguments();

        if (!src.hasPermission("minestorm.msg")) { plugin.getMessages().send(src, "no-permission"); return; }
        if (args.length < 2) { plugin.getMessages().send(src, "invalid-args", "%usage%", "/msg <player> <message>"); return; }

        Optional<Player> targetOpt = plugin.getServer().getPlayer(args[0]);
        if (!targetOpt.isPresent()) { plugin.getMessages().send(src, "player-not-found", "%player%", args[0]); return; }
        Player target = targetOpt.get();

        if (src instanceof Player && ((Player) src).getUniqueId().equals(target.getUniqueId())) {
            plugin.getMessages().send(src, "msg-self"); return;
        }

        String message = join(args, 1);
        if (src.hasPermission("minestorm.msg.color")
                && plugin.getMessages().setting("allow-msg-colors", true)) {
            message = plugin.getMessages().color(message);
        }

        if (src instanceof Player) {
            REPLIES.put(target.getUniqueId(), ((Player) src).getUniqueId());
            REPLIES.put(((Player) src).getUniqueId(), target.getUniqueId());
        }

        deliver(src, target, message);
    }

    static void deliver(com.velocitypowered.api.command.CommandSource src, Player target, String message) {
        MineStormVelocity plugin = org.minestorm.essentials.velocity.MineStormVelocityHolder.get();
        plugin.getMessages().send(src, "msg-sender", "%receiver%", target.getUsername(), "%message%", message);
        plugin.getMessages().send(target, "msg-receiver", "%sender%", nameOf(src), "%message%", message);

        if (plugin.getMessages().setting("social-spy-enabled", true)) {
            for (Player sp : plugin.getServer().getAllPlayers()) {
                if (!sp.hasPermission("minestorm.msg.spy")) continue;
                if (sp.getUsername().equals(nameOf(src)) || sp.equals(target)) continue;
                plugin.getMessages().send(sp, "social-spy",
                        "%sender%", nameOf(src),
                        "%receiver%", target.getUsername(),
                        "%message%", message);
            }
        }
    }

    static String nameOf(com.velocitypowered.api.command.CommandSource src) {
        return (src instanceof Player) ? ((Player) src).getUsername() : "CONSOLE";
    }

    static String join(String[] a, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < a.length; i++) { if (sb.length() > 0) sb.append(' '); sb.append(a[i]); }
        return sb.toString();
    }
}
