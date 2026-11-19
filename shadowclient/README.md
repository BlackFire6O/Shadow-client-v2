# Shadow Client 2.0.0

Client-side Fabric mod for Minecraft Java 1.21.11 (Java 21).

## Visuals
- Player ESP boxes with selectable purple/cyan/red/green color.
- Target ESP: gold outline for the player currently under the crosshair.
- Tracers: camera-to-player lines.
- Health ESP: vertical health indicator beside each player.
- NameTags: player name and distance, billboarded toward the camera.
- Cosmetic local player trails.
- Chams-style glowing outline for nearby players (vanilla outline pass; not a filled through-wall shader).
- HUD radar showing nearby players relative to your facing direction (32-block range).
- Auto Sprint while moving forward.
- Fullbright toggle with gamma restoration when disabled.

## Utility
- Radar and utility toggles are available in the ClickGUI.
- Chams uses Minecraft's built-in glowing outline and may be affected by server entity metadata.

## Controls
- Right Shift: open ClickGUI
- R: toggle KillAura
- T: toggle TriggerBot
- G: toggle Player ESP boxes

## Build
Install JDK 21 and Gradle 8.14+ (or generate a Gradle wrapper with a compatible Gradle installation), then run:

```bash
gradle build
```

The remapped mod jar is in `build/libs/`.

Settings are stored in `.minecraft/config/shadowclient.properties`.

Combat training helpers and projectile aiming are restricted to local single-player worlds. Visual features are client-side only.


## v2.0 additions
- **Item ESP**: draws outlines around dropped item entities nearby.
- **Armor HUD**: shows equipped armor and remaining durability.
- **Auto Tool**: selects the hotbar tool with the highest destroy speed for the targeted block.
- **Auto Totem**: equips a totem from the hotbar into the offhand (multiplayer-capable).
- **Chest Stealer**: quick-moves container contents into the inventory, one slot per tick (multiplayer-capable).
- ClickGUI includes a subtle opening slide animation.

Some inventory automation is intentionally limited to local single-player worlds. This source archive has not been compiled or tested in-game.


## v2.0 multiplayer update
- Removed single-player-only gates from KillAura, projectile aim, middle-click pearl, Auto Totem, and Chest Stealer.
- KillAura now filters dead/spectator targets, smoothly adjusts view rotation, and adds a small randomized delay variation on top of the configured attack delay.
- Server behavior still depends on server permissions, inventory rules, and server-side validation; this client does not guarantee anti-cheat bypass.
