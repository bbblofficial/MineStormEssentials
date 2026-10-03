package org.minestorm.essentials.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.minestorm.essentials.bungee.MineStormBungee;

public class MsgCommand extends Command {

    private final MineStormBungee plugin;
    public MsgCommand(MineStormBungee plugin) {
        super("msg", "minestorm.msg", "tell", "whisper", "w", "m", "pm");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestorm.msg")) { plugin.getMessages().send(sender, "no-permission"); return; }
        if (args.length < 2) { plugin.getMessages().send(sender, "invalid-args", "%usage%", "/msg <player> <message>"); return; }

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        if (target == null) { plugin.getMessages().send(sender, "player-not-found", "%player%", args[0]); return; }
        if (target.equals(sender)) { plugin.getMessages().send(sender, "msg-self"); return; }

        String msg = join(args, 1);
        if (sender.hasPermission("minestorm.msg.color") && plugin.getMessages().setting("allow-msg-colors", true))
            msg = plugin.getMessages().color(msg);

        if (sender instanceof ProxiedPlayer)
            MessageStore.setReplyTarget((ProxiedPlayer) sender, target);

        plugin.getMessages().send(sender, "msg-sender", "%receiver%", target.getName(), "%message%", msg);
        plugin.getMessages().send(target, "msg-receiver", "%sender%", sender.getName(), "%message%", msg);

        if (plugin.getMessages().setting("social-spy-enabled", true)) {
            for (ProxiedPlayer sp : ProxyServer.getInstance().getPlayers()) {
                if (!sp.hasPermission("minestorm.msg.spy")) continue;
                if (sp.equals(sender) || sp.equals(target)) continue;
                plugin.getMessages().send(sp, "social-spy",
                        "%sender%", sender.getName(), "%receiver%", target.getName(), "%message%", msg);
            }
        }
    }

    static String join(String[] a, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < a.length; i++) { if (sb.length() > 0) sb.append(' '); sb.append(a[i]); }
        return sb.toString();
    }
}
