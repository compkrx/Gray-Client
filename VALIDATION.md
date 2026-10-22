# Validation

Target: Minecraft Java 1.21.11, Fabric Loader 0.18.4, Fabric API 0.141.3+1.21.11.
Build tools: verified Temurin JDK 21.0.12.1+1, Gradle wrapper 9.2.1, pinned Loom 1.14.10.

## Build and automated checks

- `bash scripts/setup-cloud.sh` completed successfully twice after the environment was prepared; it invokes the pinned wrapper with two workers and preserves TLS/checksum verification.
- Final build: `BUILD SUCCESSFUL`, including Java compilation, resource processing, JAR remapping and tests.
- Five `SettingTest` cases ran: 5 passed, 0 failed, 0 errors, 0 skipped. They cover corrupted types/nonfinite input, clamping after rounding, decimal-step stability, boolean validation and serialization round trips.
- Entrypoint and all three mixin classes exist. All 34 registered modules have translated toggle keys.
- The isolated settings test project also passed all five tests (`./gradlew -p verification test`).
- Headless display helper was checked with both a new authenticated display and an existing display, preserving child exit status and cleaning up only its own server.

## Live checks

A development client ran with software OpenGL under Xorg and loaded a disposable superflat single-player world.

- Fabric loaded Gray Client 3.0.0; log: `Initialized 34 modules; Right Shift opens the menu.`
- Right Shift opened the gray native-widget menu.
- Searching `infohud` found the correct module; settings pagination and module toggling worked.
- Changing InfoHUD's Speed setting saved `Speed: true` in `run/config/grayclient.json`. The file contained settings for all 34 modules.
- InfoHUD visibly rendered coordinates, facing, FPS, speed and separate block/sky light values.
- SilentAura caused a nearby cow's server-reported Health to change from `10.0f` to `6.0f` in the test world. This checks a normal passive living entity as well as the targeting/attack path.
- AutoPlant planted wheat on hydrated farmland using hotbar wheat seeds and restored the original selected item. The server confirmed it with `execute if block 6 -60 -3 minecraft:wheat run say GRAY_PLANT_OK`.
- No Gray Client module exception/disable or mixin application failure was observed during these checks.

## Limits

These are representative checks, not exhaustive testing of every module, interaction or server. Fishing bites, animal breeding, crystal/mace combat, knockback behavior, every UI resolution and multiplayer/anticheat behavior were not all exercised. The development launch uses compiled source and the mapped game; the distributable JAR was built/remapped successfully but was not separately installed in a Windows launcher during this session.

The cloud has no logged-in Minecraft account or audio device. Vanilla emitted expected authentication/Realms, audio and cursor warnings; single-player development checks continued. Software-rendered FPS is not a performance benchmark for a normal desktop.

Features from the uploaded catalog that are not implemented are listed honestly in MODULES.md. The uploaded reference clients were inspected as references; their GPL implementations were not copied into Gray Client.
