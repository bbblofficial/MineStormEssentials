package org.minestorm.essentials.util;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {

    private final JavaPlugin plugin;

    public MessageUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public String raw(String path) {
        FileConfiguration cfg = this.plugin.getConfig();
        String msg = cfg.getString("messages." + path);
        return msg == null ? "" : msg;
    }

    public String get(String path) {
        return color(raw(path));
    }

    public String get(String path, String... replacements) {
        String msg = raw(path);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return color(msg);
    }

    public void send(CommandSender sender, String path, String... replacements) {
        String msg = get(path, replacements);
        if (!msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void sendRaw(CommandSender sender, String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(color(message));
        }
    }

    public String color(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public boolean setting(String path, boolean def) {
        return this.plugin.getConfig().getBoolean("settings." + path, def);
    }

    public String setting(String path, String def) {
        return this.plugin.getConfig().getString("settings." + path, def);
    }

    public double setting(String path, double def) {
        return this.plugin.getConfig().getDouble("settings." + path, def);
    }

    /**
     * Play a sound to a player using a configurable sound name.
     */
    @SuppressWarnings("deprecation")
    public void playSound(Player player, String soundName, double volume, double pitch) {
        if (player == null || soundName == null) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, (float) volume, (float) pitch);
        } catch (IllegalArgumentException ignored) {
            // Unknown sound name — skip silently
        }
    }
}
