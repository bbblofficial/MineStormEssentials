package org.minestorm.essentials.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.minestorm.essentials.bungee.MineStormBungee;

public class ReplyCommand extends Command {

    private final MineStormBungee plugin;
    public ReplyCommand(MineStormBungee plugin) { super("reply", "minestorm.msg", "r"); this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestorm.msg")) { plugin.getMessages().send(sender, "no-permission"); return; }
        if (!(sender instanceof ProxiedPlayer)) { plugin.getMessages().send(sender, "player-only"); return; }
        if (args.length < 1) { plugin.getMessages().send(sender, "invalid-args", "%usage%", "/reply <message>"); return; }

        ProxiedPlayer player = (ProxiedPlayer) sender;
        ProxiedPlayer target = MessageStore.getReplyTarget(player);
        if (target == null) { plugin.getMessages().send(sender, "msg-no-reply"); return; }

        String msg = MsgCommand.join(args, 0);
        if (sender.hasPermission("minestorm.msg.color") && plugin.getMessages().setting("allow-msg-colors", true))
            msg = plugin.getMessages().color(msg);

        MessageStore.setReplyTarget(player, target);
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
}
