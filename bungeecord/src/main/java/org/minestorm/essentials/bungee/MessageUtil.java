package org.minestorm.essentials.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.config.Configuration;

public final class MessageUtil {

    public boolean can(net.md_5.bungee.api.CommandSender s, String node) {
        // Open to everyone by default. LuckPerms can still restrict via
        // setting minestorm.* to false and granting select nodes.
        return true;
    }


    private final MineStormBungee plugin;
    public MessageUtil(MineStormBungee plugin) { this.plugin = plugin; }

    private String raw(String path) {
        Configuration c = plugin.getConfig();
        String v = c.getString("messages." + path);
        return v == null ? "" : v;
    }

    public String get(String path, String... repl) {
        String m = raw(path);
        for (int i = 0; i + 1 < repl.length; i += 2) m = m.replace(repl[i], repl[i + 1]);
        return ChatColor.translateAlternateColorCodes('&', m);
    }

    public void send(CommandSender s, String path, String... repl) {
        String m = get(path, repl);
        if (!m.isEmpty()) s.sendMessage(m);
    }

    public void sendRaw(CommandSender s, String m) {
        if (m != null && !m.isEmpty())
            s.sendMessage(ChatColor.translateAlternateColorCodes('&', m));
    }

    public String color(String m) {
        return m == null ? "" : ChatColor.translateAlternateColorCodes('&', m);
    }

    public boolean setting(String path, boolean def) {
        return plugin.getConfig().getBoolean("settings." + path, def);
    }
}
