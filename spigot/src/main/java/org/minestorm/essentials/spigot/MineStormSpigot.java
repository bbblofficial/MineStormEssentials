package org.minestorm.essentials.spigot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.listeners.PluginMessageListener;

public final class MineStormSpigot extends JavaPlugin {

    public static final String CHANNEL = "minestorm:main";

    private CommandManager commandManager;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        createConfigIfMissing();
        saveDefaultConfig();
        reloadConfig();

        this.commandManager = new CommandManager(this);
        this.commandManager.registerAll();

        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL,
                new PluginMessageListener(this));

        getLogger().info("=================================================");
        getLogger().info("  MineStorm Essentials (Spigot/Paper) v" + getDescription().getVersion());
        getLogger().info("  Author: Muvixo");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("MineStorm Essentials disabled.");
    }

    private void createConfigIfMissing() {
        File configFile = new File(getDataFolder(), "config.yml");
        boolean isNew = !configFile.exists();
        if (isNew) {
            try { configFile.createNewFile(); } catch (IOException e) {
                getLogger().warning("Could not create config.yml: " + e.getMessage()); return;
            }
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);
        InputStream defStream = this.getResource("config.yml");
        if (defStream != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }
        try {
            cfg.save(configFile);
            if (isNew) getLogger().info("Created default config.yml");
            else getLogger().info("Config.yml merged (existing values preserved).");
        } catch (IOException e) {
            getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    public CommandManager getCommandManager() { return commandManager; }
}
