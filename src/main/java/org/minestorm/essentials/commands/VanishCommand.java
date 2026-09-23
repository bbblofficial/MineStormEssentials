package org.minestorm.essentials.commands;

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
import org.minestorm.essentials.util.MessageUtil;

/**
 * /vanish [player]
 *
 * Vanish ON  -> hidden from players WITHOUT minestorm.vanish.see
 *               visible to players WITH minestorm.vanish.see
 *               action bar shows "VANISHED MODE"
 *
 * Vanish OFF -> hidden from EVERYONE for 5 seconds, then re-shown.
 *               action bar shows "NORMAL MODE" for those 5 seconds.
 */
public class VanishCommand implements CommandExecutor, TabCompleter {

    private static final Set<UUID> vanished = new HashSet<UUID>();
    private static final Set<UUID> hiding   = new HashSet<UUID>();

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public VanishCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!messages.setting("vanish.enabled", true)) {
            messages.sendRaw(sender, "&cVanish is disabled in the config.");
            return true;
        }

        Player target;
        boolean self;

        if (args.length >= 1) {
            if (!sender.hasPermission("minestorm.vanish.others")) {
                messages.send(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                messages.send(sender, "player-not-found", "%player%", args[0]);
                return true;
            }
            self = sender.equals(target);
        } else {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            self = true;
        }

        if (!sender.hasPermission("minestorm.vanish")) {
            messages.send(sender, "no-permission");
            return true;
        }

        boolean newState = !vanished.contains(target.getUniqueId());

        if (newState) {
            enableVanish(this.plugin, target);
        } else {
            disableVanishWithDelay(this.plugin, target);
        }

        String state = newState ? "enabled" : "disabled";

        if (self) {
            messages.send(sender, newState ? "vanish-enabled-self" : "vanish-disabled-self");
        } else {
            messages.send(sender, newState ? "vanish-enabled-other" : "vanish-disabled-other",
                    "%player%", target.getName());
            messages.send(target, "vanish-notify",
                    "%state%", state,
                    "%by%", sender.getName());
        }

        return true;
    }

    // ============================================================
    //  ENABLE VANISH
    // ============================================================
    private void enableVanish(JavaPlugin plugin, Player player) {
        vanished.add(player.getUniqueId());
        hiding.remove(player.getUniqueId());

        // Hide from everyone who does NOT have the see permission
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (online.hasPermission("minestorm.vanish.see")) {
                // Staff can still see them
                online.showPlayer(player);
            } else {
                // Regular players cannot
                online.hidePlayer(player);
            }
        }

        // Action bar
        showActionBar(player, "&fYou are currently &c&lVANISHED MODE");
        startActionBarLoop(plugin, player);
    }

    // ============================================================
    //  DISABLE VANISH (hide from EVERYONE for 5s, then re-show)
    // ============================================================
    private void disableVanishWithDelay(JavaPlugin plugin, Player player) {
        vanished.remove(player.getUniqueId());
        hiding.add(player.getUniqueId());

        // Hide from everyone (including staff) for 5 seconds
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.hidePlayer(player);
        }

        final int[] ticks = {0};
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                showActionBar(player, "&fYou are currently &a&lNORMAL MODE");
                ticks[0] += 10;
                if (ticks[0] >= 100) { // 5 seconds
                    hiding.remove(player.getUniqueId());
                    // Re-show to everyone
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.equals(player)) continue;
                        online.showPlayer(player);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    // ============================================================
    //  ACTION BAR (1.8.8 NMS)
    // ============================================================
    private void showActionBar(Player player, String message) {
        String colored = ChatColor.translateAlternateColorCodes('&', message);
        try {
            Object chatComponent = Class.forName("net.minecraft.server.v1_8_R3.ChatComponentText")
                    .getConstructor(String.class)
                    .newInstance(colored);

            Object packet = Class.forName("net.minecraft.server.v1_8_R3.PacketPlayOutChat")
                    .getConstructor(Class.forName("net.minecraft.server.v1_8_R3.IChatBaseComponent"),
                            byte.class)
                    .newInstance(chatComponent, (byte) 2);

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);

            playerConnection.getClass()
                    .getMethod("sendPacket", Class.forName("net.minecraft.server.v1_8_R3.Packet"))
                    .invoke(playerConnection, packet);
        } catch (Exception e) {
            // Fallback if NMS fails
            player.sendMessage(colored);
        }
    }

    private void startActionBarLoop(JavaPlugin plugin, final Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !vanished.contains(player.getUniqueId())) {
                    cancel();
                    return;
                }
                showActionBar(player, "&fYou are currently &c&lVANISHED MODE");
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    // ============================================================
    //  STATE QUERIES
    // ============================================================
    public static boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public static boolean isHiding(Player player) {
        return hiding.contains(player.getUniqueId());
    }

    public static Set<UUID> getVanished() {
        return vanished;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && sender.hasPermission("minestorm.vanish.others")) {
            String partial = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(partial)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}