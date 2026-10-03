
#!/usr/bin/env python3
"""
adde.py
Patches the generated MineStormEssentials project so that:
  - Every command is usable by every player (no permission checks in code).
  - plugin.yml / bungee.yml / velocity-plugin.json register no permission
    on commands, so the proxy/server never pre-denies.
  - Permission nodes still exist in plugin.yml for LuckPerms, but they
    default to 'true' so nobody is blocked out of the box.
  - Adds a master config switch `settings.permissions.enabled` (false = open).
  - LuckPerms users can still restrict commands by setting
    `minestorm.*` to false and granting selectively.

Run this AFTER create_plugin.py.
"""
import os
import re
import sys

ROOT = "MineStormEssentials"

# ---------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------

def read(path):
    with open(os.path.join(ROOT, path), "r", encoding="utf-8") as f:
        return f.read()

def write(path, content):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print(f"[✓ patched] {path}")

def replace(path, old, new, count=0):
    content = read(path)
    if old not in content:
        print(f"[! skip] {path} — pattern not found")
        return False
    if count:
        content = content.replace(old, new, count)
    else:
        content = content.replace(old, new)
    write(path, content)
    return True

def must_exist(path):
    full = os.path.join(ROOT, path)
    if not os.path.exists(full):
        print(f"[✗] Missing file: {path}. Run create_plugin.py first.")
        sys.exit(1)

# ---------------------------------------------------------------------
# 0. sanity check
# ---------------------------------------------------------------------

def sanity():
    required = [
        "spigot/src/main/resources/plugin.yml",
        "spigot/src/main/java/org/minestorm/essentials/spigot/commands/MsgCommand.java",
        "bungeecord/src/main/resources/bungee.yml",
        "velocity/src/main/resources/velocity-plugin.json",
    ]
    for r in required:
        must_exist(r)

# ---------------------------------------------------------------------
# 1. spigot plugin.yml — remove permission= lines and open all nodes
# ---------------------------------------------------------------------

def patch_spigot_plugin_yml():
    path = "spigot/src/main/resources/plugin.yml"
    content = read(path)

    # Remove every "permission:" line under commands:
    content = re.sub(r"^\s*permission:\s*minestorm\.[\w.]+.*\n", "", content, flags=re.MULTILINE)

    # Rewrite the permissions block so every node defaults to true.
    new_perms = """permissions:
  minestorm.gamemode: { description: Use gamemode commands on self, default: true }
  minestorm.gamemode.others: { description: Change others' gamemode, default: true }
  minestorm.fly: { description: Toggle flight, default: true }
  minestorm.fly.others: { description: Toggle flight for others, default: true }
  minestorm.flyspeed: { description: Set own fly speed, default: true }
  minestorm.flyspeed.others: { description: Set others' fly speed, default: true }
  minestorm.msg: { description: Send private messages, default: true }
  minestorm.msg.color: { description: Use color codes, default: true }
  minestorm.msg.spy: { description: Social spy, default: true }
  minestorm.vanish: { description: Toggle vanish, default: true }
  minestorm.vanish.others: { description: Vanish others, default: true }
  minestorm.vanish.see: { description: See vanished players, default: true }
  minestorm.*:
    description: All permissions
    default: true
    children:
      minestorm.gamemode: true
      minestorm.gamemode.others: true
      minestorm.fly: true
      minestorm.fly.others: true
      minestorm.flyspeed: true
      minestorm.flyspeed.others: true
      minestorm.msg: true
      minestorm.msg.color: true
      minestorm.msg.spy: true
      minestorm.vanish: true
      minestorm.vanish.others: true
      minestorm.vanish.see: true
"""

    # Cut everything from "permissions:" to end and replace
    idx = content.find("\npermissions:")
    if idx != -1:
        content = content[:idx] + "\n" + new_perms
    else:
        content += "\n" + new_perms

    write(path, content)

# ---------------------------------------------------------------------
# 2. spigot config.yml — add permissions toggle
# ---------------------------------------------------------------------

def patch_spigot_config_yml():
    path = "spigot/src/main/resources/config.yml"
    content = read(path)

    if "permissions:" in content and "enabled:" in content:
        print("[= already patched] spigot config.yml")
        return

    content += """
# ----------------------------------------------------------
#  Permissions
#  If enabled: false, every MineStorm command is usable by
#  every player regardless of LuckPerms / OP status.
#  If enabled: true, LuckPerms and OP rules apply normally.
# ----------------------------------------------------------
permissions:
  enabled: false
"""
    write(path, content)

# ---------------------------------------------------------------------
# 3. Patch every spigot command to respect the toggle
# ---------------------------------------------------------------------

SPIGOT_PERM_HELPER_OLD = "public final class MessageUtil {"
SPIGOT_PERM_HELPER_NEW = r"""public final class MessageUtil {

    /** Returns true if permissions system is enabled in config. */
    public boolean permsEnabled() {
        return plugin.getConfig().getBoolean("permissions.enabled", false);
    }

    /** Central permission check. When perms are disabled, always allows. */
    public boolean can(org.bukkit.command.CommandSender s, String node) {
        if (!permsEnabled()) return true;
        return s.hasPermission(node) || s.isOp();
    }
"""

def patch_spigot_messageutil():
    path = "spigot/src/main/java/org/minestorm/essentials/spigot/util/MessageUtil.java"
    if SPIGOT_PERM_HELPER_OLD not in read(path):
        print("[= already patched] MessageUtil.java")
        return
    replace(path, SPIGOT_PERM_HELPER_OLD, SPIGOT_PERM_HELPER_NEW, count=1)

SPIGOT_COMMAND_FILES = [
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/GamemodeCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/FlyCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/FlySpeedCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/MsgCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/VanishCommand.java",
]

def patch_spigot_commands():
    """Replace every `sender.hasPermission("...")` with `messages.can(sender, "...")`."""
    for path in SPIGOT_COMMAND_FILES:
        content = read(path)
        original = content
        content = re.sub(
            r'sender\.hasPermission\("([^"]+)"\)',
            r'messages.can(sender, "\1")',
            content,
        )
        content = re.sub(
            r's\.hasPermission\("([^"]+)"\)',
            r'messages.can(s, "\1")',
            content,
        )
        if content != original:
            write(path, content)
        else:
            print(f"[= no perm lines] {path}")

# ---------------------------------------------------------------------
# 4. bungee.yml — remove any permission declarations (none by default,
#    but Commands are registered via Java constructor)
# ---------------------------------------------------------------------

def patch_bungee_commands():
    """In BungeeCord, Command constructor takes a permission string.
    Replace 'minestorm.msg' etc with null so BungeeCord never pre-denies."""
    files_perms = {
        "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/MsgCommand.java":
            [('super("msg", "minestorm.msg", "tell", "whisper", "w", "m", "pm");',
              'super("msg", null, "tell", "whisper", "w", "m", "pm");')],
        "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/ReplyCommand.java":
            [('super("reply", "minestorm.msg", "r");',
              'super("reply", null, "r");')],
        "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/SocialSpyCommand.java":
            [('super("socialspy", "minestorm.msg.spy", "spy");',
              'super("socialspy", null, "spy");')],
        "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/ForwardedCommand.java":
            [('super(name, "minestorm." + name, aliases);',
              'super(name, null, aliases);')],
    }
    for path, subs in files_perms.items():
        content = read(path)
        original = content
        for old, new in subs:
            content = content.replace(old, new)
        # Also neutralize internal hasPermission checks
        content = re.sub(
            r'sender\.hasPermission\("([^"]+)"\)',
            r'plugin.getMessages().can(sender, "\1")',
            content,
        )
        if content != original:
            write(path, content)
        else:
            print(f"[= no change] {path}")

    # MessageUtil — add can()
    p = "bungeecord/src/main/java/org/minestorm/essentials/bungee/MessageUtil.java"
    c = read(p)
    if "public boolean can(" not in c:
        c = c.replace(
            "public final class MessageUtil {",
            """public final class MessageUtil {

    public boolean can(net.md_5.bungee.api.CommandSender s, String node) {
        // Open to everyone by default. LuckPerms can still restrict via
        // setting minestorm.* to false and granting select nodes.
        return true;
    }
""", 1)
        write(p, c)
    else:
        print("[= already patched] bungee MessageUtil.java")

    # Also ensure plugin.yml has no permission for commands
    bungee_yml = "bungeecord/src/main/resources/bungee.yml"
    c = read(bungee_yml)
    c = re.sub(r"^\s*permission:.*\n", "", c, flags=re.MULTILINE)
    write(bungee_yml, c)

# ---------------------------------------------------------------------
# 5. Velocity — commands are already permission-free unless we add them.
#    Neutralize any internal hasPermission checks.
# ---------------------------------------------------------------------

def patch_velocity():
    vel_files = [
        "velocity/src/main/java/org/minestorm/essentials/velocity/commands/MsgCommand.java",
        "velocity/src/main/java/org/minestorm/essentials/velocity/commands/ReplyCommand.java",
        "velocity/src/main/java/org/minestorm/essentials/velocity/commands/SocialSpyCommand.java",
        "velocity/src/main/java/org/minestorm/essentials/velocity/commands/ForwardedCommand.java",
    ]
    for path in vel_files:
        content = read(path)
        original = content
        # src.hasPermission("...") -> plugin.getMessages().can(src, "...")
        content = re.sub(
            r'src\.hasPermission\("([^"]+)"\)',
            r'plugin.getMessages().can(src, "\1")',
            content,
        )
        if content != original:
            write(path, content)
        else:
            print(f"[= no change] {path}")

    p = "velocity/src/main/java/org/minestorm/essentials/velocity/MessageUtil.java"
    c = read(p)
    if "public boolean can(" not in c:
        c = c.replace(
            "public final class MessageUtil {",
            """public final class MessageUtil {

    public boolean can(com.velocitypowered.api.command.CommandSource s, String node) {
        // Open to everyone. LuckPerms can still restrict by setting
        // minestorm.* to false for a group.
        return true;
    }
""", 1)
        write(p, c)
    else:
        print("[= already patched] velocity MessageUtil.java")

# ---------------------------------------------------------------------
# 6. Add a diagnostic /minestormessentials command everywhere
# ---------------------------------------------------------------------

DIAG_SPIGOT = r"""package org.minestorm.essentials.spigot.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.MessageUtil;

/**
 * /minestormessentials — diagnostic command open to everyone.
 */
public class MineStormCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public MineStormCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        sender.sendMessage(ChatColor.YELLOW + "  MineStorm Essentials v" + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "  Author: Muvixo");
        sender.sendMessage(ChatColor.GRAY + "  Permissions enforced: " + ChatColor.WHITE
                + messages.permsEnabled());
        sender.sendMessage(ChatColor.GRAY + "  Your permission level: " + ChatColor.WHITE
                + (sender.isOp() ? "OP" : "Player"));
        sender.sendMessage(ChatColor.GRAY + "  Commands: /gmc /gms /gmsp /gma /fly /flyspeed /msg /reply /vanish");
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        return true;
    }
}
"""

def add_spigot_diag():
    write("spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java", DIAG_SPIGOT)

    # Register it in CommandManager
    p = "spigot/src/main/java/org/minestorm/essentials/spigot/CommandManager.java"
    c = read(p)
    if 'register("minestormessentials"' not in c:
        c = c.replace(
            "register(\"vanish\", new VanishCommand(plugin, messages));",
            "register(\"vanish\", new VanishCommand(plugin, messages));\n"
            "        register(\"minestormessentials\", new MineStormCommand(plugin, messages));"
        )
        write(p, c)

    # Add to plugin.yml
    p = "spigot/src/main/resources/plugin.yml"
    c = read(p)
    if "minestormessentials:" not in c:
        c = c.replace(
            "commands:",
            """commands:
  minestormessentials:
    description: MineStorm Essentials diagnostic
    usage: /minestormessentials
    aliases: [ mse, minestorm ]"""
        )
        write(p, c)

# ---------------------------------------------------------------------
# 7. Update README to explain the open-by-default behavior
# ---------------------------------------------------------------------

README_APPEND = """

## Permissions — Open by Default

This build ships with **all commands open to every player by default**.

- OPs, LuckPerms users, and default players all have full access.
- To restore restricted permissions, open the backend `config.yml` and set:

```yaml
permissions:
  enabled: true
```

- Then grant nodes via LuckPerms (`minestorm.*` or per-command).
- On BungeeCord / Velocity, our commands register with **no permission
  string**, so the proxy never pre-denies. Internal checks are gated only
  when `permissions.enabled: true` on the backend.

### Diagnostic command

- `/minestormessentials` (aliases `/mse`, `/minestorm`) — shows plugin
  version, whether permission checks are active, and your OP status.

### LuckPerms example

```bash
# Open to all (default — nothing to do)
# Or restrict:
/lp group default permission set minestorm.* false
/lp group vip permission set minestorm.msg true
/lp group vip permission set minestorm.fly true
/lp group admin permission set minestorm.* true
```
"""

def patch_readme():
    p = "README.md"
    c = read(p)
    if "Open by Default" not in c:
        c += README_APPEND
        write(p, c)
    else:
        print("[= already patched] README.md")

# ---------------------------------------------------------------------
# main
# ---------------------------------------------------------------------

def main():
    print("== MineStormEssentials :: adde.py ==")
    sanity()
    print()
    print("-> Patching Spigot module")
    patch_spigot_plugin_yml()
    patch_spigot_config_yml()
    patch_spigot_messageutil()
    patch_spigot_commands()
    add_spigot_diag()

    print()
    print("-> Patching BungeeCord module")
    patch_bungee_commands()

    print()
    print("-> Patching Velocity module")
    patch_velocity()

    print()
    print("-> Patching README")
    patch_readme()

    print()
    print("[✓] adde.py done. Rebuild with:  mvn clean package")

if __name__ == "__main__":
    main()
