package org.minestorm.essentials.spigot.util;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {

    private final JavaPlugin plugin;

    public MessageUtil(JavaPlugin plugin) { this.plugin = plugin; }

    public String raw(String path) {
        FileConfiguration cfg = plugin.getConfig();
        String msg = cfg.getString("messages." + path);
        return msg == null ? "" : msg;
    }

    public String get(String path) { return color(raw(path)); }

    public String get(String path, String... r) {
        String msg = raw(path);
        for (int i = 0; i + 1 < r.length; i += 2) msg = msg.replace(r[i], r[i + 1]);
        return color(msg);
    }

    public void send(CommandSender s, String path, String... r) {
        String m = get(path, r);
        if (!m.isEmpty()) s.sendMessage(m);
    }

    public void sendRaw(CommandSender s, String msg) {
        if (msg != null && !msg.isEmpty()) s.sendMessage(color(msg));
    }

    public String color(String m) {
        return m == null ? "" : ChatColor.translateAlternateColorCodes('&', m);
    }

    public boolean setting(String path, boolean def) {
        return plugin.getConfig().getBoolean("settings." + path, def);
    }
    public String setting(String path, String def) {
        return plugin.getConfig().getString("settings." + path, def);
    }
    public double setting(String path, double def) {
        return plugin.getConfig().getDouble("settings." + path, def);
    }

    @SuppressWarnings("deprecation")
    public void playSound(Player p, String name, double vol, double pitch) {
        if (p == null || name == null) return;
        try {
            Sound s = Sound.valueOf(name.toUpperCase());
            p.playSound(p.getLocation(), s, (float) vol, (float) pitch);
        } catch (IllegalArgumentException ignored) {}
    }
}
