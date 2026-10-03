package org.minestorm.essentials.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import org.minestorm.essentials.bungee.MineStormBungee;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SocialSpyCommand extends Command {

    public static final Set<UUID> SPY = new HashSet<>();
    private final MineStormBungee plugin;

    public SocialSpyCommand(MineStormBungee plugin) { super("socialspy", "minestorm.msg.spy", "spy"); this.plugin = plugin; }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) { plugin.getMessages().send(sender, "player-only"); return; }
        ProxiedPlayer p = (ProxiedPlayer) sender;
        if (SPY.contains(p.getUniqueId())) {
            SPY.remove(p.getUniqueId());
            plugin.getMessages().send(sender, "spy-disabled");
        } else {
            SPY.add(p.getUniqueId());
            plugin.getMessages().send(sender, "spy-enabled");
        }
    }

    public static boolean isSpying(ProxiedPlayer p) { return SPY.contains(p.getUniqueId()); }
}
