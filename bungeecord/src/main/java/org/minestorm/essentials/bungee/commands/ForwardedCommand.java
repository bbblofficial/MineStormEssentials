package org.minestorm.essentials.bungee.commands;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.minestorm.essentials.bungee.MineStormBungee;
import org.minestorm.essentials.bungee.ProxyForwarder;

import java.util.ArrayList;
import java.util.List;

/**
 * A proxy command that forwards the whole line to the player's current backend
 * server via the plugin messaging channel "minestorm:main".
 * Requires the backend MineStormEssentials (Spigot/Paper jar) on the destination.
 */
public class ForwardedCommand extends Command implements TabExecutor {

    private final MineStormBungee plugin;

    public ForwardedCommand(MineStormBungee plugin, String name, String... aliases) {
        super(name, "minestorm." + name, aliases);
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            plugin.getMessages().send(sender, "player-only");
            return;
        }
        if (!sender.hasPermission("minestorm." + getName())) {
            plugin.getMessages().send(sender, "no-permission");
            return;
        }
        ProxiedPlayer p = (ProxiedPlayer) sender;

        StringBuilder sb = new StringBuilder(getName());
        for (String a : args) sb.append(' ').append(a);

        String token = ProxyForwarder.newToken();
        ProxyForwarder.forward(p, token, sb.toString());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        String name = getName().toLowerCase();
        if (args.length == 1) {
            if (name.equals("flyspeed")) {
                for (int i = 1; i <= 10; i++) {
                    String v = String.valueOf(i);
                    if (v.startsWith(args[0])) out.add(v);
                }
            } else {
                for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) out.add(p.getName());
                }
            }
        } else if (args.length == 2 && name.equals("flyspeed")) {
            for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(p.getName());
            }
        }
        return out;
    }
}
