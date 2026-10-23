# Carbon: Java Edition

A Java desktop migration of **Carbon: Prime 7.9.9+ls**, separated from the TypeScript application. The original source repository and its 7.9.9 tree are preserved unchanged.

> **Status: core migration in progress.** This checkout is not yet a complete JavaFX desktop release. The drawing/document/persistence core and focused tests exist; the JavaFX application, several advanced studios, full compatibility acceptance, and Windows packaging are still pending.

## Implemented in this checkpoint

- Bounded document, raster/Ink/comic/group/adjustment layer models and view-only viewport transforms.
- Incremental seeded raster strokes, procedural brush tips, erasing, symmetry, selection clipping, and tiled undo/redo.
- Undoable layer commands, fill, shapes, text, gradients, affine raster transforms, and shared Java2D compositing.
- Crisp Ink path storage, outline rendering, and rasterization foundation.
- Audited offline data: 152 base brushes, 19 packs with 396 presets, and 40 themes.
- `.carbon` v1 ZIP/JSON/PNG reading/writing, required-payload validation, unknown JSON/entry preservation, atomic saves, backups, and reopen support.
- SQLite/JDBC schema migrations and prepared-statement settings/palette/project-index repositories. Artwork remains in project files, not per-pixel database rows.
- A programmatic JavaFX workspace with interactive tools, layer controls, document tabs, file dialogs, close guards, and autosave/recovery wiring. It has compiled, but not launched in this sandbox.

See [the evidence report](docs/VERIFICATION.md) and [all 60 migration groups](docs/FEATURE_MIGRATION.md) for precise limitations. No performance parity, full `.carbon` interoperability, GUI, or Windows installer claim is made.

## Build requirements

- JDK **21** (including `javac`; a JRE is not enough)
- Maven **3.9+**
- Network access to Maven Central on the first build
- Windows + `jpackage` for Windows application images/installers

The normal verification command is:

```bash
mvn clean test
```

The JavaFX entry point is `com.carbon.studio.CarbonLauncher`. On a desktop with the dependencies resolved, run `mvn javafx:run`. This command has not executed in the sandbox. No installer command is claimed yet.

## Source layout

```text
src/main/java/com/carbon/studio/
  canvas/    documents, viewport, selection, compositing, filters
  tools/     brushes, stroke samples/sessions, raster editing
  layers/    layer types, Crisp Ink, structural operations
  history/   bounded tiled and structural undo/redo
  projects/  document kinds
  settings/  built-in theme library
  storage/   .carbon archives, atomic files, app paths, JDBC repositories
src/main/resources/com/carbon/studio/data/
  brushes.json
  themes.json
src/test/java/  focused JUnit 4 tests
docs/           source audit, plan, migration ledger, verification
```

## Compatibility and local data

The archive codec targets Carbon **format v1** and rejects newer versions. It reads/writes raster PNGs, masks, Crisp Ink, comic object payloads, embedded assets, selections, metadata, and thumbnails. Unknown entries and JSON fields are retained where possible. Advanced modules without a Java editor are not thereby considered migrated, and preserved replay data does not mean new Java edits are being recorded.

Per-user data defaults to `%APPDATA%/Carbon Java Edition` on Windows, `~/Library/Application Support/Carbon Java Edition` on macOS, and `$XDG_DATA_HOME/carbon-java-edition` or `~/.local/share/carbon-java-edition` elsewhere. Override it for testing with `-Dcarbon.dataDir=/path/to/data`.

## Migration references

- [Source audit](docs/SOURCE_AUDIT.md)
- [Architecture and migration plan](docs/MIGRATION_PLAN.md)
- [Feature ledger](docs/FEATURE_MIGRATION.md)
- [Verification and known gaps](docs/VERIFICATION.md)
