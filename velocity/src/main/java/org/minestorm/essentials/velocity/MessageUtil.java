package org.minestorm.essentials.velocity;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;

public final class MessageUtil {

    private final MineStormVelocity plugin;
    private Map<String, Object> config;

    public MessageUtil(MineStormVelocity plugin) {
        this.plugin = plugin;
        reload();
    }

    @SuppressWarnings("unchecked")
    public void reload() {
        try (InputStream in = Files.newInputStream(
                plugin.getDataDirectory().resolve("config.yml"))) {
            this.config = new Yaml().load(in);
        } catch (IOException e) {
            plugin.getLogger().warn("Could not read config.yml: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private String raw(String path) {
        if (config == null) return "";
        Object cur = config;
        for (String part : ("messages." + path).split("\\.")) {
            if (!(cur instanceof Map)) return "";
            cur = ((Map<String, Object>) cur).get(part);
        }
        return cur == null ? "" : cur.toString();
    }

    public String get(String path, String... repl) {
        String m = raw(path);
        for (int i = 0; i + 1 < repl.length; i += 2) m = m.replace(repl[i], repl[i + 1]);
        return m;
    }

    public void send(CommandSource s, String path, String... repl) {
        String m = get(path, repl);
        if (!m.isEmpty())
            s.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(m));
    }

    public void sendRaw(CommandSource s, String msg) {
        if (msg != null && !msg.isEmpty())
            s.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    public String color(String s) { return s == null ? "" : s; }

    @SuppressWarnings("unchecked")
    public boolean setting(String path, boolean def) {
        if (config == null) return def;
        Object cur = config;
        for (String part : ("settings." + path).split("\\.")) {
            if (!(cur instanceof Map)) return def;
            cur = ((Map<String, Object>) cur).get(part);
        }
        if (cur == null) return def;
        if (cur instanceof Boolean) return (Boolean) cur;
        return Boolean.parseBoolean(cur.toString());
    }
}
