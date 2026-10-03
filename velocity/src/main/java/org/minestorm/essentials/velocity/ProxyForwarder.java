package org.minestorm.essentials.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

public final class ProxyForwarder {

    private ProxyForwarder() {}

    public static void forward(Player sender, String command) {
        Optional<ServerConnection> conn = sender.getCurrentServer();
        if (!conn.isPresent()) {
            sender.sendMessage(Component.text("You are not connected to a server."));
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("EXEC");
        out.writeUTF(UUID.randomUUID().toString());
        out.writeUTF(sender.getUsername());
        out.writeUTF(command);
        conn.get().sendPluginMessage(MineStormVelocity.CHANNEL, out.toByteArray());
    }
}
