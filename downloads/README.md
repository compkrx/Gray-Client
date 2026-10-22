# Gray Client downloads

Minecraft Java 1.21.11, Gray Client 3.0.0 (34 modules).

- [Built mod JAR](gray-client-1.21.11-3.0.0.jar): open the file on GitHub and choose **Download raw file**.
- [Complete source ZIP](gray-client-1.21.11-source.zip): includes the Gradle wrapper, source and build instructions.
- [Menu screenshot](gray-client-menu.png).
- [SHA-256 checksums](SHA256SUMS.txt).

Use Fabric Loader and Fabric API for Minecraft 1.21.11. Put the mod JAR in your instance's `mods` folder; Right Shift opens the menu in a world.

To build the source in Windows PowerShell with JDK 21, open the extracted project folder containing `gradlew.bat` and run:

```powershell
.\gradlew.bat build
```

See [Windows build instructions](../BUILD-WINDOWS.md), [feature coverage](../MODULES.md), and [validation results](../VALIDATION.md). The build and five tests passed; representative menu, HUD, settings, mob-hit and planting checks passed. Every module and multiplayer server have not been exhaustively tested.
