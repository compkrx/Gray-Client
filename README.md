# Gray Client

A client-side Fabric mod for **Minecraft Java Edition 1.21.11**, built from the uploaded FarmClient project. The menu uses an original charcoal-gray frame with Minecraft's native widgets. This source version adds farming, inventory, and combat helpers alongside the existing modules.

**Requirements:** Java 21, Minecraft 1.21.11, Fabric Loader 0.18.4 or newer, and Fabric API for 1.21.11. Fabric API is a separate mod; it is not bundled into Gray Client. See [Windows build instructions](BUILD-WINDOWS.md) for the PowerShell and VS Code steps.

## Build and install

Use the included Gradle wrapper; a system `gradle` command is unnecessary.

```powershell
# Windows PowerShell, inside the folder containing gradlew.bat:
.\gradlew.bat build
```

```sh
# Linux/macOS, inside the project folder:
./gradlew build
```

A successful build produces `build/libs/gray-client-1.21.11-3.0.0.jar`. Put that JAR and a compatible Fabric API JAR in the `mods` folder of a Fabric 1.21.11 installation. Do not install a `-sources.jar`, the old FarmClient JAR, or a second copy of Gray Client alongside it.

This project targets Java 21 bytecode. JDK 21 is the recommended build JDK. JDK 25 requires a Gradle wrapper version that supports running on Java 25; installing Java extensions in VS Code does not select a compatible Java runtime or install a system Gradle command.

## Menu and controls

- Join a world and press **Right Shift** to open Gray Client.
- Choose a category or **All**, or search module names and descriptions.
- Click a module's **On/Off** button to toggle it. Use its arrow button to show settings.
- Adjust boolean settings or numeric **−/+** controls. Module and settings pages have separate navigation arrows, so long lists fit small GUI sizes.
- **Disable all** turns off every module, including the HUD. **Done** or **Esc** closes the menu.
- Change the menu key and assign optional module keys under **Options → Controls → Key Binds → Gray Client**. Module keys are unbound initially.

Settings are stored in `config/grayclient.json` inside the game directory. An existing `config/farmclient.json` is used for migration when no Gray Client configuration exists; the legacy file is retained. Setting values persist, but module toggles intentionally reset when the game restarts: every module starts off except HUD.

## Included modules

There are 34 registered module implementations. Their names appear in the menu with spaces for readability. The [feature coverage catalog](MODULES.md) maps the uploaded feature list to present, partial, and missing functionality; it does not count reference-client features as implemented modules.

| Area | Modules |
| --- | --- |
| Combat | SilentAura, AimAssist, TriggerBot, AutoClicker, HitSelect, WTap, Velocity, CrystalAssist, CrystalAura, MaceBot, AutoWeapon, ShieldBreaker, BowRelease |
| Player and inventory | AutoSprint, AutoEat, AutoTotem, AutoRespawn, AutoTool, AutoReplenish, FastPlace |
| Farming | AutoFarm, Nuker, VeinMiner, AutoPlant, OreFinder, AutoFish, AutoShear, AutoBreed, BonemealAura |
| Visual | Fullbright, ESP, HUD, InfoHUD, Zoom |

The added helpers work as follows:

- **VeinMiner:** while attack is held, searches a capped, six-neighbor group of matching ore or log blocks and mines through normal client mining progress within reach. It does not add server-side instant vein mining.
- **OreFinder:** scans a limited number of loaded positions per tick for ore next to air and reports coordinates. It cannot discover blocks that the server has not supplied to the client.
- **AutoPlant:** plants using seeds available in the hotbar and restores the previous selected slot.
- **AutoReplenish:** replaces low hotbar stacks from the main inventory when the item and its components match.
- **FastPlace:** repeats normal block placement while use is held, with a configurable 2–20 tick interval.
- **ShieldBreaker:** selects a hotbar axe while manually attacking a blocking target, then restores the previous slot.
- **BowRelease:** releases a bow that the player has already started drawing, after the configured 3–40 ticks; the default is 20 ticks.
- **AutoFish:** listens for a splash near your own fishing bobber, reels after a configured reaction delay, and optionally casts again. It needs a held rod or a hotbar rod it can select; fishing results and timing remain server-controlled.
- **AutoShear:** uses hotbar shears on nearby, visible, shearable sheep and restores the selected slot.
- **AutoBreed:** feeds eligible adult animals the matching breeding food from your hotbar, with a per-animal retry delay and normal server breeding rules.
- **BonemealAura:** uses hotbar bone meal on nearby fertilizable plants that can still grow. Crops-only targeting is enabled by default.
- **InfoHUD:** optionally shows coordinates, facing, FPS, horizontal movement speed, held-item or lowest-equipped durability, target-block name, and light level. Ping and saturation are not included.
- **Zoom:** toggles a configurable lower FOV and restores the previous FOV on disable or while a menu is open. Assign its toggle key in Minecraft Controls; it has no default hold-to-zoom key.

Combat helpers apply to eligible living entities according to their settings; armor stands and tamed pets are excluded where the shared target filter is used. They do not make every entity type attackable. Normal server reach, cooldowns, inventory state, line of sight, and server rules still affect the result. Crystal and mace modules require the corresponding items and valid game conditions. Baritone is not bundled.

## Cloud development

In the prepared cloud checkout, install or refresh the pinned JDK and build dependencies with:

```sh
cd /workspace/Gray-Client
bash scripts/setup-cloud.sh
```

The helper verifies the downloaded JDK checksum, uses the cloud's existing proxy and managed Java trust store, and runs the Gradle build with two workers. Normal Windows builds use the wrapper directly.

For a headless Linux development launch in this cloud machine:

```sh
cd /workspace/Gray-Client
JAVA_HOME=/workspace/toolchains/jdk-21 LIBGL_ALWAYS_SOFTWARE=1 scripts/start-headless.sh python3 scripts/cloud-build.py ./gradlew runClient --no-daemon --max-workers=2
```

The display helper reuses an accessible `DISPLAY` or creates an authenticated local Xorg dummy display, then cleans up only the display it started when the command exits. It needs the existing Xorg dummy driver, `xauth`, and `xset`; it does not install packages. Display logs are under `${TMPDIR:-/tmp}/gray-client-display`. This supports internal launch checks, not a browser game preview. A graphics driver providing the Minecraft-required OpenGL version is still required; cloud software rendering must be configured separately if the host lacks that support.

## Validation and limitations

The 34-module build passed on Java 21 / Gradle 9.2.1 / Loom 1.14.10, with all five settings tests passing. A live Minecraft 1.21.11 development client initialized all 34 modules and loaded a disposable single-player world. Right Shift, menu search, settings pagination, toggles, JSON settings saves, and InfoHUD rendering were checked. SilentAura reduced a nearby cow’s server-reported health from 10 to 6, and AutoPlant placed wheat on prepared farmland, confirmed by a server command. See [VALIDATION.md](VALIDATION.md) for evidence and limits. Other module paths and multiplayer behavior have not been exhaustively tested; no anticheat compatibility or undetectability is promised.

## Development

Modules extend `Module`, define settings with `bool()` and `num()`, and are registered in `Modules.ALL`. The registry supplies the menu, key bindings, and configuration entries. The Java namespace is `com.grayclient`, the mod ID is `grayclient`, and the displayed name is Gray Client. AutoFarm, Nuker, and VeinMiner use one mining controller at a time; enabling one disables the others.
