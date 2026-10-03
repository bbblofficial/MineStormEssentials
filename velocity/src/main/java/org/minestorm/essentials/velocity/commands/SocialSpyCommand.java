package org.minestorm.essentials.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.minestorm.essentials.velocity.MineStormVelocity;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SocialSpyCommand implements SimpleCommand {

    public static final Set<UUID> SPY = new HashSet<>();
    private final MineStormVelocity plugin;

    public SocialSpyCommand(MineStormVelocity plugin) { this.plugin = plugin; }

    @Override
    public void execute(Invocation inv) {
        var src = inv.source();
        if (!(src instanceof Player)) { plugin.getMessages().send(src, "player-only"); return; }
        Player p = (Player) src;
        if (SPY.contains(p.getUniqueId())) {
            SPY.remove(p.getUniqueId());
            plugin.getMessages().send(src, "spy-disabled");
        } else {
            SPY.add(p.getUniqueId());
            plugin.getMessages().send(src, "spy-enabled");
        }
    }
}
