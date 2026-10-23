# Carbon: Java Edition — migration plan

## Target architecture

- **Language/runtime:** Java 21.
- **Interface:** JavaFX controls and Canvas for the viewport.
- **Document rendering:** UI-independent Java2D `BufferedImage` layers. This keeps drawing, export and tests deterministic without starting a JavaFX toolkit.
- **Build:** Maven; standard JavaFX, SQLite JDBC, Gson and JUnit dependencies.
- **Persistence:** SQLite through JDBC for application metadata and preferences; versioned SQL migrations.
- **Artwork/recovery blobs:** `.carbon` / `.carboncomic` files in user-selected locations and the per-user Carbon application-data directory.
- **Windows:** bundled-runtime `jpackage` app-image and EXE configuration, built and accepted on Windows.

## Package map

| Package | Responsibility |
|---|---|
| `ui` | JavaFX stage, Home, studio, panels, menus and dialogs |
| `canvas` | Document model, viewport, compositor and coordinate transforms |
| `tools` | Brush presets, stroke engine, input samples, selection and symmetry |
| `layers` | Layer operations and layer-specific data |
| `history` | Tile/region edits, structural edits, undo and redo |
| `projects` | Document lifecycle, multi-document state and save coordination |
| `storage` | Compatible archives, safe writes, autosave, recovery and assets |
| `database` | JDBC connection ownership, migrations and repositories |
| `settings` | Preferences, workspace state and themes |
| `export` | Composite image and vector/raster export |
| `services` | Shared application services and background-task boundaries |

## Compatibility contract

1. Keep `.carbon` format version 1; do not invent a v2 merely because the implementation language changed.
2. Read and write `mimetype`, `manifest.json`, layer PNGs/masks, ink/comic data, assets, thumbnail and existing extension fields.
3. Reject corrupt input, unsupported newer versions, invalid dimensions and missing referenced payloads with actionable errors.
4. Preserve unknown manifest/layer fields and unmodified archive payloads. If an advanced module cannot be edited, its data must not be silently discarded.
5. Verify a serialized archive before replacing a valid destination. Use same-directory temporary files and atomic move when supported.
6. SQLite stores metadata—not a row per brush stroke or canvas pixel.

## SQLite design

The initial migrations create:

- `schema_migrations` and `app_metadata` for versioning;
- `settings` and `workspace_preferences` for durable preferences;
- `projects` for local library/recent metadata and file references;
- `recovery_entries` for autosave generation, checksums and restore state;
- `palettes` / `palette_colors` for ordered user palettes;
- `brush_packs` / `brush_presets` for custom and installed definitions;
- `themes` for custom theme definitions and selection.

Foreign keys, uniqueness, indexes, prepared statements, transactions, WAL, busy timeout and foreign-key enforcement are mandatory. The database lives under the OS per-user application-data directory, not `target/` or a temporary folder.

## Delivery sequence and risk controls

1. **Foundation:** Maven, application paths, database/migrations, model and audit/checklist.
2. **Core engine:** raster layers, seeded stroke sessions, compositor, tile undo/redo, tools and viewport.
3. **Compatibility:** `.carbon` round trips, safe writes, import/export, autosave/recovery and repositories.
4. **JavaFX studio:** Carbon identity, menus, panels, shortcuts, multi-document workflows and background tasks.
5. **Advanced migration:** Crisp Ink editing, replay, advanced layers/effects, library/brush assets, themes, comics and spatial/extensions. Each receives its own evidence; placeholders are not completion.
6. **Performance:** representative canvas/layer/stroke workloads, bounded history and off-FX-thread file/database/image work.
7. **Distribution:** Windows Maven build, application startup, `jpackage` app-image/EXE and independent packaged-app acceptance.

## Principal risks and decisions

- **Brush parity:** browser Canvas and Java2D differ in antialiasing and compositing. Compatibility is tested by document behavior and image changes; bit-for-bit cross-runtime brush equality is not claimed without evidence.
- **Pressure/tilt:** JavaFX mouse events do not expose the full Pointer Events stylus contract. Mouse drawing works with explicit pressure defaults; physical stylus pressure/tilt requires a native input adapter and hardware acceptance.
- **Large canvases:** tile-based paint history avoids copying a canvas per pointer movement. Structural snapshots use copy-on-write layer images. Rendering and persistence must remain off the FX thread where appropriate.
- **Advanced extensions:** opening and re-saving a 3D/comic/replay project must not silently erase unknown data. Editing support is tracked separately from container preservation.
- **Native packaging:** a Linux source/test run cannot prove a Windows installer. Packaging claims require a Windows build log and an independent launch of the packaged app.
