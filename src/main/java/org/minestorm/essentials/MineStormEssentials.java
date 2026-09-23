package org.minestorm.essentials;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineStormEssentials extends JavaPlugin {

    private CommandManager commandManager;

    @Override
    public void onEnable() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        createConfigIfMissing();
        saveDefaultConfig();
        reloadConfig();

        this.commandManager = new CommandManager(this);
        this.commandManager.registerAll();

        getLogger().info("=================================================");
        getLogger().info("  MineStorm Essentials v" + getDescription().getVersion());
        getLogger().info("  Commands: /gmc /gms /gmsp /gma /fly /flyspeed /msg /reply /vanish");
        getLogger().info("  Author: Muvixo");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineStorm Essentials disabled.");
    }

    // ============================================================
    //  CONFIG AUTO-MERGE
    // ============================================================
    private void createConfigIfMissing() {
        File configFile = new File(getDataFolder(), "config.yml");
        boolean isNew = !configFile.exists();

        if (isNew) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create config.yml: " + e.getMessage());
                return;
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = this.getResource("config.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            cfg.setDefaults(defaults);
        }

        // -------- messages --------
        setIfMissing(cfg, "messages.msg-sender",
                "&7[&6me &7-> &6%receiver%&7] &f%message%");
        setIfMissing(cfg, "messages.msg-receiver",
                "&7[&6%sender% &7-> &6me&7] &f%message%");
        setIfMissing(cfg, "messages.social-spy",
                "&8[&cSPY&8] &7[&6%sender% &7-> &6%receiver%&7] &f%message%");
        setIfMissing(cfg, "messages.msg-no-reply",
                "&cYou have nobody to reply to.");
        setIfMissing(cfg, "messages.msg-self",
                "&cYou cannot message yourself.");
        setIfMissing(cfg, "messages.no-permission",
                "&cYou do not have permission to do this.");
        setIfMissing(cfg, "messages.player-not-found",
                "&cPlayer not found: &e%player%");
        setIfMissing(cfg, "messages.player-only",
                "&cOnly players can use this command.");
        setIfMissing(cfg, "messages.invalid-args",
                "&cUsage: &e%usage%");

        setIfMissing(cfg, "messages.gamemode-changed-self",
                "&aYour gamemode has been set to &e%mode%&a.");
        setIfMissing(cfg, "messages.gamemode-changed-other",
                "&aSet gamemode of &e%player% &ato &e%mode%&a.");
        setIfMissing(cfg, "messages.gamemode-notify",
                "&eYour gamemode was set to &6%mode% &eby &6%by%&e.");

        setIfMissing(cfg, "messages.fly-enabled-self",
                "&aFlight &lENABLED&a.");
        setIfMissing(cfg, "messages.fly-disabled-self",
                "&cFlight &lDISABLED&c.");
        setIfMissing(cfg, "messages.fly-enabled-other",
                "&aEnabled flight for &e%player%&a.");
        setIfMissing(cfg, "messages.fly-disabled-other",
                "&cDisabled flight for &e%player%&c.");
        setIfMissing(cfg, "messages.fly-notify",
                "&eYour flight was %state% &eby &6%by%&e.");

        setIfMissing(cfg, "messages.flyspeed-set-self",
                "&aFly speed set to &e%speed%&a.");
        setIfMissing(cfg, "messages.flyspeed-set-other",
                "&aSet fly speed of &e%player% &ato &e%speed%&a.");
        setIfMissing(cfg, "messages.flyspeed-invalid",
                "&cSpeed must be between &e1 &cand &e10&c.");
        setIfMissing(cfg, "messages.flyspeed-notify",
                "&eYour fly speed was set to &6%speed% &eby &6%by%&e.");

        // -------- vanish messages --------
        setIfMissing(cfg, "messages.vanish-enabled-self",
                "&aYou are now &lVANISHED&a.");
        setIfMissing(cfg, "messages.vanish-disabled-self",
                "&cYou are no longer vanished.");
        setIfMissing(cfg, "messages.vanish-enabled-other",
                "&aEnabled vanish for &e%player%&a.");
        setIfMissing(cfg, "messages.vanish-disabled-other",
                "&cDisabled vanish for &e%player%&c.");
        setIfMissing(cfg, "messages.vanish-notify",
                "&eYour vanish mode was %state% &eby &6%by%&e.");

        // -------- settings --------
        setIfMissing(cfg, "settings.allow-msg-colors", Boolean.TRUE);
        setIfMissing(cfg, "settings.social-spy-enabled", Boolean.TRUE);
        setIfMissing(cfg, "settings.msg-sound", Boolean.TRUE);
        setIfMissing(cfg, "settings.msg-sound-name", "ORB_PICKUP");
        setIfMissing(cfg, "settings.msg-sound-volume", Double.valueOf(0.5D));
        setIfMissing(cfg, "settings.msg-sound-pitch", Double.valueOf(1.5D));

        // -------- vanish settings --------
        setIfMissing(cfg, "settings.vanish.enabled", Boolean.TRUE);
        setIfMissing(cfg, "settings.vanish.hide-from-mobs", Boolean.TRUE);
        setIfMissing(cfg, "settings.vanish.notify-staff", Boolean.TRUE);

        try {
            cfg.save(configFile);
            if (isNew) {
                getLogger().info("Created default config.yml");
            } else {
                getLogger().info("Config.yml merged (existing values preserved).");
            }
        } catch (IOException e) {
            getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    private void setIfMissing(FileConfiguration cfg, String path, Object value) {
        if (!cfg.contains(path)) {
            cfg.set(path, value);
        }
    }

    public CommandManager getCommandManager() {
        return this.commandManager;
    }
}
