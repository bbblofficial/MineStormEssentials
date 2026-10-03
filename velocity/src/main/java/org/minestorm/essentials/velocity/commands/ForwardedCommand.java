package org.minestorm.essentials.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.minestorm.essentials.velocity.MineStormVelocity;
import org.minestorm.essentials.velocity.ProxyForwarder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Forwards the command line to the player's current backend server via the
 * "minestorm:main" plugin-message channel. Requires the backend plugin to be
 * installed on the destination server for the command to actually execute.
 */
public class ForwardedCommand implements SimpleCommand {

    private final MineStormVelocity plugin;
    private final String name;

    public ForwardedCommand(MineStormVelocity plugin, String name) {
        this.plugin = plugin; this.name = name;
    }

    @Override
    public void execute(Invocation inv) {
        var src = inv.source();
        if (!(src instanceof Player)) { plugin.getMessages().send(src, "player-only"); return; }
        if (!src.hasPermission("minestorm." + name)) { plugin.getMessages().send(src, "no-permission"); return; }

        Player p = (Player) src;
        StringBuilder sb = new StringBuilder(name);
        for (String a : inv.arguments()) sb.append(' ').append(a);
        ProxyForwarder.forward(p, sb.toString());
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation inv) {
        List<String> out = new ArrayList<>();
        String[] args = inv.arguments();
        if (args.length == 1) {
            if (name.equals("flyspeed")) {
                for (int i = 1; i <= 10; i++) {
                    String v = String.valueOf(i);
                    if (v.startsWith(args[0])) out.add(v);
                }
            } else {
                for (Player p : plugin.getServer().getAllPlayers()) {
                    if (p.getUsername().toLowerCase().startsWith(args[0].toLowerCase()))
                        out.add(p.getUsername());
                }
            }
        } else if (args.length == 2 && name.equals("flyspeed")) {
            for (Player p : plugin.getServer().getAllPlayers()) {
                if (p.getUsername().toLowerCase().startsWith(args[1].toLowerCase()))
                    out.add(p.getUsername());
            }
        }
        return CompletableFuture.completedFuture(out);
    }
}
