# MineStormEssentials v1.0

Multi-platform essentials plugin for **Minecraft 1.8.8**.

**Created by Muvixo**

Supported platforms:
- **Spigot / Paper** — full backend plugin (needed for gamemode, fly, vanish, etc.)
- **BungeeCord** — proxy plugin (chat messages work standalone; server commands are forwarded)
- **Velocity** — proxy plugin (same behaviour as BungeeCord)

## Module overview

| Module | Purpose | Install on |
|---|---|---|
| `spigot` | Backend plugin for Spigot / Paper 1.8.8 | Each backend server |
| `paper` | Identical to spigot, packaged for Paper 1.8.8 | Each Paper server |
| `bungeecord` | Proxy plugin for BungeeCord | Bungee proxy only |
| `velocity` | Proxy plugin for Velocity | Velocity proxy only |

## Command coverage

- `/gmc`, `/gms`, `/gmsp`, `/gma`  — gamemode change
- `/fly [player]` — toggle flight
- `/flyspeed <1-10> [player]` — set fly speed
- `/msg`, `/tell`, `/whisper`, `/w`, `/m`, `/pm` — private messages
- `/reply`, `/r` — reply
- `/vanish`, `/v` — vanish
- `/socialspy`, `/spy` (proxy) — social spy toggle

## Permissions

See the spigot module's `plugin.yml`.

## Proxy-only installation (no backend plugin)

Install the **BungeeCord** or **Velocity** jar on your proxy.

- Private messages, reply, and social spy work immediately, across all backend servers.
- Backend commands (`gmc`, `gms`, `fly`, `vanish`, ...) are forwarded to the backend
  via `minestorm:main` plugin messaging. For those specific commands to execute you
  must run the corresponding backend jar on the destination server.

If a server does not run the backend jar, the forwarded command will silently do nothing
there.

## Build

```bash
mvn clean package
```

Outputs:
- `spigot/target/MineStormEssentials-Spigot.jar`
- `paper/target/MineStormEssentials-Paper.jar`
- `bungeecord/target/MineStormEssentials-BungeeCord.jar`
- `velocity/target/MineStormEssentials-Velocity.jar`

GitHub Actions builds all modules automatically on push. Velocity is skipped on JDK 8
because Velocity requires Java 17.

## Credits

- **Muvixo** — Creator


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
