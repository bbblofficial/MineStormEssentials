#!/usr/bin/env python3
"""
fixer.py
========
Auto-fixer + auto-merger for the MineStormEssentials multi-platform project.

What it does
------------
1.  Locates the real project root (folder containing pom.xml AND spigot/pom.xml).
2.  Removes stray `spigot/` folders that a previous mis-run may have created
    in unrelated projects.
3.  Regenerates any missing project files (Java, POM, YAML, workflow, README)
    from an embedded template — without overwriting existing files.
4.  Rewrites MessageUtil.java on the Spigot module so it contains the
    `permsEnabled()` and `can()` helpers.
5.  Replaces every `sender.hasPermission("minestorm.X")` / `s.hasPermission(...)`
    in the Spigot command classes with `messages.can(sender, "minestorm.X")`.
6.  Sets every command registration on Bungee/Velocity to pass `null`
    permission (so the proxy never pre-denies).
7.  Appends `permissions:\n  enabled: false` to the Spigot, Bungee, and
    Velocity config.yml if missing.
8.  Adds the /minestormessentials diagnostic command everywhere.
9.  Runs a sanity check and prints a clear summary.

Usage
-----
    py -3.12 fixer.py
    py -3.12 fixer.py --root "C:\\path\\to\\MineStormEssentials"
    py -3.12 fixer.py --dry-run
    py -3.12 fixer.py --verify-only

Requires Python 3.8+. No third-party packages.
"""
from __future__ import annotations

import argparse
import io
import os
import re
import shutil
import sys
from pathlib import Path

# ======================================================================
#  ANSI colours (disabled when not a TTY)
# ======================================================================

class C:
    OK    = "\033[92m"
    WARN  = "\033[93m"
    ERR   = "\033[91m"
    INFO  = "\033[96m"
    DIM   = "\033[2m"
    BOLD  = "\033[1m"
    END   = "\033[0m"

    @staticmethod
    def auto():
        if not sys.stdout.isatty():
            C.OK = C.WARN = C.ERR = C.INFO = C.DIM = C.BOLD = C.END = ""

def ok(m):    print(f"{C.OK}[+]{C.END} {m}")
def warn(m):  print(f"{C.WARN}[!]{C.END} {m}")
def err(m):   print(f"{C.ERR}[x]{C.END} {m}")
def info(m):  print(f"{C.INFO}[i]{C.END} {m}")
def dim(m):   print(f"{C.DIM}    {m}{C.END}")
def head(m):  print(f"\n{C.BOLD}=== {m} ==={C.END}")

# ======================================================================
#  Project root discovery
# ======================================================================

def is_real_root(p: Path) -> bool:
    return (
        (p / "pom.xml").is_file()
        and (p / "spigot" / "pom.xml").is_file()
        and (p / "spigot" / "src" / "main" / "resources" / "plugin.yml").is_file()
    )

def find_real_root(start: Path) -> Path | None:
    # 1. exact match
    if is_real_root(start):
        return start

    # 2. walk up
    for parent in [start.resolve(), *start.resolve().parents]:
        if is_real_root(parent):
            return parent

    # 3. immediate children + grandchildren
    try:
        for entry in start.iterdir():
            if not entry.is_dir():
                continue
            if is_real_root(entry):
                return entry
            try:
                for sub in entry.iterdir():
                    if sub.is_dir() and is_real_root(sub):
                        return sub
            except PermissionError:
                continue
    except PermissionError:
        pass

    return None

# ======================================================================
#  Stray folder cleanup
# ======================================================================

STRAY_MARKERS = [
    Path("spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java"),
    Path("spigot/src/main/java/org/minestorm/essentials/spigot/MineStormSpigot.java"),
]

def clean_stray(cwd: Path, real_root: Path, dry: bool) -> None:
    """Remove a `spigot/` folder that exists OUTSIDE the real project root
    and looks like our partial tree (no sibling pom.xml, contains our marker files)."""
    if cwd == real_root or (cwd / "pom.xml").exists():
        return

    stray = cwd / "spigot"
    if not stray.is_dir():
        return

    looks_like_ours = any((cwd / m).exists() for m in STRAY_MARKERS)
    if not looks_like_ours:
        return

    warn(f"Removing stray partial tree: {stray}")
    if not dry:
        shutil.rmtree(stray, ignore_errors=True)
    else:
        dim("(dry-run, not deleted)")

# ======================================================================
#  Embedded templates  (only regenerated if a file is missing)
# ======================================================================

TEMPLATES: dict[str, str] = {}

# ---------- parent pom ----------
TEMPLATES["pom.xml"] = r"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>org.minestorm</groupId>
    <artifactId>minestorm-essentials-parent</artifactId>
    <version>1.0</version>
    <packaging>pom</packaging>
    <name>MineStormEssentials Parent</name>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>
    <modules>
        <module>spigot</module>
        <module>paper</module>
        <module>bungeecord</module>
        <module>velocity</module>
    </modules>
    <repositories>
        <repository><id>spigot-repo</id><url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url></repository>
        <repository><id>sonatype-snapshots</id><url>https://oss.sonatype.org/content/repositories/snapshots/</url></repository>
        <repository><id>papermc</id><url>https://repo.papermc.io/repository/maven-public/</url></repository>
    </repositories>
    <build>
        <pluginManagement>
            <plugins>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.11.0</version></plugin>
                <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-shade-plugin</artifactId><version>3.5.1</version></plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
"""

# ---------- .gitignore ----------
TEMPLATES[".gitignore"] = """target/
*.class
*.jar
!.mvn/wrapper/maven-wrapper.jar
.idea/
*.iml
.vscode/
.settings/
.classpath
.project
.DS_Store
__pycache__/
*.pyc
"""

# ---------- README ----------
TEMPLATES["README.md"] = """# MineStormEssentials v1.0

Multi-platform essentials plugin for **Minecraft 1.8.8**.
**Created by Muvixo**

Modules: spigot, paper, bungeecord, velocity.
See the project README for full details.
"""

# ---------- GitHub Actions workflow ----------
TEMPLATES[".github/workflows/build.yml"] = r"""name: Build MineStormEssentials

on:
  push:
    branches: [ main, master ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        include:
          - jdk: 8
            modules: 'spigot,paper,bungeecord'
          - jdk: 17
            modules: 'spigot,paper,bungeecord,velocity'
          - jdk: 21
            modules: 'spigot,paper,bungeecord,velocity'
          - jdk: 25
            modules: 'spigot,paper,bungeecord,velocity'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: ${{ matrix.jdk }}
          cache: maven
      - name: Build
        run: mvn -B clean package -pl ${{ matrix.modules }} -am -DskipTests
      - uses: actions/upload-artifact@v4
        with:
          name: MineStormEssentials-JDK${{ matrix.jdk }}
          path: |
            spigot/target/*.jar
            paper/target/*.jar
            bungeecord/target/*.jar
            velocity/target/*.jar
          if-no-files-found: warn
      - name: Release
        if: startsWith(github.ref, 'refs/tags/')
        uses: softprops/action-gh-release@v2
        with:
          files: |
            spigot/target/*.jar
            paper/target/*.jar
            bungeecord/target/*.jar
            velocity/target/*.jar
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
"""

# ---------- spigot/pom.xml ----------
TEMPLATES["spigot/pom.xml"] = r"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.minestorm</groupId>
        <artifactId>minestorm-essentials-parent</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>MineStormEssentials-Spigot</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.8.8-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion><groupId>net.md-5</groupId><artifactId>bungeecord-chat</artifactId></exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>net.md-5</groupId>
            <artifactId>bungeecord-chat</artifactId>
            <version>1.16-R0.4</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <finalName>MineStormEssentials-Spigot</finalName>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
</project>
"""

# ---------- paper/pom.xml ----------
TEMPLATES["paper/pom.xml"] = r"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.minestorm</groupId>
        <artifactId>minestorm-essentials-parent</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>MineStormEssentials-Paper</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.8.8-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion><groupId>net.md-5</groupId><artifactId>bungeecord-chat</artifactId></exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>net.md-5</groupId>
            <artifactId>bungeecord-chat</artifactId>
            <version>1.16-R0.4</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <sourceDirectory>${project.basedir}/../spigot/src/main/java</sourceDirectory>
        <resources>
            <resource>
                <directory>${project.basedir}/../spigot/src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <finalName>MineStormEssentials-Paper</finalName>
    </build>
</project>
"""

# ---------- bungeecord/pom.xml ----------
TEMPLATES["bungeecord/pom.xml"] = r"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.minestorm</groupId>
        <artifactId>minestorm-essentials-parent</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>MineStormEssentials-BungeeCord</artifactId>
    <packaging>jar</packaging>
    <dependencies>
        <dependency>
            <groupId>net.md-5</groupId>
            <artifactId>bungeecord-api</artifactId>
            <version>1.20-R0.2</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <finalName>MineStormEssentials-BungeeCord</finalName>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>
</project>
"""

# ---------- velocity/pom.xml ----------
TEMPLATES["velocity/pom.xml"] = r"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.minestorm</groupId>
        <artifactId>minestorm-essentials-parent</artifactId>
        <version>1.0</version>
    </parent>
    <artifactId>MineStormEssentials-Velocity</artifactId>
    <packaging>jar</packaging>
    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.velocitypowered</groupId>
            <artifactId>velocity-api</artifactId>
            <version>3.3.0-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
    <build>
        <finalName>MineStormEssentials-Velocity</finalName>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>17</source>
                    <target>17</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"""

# ---------- spigot plugin.yml ----------
TEMPLATES["spigot/src/main/resources/plugin.yml"] = r"""name: MineStormEssentials
version: 1.0
main: org.minestorm.essentials.spigot.MineStormSpigot
author: Muvixo
description: Lightweight essentials plugin for Minecraft 1.8.8
api-version: 1.8

commands:
  gmc: { description: Creative, usage: /gmc [player] }
  gms: { description: Survival, usage: /gms [player] }
  gmsp: { description: Spectator, usage: /gmsp [player] }
  gma: { description: Adventure, usage: /gma [player] }
  fly: { description: Toggle flight, usage: /fly [player] }
  flyspeed: { description: Fly speed, usage: /flyspeed <1-10> [player], aliases: [ fspeed, fs ] }
  msg: { description: Private message, usage: /msg <player> <message>, aliases: [ tell, whisper, w, m, pm ] }
  reply: { description: Reply, usage: /reply <message>, aliases: [ r ] }
  vanish: { description: Toggle vanish, usage: /vanish [player], aliases: [ v ] }
  minestormessentials: { description: MineStorm Essentials diagnostic, usage: /minestormessentials, aliases: [ mse, minestorm ] }

permissions:
  minestorm.*:
    description: All MineStorm permissions (default for everyone)
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
  minestorm.deny.%node%:
    description: Set true to deny a specific node
    default: false
"""

# ---------- spigot config.yml ----------
TEMPLATES["spigot/src/main/resources/config.yml"] = r"""messages:
  msg-sender: '&7[&6me &7-> &6%receiver%&7] &f%message%'
  msg-receiver: '&7[&6%sender% &7-> &6me&7] &f%message%'
  social-spy: '&8[&cSPY&8] &7[&6%sender% &7-> &6%receiver%&7] &f%message%'
  msg-no-reply: '&cYou have nobody to reply to.'
  msg-self: '&cYou cannot message yourself.'
  no-permission: '&cYou do not have permission to do this.'
  player-not-found: '&cPlayer not found: &e%player%'
  player-only: '&cOnly players can use this command.'
  invalid-args: '&cUsage: &e%usage%'
  gamemode-changed-self: '&aYour gamemode has been set to &e%mode%&a.'
  gamemode-changed-other: '&aSet gamemode of &e%player% &ato &e%mode%&a.'
  gamemode-notify: '&eYour gamemode was set to &6%mode% &eby &6%by%&e.'
  fly-enabled-self: '&aFlight &lENABLED&a.'
  fly-disabled-self: '&cFlight &lDISABLED&c.'
  fly-enabled-other: '&aEnabled flight for &e%player%&a.'
  fly-disabled-other: '&cDisabled flight for &e%player%&c.'
  fly-notify: '&eYour flight was %state% &eby &6%by%&e.'
  flyspeed-set-self: '&aFly speed set to &e%speed%&a.'
  flyspeed-set-other: '&aSet fly speed of &e%player% &ato &e%speed%&a.'
  flyspeed-invalid: '&cSpeed must be between &e1 &cand &e10&c.'
  flyspeed-notify: '&eYour fly speed was set to &6%speed% &eby &6%by%&e.'
  vanish-enabled-self: '&aYou are now &lVANISHED&a.'
  vanish-disabled-self: '&cYou are no longer vanished.'
  vanish-enabled-other: '&aEnabled vanish for &e%player%&a.'
  vanish-disabled-other: '&cDisabled vanish for &e%player%&c.'
  vanish-notify: '&eYour vanish mode was %state% &eby &6%by%&e.'

settings:
  allow-msg-colors: true
  social-spy-enabled: true
  msg-sound: true
  msg-sound-name: 'ORB_PICKUP'
  msg-sound-volume: 0.5
  msg-sound-pitch: 1.5
  vanish:
    enabled: true
    hide-from-mobs: true
    notify-staff: true

permissions:
  enabled: false
"""

# ---------- bungee bungee.yml ----------
TEMPLATES["bungeecord/src/main/resources/bungee.yml"] = r"""name: MineStormEssentials
version: 1.0
main: org.minestorm.essentials.bungee.MineStormBungee
author: Muvixo
description: Proxy-side MineStorm Essentials for BungeeCord
"""

# ---------- bungee config.yml ----------
TEMPLATES["bungeecord/src/main/resources/config.yml"] = r"""messages:
  msg-sender: '&7[&6me &7-> &6%receiver%&7] &f%message%'
  msg-receiver: '&7[&6%sender% &7-> &6me&7] &f%message%'
  social-spy: '&8[&cSPY&8] &7[&6%sender% &7-> &6%receiver%&7] &f%message%'
  msg-no-reply: '&cYou have nobody to reply to.'
  msg-self: '&cYou cannot message yourself.'
  no-permission: '&cYou do not have permission to do this.'
  player-not-found: '&cPlayer not found: &e%player%'
  player-only: '&cOnly players can use this command.'
  invalid-args: '&cUsage: &e%usage%'
  spy-enabled: '&aSocial spy &lENABLED&a.'
  spy-disabled: '&cSocial spy &lDISABLED&c.'

settings:
  social-spy-enabled: true
  allow-msg-colors: true
  forward-server-commands: true

permissions:
  enabled: false
"""

# ---------- velocity plugin json ----------
TEMPLATES["velocity/src/main/resources/velocity-plugin.json"] = r"""{
  "id": "minestormessentials",
  "name": "MineStormEssentials",
  "version": "1.0",
  "description": "Proxy-side MineStorm Essentials for Velocity",
  "authors": ["Muvixo"],
  "main": "org.minestorm.essentials.velocity.MineStormVelocity"
}
"""

# ---------- velocity config.yml ----------
TEMPLATES["velocity/src/main/resources/config.yml"] = r"""messages:
  msg-sender: '&7[&6me &7-> &6%receiver%&7] &f%message%'
  msg-receiver: '&7[&6%sender% &7-> &6me&7] &f%message%'
  social-spy: '&8[&cSPY&8] &7[&6%sender% &7-> &6%receiver%&7] &f%message%'
  msg-no-reply: '&cYou have nobody to reply to.'
  msg-self: '&cYou cannot message yourself.'
  no-permission: '&cYou do not have permission to do this.'
  player-not-found: '&cPlayer not found: &e%player%'
  player-only: '&cOnly players can use this command.'
  invalid-args: '&cUsage: &e%usage%'
  spy-enabled: '&aSocial spy &lENABLED&a.'
  spy-disabled: '&cSocial spy &lDISABLED&c.'

settings:
  social-spy-enabled: true
  allow-msg-colors: true
  forward-server-commands: true

permissions:
  enabled: false
"""

# ---------- Spigot: MineStormCommand.java ----------
TEMPLATES["spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java"] = r"""package org.minestorm.essentials.spigot.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.minestorm.essentials.spigot.util.MessageUtil;

/**
 * /minestormessentials - diagnostic command open to everyone.
 */
public class MineStormCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MessageUtil messages;

    public MineStormCommand(JavaPlugin plugin, MessageUtil messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String label, String[] args) {
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        sender.sendMessage(ChatColor.YELLOW + "  MineStorm Essentials v"
                + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "  Author: Muvixo");
        sender.sendMessage(ChatColor.GRAY + "  Permissions enforced: "
                + ChatColor.WHITE + messages.permsEnabled());
        sender.sendMessage(ChatColor.GRAY + "  Your OP: "
                + ChatColor.WHITE + sender.isOp());
        sender.sendMessage(ChatColor.GOLD + "=================================================");
        return true;
    }
}
"""

# ======================================================================
#  File I/O
# ======================================================================

def write(path: Path, content: str, dry: bool) -> bool:
    """Write file. Return True if written (or would write)."""
    if path.exists():
        return False
    if dry:
        dim(f"(dry-run) would create {path}")
        return True
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    ok(f"created {path}")
    return True

def read(path: Path) -> str:
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

def write_existing(path: Path, content: str, dry: bool) -> None:
    if dry:
        dim(f"(dry-run) would update {path}")
        return
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    ok(f"updated {path}")

# ======================================================================
#  Auto-merge missing files
# ======================================================================

def auto_merge_missing(root: Path, dry: bool) -> tuple[int, int]:
    """Create any file in TEMPLATES that is missing. Return (created, existing)."""
    created = 0
    existing = 0
    for rel, content in TEMPLATES.items():
        target = root / rel
        if write(target, content, dry):
            created += 1
        else:
            existing += 1
    return created, existing

# ======================================================================
#  Spigot: MessageUtil patch
# ======================================================================

MSGUTIL_HELPERS = """
    /** Returns true if the permissions system is enabled in config. */
    public boolean permsEnabled() {
        return plugin.getConfig().getBoolean("permissions.enabled", false);
    }

    /** Central permission check. When perms are disabled, always allows. */
    public boolean can(org.bukkit.command.CommandSender s, String node) {
        if (!permsEnabled()) return true;
        return s.hasPermission(node) || s.isOp();
    }
"""

def patch_spigot_messageutil(root: Path, dry: bool) -> bool:
    p = root / "spigot/src/main/java/org/minestorm/essentials/spigot/util/MessageUtil.java"
    if not p.is_file():
        err(f"missing {p}")
        return False
    src = read(p)
    if "public boolean permsEnabled()" in src and "public boolean can(" in src:
        info("Spigot MessageUtil.java already patched")
        return False
    # Insert helpers right after the constructor's closing brace
    anchor = "public MessageUtil(JavaPlugin plugin) { this.plugin = plugin; }"
    if anchor in src:
        src = src.replace(anchor, anchor + "\n" + MSGUTIL_HELPERS, 1)
    else:
        # Fallback: insert after class opening
        src = re.sub(
            r"(public final class MessageUtil\s*\{)",
            r"\1" + MSGUTIL_HELPERS,
            src,
            count=1,
        )
    write_existing(p, src, dry)
    return True

# ======================================================================
#  Spigot: replace hasPermission with messages.can
# ======================================================================

SPIGOT_CMD_FILES = [
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/GamemodeCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/FlyCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/FlySpeedCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/MsgCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/VanishCommand.java",
]

def patch_spigot_commands(root: Path, dry: bool) -> int:
    changed = 0
    for rel in SPIGOT_CMD_FILES:
        p = root / rel
        if not p.is_file():
            warn(f"missing {rel}")
            continue
        src = read(p)
        original = src
        src = re.sub(r'sender\.hasPermission\("([^"]+)"\)',
                     r'messages.can(sender, "\1")', src)
        src = re.sub(r'(?<![A-Za-z0-9_.])s\.hasPermission\("([^"]+)"\)',
                     r'messages.can(s, "\1")', src)
        if src != original:
            write_existing(p, src, dry)
            changed += 1
        else:
            info(f"{rel} — no permission checks to rewrite")
    return changed

# ======================================================================
#  BungeeCord: null permissions + can()
# ======================================================================

BUNGEE_SUPER_FIXES = {
    "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/MsgCommand.java": [
        ('super("msg", "minestorm.msg", "tell", "whisper", "w", "m", "pm");',
         'super("msg", null, "tell", "whisper", "w", "m", "pm");'),
    ],
    "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/ReplyCommand.java": [
        ('super("reply", "minestorm.msg", "r");',
         'super("reply", null, "r");'),
    ],
    "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/SocialSpyCommand.java": [
        ('super("socialspy", "minestorm.msg.spy", "spy");',
         'super("socialspy", null, "spy");'),
    ],
    "bungeecord/src/main/java/org/minestorm/essentials/bungee/commands/ForwardedCommand.java": [
        ('super(name, "minestorm." + name, aliases);',
         'super(name, null, aliases);'),
    ],
}

BUNGEE_CAN_HELPER = """
    public boolean can(net.md_5.bungee.api.CommandSender s, String node) {
        // Open by default; only restrict when permissions.enabled is true.
        if (!setting("permissions.enabled", false)) return true;
        return s.hasPermission(node);
    }
"""

def patch_bungee(root: Path, dry: bool) -> None:
    # 1. super(null) + hasPermission→can
    for rel, pairs in BUNGEE_SUPER_FIXES.items():
        p = root / rel
        if not p.is_file():
            warn(f"missing {rel}")
            continue
        src = read(p)
        original = src
        for old, new in pairs:
            src = src.replace(old, new)
        src = re.sub(r'sender\.hasPermission\("([^"]+)"\)',
                     r'plugin.getMessages().can(sender, "\1")', src)
        if src != original:
            write_existing(p, src, dry)
        else:
            info(f"{rel} — already patched")

    # 2. MessageUtil.can
    p = root / "bungeecord/src/main/java/org/minestorm/essentials/bungee/MessageUtil.java"
    if p.is_file():
        src = read(p)
        if "public boolean can(" not in src:
            src = src.replace(
                "public final class MessageUtil {",
                "public final class MessageUtil {" + BUNGEE_CAN_HELPER,
                1,
            )
            write_existing(p, src, dry)
        else:
            info("Bungee MessageUtil already has can()")
    else:
        warn("missing bungeecord MessageUtil.java")

# ======================================================================
#  Velocity: null permissions + can()
# ======================================================================

VELOCITY_CMD_FILES = [
    "velocity/src/main/java/org/minestorm/essentials/velocity/commands/MsgCommand.java",
    "velocity/src/main/java/org/minestorm/essentials/velocity/commands/ReplyCommand.java",
    "velocity/src/main/java/org/minestorm/essentials/velocity/commands/SocialSpyCommand.java",
    "velocity/src/main/java/org/minestorm/essentials/velocity/commands/ForwardedCommand.java",
]

VELOCITY_CAN_HELPER = """
    public boolean can(com.velocitypowered.api.command.CommandSource s, String node) {
        // Open by default.
        return true;
    }
"""

def patch_velocity(root: Path, dry: bool) -> None:
    for rel in VELOCITY_CMD_FILES:
        p = root / rel
        if not p.is_file():
            warn(f"missing {rel}")
            continue
        src = read(p)
        original = src
        src = re.sub(r'src\.hasPermission\("([^"]+)"\)',
                     r'plugin.getMessages().can(src, "\1")', src)
        if src != original:
            write_existing(p, src, dry)
        else:
            info(f"{rel} — already patched")

    p = root / "velocity/src/main/java/org/minestorm/essentials/velocity/MessageUtil.java"
    if p.is_file():
        src = read(p)
        if "public boolean can(" not in src:
            src = src.replace(
                "public final class MessageUtil {",
                "public final class MessageUtil {" + VELOCITY_CAN_HELPER,
                1,
            )
            write_existing(p, src, dry)
        else:
            info("Velocity MessageUtil already has can()")
    else:
        warn("missing velocity MessageUtil.java")

# ======================================================================
#  Config permission block
# ======================================================================

PERM_BLOCK = "\npermissions:\n  enabled: false\n"

CONFIG_FILES = [
    "spigot/src/main/resources/config.yml",
    "bungeecord/src/main/resources/config.yml",
    "velocity/src/main/resources/config.yml",
]

def patch_configs(root: Path, dry: bool) -> None:
    for rel in CONFIG_FILES:
        p = root / rel
        if not p.is_file():
            warn(f"missing {rel}")
            continue
        src = read(p)
        if "permissions:" in src and "enabled:" in src:
            info(f"{rel} — permissions block present")
            continue
        src = src.rstrip() + "\n" + PERM_BLOCK
        write_existing(p, src, dry)

# ======================================================================
#  plugin.yml: strip permission lines, ensure /minestormessentials
# ======================================================================

def patch_spigot_plugin_yml(root: Path, dry: bool) -> None:
    p = root / "spigot/src/main/resources/plugin.yml"
    if not p.is_file():
        warn("missing spigot plugin.yml")
        return
    src = read(p)
    original = src
    # strip `permission:` lines under each command
    src = re.sub(r"^\s*permission:\s*minestorm\.[\w.]+.*\n",
                 "", src, flags=re.MULTILINE)
    # ensure minestormessentials block exists
    if "minestormessentials:" not in src:
        if "commands:" in src:
            src = src.replace(
                "commands:",
                "commands:\n"
                "  minestormessentials: { description: Diagnostic, usage: /minestormessentials, aliases: [ mse, minestorm ] }",
                1,
            )
    if src != original:
        write_existing(p, src, dry)
    else:
        info("spigot plugin.yml already clean")

# ======================================================================
#  CommandManager: register MineStormCommand
# ======================================================================

def patch_command_manager(root: Path, dry: bool) -> None:
    p = root / "spigot/src/main/java/org/minestorm/essentials/spigot/CommandManager.java"
    if not p.is_file():
        warn("missing CommandManager.java")
        return
    src = read(p)
    original = src

    import_line = "import org.minestorm.essentials.spigot.commands.MineStormCommand;"
    if import_line not in src:
        anchor = "import org.minestorm.essentials.spigot.commands.MsgCommand;"
        if anchor in src:
            src = src.replace(anchor, f"{import_line}\n{anchor}", 1)

    reg_line = ('        register("minestormessentials", '
                'new MineStormCommand(plugin, messages));')
    if 'register("minestormessentials"' not in src:
        anchor = '        register("vanish", new VanishCommand(plugin, messages));'
        if anchor in src:
            src = src.replace(anchor, f"{anchor}\n{reg_line}", 1)

    if src != original:
        write_existing(p, src, dry)
    else:
        info("CommandManager already patched")

# ======================================================================
#  Sanity check
# ======================================================================

REQUIRED = [
    "pom.xml",
    "spigot/pom.xml",
    "paper/pom.xml",
    "bungeecord/pom.xml",
    "velocity/pom.xml",
    "spigot/src/main/resources/plugin.yml",
    "spigot/src/main/resources/config.yml",
    "spigot/src/main/java/org/minestorm/essentials/spigot/MineStormSpigot.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/CommandManager.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/util/MessageUtil.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/FlyCommand.java",
    "bungeecord/src/main/resources/bungee.yml",
    "velocity/src/main/resources/velocity-plugin.json",
]

def sanity(root: Path) -> int:
    missing = [rel for rel in REQUIRED if not (root / rel).is_file()]
    if not missing:
        ok("Sanity check passed — all required files present.")
        return 0
    err("Missing required files:")
    for m in missing:
        dim(f"- {m}")
    return 1

# ======================================================================
#  Main
# ======================================================================

def main() -> int:
    C.auto()
    ap = argparse.ArgumentParser(description="Auto-fix + auto-merge MineStormEssentials.")
    ap.add_argument("--root", type=Path, default=None)
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--verify-only", action="store_true")
    args = ap.parse_args()

    cwd = Path.cwd()
    info(f"Working directory: {cwd}")

    if args.root:
        root = args.root.resolve()
        if not is_real_root(root):
            err(f"--root is not the real project: {root}")
            dim("Expected: pom.xml, spigot/pom.xml, spigot/src/main/resources/plugin.yml")
            return 2
    else:
        root = find_real_root(cwd)
        if root is None:
            err("Could not locate the MineStormEssentials project root.")
            dim("Looked for a folder containing pom.xml AND spigot/pom.xml.")
            dim(f"Searched up from: {cwd}")
            dim("Tip: pass --root \"C:\\full\\path\\to\\MineStormEssentials\"")
            return 2

    ok(f"Project root: {root}")

    if args.verify_only:
        head("Sanity check (verify-only)")
        return sanity(root)

    # 1. Clean stray
    head("Cleanup")
    clean_stray(cwd, root, dry=args.dry_run)

    # 2. Auto-merge missing files
    head("Auto-merge missing files")
    created, existing = auto_merge_missing(root, dry=args.dry_run)
    info(f"Created: {created}, already present: {existing}")

    # 3. Spigot patches
    head("Spigot patches")
    patch_spigot_messageutil(root, dry=args.dry_run)
    n = patch_spigot_commands(root, dry=args.dry_run)
    info(f"{n} Spigot command files updated")
    patch_command_manager(root, dry=args.dry_run)
    patch_spigot_plugin_yml(root, dry=args.dry_run)

    # 4. Bungee patches
    head("BungeeCord patches")
    patch_bungee(root, dry=args.dry_run)

    # 5. Velocity patches
    head("Velocity patches")
    patch_velocity(root, dry=args.dry_run)

    # 6. Config permissions blocks
    head("Config permissions blocks")
    patch_configs(root, dry=args.dry_run)

    # 7. Sanity
    head("Sanity check")
    rc = sanity(root)

    print()
    if rc == 0:
        ok("All fixes applied.")
        dim("Next:")
        dim("  mvn clean package")
        dim("  git add . && git commit -m \"Fix build + open permissions\" && git push")
    else:
        warn("Some files are still missing — check output above.")

    return rc

if __name__ == "__main__":
    sys.exit(main())