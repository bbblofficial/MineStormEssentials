package org.minestorm.essentials.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.nio.file.Files;

public class MineStormBungee extends Plugin {

    public static final String CHANNEL = "minestorm:main";
    private static MineStormBungee instance;
    private Configuration config;
    private MessageUtil messages;
    private CommandRegistrar registrar;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        this.messages = new MessageUtil(this);
        this.registrar = new CommandRegistrar(this);
        this.registrar.register();

        getProxy().registerChannel(CHANNEL);

        getLogger().info("=================================================");
        getLogger().info("  MineStorm Essentials (BungeeCord) v1.0");
        getLogger().info("  Author: Muvixo");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(CHANNEL);
        getLogger().info("MineStorm Essentials disabled.");
    }

    public static MineStormBungee get() { return instance; }
    public MessageUtil getMessages() { return messages; }

    public void saveDefaultConfig() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        File f = new File(getDataFolder(), "config.yml");
        if (!f.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, f.toPath());
            } catch (IOException e) {
                getLogger().warning("Could not save config.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().warning("Could not load config.yml: " + e.getMessage());
        }
    }

    public Configuration getConfig() { return config; }
    public ProxyServer proxy() { return getProxy(); }
}
