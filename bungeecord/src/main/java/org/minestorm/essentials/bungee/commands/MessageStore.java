package org.minestorm.essentials.bungee.commands;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageStore {
    private static final Map<UUID, UUID> reply = new HashMap<>();
    private MessageStore() {}

    public static void setReplyTarget(ProxiedPlayer a, ProxiedPlayer b) {
        reply.put(a.getUniqueId(), b.getUniqueId());
        reply.put(b.getUniqueId(), a.getUniqueId());
    }

    public static ProxiedPlayer getReplyTarget(ProxiedPlayer a) {
        UUID id = reply.get(a.getUniqueId());
        return id == null ? null : ProxyServer.getInstance().getPlayer(id);
    }
}
