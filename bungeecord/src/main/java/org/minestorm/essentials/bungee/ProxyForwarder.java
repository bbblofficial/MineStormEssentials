package org.minestorm.essentials.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;

import java.util.UUID;

public final class ProxyForwarder {

    private ProxyForwarder() {}

    public static void forward(ProxiedPlayer sender, String token, String command) {
        Server server = sender.getServer();
        if (server == null) {
            sender.sendMessage(MineStormBungee.get().getMessages()
                    .color("&cYou are not connected to a server."));
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("EXEC");
        out.writeUTF(token);
        out.writeUTF(sender.getName());
        out.writeUTF(command);
        server.sendData(MineStormBungee.CHANNEL, out.toByteArray());
    }

    public static String newToken() { return UUID.randomUUID().toString(); }
}
