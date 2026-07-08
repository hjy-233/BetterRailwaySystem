# BetterRailwaySystem

BetterRailwaySystem is a Fabric mod for Minecraft `1.21.1` that enhances the vanilla minecart and rail system without replacing it.

The goal is simple:

- keep full compatibility with vanilla rails, powered rails, detector rails, activator rails, and redstone behavior
- enhance minecart movement, operation, station handling, and line management
- avoid introducing a separate railway system like MTR

## Current Scope

This mod is built around the vanilla minecart entity and vanilla track network.

Implemented feature areas:

- enhanced vanilla minecart physics and control
- operational railway blocks for spawning, stopping, collecting, and announcing
- world line recording and line map visualization
- in-game configuration, commands, and balise asset management

## Features

### Minecart Behavior

- smooth acceleration on powered rails instead of instantly jumping to top speed
- smooth deceleration on unpowered powered rails instead of immediate hard stop
- configurable `maxSpeed`, `acceleration`, and `deceleration` in blocks per second
- configurable safe following distance for automatic braking behind a minecart on the same track path
- automatic recovery and re-acceleration after temporary blocking or rebound
- substep-based high-speed rail movement to improve curve and slope stability
- slope and high-speed movement tuning focused on keeping momentum on vanilla rails
- minecart chunk loading while the train is running
- unattended minecart auto-despawn after a configurable timeout
- minecart collision and push behavior against entities disabled to reduce traffic jams
- vanilla minecart rail friction sound reduced to nearly inaudible

### Multi-Passenger Minecart

- one minecart can carry up to `24` players by default
- all players share the same vanilla minecart entity
- each player keeps normal local first-person and third-person rendering
- other passengers in the same minecart are hidden to avoid player model overlap
- vanilla dismount flow is preserved

### Railway Balise

The `railway_balise` block is a rail-side trigger block for operational events.

Supported balise modes:

- `arrival`
- `departure`
- `announcement`
- `speed_limit_start`
- `speed_limit_end`

Balise behavior includes:

- sending title and subtitle announcements to current minecart passengers only
- playing custom audio to minecart passengers only
- showing a custom HUD image to minecart passengers only
- optional image persistence until the next balise or until the player leaves the minecart
- updating current station and next station state
- optional bossbar updates for onboard passengers
- temporary line speed limits with configurable speed limit start and end markers

### Stop Rail

The `stop_rail` block works as a rail-side stop control block for precise station stopping.

Supported behavior:

- configurable stop distance
- configurable dwell time
- wait modes: `immediate`, `timer`, `redstone`
- automatic restart after release, including a short launch boost so the minecart does not remain stuck

### Train Spawner

The `train_spawner` block can automatically create minecarts on existing vanilla rails.

Supported settings:

- city name
- line id
- line theme color
- spawn direction
- interval-based dispatch
- redstone-controlled dispatch
- circular line flag

Spawner behavior:

- spawns only when a valid rail exists at or above the block
- refuses to spawn if another minecart is already occupying the spawn area
- launches the minecart automatically in the configured or detected rail direction
- binds spawned minecarts to city, line, color, route recording, and circular-line state
- forces the spawner chunk to stay loaded

### Train Collector

The `train_collector` block acts as the end-of-line recovery point.

Supported behavior:

- removes the minecart when it reaches the collector
- records visited stations into the saved railway line map before removing the train
- saves line color and station coordinates along with station order

### Railway Map and Line Map

The mod includes both onboard line maps and world railway map views.

Supported behavior:

- open the map with the grave accent key `` ` `` / `~`
- open the current line map while riding
- open the world railway map while not riding
- city-based grouping of lines
- station transfer merging by shared station name within the same city
- line theme colors shown in the map and bossbar
- station search
- mouse wheel zoom
- drag-to-pan navigation
- station tooltip display with station name, city, line list, and saved coordinates
- per-city legends for line color lookup
- railway map clear actions for all data, one city, or one line from the config screen

### Bossbar and Line Theme

- current station and next station bossbar for onboard passengers
- bossbar color follows the line theme color
- line theme color is also reused by saved line data and map rendering

### Balise Asset Library

Custom balise media can be managed from the in-game library UI.

Supported behavior:

- image library
- sound library
- upload new `.png` images and `.ogg` sounds
- click-to-preview media
- select a media identifier directly into the balise editor
- automatic local resource pack generation and reload for uploaded assets

### Configuration

Available config values include:

- `maxSpeed`
- `acceleration`
- `deceleration`
- `safeFollowingDistance`
- `maxPassengers`
- `stopRailApproachDistance`
- `unattendedDespawnSeconds`

Configuration is available through:

- in-game config screen
- commands: `/betterrailwaysystem config show`, `/betterrailwaysystem config reload`, `/betterrailwaysystem config set <key> <value>`

## Environment

- Minecraft `1.21.1`
- Fabric Loader
- Fabric API
- Yarn mappings
- Java `21`

## Development

Common commands:

```bash
./gradlew build
./gradlew runClient
```

## Project Structure

Source layout:

- `src/main/java/org/dcstudio/minecart` minecart runtime logic
- `src/main/java/org/dcstudio/renderer` client UI and overlay rendering
- `src/main/java/org/dcstudio/network` client/server payloads and sync
- `src/main/java/org/dcstudio/station` railway block and block entity logic
- `src/main/java/org/dcstudio/config` config and command handling

## Notes

- The mod id is `betterrailwaysystem`.
- All custom behavior is designed around vanilla rails instead of replacing them.
- The mod keeps compatibility with vanilla powered rails, detector rails, activator rails, and normal rails.
- Train operation, station events, and map recording are all built on top of vanilla minecarts.

## License

This repository is licensed under `GPL-3.0`. See [LICENSE](LICENSE).

Author of the 3D model in the icon:[Mareon](https://sketchfab.com/3d-models/minecraft-powered-rail-1d23a225ea6f4ec8b357f12ad6588182),[khj008008](https://sketchfab.com/3d-models/minecraft-minecart-2nd-8d46f8e5649246d5a2dc034fffa7c79d)
