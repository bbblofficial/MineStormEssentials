# MineStormEssentials v1.0

Lightweight essentials plugin for **Minecraft 1.8.8** (Spigot / CarbonSpigot compatible).

**Created by Muvixo**

## Commands

| Command | Description | Aliases |
|---------|-------------|---------|
| `/gmc [player]` | Set gamemode to Creative | — |
| `/gms [player]` | Set gamemode to Survival | — |
| `/gmsp [player]` | Set gamemode to Spectator | — |
| `/gma [player]` | Set gamemode to Adventure | — |
| `/fly [player]` | Toggle flight | — |
| `/flyspeed <1-10> [player]` | Set fly speed | `/fspeed`, `/fs` |
| `/msg <player> <message>` | Send a private message | `/tell`, `/whisper`, `/w`, `/m`, `/pm` |
| `/reply <message>` | Reply to last message | `/r` |
| `/vanish [player]` | Toggle vanish mode | `/v` |

## Permissions

### Gamemode
| Permission | Default | Description |
|---|---|---|
| `minestorm.gamemode` | op | Use /gmc, /gms, /gmsp, /gma on self |
| `minestorm.gamemode.others` | op | Change gamemode of other players |

### Fly
| Permission | Default | Description |
|---|---|---|
| `minestorm.fly` | op | Toggle flight on self |
| `minestorm.fly.others` | op | Toggle flight for other players |

### Fly Speed
| Permission | Default | Description |
|---|---|---|
| `minestorm.flyspeed` | op | Set own fly speed |
| `minestorm.flyspeed.others` | op | Set fly speed for others |

### Private Messages
| Permission | Default | Description |
|---|---|---|
| `minestorm.msg` | true | Send private messages |
| `minestorm.msg.color` | op | Use color codes in messages |
| `minestorm.msg.spy` | op | Receive social spy messages |

### Vanish
| Permission | Default | Description |
|---|---|---|
| `minestorm.vanish` | op | Toggle vanish on self |
| `minestorm.vanish.others` | op | Toggle vanish for other players |
| `minestorm.vanish.see` | op | See vanished players |

### Wildcard
| Permission | Default | Description |
|---|---|---|
| `minestorm.*` | op | Grants all of the above |

## LuckPerms Examples

```
# Basic helper rank
/lp group helper permission set minestorm.fly true
/lp group helper permission set minestorm.gamemode true
/lp group helper permission set minestorm.msg true
/lp group helper permission set minestorm.vanish.see true

# Moderator rank
/lp group mod permission set minestorm.gamemode.others true
/lp group mod permission set minestorm.fly.others true
/lp group mod permission set minestorm.msg.spy true
/lp group mod permission set minestorm.vanish true
/lp group mod permission set minestorm.vanish.see true

# Admin (full access)
/lp group admin permission set minestorm.* true

# Grant a specific player social spy only
/lp user Notch permission set minestorm.msg.spy true

# Grant a specific player vanish only
/lp user Notch permission set minestorm.vanish true
```

## Installation

1. Drop the JAR into `plugins/`
2. Restart the server
3. `config.yml` is auto-created and safely merged on updates
4. Adjust permissions with LuckPerms

## Building

### Automatic (GitHub Actions)

Push to `main` or `master` → the workflow builds automatically and uploads
`MineStormEssentials-JAR` as an artifact.

Tag a commit to publish a GitHub Release:

```bash
git tag v1.0
git push --tags
```

### Manual

```bash
mvn clean package
```

Output: `target/MineStormEssentials.jar`

## Project Generator

If you cloned only `create.py`, run:

```bash
python create.py
```

This regenerates the entire project structure.

## Credits

- **Muvixo** — Creator
