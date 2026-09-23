package org.minestorm.essentials;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.commands.FlyCommand;
import org.minestorm.essentials.commands.FlySpeedCommand;
import org.minestorm.essentials.commands.GamemodeCommand;
import org.minestorm.essentials.commands.MsgCommand;
import org.minestorm.essentials.commands.VanishCommand;
import org.minestorm.essentials.listeners.VanishListener;
import org.minestorm.essentials.util.MessageUtil;

public class CommandManager {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public CommandManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messages = new MessageUtil(plugin);
    }

    public void registerAll() {
        GamemodeCommand gm = new GamemodeCommand(this.plugin, this.messages);
        register("gmc", gm);
        register("gms", gm);
        register("gmsp", gm);
        register("gma", gm);

        FlyCommand fly = new FlyCommand(this.plugin, this.messages);
        register("fly", fly);

        FlySpeedCommand fs = new FlySpeedCommand(this.plugin, this.messages);
        register("flyspeed", fs);

        MsgCommand msg = new MsgCommand(this.plugin, this.messages);
        register("msg", msg);
        register("reply", msg);

        VanishCommand vanish = new VanishCommand(this.plugin, this.messages);
        register("vanish", vanish);

        // Register listeners
        this.plugin.getServer().getPluginManager()
                .registerEvents(new VanishListener(this.plugin), this.plugin);
    }

    private void register(String name, Object executor) {
        PluginCommand cmd = this.plugin.getCommand(name);
        if (cmd == null) {
            this.plugin.getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        if (executor instanceof CommandExecutor) {
            cmd.setExecutor((CommandExecutor) executor);
        }
        if (executor instanceof TabCompleter) {
            cmd.setTabCompleter((TabCompleter) executor);
        }
    }

    public MessageUtil getMessages() {
        return this.messages;
    }
}
