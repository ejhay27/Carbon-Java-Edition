# Verification evidence

Date: **2026-10-23**. This report distinguishes executed checks from pending product acceptance.

## Executed Java checks

The current core was compiled with **ECJ 3.45.0** and run on **Temurin JRE 17.0.19+10** because this sandbox does not provide a Java 21 JDK or resolvable Maven Central connection. The source remains targeted to Java 21 in `pom.xml`; the Java 17 fallback is an early core check, not the supported product build.

Result: **19 JUnit 4 tests passed, 0 failed**.

| Suite | Tests | What was actually exercised |
|---|---:|---|
| `DrawingEngineTest` | 5 | Real stroke pixels, undo/redo, partial 64×64 edge tiles, eraser, alpha lock, clipped flood fill, duplicate/merge/resize undo |
| `ViewportAndCompositeTest` | 4 | Rotated/flipped viewport round-trip, background/visibility compositing, smoke execution of all 18 blend modes, inverted selection |
| `CarbonArchiveTest` | 3 | Raster/Ink/mask/asset/selection round-trip, unknown entry/JSON preservation across a second save, newer/missing payload rejection, existing-file preservation after failed save |
| `BrushPresetLibraryTest` | 4 | 152 base brushes, 19 packs/396 presets, pack import/export, duplicate rejection, multi-field search |
| `DatabaseTest` | 3 | Schema migration/reopen, JSON settings, prepared-statement palettes with hostile names, metadata-only schema |

The tests ran headlessly against compiled Java classes and the real bundled brush/theme resources. They do not mock pixel writes, archive reads, or SQL queries.

### SQLite driver check

Maven Central was unreachable, so the maintained **xerial sqlite-jdbc 3.50.3.0** tagged source (`4781e381921a348fa2e533d0c43767525374b26d`) was compiled only in the ephemeral verification cache. Its bundled Linux x86_64 native library executed `SELECT 40+2` and returned **42**, then ran the three database tests above. The project continues to declare the normal Maven artifact; no third-party driver binaries or sources were vendored into this repository.

## Baseline TypeScript evidence

Before Java implementation edits, the untouched authoritative Carbon 7.9.9 tree passed strict typecheck, **568/568 Node assertions**, and the production Vite build. Those checks establish the audited baseline only; they do not test this Java port.

## Not established

- No `mvn clean test` has run: Maven dependencies/plugins cannot currently be resolved in the sandbox.
- No Java 21 compiler, JavaFX runtime, GUI launch, or physical stylus test has run.
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
