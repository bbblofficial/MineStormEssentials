package org.minestorm.essentials.spigot;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.commands.FlyCommand;
import org.minestorm.essentials.spigot.commands.FlySpeedCommand;
import org.minestorm.essentials.spigot.commands.GamemodeCommand;
import org.minestorm.essentials.spigot.commands.MsgCommand;
import org.minestorm.essentials.spigot.commands.VanishCommand;
import org.minestorm.essentials.spigot.listeners.VanishListener;
import org.minestorm.essentials.spigot.util.MessageUtil;

public class CommandManager {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new MessageUtil(plugin);
    }

    public void registerAll() {
        GamemodeCommand gm = new GamemodeCommand(plugin, messages);
        register("gmc", gm); register("gms", gm); register("gmsp", gm); register("gma", gm);

        register("fly", new FlyCommand(plugin, messages));
        register("flyspeed", new FlySpeedCommand(plugin, messages));

        MsgCommand msg = new MsgCommand(plugin, messages);
        register("msg", msg); register("reply", msg);

        register("vanish", new VanishCommand(plugin, messages));

        plugin.getServer().getPluginManager().registerEvents(new VanishListener(plugin), plugin);
    }

    private void register(String name, Object executor) {
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) { plugin.getLogger().warning("Command not in plugin.yml: " + name); return; }
        if (executor instanceof CommandExecutor) cmd.setExecutor((CommandExecutor) executor);
        if (executor instanceof TabCompleter) cmd.setTabCompleter((TabCompleter) executor);
    }

    public MessageUtil getMessages() { return messages; }
}
