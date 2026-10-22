# Build Gray Client on Windows

Use the **Gray Client source archive**, which includes `gradlew.bat`, `gradlew`, and `gradle/wrapper/`. The original FarmClient v2 archive did not include those wrapper files. The instructions below do not require `winget` or a system Gradle installation.

## 1. Install a JDK

Download and install **Temurin JDK 21** from the official [Eclipse Adoptium download page](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jdk). Choose the Windows installer for your machine. In the installer, enable adding Java to `PATH` and setting `JAVA_HOME` if those options are offered.

Close and reopen PowerShell and VS Code after installation. Then run:

```powershell
java -version
javac -version
```

Both commands should report version 21. If they do not, you can select the JDK for this terminal session. Replace the example folder with the actual JDK installation folder; it must contain `bin\java.exe` and `bin\javac.exe`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21-REPLACE-WITH-YOUR-INSTALLED-VERSION'
if (!(Test-Path "$env:JAVA_HOME\bin\javac.exe")) { throw 'Set JAVA_HOME to your actual JDK folder first.' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

**Can you use JDK 25?** Only if the project's Gradle wrapper supports running on Java 25. Java 21 is the recommended choice for this Minecraft target and avoids that extra compatibility requirement. The mod is compiled for Java 21 regardless.

## 2. Extract and open the correct project folder

Extract the new Gray Client source ZIP. Open the folder that directly contains `build.gradle`, `settings.gradle`, and `gradlew.bat`. It may be one folder inside the folder created by ZIP extraction.

In VS Code, use **File → Open Folder**, select that project folder, then use **Terminal → New Terminal**. VS Code's Java and Gradle extensions are optional conveniences; the wrapper below runs the build.

For a standalone PowerShell terminal, use `Set-Location` with your actual extracted folder. For example, if you extracted the new sources into this folder:

```powershell
Set-Location 'C:\Users\muhit\Downloads\Gray-Client'
Test-Path .\gradlew.bat
```

The second command must print `True`. Your earlier folder was `C:\Users\muhit\Downloads\farmclient-1.21.11\farmclient`; use that path only if it now contains the **new Gray Client sources and wrapper**. An unchanged old v2 extraction is not sufficient.

If you are unsure where the wrapper is, this command lists candidate source folders under Downloads:

```powershell
Get-ChildItem 'C:\Users\muhit\Downloads' -Filter gradlew.bat -Recurse -ErrorAction SilentlyContinue | Select-Object -ExpandProperty DirectoryName
```

Choose the result belonging to the extracted Gray Client project, then run `Set-Location 'THE-PATH-SHOWN'`.

## 3. Build

Once `Test-Path .\gradlew.bat` prints `True`, copy and paste:

```powershell
.\gradlew.bat --version
.\gradlew.bat build
```

The wrapper downloads the pinned Gradle distribution automatically. The first build also downloads Minecraft, mappings, Fabric, and other dependencies, so it needs Internet access. Keep download verification enabled.

Wait for `BUILD SUCCESSFUL`. Then locate the mod:

```powershell
Get-Item .\build\libs\gray-client-1.21.11-3.0.0.jar
explorer.exe .\build\libs
```

You do not need to run `gradle build`, add your separately downloaded Gradle folder to `PATH`, or use `winget`. PowerShell requires the `'.\'` prefix when running the wrapper in the current folder.

## 4. Install and open the menu

1. Install Fabric Loader for **Minecraft 1.21.11** using the [official Fabric installer](https://fabricmc.net/use/installer/).
2. Download Fabric API for **1.21.11** from the [Fabric API project page](https://modrinth.com/mod/fabric-api/versions?g=1.21.11).
3. Put Fabric API and `gray-client-1.21.11-3.0.0.jar` in that installation's `mods` directory. The default game directory is `%APPDATA%\.minecraft`; launchers with separate instances may use another directory.
4. Remove the previous FarmClient JAR or another copy of Gray Client from that same `mods` directory.
5. Launch the Fabric 1.21.11 profile, join a test world, and press **Right Shift**.

If the menu does not open, check **Options → Controls → Key Binds → Gray Client** for a conflicting or changed menu key. Module toggle keys start unbound. Settings are saved in the game directory's `config\grayclient.json`; module toggles reset off at restart except HUD. The menu contains 34 registered modules, including AutoFish, AutoShear, AutoBreed, BonemealAura, InfoHUD, and Zoom. Zoom uses a toggle key you assign in Controls; it has no preassigned hold key. See [the README](README.md#included-modules) for module behavior and the [feature coverage catalog](MODULES.md) for implementation status.

## If a command fails

| Message | Next step |
| --- | --- |
| `winget` is not recognized | Use the Temurin installer link in step 1. |
| `gradle` is not recognized | Run `.\gradlew.bat build` in the project folder. |
| `.\gradlew.bat` is not recognized | Check `Test-Path .\gradlew.bat`; locate the new source folder in step 2. |
| `JAVA_HOME` is invalid | Point it to the JDK folder containing `bin\javac.exe`, not its `bin` folder. |
| Unsupported Java or class version | Select JDK 21, reopen the terminal, and check `.\gradlew.bat --version`. |
| Fabric API or another required mod is missing | Install the dependency version matching Minecraft 1.21.11 into the same instance. |
| Dependency download fails | Check the first failing host/error, Internet access, and any proxy policy. Do not disable TLS or checksum checks. |
| Build still fails | Capture the first error and the output of `.\gradlew.bat build --stacktrace`; do not assume a JAR exists unless the build succeeds. |

The final 34-module build and all five settings tests passed in the cloud. A running Minecraft 1.21.11 development client passed menu, HUD, settings-save, representative mob-hit and planting checks. Other modules and multiplayer behavior are not exhaustively tested; see [VALIDATION.md](VALIDATION.md). Cloud-specific setup and headless launch commands are documented in [the README](README.md#cloud-development); Windows builds use the wrapper commands above.
