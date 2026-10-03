package org.minestorm.essentials.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.minestorm.essentials.velocity.MineStormVelocity;

import java.util.Optional;
import java.util.UUID;

public class ReplyCommand implements SimpleCommand {

    private final MineStormVelocity plugin;
    public ReplyCommand(MineStormVelocity plugin) { this.plugin = plugin; }

    @Override
    public void execute(Invocation inv) {
        var src = inv.source();
        String[] args = inv.arguments();

        if (!plugin.getMessages().can(src, "minestorm.msg")) { plugin.getMessages().send(src, "no-permission"); return; }
        if (!(src instanceof Player)) { plugin.getMessages().send(src, "player-only"); return; }
        if (args.length < 1) { plugin.getMessages().send(src, "invalid-args", "%usage%", "/reply <message>"); return; }

        Player player = (Player) src;
        UUID targetId = MsgCommand.REPLIES.get(player.getUniqueId());
        if (targetId == null) { plugin.getMessages().send(src, "msg-no-reply"); return; }

        Optional<Player> targetOpt = plugin.getServer().getPlayer(targetId);
        if (!targetOpt.isPresent()) { plugin.getMessages().send(src, "player-not-found", "%player%", "unknown"); return; }
        Player target = targetOpt.get();

        String message = MsgCommand.join(args, 0);
        if (plugin.getMessages().can(src, "minestorm.msg.color")
                && plugin.getMessages().setting("allow-msg-colors", true)) {
            message = plugin.getMessages().color(message);
        }

        MsgCommand.REPLIES.put(target.getUniqueId(), player.getUniqueId());
        MsgCommand.REPLIES.put(player.getUniqueId(), target.getUniqueId());

        MsgCommand.deliver(src, target, message);
    }
}
