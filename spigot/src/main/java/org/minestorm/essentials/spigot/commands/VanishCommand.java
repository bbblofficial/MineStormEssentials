package org.minestorm.essentials.spigot.commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.minestorm.essentials.spigot.util.MessageUtil;

public class VanishCommand implements CommandExecutor, TabCompleter {

    private static final Set<UUID> vanished = new HashSet<>();
    private static final Set<UUID> hiding = new HashSet<>();
    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public VanishCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin; this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!messages.setting("vanish.enabled", true)) { messages.sendRaw(sender, "&cVanish disabled."); return true; }

        Player target; boolean self;
        if (args.length >= 1) {
            if (!sender.hasPermission("minestorm.vanish.others")) { messages.send(sender, "no-permission"); return true; }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) { messages.send(sender, "player-not-found", "%player%", args[0]); return true; }
            self = sender.equals(target);
        } else {
            if (!(sender instanceof Player)) { messages.send(sender, "player-only"); return true; }
            target = (Player) sender; self = true;
        }
        if (!sender.hasPermission("minestorm.vanish")) { messages.send(sender, "no-permission"); return true; }

        boolean newState = !vanished.contains(target.getUniqueId());
        if (newState) enableVanish(target); else disableVanish(target);

        String state = newState ? "enabled" : "disabled";
        if (self) messages.send(sender, newState ? "vanish-enabled-self" : "vanish-disabled-self");
        else {
            messages.send(sender, newState ? "vanish-enabled-other" : "vanish-disabled-other", "%player%", target.getName());
            messages.send(target, "vanish-notify", "%state%", state, "%by%", sender.getName());
        }
        return true;
    }

    private void enableVanish(Player player) {
        vanished.add(player.getUniqueId());
        hiding.remove(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (online.hasPermission("minestorm.vanish.see")) online.showPlayer(player);
            else online.hidePlayer(player);
        }
        showActionBar(player, "&fYou are currently &c&lVANISHED MODE");
        startActionBarLoop(player);
    }

    private void disableVanish(Player player) {
        vanished.remove(player.getUniqueId());
        hiding.add(player.getUniqueId());
        for (Player online : Bukkit.getOnlinePlayers())
            if (!online.equals(player)) online.hidePlayer(player);

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); return; }
                showActionBar(player, "&fYou are currently &a&lNORMAL MODE");
                ticks += 10;
                if (ticks >= 100) {
                    hiding.remove(player.getUniqueId());
                    for (Player online : Bukkit.getOnlinePlayers())
                        if (!online.equals(player)) online.showPlayer(player);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    private void startActionBarLoop(final Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                if (!player.isOnline() || !vanished.contains(player.getUniqueId())) { cancel(); return; }
                showActionBar(player, "&fYou are currently &c&lVANISHED MODE");
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void showActionBar(Player player, String message) {
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        try {
            Object cc = Class.forName("net.minecraft.server.v1_8_R3.ChatComponentText")
                    .getConstructor(String.class).newInstance(colored);
            Object pkt = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat")
                    .getConstructor(Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent"), byte.class)
                    .newInstance(cc, (byte) 2);
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object conn = handle.getClass().getField("playerConnection").get(handle);
            conn.getClass().getMethod("sendPacket", Class.forName("net.minecraft.server.v1_8_R3.Packet"))
                    .invoke(conn, pkt);
        } catch (Throwable t) {
            player.sendMessage(colored);
        }
    }

    public static boolean isVanished(Player p) { return vanished.contains(p.getUniqueId()); }
    public static boolean isHiding(Player p) { return hiding.contains(p.getUniqueId()); }
    public static Set<UUID> getVanished() { return vanished; }
    public static Set<UUID> getHiding() { return hiding; }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1 && s.hasPermission("minestorm.vanish.others")) {
            String p = args[0].toLowerCase();
            for (Player pl : Bukkit.getOnlinePlayers())
                if (pl.getName().toLowerCase().startsWith(p)) out.add(pl.getName());
        }
        return out;
    }
}
