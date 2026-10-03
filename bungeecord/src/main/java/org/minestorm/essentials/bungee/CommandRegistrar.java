package org.minestorm.essentials.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.PluginManager;
import org.minestorm.essentials.bungee.commands.ForwardedCommand;
import org.minestorm.essentials.bungee.commands.MsgCommand;
import org.minestorm.essentials.bungee.commands.ReplyCommand;
import org.minestorm.essentials.bungee.commands.SocialSpyCommand;

public class CommandRegistrar {

    private final MineStormBungee plugin;
    public CommandRegistrar(MineStormBungee plugin) { this.plugin = plugin; }

    public void register() {
        PluginManager pm = ProxyServer.getInstance().getPluginManager();

        pm.registerCommand(plugin, new MsgCommand(plugin));
        pm.registerCommand(plugin, new ReplyCommand(plugin));
        pm.registerCommand(plugin, new SocialSpyCommand(plugin));

        pm.registerCommand(plugin, new ForwardedCommand(plugin, "gmc"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "gms"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "gmsp"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "gma"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "fly"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "flyspeed", "fspeed", "fs"));
        pm.registerCommand(plugin, new ForwardedCommand(plugin, "vanish", "v"));
    }
}
