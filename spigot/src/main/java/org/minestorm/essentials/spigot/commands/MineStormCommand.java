package org.minestorm.essentials.spigot.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.MessageUtil;

/**
 * /minestormessentials — diagnostic command open to everyone.
 */
public class MineStormCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public MineStormCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        sender.sendMessage(ChatColor.YELLOW + "  MineStorm Essentials v" + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "  Author: Muvixo");
        sender.sendMessage(ChatColor.GRAY + "  Permissions enforced: " + ChatColor.WHITE
                + messages.permsEnabled());
        sender.sendMessage(ChatColor.GRAY + "  Your permission level: " + ChatColor.WHITE
                + (sender.isOp() ? "OP" : "Player"));
        sender.sendMessage(ChatColor.GRAY + "  Commands: /gmc /gms /gmsp /gma /fly /flyspeed /msg /reply /vanish");
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        return true;
    }
}
