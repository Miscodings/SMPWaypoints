# CKSMPWaypoints

A Minecraft Paper/Folia waypoint plugin for CKSMP, styled after the waypoint system in Wynncraft.

## Indicator

Selected waypoints display a Wynncraft-style beacon beam made of two `BlockDisplay` entities (an inner colored beam and a slightly wider outer shell) anchored to the terrain surface. A `TextDisplay` floats at the player's eye level showing the waypoint name and distance. The text remains visible through walls via a dual-display approach.

The beam and label clamp horizontally to a configurable min/max radius around the player so it is always visible regardless of distance, without moving with the player.

Key config options (`config.yml` → `pointers.hologram`):

| Option | Description |
|---|---|
| `minDistanceFromPlayer` | Snaps to the waypoint when within this many blocks |
| `maxDistanceFromPlayer` | Clamps to this horizontal distance when farther away |
| `hologramHeightOffset` | Adjusts label height above player eye level |
| `textScaleNear` / `textScaleFar` | Label scale at min and max distance |
| `textForwardOffset` | Pushes label toward the player so it renders in front of the beam |
| `beam.height` / `beam.width` | Beam dimensions in blocks |
| `beam.block` | Block used for the inner beam |
| `beam.outer.extraWidth` / `beam.outer.block` / `beam.outer.brightness` | Outer shell settings |

## Waypoint Types

- **Private** — visible only to the creating player
- **Public** — visible to all players; shown on supported web maps
- **Permission** — visible only to players with the assigned permission node
- **Death** — created automatically on death; deleted after use
- **Temporary** — session-only, not persisted across logins

## Features

- GUI opened by sneaking and right-clicking with a compass (configurable)
- Organize waypoints into folders
- Share private waypoints with specific players
- Set custom icons for waypoints and folders using the item in your main hand
- Add descriptions to waypoints and folders
- Teleport to waypoints with optional Vault economy cost
- Player tracking — track the live location of another player
- Only one waypoint can be selected at a time; selection clears on logout or server restart
- Multi-world support with automatic Nether ↔ Overworld coordinate scaling
- SQLite storage with configurable periodic backups
- Folia compatible

## Web Map Integrations

Public waypoints appear on:
- [Dynmap](https://www.spigotmc.org/resources/dynmap.274/)
- [SquareMap](https://github.com/jpenilla/squaremap)
- [BlueMap](https://bluemap.bluecolored.de/)
- [Pl3xMap](https://modrinth.com/plugin/pl3xmap)

## Commands

| Command | Description |
|---|---|
| `/waypoints` | Open the GUI |
| `/waypoints set <Name>` | Create a private waypoint at your location |
| `/waypoints setPublic <Name>` | Create a public waypoint |
| `/waypoints setPermission <Permission> <Name>` | Create a permission-gated waypoint |
| `/waypoints setTemporary <X> <Y> <Z>` | Create a temporary waypoint |
| `/waypoints select <Name>` | Select a waypoint by name |
| `/waypoints deselectAll` | Deselect the current waypoint |
| `/waypoints teleport <Name>` | Teleport to a waypoint |
| `/waypoints other <Player>` | View another player's waypoints |
| `/waypoints statistics` | Show database statistics |
| `/waypoints reload` | Reload config (use this, not `/reload`) |

### Script Commands

| Command | Description |
|---|---|
| `/waypointsscript selectWaypoint <Player> <UUID>` | Select a waypoint for a player |
| `/waypointsscript deselectWaypoint <Player>` | Deselect a player's current waypoint |
| `/waypointsscript temporaryWaypoint <Player> <X> <Y> <Z>` | Create a temporary waypoint for a player |
| `/waypointsscript uuid <Query>` | Search waypoints by name to get their UUID |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `waypoints.command.use` | ✅ | Use `/waypoints` and open the GUI |
| `waypoints.command.other` | ❌ | View another player's waypoints |
| `waypoints.command.statistics` | ❌ | View database statistics |
| `waypoints.command.reload` | ❌ | Reload the configuration |
| `waypoints.command.scripting` | ❌ | Use `/waypointsscript` |
| `waypoints.modify.private` | ✅ | Create and manage private waypoints |
| `waypoints.modify.public` | ❌ | Create and manage public waypoints |
| `waypoints.modify.permission` | ❌ | Create and manage permission waypoints |
| `waypoints.modify.other` | ❌ | Modify another player's waypoints |
| `waypoints.modify.anywhere` | ❌ | Place waypoints in disabled worlds |
| `waypoints.unlimited` | ❌ | Bypass waypoint and folder limits |
| `waypoints.teleport.private` | ❌ | Teleport to private waypoints |
| `waypoints.teleport.public` | ❌ | Teleport to public waypoints |
| `waypoints.teleport.permission` | ❌ | Teleport to permission waypoints |
| `waypoints.temporaryWaypoint` | ✅ | Create temporary waypoints |
| `waypoints.temporaryWaypoint.other` | ✅ | Create temporary waypoints for other players |
| `waypoints.tracking.enabled` | ✅ | Use player tracking |
| `waypoints.tracking.trackAll` | ❌ | Track players who have hidden themselves |
| `waypoints.updateNotification` | ❌ | Receive update notifications on join |

## Requirements

- Paper or Folia (latest stable)
- Java 21
