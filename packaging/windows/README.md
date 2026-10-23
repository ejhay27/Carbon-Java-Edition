# Windows packaging

**Configuration status:** scripted but not executed in the Linux migration sandbox. No EXE, MSI, app image, Windows launch, or installer acceptance is claimed.

## Requirements

- 64-bit Windows 10/11
- JDK 21+ with `jpackage` on `PATH`
- Maven 3.9+ on `PATH`
- Network access for the initial Maven dependency download
- For EXE/MSI: the WiX Toolset version required by your JDK's `jpackage`

Run from PowerShell:

```powershell
# Compile, run JUnit, stage runtime dependencies, and create a zipped app image
./packaging/windows/package.ps1 -Type app-image

# Only after the app image has been independently launched and accepted
./packaging/windows/package.ps1 -Type exe
```

Outputs are under `target/dist/`. The app image contains its own Java runtime and does not ask users to install Java, Node, npm, Vite, Electron, or TypeScript. Artwork is read/written in the user's chosen locations; settings/database/recovery use the per-user app-data directory.

`-SkipTests` exists for iteration but must not be used for a release candidate. `jpackage` must run on Windows for Windows output; this script intentionally rejects cross-building.

## Manual release gate

1. `mvn clean verify` succeeds.
2. The packaged `Carbon-Java-Edition.exe` launches on a clean Windows user account.
3. Create/draw/save/reopen, Save As, PNG export, SQLite settings, and recovery are checked.
4. The app is tested with no Node/npm installation and no repository checkout.
5. Installer and portable app are scanned and their hashes recorded.
6. A second Windows machine verifies installation, launch, and uninstall.

Do not publish an installer until these checks have real evidence.
