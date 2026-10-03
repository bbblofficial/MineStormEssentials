package org.minestorm.essentials.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.minestorm.essentials.velocity.commands.ForwardedCommand;
import org.minestorm.essentials.velocity.commands.MsgCommand;
import org.minestorm.essentials.velocity.commands.ReplyCommand;
import org.minestorm.essentials.velocity.commands.SocialSpyCommand;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Plugin(id = "minestormessentials", name = "MineStormEssentials",
        version = "1.0", description = "Proxy-side MineStorm Essentials",
        authors = {"Muvixo"})
public class MineStormVelocity {

    public static final MinecraftChannelIdentifier CHANNEL =
            MinecraftChannelIdentifier.from("minestorm:main");

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private MessageUtil messages;

    @Inject
    public MineStormVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server; this.logger = logger; this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent e) {
        saveDefaultConfig();
        this.messages = new MessageUtil(this);

        server.getChannelRegistrar().register(CHANNEL);

        CommandManager cm = server.getCommandManager();

        register(cm, new MsgCommand(this), "msg", "tell", "whisper", "w", "m", "pm");
        register(cm, new ReplyCommand(this), "reply", "r");
        register(cm, new SocialSpyCommand(this), "socialspy", "spy");

        register(cm, new ForwardedCommand(this, "gmc"), "gmc");
        register(cm, new ForwardedCommand(this, "gms"), "gms");
        register(cm, new ForwardedCommand(this, "gmsp"), "gmsp");
        register(cm, new ForwardedCommand(this, "gma"), "gma");
        register(cm, new ForwardedCommand(this, "fly"), "fly");
        register(cm, new ForwardedCommand(this, "flyspeed"), "flyspeed", "fspeed", "fs");
        register(cm, new ForwardedCommand(this, "vanish"), "vanish", "v");

        logger.info("=================================================");
        logger.info("  MineStorm Essentials (Velocity) v1.0");
        logger.info("  Author: Muvixo");
        logger.info("=================================================");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent e) {
        server.getChannelRegistrar().unregister(CHANNEL);
        logger.info("MineStorm Essentials disabled.");
    }

    private void register(CommandManager cm, Command cmd, String name, String... aliases) {
        CommandMeta.Builder b = cm.metaBuilder(name).plugin(this);
        if (aliases.length > 0) b = b.aliases(aliases);
        cm.register(b.build(), cmd);
    }

    private void saveDefaultConfig() {
        try {
            if (!Files.exists(dataDirectory)) Files.createDirectories(dataDirectory);
            Path target = dataDirectory.resolve("config.yml");
            if (!Files.exists(target)) {
                try (InputStream in = getClass().getResourceAsStream("/config.yml")) {
                    if (in != null) Files.copy(in, target);
                }
            }
        } catch (IOException ex) {
            logger.warn("Could not save config.yml: " + ex.getMessage());
        }
    }

    public ProxyServer getServer() { return server; }
    public Logger getLogger() { return logger; }
    public Path getDataDirectory() { return dataDirectory; }
    public MessageUtil getMessages() { return messages; }
}
