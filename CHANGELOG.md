# Changelog

All notable changes to KZKT are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [v1.38.1] - 2026-09-04

### Fixed

- **Model Download Dialog Crash (Duplicate Keys)**: resolved `IllegalArgumentException: Key was already used` in Jetpack Compose `LazyColumn` when downloading LiteRT models by namespacing item keys across active downloads, installed models, and recommended presets.

## [v1.38.0] - 2026-08-26

### Added

- **On-Device LiteRT Provider (Offline AI Engine & GPU Acceleration)**:
  - Integrated Google AI Edge LiteRT-LM (`0.16.0`) runtime with native Qualcomm Adreno OpenCL GPU hardware acceleration and XNNPACK CPU fallback.
  - Catalog of 5 optimized preset models: `Gemma 4 E2B IT`, `Gemma 4 E4B IT`, `Qwen 3 1.7B`, `Qwen 3 0.6B`, and `Gemma 3 1B IT` with speed tier badges (`Ultra Fast`, `Balanced`, `High Accuracy`, `Pro Quality`) and minimum RAM indicators.
  - `ModelDownloadService` background downloader with foreground notification progress, cancellation support, Hugging Face access token support for gated repositories, and custom `.litertlm` file import via SAF.
  - **Inference Benchmark & Speed Metrics**: run sample inference tests from the Parameters tab to verify GPU shaders, compute latency, and measure generation speed in tokens per second (`tok/s`).
  - **Device Hardware & RAM Monitor**: real-time display of device model, SoC chipset, CPU core count/architecture, and available vs total RAM in settings.
  - **Smart Preset Tuning**: one-tap auto configuration of recommended temperature, top-K, max tokens, and micro-batch size tailored for the active LiteRT model.
  - **Shader Cache Maintenance**: clear compiled OpenCL GPU binaries and temporary inference buffers without removing model files.
  - **LiteRT Engine Logs Bottom Sheet**: dedicated telemetry log viewer with performance metrics, prompt preview, memory usage, and GPU-to-CPU fallback events.
- **Intelligent Hyphenation & Diamond Text Wrapping**:
  - Added dedicated `Hyphenator` engine supporting Indonesian phonotactic rules and English syllable hyphenation.
  - Implemented elliptical line-width calculation (`getEllipticalLineWidth`) to naturally shape multi-line dialogue into comic diamond/inscribed oval curves.
  - Tuned hyphenation threshold (10+ characters) to strictly reserve syllable breaking for true overflow words while keeping common dialogue words intact.
- **Conversational Scanlation Prompt & Font Aliases**:
  - Calibrated comic translation prompt in `Constants.buildOcrPrompt` for natural conversational Indonesian dialogue with smart OCR typo correction.
  - Added Wild Words font preset aliases in `TextRenderer`.

### Changed

- **Optical Vertical Centering & Baseline Typography**:
  - Measured exact font cap-height via `Paint.getTextBounds` for uppercase comic dialogue.
  - Calculated vertical baseline placement using symmetric 50/50 optical centering.
  - Dynamic line gap expansion (18%–22%) on tall bubbles to prevent tight text clumping.
  - Calibrated default font scaling (0.84x–0.88x) and bubble margins (0.72x–0.78x) for clean, non-crowded initial text rendering.
- **Clean & High-Contrast Translation Logs**:
  - Simplified log outputs by removing verbose absolute file paths in favor of clean file names (`Saved: filename.jpg`).
  - Cleaned up tree indentation characters across single-page, chunk, and worker log pipelines.
  - Added smart color-coding and highlight tags for page headers, OCR detection, inference metrics, and error states in Translation Logs.
- **Core Pipeline & UI Architecture Modularization**:
  - Extracted `BatchPageTranslator` and `SinglePageTranslator` from monolithic `TranslationPipeline`.
  - Extracted `WorkerNotificationManager` from `TranslationWorker`.
  - Modularized `InteractiveEditorDialog` into `EditorToolbar`, `EditorCanvasArea`, and `EditorBottomBar`.
  - Modularized `MangaReaderDialog` into `ReaderTopBar`, `ReaderBottomBar`, and `WebtoonScroller`.
  - Extracted UI delegate handlers (`LocalAiUiDelegate`, `ProviderUiDelegate`, `UpdateUiDelegate`) from `MainViewModel`.
  - Extracted provider instantiation and failover resolution into `WorkerProviderHelper`.

### Fixed

- **Model Download Cancellation**: resolved issue where cancelling a download did not stop the background network stream promptly and produced false error alerts.
- **Direct Public Model URLs**: updated preset endpoints for `Qwen 3 1.7B` and `Gemma 3 1B IT` to public direct download links requiring no Hugging Face authentication tokens or gated agreements.
- **Model Downloader Foreground Notification Crash**: fixed immediate force close when starting model downloads caused by invalid `<adaptive-icon>` XML resource in `setSmallIcon`, replacing it with standard monochrome system drawables.
- **LiteRT Active Model Validation**: resolved issue where switching LiteRT models failed pre-translation check with `Model not found on endpoint` due to missing provider mapping in `modelFor` and stale defaults.
- **Worker Fallback Mapping**: added missing Anthropic API key resolution in `WorkerProviderHelper` failover chain.

## [v1.37.1] - 2026-08-18

### Added

- **Multi-Image Parallel Batching**: selecting multiple images (`files.size > 1`) now runs through the batched translation pipeline in groups of up to 6 pages in parallel, combining YOLO bubble detection and making unified single-request LLM calls instead of sequential one-by-one translation, matching PDF speed (3x–5x faster).
- **Touch-up Metadata Persistence for Batch Pages**: batch and PDF translated pages now save full edit metadata (`EditMetadataRepository`), allowing the interactive bubble editor to be used on all batch results.

### Fixed

- **File Card Progress Spinner on Re-Translation**: fixed an issue where the circular loading animation on file cards stopped spinning or failed to appear when translating again or re-running batches by refining the state check (`state != "done" && state != "failed"`) and resetting `isCancelled` state properly.

## [v1.37.0] - 2026-08-18

### Added

- **First-Run Onboarding Tutorial & Guide**: interactive 4-step walkthrough modal upon first launch highlighting core features, crucial API Key configuration with one-tap shortcut to Settings, import workflows, and History touch-up editor. Can also be reopened anytime from Settings → Data & Updates → App Tutorial & Quick Guide.
- **History Multi-Select & Select All**: modernized selection mode with a dedicated checklist icon in the header, dynamic select all / deselect all action, and back-button handling to exit selection mode.
- **Interactive Quick Config Badges**: tap the Target Language (`→ [Language]`) or Provider badge on the main Translate screen to open a modal bottom sheet picker and switch preferences instantly without going to Settings.
- **Modern Stepped Progress UI**: 3-phase translation progress tracker (`Scan` → `Translate` → `Render`) with active step highlighting and continuous progress indicator.
- **Modal Bottom Sheet System Logs**: tap the System Logs button to view live logs in a slide-up modal bottom sheet with category color tags, log counter, and one-tap copy button.
- **Integrated Inter UI Font**: bundled the Inter font family (Regular, Medium, SemiBold, Bold) across all Material 3 typography tokens for a crisp, modern UI.
- **Top Jump Chips in Settings**: horizontal quick navigation bar to jump directly to any settings category.
- **Back Navigation in Glossary**: integrated top navigation bar with back arrow in the Custom Glossary screen.
- **Streaming (SSE) Responses**: all providers (OpenAI-compatible, Gemini, Anthropic) now stream responses over SSE with automatic fallback to plain requests when streaming is unsupported; a new *Streaming (SSE)* toggle in Settings disables streaming entirely, and stalled streams fall back after a 120-second deadline instead of hanging.
- **Native Anthropic Provider**: dedicated Anthropic provider using the Messages API with `x-api-key` auth, vision + text support, and `content_block_delta` SSE parsing — no longer needs the Custom-endpoint workaround.
- **Clean Render Style**: new flat render preset (solid patch, no stroke/uppercase) alongside the default manga style, selectable from the Render Style setting.
- **Multi-Script Local OCR**: OCR script options for English, Japanese, Korean, Chinese and Auto — ML Kit loads the matching recognizer, so bubble and free-text OCR works on Korean/Chinese comics too.
- **Auto-Detect & Verify Provider Models**: switching providers in Settings automatically fetches the endpoint's model list (no manual Detect needed); before a run starts, the saved model is checked against that list — an unknown model aborts fast with a clear log message instead of hanging for minutes, and is flagged under the model dropdown.
- **In-App System Error Logs**: background failures (OpenCV init, YOLO model load, corrupted history, backup/restore, PDF MediaStore, API-key encryption) are no longer silent — Settings → Advanced → **System Logs** opens a bottom-sheet viewer with copy and clear buttons. The entry only appears while *Verbose Developer Logs* is enabled.

### Changed

- **Settings & Editor Modularization**: extracted large components from `InteractiveEditorDialog` (`BubbleEditCard`, `BatchEditDialog`) and `SettingsScreen` (`SettingsSections`, `SettingsDialogs`) into focused modular files.
- **Translation Logs UI**: renamed System Logs to Translation Logs on the main translation screen and added a one-tap clear logs button.
- **Lock-Free Multi-Core OpenCV Inpainting**: parallelized bubble inpainting across all CPU cores without global mutex locks for significantly faster rendering.
- **Mask-Aware Free Text Detection**: speech bubbles detected by YOLO are masked out before ML Kit OCR scans the full page, eliminating redundant recognition work.
- **Adaptive Rate Limiting**: reduced default minimum request delay from 2.0s to 0.5s for faster multi-chunk and batch transitions while preserving exponential backoff on HTTP 429.
- **Pre-Allocated Base64 JPEG Buffer**: pre-sized compression stream buffer to minimize memory re-allocations and GC pressure during vision payload preparation.
- **Internal stability improvements**: refactored the monolithic image-processing and History UI modules into smaller focused units for easier maintenance — no behavior change.
- **Settings Screen Reorganization**: streamlined settings into 5 structured categories (*AI & Provider*, *Detection & Engine*, *Text & Rendering*, *Appearance*, *Data & Updates*) with dedicated section icons and card containers.
- **Full English Localization**: unified all UI labels, action buttons, descriptions, and empty state placeholders into standard English.
- **History Screen Visual Upgrade**: pill-shaped search bar with clear button, pill sort chips, and updated folder/item card styling.
- **Result Preview Card**: modernized result container with pill action buttons (*View in App*, *Gallery*, *Edit*, *Share*).
- **History Folder View**: translation batches now render as folder cards with drill-down, two-level selection (folder selects all pages, page-level selection inside), and search result counts; single-image runs save directly to Downloads/KZKT.
- **Numeric-Aware Sorting in History**: sorting by name now orders pages naturally (1, 2, 10) inside each batch instead of lexicographically.
- **Coverage-Checked Vision Retry**: when a vision batch still misses bubbles after translation, the failed mosaic is split in half and retried for better coverage.

### Fixed

- **OpenCV Native Use-After-Free**: resolved fatal SIGSEGV crash in `PagePreparer` caused by releasing `cropMat` before bitmap conversion.
- **Bitmap Allocation Overhead**: removed redundant OpenCV Mat allocations for dimension checks and non-inpainting renders in `TranslationPipeline`.
- **Delicate GlobalScope API**: switched `PdfReaderDialog` lifecycle disposal to structured `CoroutineScope`.
- **Exception Logging**: standardized raw `printStackTrace()` calls to structured Android log outputs.
- **History Selection Action Bar Placement**: fixed floating ZIP/PDF/Delete action bar appearing at the top of the screen overlapping header controls; positioned at the bottom with animated slide transition and adjusted list content padding.
- **Theme Mode Auto Persistence**: resolved issue where selecting `Auto` (system theme) was immediately overwritten with `dark` or `light` in DataStore.
- **Appearance Spacing & Squeezed Labels**: resolved cramped layout in Theme Mode and Accent Color settings by moving selectors into full-width card description rows.
- **Free Text Not Rendered in PDF Batches**: free-text crops in the batch path used page-prefixed ids that vision models dropped from their responses — they now use globally unique bare `ftN` ids (matching the working single-image format) with key normalization before render.
- **"Immutable bitmap passed to Canvas" Crash**: the non-inpainting batch render path crashed with this error on translated pages — the bitmap is now copied before drawing.
- **Silently Dropped OCR Batches**: when the LLM did not echo crop ids in its JSON, a batch was marked successful but pages were saved without translations — batches now detect missing ids and automatically fall back to the vision path (only for ids actually sent, so no-text bubbles no longer trigger pointless fallbacks or cancel/retry loops).
- **Vision Fallback Crash Aborted the Whole Run**: an error inside the vision fallback (mosaic build) killed the run and forced a manual retry — it is now caught and logged, and the remaining batches continue.

## [v1.36.7] - 2026-08-16

### Added

- **Per-batch output folders**: every translation run (folder, multi-select, ZIP/CBZ or PDF) now saves its translated pages into its own folder under `Downloads/KZKT`, named with the date, time and page count (e.g. `2026-08-16 14-32 (4 pages)`). Translated PDFs and ZIP/PDF exports stay in the main `Downloads/KZKT` folder.
- **Batch separators in History**: translation runs are now visually separated in the History tab with a divider and a label showing page count, finish time, and any failed pages (e.g. `4 pages · 14:32`).
- **By Time / By Name sorting in History**: sort the list by translation time or by file name, with a direction toggle — ascending flips pages within each batch so page 1 is always on top. The reader (gallery view) follows the same order.

### Changed

- **Reader opens on the tapped page**: opening a result from History or from a batch preview now starts the reader on the exact page you tapped, instead of jumping to a previously saved reading position.
- **History filters simplified**: provider, language and date-range filters were removed; the search box and the new sort controls remain.
- **History grouping is now per run**: entries are grouped by their translation run (batch ID) instead of by file-name heuristics, so pages stay together even when the source file names differ. Legacy entries fall back to the old name-based grouping.

### Fixed

- **History reader showed only "1/1"**: pages of a batch with unrelated file names no longer get split into one-page groups, so swiping and webtoon view work across the whole run.
- **Reader pages appeared in random order**: pages are now ordered by translation time (oldest = page 1) rather than by file name, and the order matches the sort chosen in History.
- **Progress UI stuck at "3/4"**: when a batch finished, the progress bar could stay at 3/4 with the Cancel button still visible — coalesced completion events are now drained, so the UI always settles on the final count and hides Cancel.

## [v1.35.0] - 2026-08-11

### Added

- **Retry failed pages from History**: failed translations are now recorded in the History tab with a "Failed" badge and a Retry button. Tapping it re-runs only that source file, and a successful retry replaces the failed entry instead of duplicating it. (The same cleanup applies to retried PDFs.)
- **Retry failed pages from the Translate tab**: after a batch finishes, a "Retry Failed (n)" button appears when any file failed and re-enqueues only the failed pages, leaving the successful ones untouched.
- **Per-file batch status**: every selected file in the Translate tab now shows a live status icon (processing / done / failed) as the batch runs.
- **Reading-position bookmark**: the reader remembers the last-read page per book (grouped chapter/folder). Reopening a book from History or from a batch result resumes where you left off instead of always starting at page 1.
- **Rendered text settings**: choose the translated bubble text color (Auto / White / Black) and a global font scale slider (80%–150%) in Settings — applied to all new translations.
- **JPEG output quality setting**: a new slider in Advanced settings (70–100) controls the compression quality of saved .jpg/.jpeg translations, trading file size against image quality.
- **Multi-select in History with ZIP/PDF export**: the History tab gains a Select mode. Pick several entries, then export them as a single ZIP or PDF (images only, ordered by page name) or delete them in bulk with one Undo action.
- **Immersive fullscreen in the reader**: a new fullscreen toggle hides the system status/navigation bars for distraction-free reading (swipe from an edge to bring them back).

### Changed

- **Real pinch-to-zoom in the reader**: pinch gestures now zoom in/out from any level, not just double-tap — the previous gesture handler only enabled zoom after a double-tap had already zoomed in.
- **More responsive panning while zoomed**: single-finger drags move the page 1.8x faster than the finger, and the pan is clamped so the page can never be pushed fully off-screen.
- **History export saves straight to Downloads/KZKT**: exporting selected pages as ZIP or PDF copies the file into the public Downloads/KZKT folder and shows a confirmation toast — no share sheet anymore (ZIP/CBZ files now get the correct `application/zip` MIME type in MediaStore).
- **Better selection-mode icons**: the History action bar now shows a proper folder-zip icon for ZIP export and an outline delete icon, matching the PDF/file icons used elsewhere.
- Failed History entries now keep their source input path so they can always be retried later.

## [v1.30.4] - 2026-08-11

### Changed

- **4x faster image saving**: translated `.jpg`/`.jpeg` pages are now encoded as JPEG at quality 92 instead of lossless PNG, cutting encode time from ~1.4 s to ~0.33 s on upscaled 2x pages and shrinking output files about 4x (16.6 MB -> 4.3 MB) — measured with a dedicated encode benchmark. `.png`/`.webp` and other extensions keep lossless PNG so the filename extension and gallery MIME type always match.
- **Lower peak memory in batch translation**: the batch pipeline no longer makes a redundant full-resolution copy of every page before rendering — pages are rendered directly from the loaded bitmap, freeing roughly 23-93 MB per page (up to ~280 MB across the 3 concurrent pages) while a batch runs.
- **Local OCR batches are ~50% larger**: the on-device OCR chunk size is raised from 6 to 12 bubbles per LLM request, halving the number of text-translation API calls for OCR mode (the request pacing from the rate limiter is unchanged).
- **No more dead 500 ms wait between OCR batches**: the extra fixed delay after every OCR batch was removed; the rate limiter already enforces the minimum request interval.

### Fixed

- **Unscaled crop bitmaps are now recycled**: the single-image translation path freed the scaled crop copy but let the original-size crop become garbage — it now recycles it immediately, matching the batch path and keeping the memory spike flat on bubble-heavy pages.
- **Potential OpenCV Mat double-release**: when outside-bubble masking is disabled, the same Mat could be released twice (corrupting its reference count). A guard now releases the mask Mat only when it is a separate object.

## [v1.30.3] - 2026-08-11

### Added

- **Instant in-app PDF reader**: translated PDFs now open in a new lazy reader that renders only the pages on screen via Android's built-in `PdfRenderer` — no more waiting for the whole document to be rasterized to disk first. Includes pinch-to-zoom (1x–4x), webtoon mode, a page counter, and sharing the PDF. Wired into both the History tab and the Translate result preview.
- **Save feedback in the touch-up editor**: the Save button now shows a "Saving…" spinner and blocks dismissal while edits are being written, then confirms with a toast — and reports failure honestly instead of silently closing.
- **Persistent theme**: dark mode, pure black, and the accent color are now saved to settings and restored on the next launch (previously they reset at every app start).

### Changed

- **Smart Image Upscaler no longer distorts pages**: PDF page rasterization now caps resolution with one uniform scale factor so the page aspect ratio is always preserved. Previously the 2048px cap was applied to width and height independently, which stretched or squished large pages — most visible when the upscaler doubled translated PDF pages.
- **Saved edits show up immediately**: the reader reloads the edited page from disk right after saving, instead of showing the stale cached image until the page was swiped away and back.
- **Touch-up editing for auto-split (landscape) pages**: bubble metadata from every split part is merged into the recombined page, so the editor works on wide images too. Intermediate bitmaps are now recycled (small memory leak fixed).
- **Local OCR falls back to the vision LLM**: when ML Kit recognizes no text in a chunk, that chunk is sent to the image-capable provider chain instead of being dropped — a page no longer fails outright just because OCR found nothing.

### Fixed

- **Reader crash on app exit during a background translation**: the ViewModel no longer recycles bitmaps the background worker may still be using — the retry cache is now owned and cleared by the worker itself.
- **Webtoon mode stuck (unable to scroll)**: tapping the toolbar no longer hijacks the drag gesture, and each page reserves its aspect-ratio space while decoding so the list scrolls smoothly.
- **History reader showing a single page**: sibling pages with pure-numbered names (`001`, `002`, …) or ` (1)` name collisions are now grouped into one reader session instead of opening one isolated page.
- **Glossary UI jank and lost terms**: glossary file I/O moved off the main thread and mutations are serialized, so rapid add/remove can no longer drop or overwrite existing terms.

## [v1.30.2] - 2026-08-09

### Added

- **Foreground-service download**: the update download now runs in a dedicated foreground service (`dataSync` type, same pattern as the translation worker), so it survives app backgrounding / swipe-from-recents and no longer aborts mid-way. Progress keeps streaming to the notification shade and the dialog reflects the live state when the app is reopened.
- **Speed + ETA in the UI**: the update dialog and notification now show the transferred amount, download speed, and estimated time remaining (e.g. `12.3 / 110 MB · 0.3 MB/s · ~5 min`), reported every 1% or every 500 ms — so slow downloads never look frozen.

### Changed

- **Resumable downloads (HTTP Range)**: partial files are stored under a stable name (`kzkt-update-<version>.apk`) and resumed from the last byte via the `Range` header (server replies `206 Partial Content`). If a transfer is interrupted, the next attempt continues where it left off instead of restarting from zero.
- **Separate download timeout**: the download client now uses a 120-second read timeout instead of the 15-second API timeout, so short network stalls no longer kill the transfer.
- **Automatic retry**: interrupted downloads retry up to 3 times with escalating backoff (2 s / 4 s), each attempt resuming from the partial file. User cancellations are respected and never retried.

## [v1.30.1] - 2026-08-09

### Fixed

- **Startup update check no longer pops up a dialog**: the launch-time auto-check now runs fully in the background and only shows the update dialog when a newer version is actually available (previously a "Checking for updates…" dialog appeared at every app launch). Manual checks from Settings keep their spinner + feedback.
- **Stuck "Checking for updates…" dialog**: when the background check found nothing new (or failed), its state was never reset and the spinner dialog stayed on screen indefinitely — it now clears silently. A dedicated concurrency guard also prevents overlapping auto/manual checks.

## [v1.30.0] - 2026-08-08

### Added

- **Update download notification**: the in-app self-update now mirrors its download progress (percent + progress bar) in the notification shade via a dedicated low-importance channel, so progress stays visible even if the app is backgrounded. No-ops cleanly when notification permission is off.

### Changed

- **Release descriptions now come from `CHANGELOG.md`**: the CI release workflow extracts this version's section from this file as the release body (instead of auto-generating notes from commit history), so every release page reads exactly like the changelog.

### Fixed

- **White update dialog**: the update popup ignored the app theme (rendered with the default light scheme) because it was composed outside `KzktTheme` — it now follows dark/light mode and dynamic Material You colors.
- **Raw markdown in release notes**: the update dialog now renders release-note markdown properly (headers, bullet lists, bold, inline code, links) via a lightweight in-app renderer instead of showing literal `##` text.

## [v1.25.1] - 2026-08-08

### Added

- **Self-Update via GitHub Releases**: `UpdateManager` checks the public `releases/latest` endpoint on app launch (toggle in Settings, ON by default) and via a "Check for Updates" button. It picks the APK matching the device ABI (arm64-v8a → armeabi-v7a → x86_64 → x86, universal fallback), downloads it with live progress, and opens the system installer through FileProvider (`REQUEST_INSTALL_PACKAGES`).
- **Auto Version Bump**: CI release workflow now takes a version input (e.g. `1.25.2`) and passes `-PversionName`/`-PversionCode` to Gradle — `versionCode` is derived automatically from the name (`1.25.2` → `12502000`), so releases no longer require editing `build.gradle.kts`.
- **Per-ABI Release APKs (ala Komikku)**: `assembleRelease` now splits output into one APK per ABI (arm64-v8a, armeabi-v7a, x86, x86_64) plus a universal APK; `-PabiFilter=<abi>` builds just one variant.
- **Multi-Job CI Matrix**: `.github/workflows/kzkt.yml` restructures the release pipeline into `prepare → build (5 parallel ABI jobs) → create release` — with the arrow flowchart on the Actions page and a Telegram notification containing clickable links.
- **AGENTS.md**: AI-agent instruction file so external AI tools follow the same build/verify/sign/send workflow as this project's maintainers.

### Changed

- **CI artifact naming**: debug APK artifact path fixed after per-ABI splits (`app-arm64-v8a-debug.apk` / `app-universal-debug.apk`).
- **GitHub Release flow**: releases are published immediately (no draft), with APK assets + `sha256sums.txt`.

### Fixed

- **WorkManager 10 KB limit crash**: importing a folder with many images threw `IllegalStateException: Data cannot occupy more than 10240 bytes when serialized` because the whole file list was passed as WorkManager input data. The list is now written to a JSON file in `cacheDir` and only its path is passed; the worker reads + deletes it, with a size-safe fallback and a stale-file sweep for orphans.
- **Telegram notification newlines**: `\n` was sent as literal text — now built with `printf` so links render on their own clickable lines.

## [v1.25.1.22] - 2026-08-07

### Added

- **Batch Edit in Touch-up Editor**: New palette-icon button opens a Batch Edit dialog — Find & Replace text across every bubble, and apply Bold / Italic / Alignment / Font Size to all bubbles at once.
- **In-Provider Model Failover**: New `ProviderFactory` builds the fallback chain — if the primary model fails or is rate-limited, alternate models of the *same* provider are tried first, then the other configured providers.
- **CBZ Export Preserves Folder Structure**: `createCbz` now writes entries relative to the deepest common parent directory, keeping chapter/folder layout intact inside the archive.
- **Share Multiple Images**: `ACTION_SEND_MULTIPLE` intent-filter + `ClipData` fallback in `MainActivity`, so the app accepts multi-image shares from galleries/file managers.
- **"Test API Key & Connection" Button**: Settings health-check that sends one tiny request through the selected provider and reports latency/error inline — validates what you are *typing*, not the debounced saved value.
- **Pick Folder Input**: SAF tree picker that recursively imports every image inside a folder (sorted), opening whole manga chapters in one tap.
- **Themed App Icon**: Adaptive icon + `anydpi-v33` monochrome layer (Material You themed icon on Android 13+).
- **Translate Sound Effects (SFX) Mode**: Settings toggle that instructs the LLM to translate onomatopoeia (ドドド → "DOKO DOKO") instead of skipping them — applied to both vision-LLM and Local OCR paths.
- **Full Backup & Restore**: New `BackupManager` exports settings, glossary, history and the translation cache into one JSON file, shareable via KDE Connect / cloud; restore overwrites everything with a confirmation dialog.
- **Auto-Detect Local OCR Script**: Removed the Latin/Japanese script selector — a single bundled ML Kit model (`gocrjapanese_and_latin`) now reads **both** Japanese and Latin text automatically.

### Changed

- **Release Signing Setup**: `app/build.gradle.kts` reads a git-ignored `keystore.properties` for a per-developer custom keystore, falling back to the local debug keystore when absent. `assembleRelease` now outputs a directly-signed `app-release.apk`.
- **Build Guide**: New `BUILD_RELEASE.md` (English) covering debug builds, release builds with custom keystores, signature verification and troubleshooting; `keystore.properties.example` added.
- **Repository Hygiene**: `.gitignore` rewritten (build outputs, keystores, IDE and local files); removed ~104 MB of unneeded tracked files (OpenCV test binaries, OpenCV javadoc, one-shot icon script, Python venv ignored).
- **Repository History Rewrite**: dropped the pre-Android Python desktop era (105 commits) so the repository starts at the native Kotlin/Compose Android port (2026-07-30) — a solo Android repo. Desktop-era tags were removed, `v1.1-beta` was re-pointed, and the commit hashes cited in this changelog were updated to their rewritten equivalents. The original full history is preserved in a local backup bundle.

### Fixed

- **Release crash — R8 + WorkManager**: `NoSuchMethodException: WorkDatabase_Impl.<init>` on every launch (v1.25.1.15). Fixed with Room `-keep <init>()` rules.
- **Release crash — Android 16 FGS**: `InvalidForegroundServiceTypeException: Starting FGS with type none` when pressing Translate (v1.25.1.17–18). Root cause was WorkManager 2.10.0 forwarding a `type=0` from the 2-arg `ForegroundInfo` — fixed with an explicit `FOREGROUND_SERVICE_TYPE_DATA_SYNC` in `ForegroundInfo` plus the manifest declaration.
- **Release crash — ML Kit two-client NPE**: `NullPointerException` inside `com.google.mlkit.vision.text.internal` (de-obfuscated via R8 mapping) when a second recognizer type was created. Now only ONE ML Kit client (Japanese + Latin model) is ever created, plus ML Kit keep rules in ProGuard.
- **History date-range filter did nothing**: The "Custom Date" button set state but never rendered a picker — a `DateRangePicker` dialog now opens (wrapped in a `Dialog`+`Card`, since `DateRangePickerDialog` no longer exists in material3 1.5.0-alpha25).
- **Filename collisions in `copyUriToCache`**: Two sources with the same display name (e.g. `001.jpg` from different folders during folder import / multi-share) silently overwrote each other — now deduplicated with numeric suffixes (`001_1.jpg`, …).
- **Translation memory ignored PDF/batch runs**: `processImageBatch` never checked nor saved the local cache — identical bubbles across pages (and PDFs) are now served free from the cache, mirroring the single-image path.
- **Custom provider had no failover**: `createFallbackProviders` now includes the Custom provider (when a base URL is configured).
- **History could point at deleted files**: when MediaStore does not expose a real `_data` path, the output is parked in app-external storage instead of returning the cache path that is deleted right after.
- **OCR all-bubbles-empty confusion**: ML Kit errors are now surfaced in the log (`ML Kit error: …`) instead of a misleading "(No text recognized)".

## [v1.25.1.14] - 2026-08-07

### Added

- **Custom Font Importer**: Integrated a custom font selection dialog supporting `.ttf` and `.otf` font imports with persistent state across translations.
- **EPUB Support**: Added support for selecting `.epub` comic/manga archives alongside CBZ and ZIP files directly from the UI.
- **CBZ Filename Retention**: The Manga Reader CBZ exporter now perfectly retains the original directory structures and CJK filenames instead of flattening into sequential numbers.
- **Webtoon Reader Mode**: Added vertical scrolling mode toggle (`LazyColumn`) in `MangaReaderDialog` for seamless Webtoon and Manhwa reading.
- **Smart Image Upscaler**: OpenCV Bicubic interpolation + Unsharp Masking enhancement filter to double resolution and sharpen low-res scan text before OCR/LLM detection.
- **Provider Cache Invalidation**: Automatic translation cache invalidation when switching LLM models or providers to prevent stale translations.
- **Undo/Redo in Editor**: Added state history stack to the Interactive Touch-up Editor, allowing users to Undo and Redo bubble edits seamlessly.
- **History Filters**: Added Language and Date Range filters to the History Screen for easier translation management.
- **Side-by-side View**: Introduced a toggleable split-screen mode in MangaReaderDialog to compare the original comic and the translated version side-by-side.

### Changed

- **Background Translation Resilience**: Migrated translation service from ForegroundService to WorkManager (`TranslationWorker`) to persist translation jobs and prevent OS from killing them prematurely.
- **Redesigned KZKT Icon**: Cleaned up the app icon to feature a large 'K' on the left and stacked 'z' and 't' on the right, maintaining a minimalistic white-on-transparent design.
- **Refined Reset Settings**: Relocated the "Reset Advanced Settings" action into the Advanced card with a proper confirmation dialog, explicitly excluding sensitive API credentials.
- **Long Filename UI**: Implemented text truncation (ellipsis) for selected filenames in the MainScreen to prevent UI push-down on lengthy manga titles.

### Fixed

- **Android Archive Extraction Bug**: Swapped `ZipInputStream` for `ZipFile` to bypass a core Android extraction bug affecting Data Descriptors, permanently fixing 0-byte extracted images.
- **CJK Filename Destruction**: Removed aggressive regex sanitization that was corrupting Japanese, Chinese, and Korean filenames during extraction and causing page overwrites.

## [v1.25.1.13] - 2026-08-01 → 2026-08-06

### Major UI Overhaul (2026-08-02)

Directly following the documentation commit that outlined the app's key features — **PDF support, 3-stage YOLO cascade, and zero memory leaks** (`4d13002`) — the whole interface was rebuilt around modern Material 3 (`1ba321f`, +2556/−671 lines across 20 files):

- **Modern Material 3 theme**: Material You seed-color theming (MaterialKolor) + M3 typography (`Theme.kt`/`Type.kt`), replacing the old custom theme.
- **Bottom navigation with History tab**: `MainScreen` rebuilt as a 3-tab bottom nav — **Translate / History / Settings** — using `navigation-compose`.
- **New History screen**: `HistoryRepository` (DataStore-backed JSON) + `HistoryScreen` with search, provider filter, and tap-to-preview bottom sheet.
- **Rebuilt component library**: `BottomSheet`, `Material3SettingsGroup`, `EmptyPlaceholder`, `Menu`, `IconButton`, `ChipsRow` — reused across all screens.
- **Settings redesigned**: `Material3SettingsGroup` cards with expandable sections and accent-color presets.
- **Fixed translation lag**: all snapshot-state writes marshaled to the main thread, non-animated `scrollToItem`, `derivedStateOf` scoping, keyed list items — eliminating jank while a translation runs in the background.
- **All UI strings translated to English**.

### Added

- **Full KZKT Rebrand**: Complete application rebranding to **KZKT** (`com.kzkt.app`), redesigned README layout, and aligned UI theme across all screens.
- **KZKT App Launcher Icon Redesign**: Updated app launcher icon across all screen densities (`mdpi` to `xxxhdpi`) featuring stylized handwritten ink-brush `kzkt` lettermark on a clean white background.
- **Consolidated Provider Configuration Card**: Dedicated per-provider card that isolates API Key, Base URL, Model Selection, and Model Detection for the selected provider.
- **Per-Provider Base URL Customization**: Individual Base URL settings for Gemini, OpenAI, OpenRouter, Zen, OpenCode Go, and Custom providers with a one-click reset button to default official endpoints.
- **Dynamic API Model Detection for All Providers**: Integrated "Detect Models from API" across all supported providers to dynamically query `/v1/models` and populate available models without fixed hardcoded lists.
- **In-App Manga & PDF Reader**: Fullscreen reader with `HorizontalPager`, pinch-to-zoom, pan, *Original vs. Translated* toggle switch, and live text touch-up editor.
- **Interactive Touch-up Editor**: Live text editing dialog directly accessible from the main translation card and History screen.
- **Verbose Developer Logs Toggle**: Settings switch to toggle between clean progress logging and detailed telemetry mode for technical debugging.
- **"Copy All Logs" Button**: Dedicated copy button on the `LogCard` header to instantly copy full execution logs to the clipboard.
- **Custom API Timeout Slider**: User-adjustable API timeout slider added to Settings.
- **On-Device Local OCR (Google ML Kit)**: Integrated local OCR (Japanese & Latin) to pre-extract text before sending to non-vision LLM endpoints.
- **Reasoning LLMs Support (DeepSeek-R1)**: Automatic stripping of `<think>...</think>` tags and extended 90s read timeout for reasoning models.
- **Expanded Multi-Provider Support**: Standalone text translation API support for `ZenProvider`, `OpenCodeGoProvider`, and `OpenRouterProvider`.
- **Smart OCR Typo Correction**: Prompt instructs the LLM to auto-correct OCR noise/typos using context before translating Local OCR text.
- **Pencil Edit on Every Reader Page**: Touch-up editing enabled on all in-app reader pages (Main preview + History), not just fresh results.
- **Android CI Workflow**: Added GitHub Actions CI configuration (`JDK 17`, `assembleDebug`).
- **Git LFS**: YOLO ONNX model (`kzkt.dat`) tracked via Git LFS with README prerequisites.

### Changed

- **Optimized Default Settings**: Updated application default configuration to 30 bubbles/request (`maxBubblesPerRequest = 30`), 2.0s minimum delay (`minRequestDelay = 2.0s`), and 30s request timeout (`customTimeoutSec = 30s`).
- **Clean Settings UI Layout**: Removed redundant stacked API Key lists and duplicate Base URL sections for a streamlined, clutter-free configuration screen.
- **Background Translation Service**: Runs the translation pipeline inside a Foreground Service with dynamic status bar progress notifications and automatic retry timers.
- **PDF Memory Optimization (6-Page Grouping)**: PDF page extraction and processing grouped into 6-page chunks with eager bitmap recycling to cap peak RAM consumption.
- **HTTP Connection Pooling & Fast Request Delay**: Enabled persistent HTTP connection pooling and reduced inter-request delay to 0.1s for ultra-fast text translation.
- **Scoped Storage & MediaStore**: Direct output routing to the public `/Download/KZKT/` folder via MediaStore API for Android 10+ compatibility.
- **Core Audit Cleanup**: Removed dead code, unified the OpenAI-compatible providers, fixed the retry cache, and corrected the Gemini base URL handling.
- **History/Settings Performance**: Eliminated recomposition storms and main-thread disk I/O that caused frame drops while scrolling.

### Fixed & Performance

- **PDF Fault Tolerance**: PDF processing continues through subsequent pages even if individual batches or pages encounter network/API errors.
- **Local OCR Early Abort**: Automatically aborts remaining local OCR batches early if 2 consecutive batches fail across all configured providers.
- **Local OCR Micro-Batching**: Text requests chunked to max 6 bubbles per batch to prevent LLM token timeouts in PDF mode.
- **Custom Provider Endpoint Fixes**: Eliminated duplicate `/v1/v1` path bug and aligned CustomProvider payload structure with standard OpenAI Chat API specs.
- **Unblocked Reader Gestures**: Replaced `pointerInput` with `combinedClickable` in the image viewer to allow unblocked `HorizontalPager` swipes.
- **120 FPS Scrolling Optimization**: Hoisted History state into `StateFlow` and cached `SimpleDateFormat` instances to eliminate scroll frame drops.
- **Tolerant Streaming JSON Parser**: Fallback `JsonReader` streaming parser to handle duplicate keys in LLM JSON responses without failing the batch.
- **Background YOLO Initialization**: Offloaded ONNX model decryption and loading to `Dispatchers.IO` to keep the UI main thread responsive at startup.
- **YOLO Header Validation**: Corrected ONNX header validation so the bundled model actually loads on release builds.
- **MediaStore Output Fixes**: Resolved `IllegalArgumentException` when saving standalone image outputs on Android 10+ (plus an ENOENT fallback resolution).

## [v1.0.0] - 2026-07-xx

### Added

- **In-App Fullscreen Image Viewer** with pinch-to-zoom & pan gestures (`96dd00c`).
- **Quick Action buttons** to open translated results in the System Gallery or share via social media (`96dd00c`).
- **Official app launcher icon** at every Android screen density (mdpi to xxxhdpi) (`1f39e00`).

### Fixed

- **Translation cancellation delay**: pressing Cancel now stops the coroutine and network request instantly — no longer waits for the batch to finish (`37df21b`).
- **Custom LLM remote endpoint compatibility**: forced `stream: false` and added dynamic JSON parsing for Ollama, LM Studio, vLLM, and Cloudflare/Ngrok tunnels (`343619a`).
- **PhotoPicker synthetic-path crash on Android 13+**: output files routed directly to public `/Download/` (`5f5fb24`).
- **OpenCV JNI `JNIEnv` library loading on release builds**: uses uncompressed legacy packaging (`5f5fb24`).
- **JSON parsing errors on non-standard LLM responses**: lenient Gson parsing (`b35de92`).
- **Released native Mat/ONNX resources**, 3-stage YOLO cascade, bubble-sized overlay (`d3be474`).
- **PDF input/output** via built-in `PdfRenderer`/`PdfDocument`, shared render path (`4d3ff98`).
- **Theme color** changed from purple to a modern Light Blue (Sky Blue) (`ad8dae0`).

---

*Full history: see `git log --oneline`.*
