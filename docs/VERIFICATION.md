# Verification evidence

Date: **2026-10-23**. This report distinguishes executed checks from pending product acceptance.

## Executed Java checks

The current core was compiled with **ECJ 3.45.0** and run on **Temurin JRE 17.0.19+10** because this sandbox does not provide a Java 21 JDK or resolvable Maven Central connection. The source remains targeted to Java 21 in `pom.xml`; the Java 17 fallback is an early core check, not the supported product build.

Result: **35 JUnit 4 tests passed, 0 failed**.

| Suite | Tests | What was actually exercised |
|---|---:|---|
| `DrawingEngineTest` | 6 | Real stroke pixels, undo/redo, partial 64×64 edge tiles, eraser, alpha lock, clipped flood fill, duplicate/merge/resize undo, Ink/selection scaling |
| `ViewportAndCompositeTest` | 5 | Rotated/flipped viewport round-trip, background/visibility compositing, smoke execution of all 18 blend modes, inverted selection |
| `CarbonArchiveTest` | 4 | Raster/Ink/mask/asset/selection round-trip, unknown entry/JSON preservation across a second save, newer/missing payload rejection, existing-file preservation after failed save |
| `BrushPresetLibraryTest` | 4 | 152 base brushes, 19 packs/396 presets, pack import/export, duplicate rejection, multi-field search |
| `DatabaseTest` | 3 | Schema migration/reopen, JSON settings, prepared-statement palettes with hostile names, metadata-only schema |
| `ProjectLifecycleTest` | 3 | Multiple tabs/history, close guard, background-save revision race, recovery restore/discard |
| `ImageIOServiceTest` | 3 | Real PNG import/export, scaling behavior, JPEG dimensions/opacity, asset undo, JDBC recents/rename/soft delete |
| `CarbonComicArchiveTest` | 2 | Nested independent Carbon pages, different dimensions/content, empty-container rejection |
| `SvgExportServiceTest` | 2 | Genuine Ink SVG path geometry, explicit PNG raster layer, declared adjustment fallback |
| `AssetRepositoriesTest` | 3 | JDBC installed packs, schema v2 custom themes, Theme Studio v2 export/import, 25 tokens and contrast validation |

The tests ran headlessly against compiled Java classes and the real bundled brush/theme resources. Separately, every production class—including the JavaFX workspace—compiled without errors against JavaFX 21.0.1 API jars. The mirror contains Windows natives, so this is API compile evidence only and did not launch a Linux GUI. They do not mock pixel writes, archive reads, or SQL queries.

### SQLite driver check

Maven Central was unreachable, so the maintained **xerial sqlite-jdbc 3.50.3.0** tagged source (`4781e381921a348fa2e533d0c43767525374b26d`) was compiled only in the ephemeral verification cache. Its bundled Linux x86_64 native library executed `SELECT 40+2` and returned **42**, then ran the three database tests above. The project continues to declare the normal Maven artifact; no third-party driver binaries or sources were vendored into this repository.

## Baseline TypeScript evidence

Before Java implementation edits, the untouched authoritative Carbon 7.9.9 tree passed strict typecheck, **568/568 Node assertions**, and the production Vite build. Those checks establish the audited baseline only; they do not test this Java port.


## Executed TypeScript ↔ Java codec probe

Using the untouched TypeScript 7.9.9 `serializeDoc`/`deserializeDoc`, real PNG encoding/decoding through the baseline's `sharp`, and browser canvas API shims:

- Java wrote a v1 project; TypeScript structurally verified and deserialized it. Assertions covered its 16×16 dimensions, red raster pixel, mask payload, nine Ink point values, eight selection values, embedded image asset, and scene extension.
- TypeScript then wrote a project; Java deserialized it and asserted dimensions, name, red raster pixel, Ink points, selection, and scene extension.
- The first probe exposed Carbon's minimum 16×16 canvas rule. Java validation was corrected; the probe then passed in both directions.
- The TypeScript-generated fixture is retained at `src/test/resources/com/carbon/studio/storage/typescript-7.9.9.carbon` (SHA-256 `1cff5fb30f6a05b8a2baaac2e5b92bfaf57662bf2e63daf171a6d28a844daeab`) and opened by `CarbonArchiveTest` on every test run.

This is meaningful codec interoperability for the covered constructs, not exhaustive compatibility for every brush, replay checkpoint, advanced module, or browser renderer.

## Windows packaging configuration

`packaging/windows/package.ps1` and `.github/workflows/windows-package.yml` stage runtime dependencies and define Java 21 test/app-image steps. They have **not executed** here. No Windows artifact or launch is included in the test count or completion claims.

## Not established

- No `mvn clean test` has run: Maven dependencies/plugins cannot currently be resolved in the sandbox.
- No Java 21 compiler or physical stylus test has run. JavaFX 21 API compilation passed, but no JavaFX GUI launch has run.
- No Windows build, `jpackage` image, installer, or Windows launch has been produced.
- No automated golden-image comparison against TypeScript rendering has run.
- No large-canvas soak, memory profile, responsiveness measurement, or performance parity result exists.
- No full `.carboncomic`, replay recording/playback, SVG/PDF/video, 3D, or advanced-studio acceptance exists.
- Passing focused tests do not make an entire feature group verified. The migration ledger keeps those distinctions.

## Repeat with the supported toolchain

On a machine with JDK 21 and Maven 3.9+:

```bash
mvn clean test
```

Later GUI/package commands will be documented only when their entry points and packaging configuration actually exist.

## GUI acceptance still required

When a JavaFX desktop runtime is available, manually verify: initial window/layout; every tool on an actual canvas; pan/zoom/rotation/mirror coordinate accuracy; layer selection and visibility; tab switching; dirty titles; Save/Save As/reopen; cancel/overwrite paths; import/export dialogs; recovery prompt; and clean application shutdown. None of those manual checks is represented as complete here.
