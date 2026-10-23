# Carbon 7.9.9 → Java feature migration checklist

Status values distinguish implementation from evidence. **Migrated—requires further testing** does not mean verified. No row is marked verified without an automated check or a recorded manual acceptance step. The 60 rows are feature groups; each source submodule may contain additional behavior covered by the group acceptance criteria.

| ID | Audited Carbon feature | Java implementation / acceptance target | Status |
|---|---|---|---|
| F01 | Document creation, background and 8192px/40MP limits | Migrated—requires further testing — core creation/limits; dialog pending | Not migrated — foundation planned |
| F02 | Zoom, pan, fit, actual size, rotate and mirror viewport | Migrated—requires further testing — viewport core; JavaFX interaction pending | Not migrated — core planned |
| F03 | Grid, rulers, draggable guides, perspective display | Canvas overlays and persisted view preferences | Not migrated — core planned |
| F04 | Normal dry build-up brush | Migrated—requires further testing — incremental dry brush; parity/UI pending | Not migrated — core planned |
| F05 | Watercolor wash, wet edges, granulation and blooms | Java2D wash renderer and regional post-processing | Not migrated — engine parity required |
| F06 | Paint-mixing brushes | Under-color pickup and carry state | Not migrated — engine parity required |
| F07 | Smudge/blending brushes | Premultiplied carry and regional sampling | Not migrated — engine parity required |
| F08 | Procedural tips, spacing, stabilization, jitter, pressure/tilt/speed dynamics | Data-driven brush and deterministic dab renderer | Not migrated — engine parity required |
| F09 | Vertical, horizontal, quad and radial symmetry | Migrated—requires further testing — symmetry core; interaction/parity pending | Not migrated — core planned |
| F10 | Eraser and independent eraser presets | Migrated—requires further testing — alpha eraser/presets; UI pending | Not migrated — core planned |
| F11 | Built-in base and Windows brush library | Migrated—requires further testing — 152 definitions loaded by test; UI/parity pending | Not migrated — resource ported; rendering/UI pending |
| F12 | Brush Studio, custom brushes and custom erasers | Editors, previews and JDBC preset repositories | Not migrated — UI/persistence planned |
| F13 | Offline catalog, pack install/remove and `.carbonpack` transfer | Migrated—requires further testing — catalog/import/export core; JDBC/UI pending | Not migrated — resource ported; workflows pending |
| F14 | Brush Fusion recipes and dual stamp pass | Fusion editor and renderer support | Not migrated — advanced brush work |
| F15 | Bounded QR brush sharing/scanning | Local QR encode/decode without camera requirement | Not migrated — advanced asset work |
| F16 | Color Studio, HSL/RGB, recent colors and palettes | Color controls and normalized JDBC palettes | Not migrated — core/UI planned |
| F17 | Canvas picker/loupe and system screen picker | Composite/active-layer sampling; native screen-picker adapter | Not migrated — canvas picker first |
| F18 | Raster layers, visibility, lock, opacity, order, duplicate, merge, clear | Migrated—requires further testing — undoable layer core; panel pending | Not migrated — core planned |
| F19 | Alpha lock, masks, clipping and reference layers | Migrated—requires further testing — alpha/mask/clip core; complete acceptance pending | Not migrated — advanced layer work |
| F20 | Group and adjustment layers | Structural model and non-destructive compositor | Not migrated — advanced layer work |
| F21 | 18 layer blend modes | Migrated—requires further testing — 18 modes smoke-tested; visual parity pending | Not migrated — core planned |
| F22 | Region undo/redo, history labels and memory/step limits | Migrated—requires further testing — tiled undo/redo tested; soak/memory acceptance pending | Not migrated — core planned |
| F23 | Creative Time Machine snapshots and comparison | Versioned file snapshots, metadata and compare UI | Not migrated — persistence feature |
| F24 | Rectangle/ellipse/circle/freehand/polygon selection, invert/fill/clear | Migrated—requires further testing — selection/fill core; complete tool UI pending | Not migrated — core planned |
| F25 | Move and raster/vector transforms | Affine sessions, commit/cancel and undo | Not migrated — core planned |
| F26 | Flood fill, tolerance, reference sampling and expansion | Migrated—requires further testing — queue fill tested; reference-layer sampling pending | Not migrated — core planned |
| F27 | Line, rectangle, ellipse and rasterized text | Migrated—requires further testing — shape/text core; interaction acceptance pending | Not migrated — core planned |
| F28 | Linear/radial gradients and editable stops | Migrated—requires further testing — gradient core; stop editor/tests pending | Not migrated — editing feature |
| F29 | Filters and liquify | Region filters and warp operations | Not migrated — editing feature |
| F30 | Crisp Ink draw/select/move/point edit/recolor/taper/split-erase/rasterize | Vector paths, outline renderer and editor | Not migrated — vector engine required |
| F31 | PNG/JPEG/WebP/GIF import and embedded project assets | Migrated—requires further testing — ImageIO import/assets tested; WebP/plugin acceptance pending | Not migrated — compatibility planned |
| F32 | Scaled PNG/JPEG/WebP export, transparency and quality | Migrated—requires further testing — scaled PNG/JPEG export tested; remaining formats/UI pending | Not migrated — compatibility planned |
| F33 | SVG with real Crisp Ink vectors and explicit raster fallback | Versioned SVG exporter; no raster mislabeled as vector | Not migrated — compatibility planned |
| F34 | `.carbon` v1 save/open/Save As/Copy/duplicate | Migrated—requires further testing — v1 codec/save core tested; complete UI/workflows pending | Not migrated — compatibility planned |
| F35 | `.carboncomic` multi-page save/open | Nested Carbon pages and index validation | Not migrated — compatibility planned |
| F36 | Autosave, hidden/lifecycle save and session recovery | Migrated—requires further testing — recovery files/restore tested; lifecycle/UI acceptance pending | Not migrated — persistence planned |
| F37 | Recents and Project Library open/rename/duplicate/delete/export/search/pin/sort | JDBC project repository and library UI | Not migrated — persistence planned |
| F38 | Multiple documents, tabs, close guards and session restore | Migrated—requires further testing — tabs/lifecycle core and close guards; session restore/GUI pending | Not migrated — lifecycle planned |
| F39 | Drawing Replay recording, markers, epochs and memory cap | Serializable command journal and checkpoints | Not migrated — replay engine required |
| F40 | Replay play/pause/seek/speed/isolated reconstruction | Independent replay document and transport | Not migrated — replay engine required |
| F41 | Encoder-dependent replay video export | Real encoder integration or explicit unsupported result | Not migrated — no synthetic video |
| F42 | Comics pages, panels, balloons, effects and PDF | Comic model/editor/export | Not migrated — advanced studio |
| F43 | 3D scene, camera, surface drawing, symmetry and capture | Java 3D equivalent and `.carbon` scene extension | Not migrated — advanced studio |
| F44 | Infinite Painter creation/deep zoom | Independent document type and viewport path | Not migrated — advanced workflow |
| F45 | Continuum scene/layer/panel links | Link metadata, navigation and refresh | Not migrated — advanced integration |
| F46 | Orbit Board references, palettes, snapshots and links | Board model and JavaFX workspace | Not migrated — advanced integration |
| F47 | Color Dock project colors and linked workflows | Dock model and project extension | Not migrated — advanced integration |
| F48 | Palette Memory extraction and project snapshots | Quantization, project extension and panel | Not migrated — advanced integration |
| F49 | Reference Board, Navigator and Document Information | JavaFX panels and shared compositor | Not migrated — UI feature |
| F50 | Dock/floating panels, layouts and floating tool palette | Persisted workspace model and controls | Not migrated — UI feature |
| F51 | Configurable shortcuts and Ctrl+K command palette | Shortcut service, editor and searchable commands | Not migrated — UI feature |
| F52 | Stylus pressure/tilt, palm rejection and touch gestures | Native input adapter and physical-device acceptance | Blocked for first increment — JavaFX lacks Pointer Events pressure/tilt contract |
| F53 | 40 themes, custom themes, v1/v2 JSON validation and contrast | Extracted `themes.json`, Java theme service/editor | Not migrated — resource ported; workflows pending |
| F54 | Carbon Home, local coast artwork and optional surf motion | JavaFX Home and licensed/source assets | Not migrated — UI feature |
| F55 | Offline Quick Start and fresh editable sample | Guide and sample-document factory | Not migrated — UI feature |
| F56 | Diagnostics, release archive, About and Project Information | Headless/UI diagnostics and original credits/copy | Not migrated — verification UI |
| F57 | Local-first durable settings and metadata | Migrated—requires further testing — JDBC settings/palettes/migrations tested; full app wiring pending | Not migrated — foundation planned |
| F58 | Verified writes, overwrite protection and invalid/newer file rejection | Migrated—requires further testing — atomic/rejection tests; GUI overwrite acceptance pending | Not migrated — compatibility planned |
| F59 | Responsive large-canvas behavior and off-UI-thread work | Tile history, bounded caches and background services | Not migrated — performance acceptance required |
| F60 | Reproducible Windows runtime and optional EXE installer | `jpackage` scripts, CI and independent Windows launch | Not migrated — Windows packaging required |

## Current totals

- Feature groups identified: **60**.
- Migrated and verified: **0**.
- Migrated but requires further testing: **22**.
- Blocked for the first increment: **1** (F52; full stylus/tilt hardware contract).
- Not migrated: **37**.

These totals reflect the 2026-10-23 core checkpoint: 26 focused JUnit tests passed. No row is fully verified yet because UI, cross-compatibility, or platform acceptance remains. Update both rows and totals only with new evidence.
