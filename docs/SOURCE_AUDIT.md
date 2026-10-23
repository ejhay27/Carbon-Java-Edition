# Carbon 7.9.9 source audit

Audit date: 2026-10-23

## Repositories inspected

| Role | Repository | Result |
|---|---|---|
| Source | `https://github.com/ejhay27/carbon-digital-drawing-studio` | Complete TypeScript/React source, tests, assets, Electron and Capacitor configuration |
| Destination | `https://github.com/ejhay27/Carbon-Java-Edition` | Initially contained only a two-line README; writable through the authorized Git workflow |

The owner repository listing contained no other repository with `Carbon` in its name. The source remote contains `main`, two Arena branches, and tags `v1.0.0` through `v1.0.4`.

## Authoritative 7.9.9 baseline

The default `main` branch is **not** the requested baseline: its package version is 7.0.0. The complete 7.9.9 source is on `arena/a19e311c-carbon-digital-drawing-studio`.

- Inspected tree: `bb391616ed706c8529a7b1c5881a582613deea1c`
- Candidate commit: `20fd5eed6b017c1344aab7a5b50735ef039ffc85`
- `git diff 20fd5ee..bb39161` is empty; the two commits identify the same tree.
- `package.json`: `carbon-studio` version `7.9.9`.
- `src/version.ts`: `WINDOWS_VERSION = '7.9.9'` with the `+ls` display suffix.
- `src/release/releases.ts` and `docs/AUDIT-7.9.9-candidate.md` explicitly call this an **unpublished local source candidate**, not a published release.

No source-repository files or refs were modified.

## Baseline verification performed in this environment

Using Node 22.22.3 and `npm ci --ignore-scripts` in a separate read/write audit clone:

- `npm run typecheck`: passed.
- `npm test`: **568 assertions passed, 0 failed**.
- `npm run build`: passed; Vite produced a single-file `dist/index.html` (2,029.58 kB, 678.85 kB gzip).

Electron's binary was intentionally not installed by this web/test dependency path. These results establish the inspected TypeScript baseline only; they do not establish Java parity or Windows installer acceptance.

## Architecture and dependencies found

- React 19, TypeScript 5.9, Zustand 5, Vite 7 and Tailwind 4.
- Canvas 2D document/brush/compositing engine; three.js spatial studio.
- Electron 33 shell and Electron Builder Windows NSIS/portable targets.
- Capacitor Android adapter, separately versioned as CARBON 1.0.0 and outside this Java Windows-edition migration.
- Roughly 23,460 lines under `src`, `electron`, and `tests` in the inspected tree.

Important source areas:

| Area | Source |
|---|---|
| Document commands, layers, history, fill | `src/engine/document.ts`, `src/engine/types.ts` |
| Seeded dab renderer and procedural tips | `src/engine/strokeRenderer.ts`, `src/engine/tips.ts` |
| Brush definitions and packs | `src/engine/brushes.ts`, `src/engine/brushPacks.ts` |
| Crisp Ink, compositor, filters, liquify | `src/engine/crispInk.ts`, `render.ts`, `filters.ts`, `liquify.ts` |
| Replay | `src/engine/replay.ts`, `src/components/ReplayPanel.tsx` |
| Project format | `src/engine/fileFormat.ts` |
| Browser persistence | `src/engine/persist.ts`, `src/platform/windowsLibrary.ts`, Zustand local-storage persistence |
| Main interface and input | `src/components/CanvasView.tsx`, `MenuBar.tsx`, `Dock.tsx` |
| Advanced Windows studios | `src/win/` |
| Themes | `src/theme/` |

## Existing persistence and file formats

- Preferences: browser local storage (`carbon-prefs-v1`) plus a separate theme key.
- Session, recents, references, backups and assets: IndexedDB (`carbon-studio`, version 2).
- Windows Project Library: a separate IndexedDB database and an Electron filesystem mirror.
- Creative Time Machine and brush assets: additional local IndexedDB paths.
- Artwork content: `.carbon` v1 ZIP, **not** one database row per stroke or pixel.
- Comics: `.carboncomic` ZIP containing complete `.carbon` pages and `comic.json`.
- Brush transfer: `.carbonbrush` / `.carbonpack` JSON; bounded QR payload support.
- Image exports: PNG/JPEG/WebP; SVG support in the 7.9.9 source has genuine Crisp Ink vector paths with a raster-wrapper fallback. Video export is encoder-dependent.

`.carbon` v1 contains `mimetype`, `manifest.json`, PNG raster layers and masks, optional embedded assets, replay events/checkpoints, and a thumbnail. Optional manifest extensions carry comics, 3D scenes, Color Dock, Orbit Board, Palette Memory, and Infinite Painter metadata. The reader rejects newer format versions, invalid dimensions, missing layers/assets/replay data, and corrupt JSON/images.

## Baseline feature inventory

The migration checklist in `FEATURE_MIGRATION.md` is the tracking authority. The inspected source includes:

1. Canvas creation and document limits; zoom/pan/rotate/mirror; grid, rulers, guides and perspective display.
2. Normal, watercolor wash, paint-mixing, smudge and Crisp Ink engines; seeded dabs, spacing, stabilization, pressure/tilt/speed dynamics, texture, scatter and symmetry.
3. 152 always-available base/Windows brush definitions in the extracted inventory, 19 offline catalog packs containing 396 generated presets, custom brushes/erasers, Brush Lab/Fusion, pack transfer and QR sharing.
4. Raster, ink, group, adjustment and comic layers; masks, clipping, reference layers, alpha lock, 18 blend modes, ordering, duplication, merging and rasterization.
5. Region pixel history, structural snapshots, configurable undo limits, Drawing Replay checkpoints and Creative Time Machine.
6. Selection, transforms, fill, shapes, rasterized text, gradients, filters and liquify.
7. Editable Crisp Ink paths and editing operations.
8. Image import/export, verified safe project writes, autosave/recovery, recents, Project Library and multiple documents.
9. Drawing Replay recording/playback/seek and encoder-dependent video export.
10. Comics, 3D, Infinite Painter, Continuum, Orbit Board, Color Dock, Palette Memory and reference/navigator panels.
11. Custom shortcuts/workspace, command palette, touch/stylus options, 40 built-in themes plus custom theme transfer, Home coast, Quick Start, diagnostics and About/release views.

The source also explicitly documents limitations: no animation timeline, no cloud/account system, no fluid simulation, visual-only perspective guide, smudge symmetry limitations, hardware-dependent stylus behavior, and encoder-dependent video/WebP behavior. These are not silently presented as newly implemented Java features.

## Environment constraints discovered

The sandbox initially had no Java or Maven command. Normal JDK release-asset redirects, Maven Central and Debian repositories are blocked by its outbound network policy. Local verification therefore uses legally redistributed compiler/runtime artifacts obtainable through allowed package/source hosts where necessary. The delivered project uses ordinary Maven coordinates and contains no sandbox-specific dependency launcher, Node runtime, or TypeScript application logic.
