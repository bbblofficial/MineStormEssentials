#!/usr/bin/env python3
"""
fixer.py
--------
Robust, self-locating build fixer for MineStormEssentials.

What it does:
  1. Finds the MineStormEssentials project root (folder containing
     pom.xml AND a spigot/ directory). Walks up + one level down.
  2. Writes the missing MineStormCommand.java into the correct package.
  3. Adds the MineStormCommand import + registration line to
     CommandManager.java if missing.
  4. Adds the /minestormessentials entry to plugin.yml if missing.
  5. Cleans up any stray spigot/ folder that a previous bad run of a
     similar script may have created in the WRONG directory.
  6. Runs a lightweight sanity check on the final tree.

Usage:
  py -3.12 fixer.py               # auto-locates project root
  py -3.12 fixer.py --root PATH   # force a specific project root
  py -3.12 fixer.py --dry-run     # show actions without writing

Runs on Python 3.8+.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import sys
from pathlib import Path


# ----------------------------------------------------------------------
# Constants
# ----------------------------------------------------------------------

JAVA_REL = Path(
    "spigot/src/main/java/org/minestorm/essentials/spigot"
)
COMMANDS_REL = JAVA_REL / "commands"
CM_REL = JAVA_REL / "CommandManager.java"
PLUGIN_YML_REL = Path("spigot/src/main/resources/plugin.yml")

DIAG_CLASS_NAME = "MineStormCommand.java"
DIAG_CLASS_SOURCE = '''package org.minestorm.essentials.spigot.commands;

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
        sender.sendMessage(ChatColor.GOLD
                + "=================================================");
        sender.sendMessage(ChatColor.YELLOW + "  MineStorm Essentials v"
                + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "  Author: Muvixo");
        sender.sendMessage(ChatColor.GRAY + "  Permissions enforced: "
                + ChatColor.WHITE + messages.permsEnabled());
        sender.sendMessage(ChatColor.GRAY + "  Your OP: "
                + ChatColor.WHITE + sender.isOp());
        sender.sendMessage(ChatColor.GOLD
                + "=================================================");
        return true;
    }
}
'''


# ----------------------------------------------------------------------
# Logging helpers
# ----------------------------------------------------------------------

class C:
    OK = "\033[92m"
    WARN = "\033[93m"
    ERR = "\033[91m"
    INFO = "\033[96m"
    DIM = "\033[2m"
    END = "\033[0m"

    @staticmethod
    def strip():
        # Disable colors if stdout is not a TTY (e.g. piped to a file)
        if not sys.stdout.isatty():
            C.OK = C.WARN = C.ERR = C.INFO = C.DIM = C.END = ""


def log_ok(msg):    print(f"{C.OK}[+]{C.END} {msg}")
def log_warn(msg):  print(f"{C.WARN}[!]{C.END} {msg}")
def log_err(msg):   print(f"{C.ERR}[x]{C.END} {msg}")
def log_info(msg):  print(f"{C.INFO}[i]{C.END} {msg}")
def log_dim(msg):   print(f"{C.DIM}    {msg}{C.END}")


# ----------------------------------------------------------------------
# Project root discovery
# ----------------------------------------------------------------------

def is_project_root(p: Path) -> bool:
    return (
        (p / "pom.xml").is_file()
        and (p / "spigot").is_dir()
    )


def find_project_root(start: Path) -> Path | None:
    # 1) exact start
    if is_project_root(start):
        return start

    # 2) walk up
    cur = start.resolve()
    for parent in [cur, *cur.parents]:
        if is_project_root(parent):
            return parent

    # 3) direct children of start (and grandchildren)
    try:
        for entry in start.iterdir():
            if entry.is_dir() and is_project_root(entry):
                return entry
            if entry.is_dir():
                try:
                    for sub in entry.iterdir():
                        if sub.is_dir() and is_project_root(sub):
                            return sub
                except PermissionError:
                    continue
    except PermissionError:
        pass

    return None


# ----------------------------------------------------------------------
# Stray folder cleanup
# ----------------------------------------------------------------------

STRAY_MARKERS = [
    # Files that only our script writes — if they exist in a folder
    # that is NOT a real project root, it's a stray from a bad run.
    Path("spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java"),
]

def clean_stray_folders(cwd: Path, real_root: Path, dry: bool) -> None:
    """
    If the user previously ran the script in the wrong folder, a 'spigot/'
    tree may exist there without pom.xml. Nuke it.
    """
    candidate = cwd / "spigot"
    if not candidate.exists():
        return
    if real_root == cwd:
        return  # it's the real one, don't touch

    # Only remove if it looks like a partial stray (no pom.xml next to it)
    if (cwd / "pom.xml").exists():
        return

    # Confirm it contains one of our marker files (safety check)
    looks_like_ours = any((cwd / m).exists() for m in STRAY_MARKERS)

    if looks_like_ours or (candidate / "src").exists():
        log_warn(f"Removing stray folder: {candidate}")
        if not dry:
            shutil.rmtree(candidate, ignore_errors=True)
        else:
            log_dim("(dry-run, not deleted)")


# ----------------------------------------------------------------------
# File mutations
# ----------------------------------------------------------------------

def write_text(path: Path, content: str, dry: bool) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if dry:
        log_dim(f"(dry-run) would write {path}")
        return
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)


def ensure_diag_class(root: Path, dry: bool) -> bool:
    target = root / COMMANDS_REL / DIAG_CLASS_NAME
    if target.exists():
        log_info(f"Already present: {target.relative_to(root)}")
        return False
    write_text(target, DIAG_CLASS_SOURCE, dry)
    log_ok(f"Wrote {target.relative_to(root)}")
    return True


def patch_command_manager(root: Path, dry: bool) -> bool:
    cm = root / CM_REL
    if not cm.is_file():
        log_err(f"Missing: {cm.relative_to(root)}")
        return False

    src = cm.read_text(encoding="utf-8")
    original = src

    # --- import ---
    import_line = (
        "import org.minestorm.essentials.spigot.commands.MineStormCommand;"
    )
    if import_line not in src:
        # Insert right above the MsgCommand import (alphabetical)
        anchor = "import org.minestorm.essentials.spigot.commands.MsgCommand;"
        if anchor in src:
            src = src.replace(anchor, f"{import_line}\n{anchor}", 1)
        else:
            # Fallback: put it after the last import
            src = re.sub(
                r"(import org\.minestorm\.essentials\.spigot\.commands\.[A-Za-z]+;\n)(?!.*\1)",
                r"\1" + import_line + "\n",
                src,
                count=1,
            )

    # --- registration ---
    reg_line = (
        '        register("minestormessentials", '
        'new MineStormCommand(plugin, messages));'
    )
    if 'register("minestormessentials"' not in src:
        anchor = (
            '        register("vanish", new VanishCommand(plugin, messages));'
        )
        if anchor in src:
            src = src.replace(anchor, f"{anchor}\n{reg_line}", 1)
        else:
            # Last-ditch: insert before closing brace of registerAll()
            src = re.sub(
                r"(\n\s*}\s*\n\s*private void register\()",
                f"\n{reg_line}\\1",
                src,
                count=1,
            )

    if src == original:
        log_info("CommandManager.java already patched")
        return False

    write_text(cm, src, dry)
    log_ok(f"Patched {cm.relative_to(root)}")
    return True


def patch_plugin_yml(root: Path, dry: bool) -> bool:
    yml = root / PLUGIN_YML_REL
    if not yml.is_file():
        log_warn(f"Not found: {yml.relative_to(root)} (skipping)")
        return False

    src = yml.read_text(encoding="utf-8")
    if "minestormessentials:" in src:
        log_info("plugin.yml already has minestormessentials")
        return False

    block = (
        "  minestormessentials:\n"
        "    description: MineStorm Essentials diagnostic\n"
        "    usage: /minestormessentials\n"
        "    aliases: [ mse, minestorm ]\n"
    )

    if "commands:" in src:
        # Insert right after the "commands:" line
        src = src.replace("commands:\n", "commands:\n" + block, 1)
    else:
        src = src.rstrip() + "\n\ncommands:\n" + block

    write_text(yml, src, dry)
    log_ok(f"Patched {yml.relative_to(root)}")
    return True


# ----------------------------------------------------------------------
# Sanity check
# ----------------------------------------------------------------------

REQUIRED_FILES = [
    "pom.xml",
    "spigot/pom.xml",
    "spigot/src/main/resources/plugin.yml",
    "spigot/src/main/java/org/minestorm/essentials/spigot/MineStormSpigot.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/CommandManager.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/commands/MineStormCommand.java",
    "spigot/src/main/java/org/minestorm/essentials/spigot/util/MessageUtil.java",
    "bungeecord/pom.xml",
    "velocity/pom.xml",
    "paper/pom.xml",
    ".github/workflows/build.yml",
]

def sanity_check(root: Path) -> int:
    missing = [rel for rel in REQUIRED_FILES if not (root / rel).exists()]
    if not missing:
        log_ok("Sanity check passed — all required files present.")
        return 0
    log_err("Missing required files:")
    for m in missing:
        log_dim(f"- {m}")
    return 1


# ----------------------------------------------------------------------
# Main
# ----------------------------------------------------------------------

def main() -> int:
    C.strip()
    ap = argparse.ArgumentParser(
        description="Fix MineStormEssentials build errors."
    )
    ap.add_argument("--root", type=Path, default=None,
                    help="Force the project root (folder with pom.xml + spigot/).")
    ap.add_argument("--dry-run", action="store_true",
                    help="Show what would be done without writing anything.")
    args = ap.parse_args()

    cwd = Path.cwd()
    log_info(f"Working directory: {cwd}")

    if args.root:
        root = args.root.resolve()
        if not is_project_root(root):
            log_err(f"--root does not look like the project: {root}")
            log_dim("Must contain both pom.xml and a spigot/ directory.")
            return 2
    else:
        root = find_project_root(cwd)
        if root is None:
            log_err("Could not locate MineStormEssentials project root.")
            log_dim("Looked for a folder containing both pom.xml and spigot/.")
            log_dim(f"Started search from: {cwd}")
            log_dim("Try:  py -3.12 fixer.py --root \"C:\\path\\to\\MineStormEssentials\"")
            return 2

    log_ok(f"Project root: {root}")

    # --- housekeeping ---
    clean_stray_folders(cwd, root, dry=args.dry_run)

    # --- apply fixes ---
    print()
    log_info("Step 1/3: ensure MineStormCommand.java exists")
    ensure_diag_class(root, dry=args.dry_run)

    print()
    log_info("Step 2/3: patch CommandManager.java")
    patch_command_manager(root, dry=args.dry_run)

    print()
    log_info("Step 3/3: patch plugin.yml")
    patch_plugin_yml(root, dry=args.dry_run)

    # --- verify ---
    print()
    log_info("Sanity check")
    rc = sanity_check(root)

    print()
    if rc == 0:
        log_ok("All fixes applied.")
        log_dim("Next:")
        log_dim("  mvn clean package")
        log_dim("  git add . && git commit -m \"Fix build\" && git push")
    else:
        log_warn("Some files are still missing. Re-run create_plugin.py to regenerate the project.")

    return rc


if __name__ == "__main__":
    sys.exit(main())