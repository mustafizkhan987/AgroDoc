# AgroDoc — Pre-Implementation Analysis
_Generated: 2026-11-01. Updated as implementation progresses._

## Current repository audit — 2026-11-01

The older baseline below describes an earlier project state and is not an accurate inventory of the current tree. The user supplied a later progress report; I compared its claims against the present source before continuing.

### Present
- Offline/connected product flavors, flavor-specific manifests and weather bindings are present. Connected weather uses Open-Meteo through `WeatherRepository` and persists forecasts in Room. Open-Meteo does not require an API key; `openweather.api.key` in Gradle is currently unused.
- Room v2 entities/migration, district/crop/disease/fertilizer JSON assets, repository loader, domain engines, and split ML classes are present.
- Home and crop browsing screens exist. The crop list was not previously wired into `AgroDocNavGraph`; crop-detail, disease-library/detail, assistant, and fertilizer screens have now been added and their routes connected.
- All 4 string resource files exist, but the newly added screens still contain English literals; localization parity in resources does not mean full UI localization.

### Still incomplete / needs verification
- CameraX capture and gallery selection now call the on-device model runner; results show predictions, confidence, heatmap blend, grounded disease guidance, optional cached-weather context, and a TTS attempt with missing-voice fallback. Scans and photo artifacts are stored locally in Room/app-private storage and exposed in History. Settings cover language, text size, voice, heatmap preference, manual location, and current location in the connected flavor.
- Connected builds can use an approximate device location, which sends only latitude/longitude to the Open-Meteo weather provider. A manual state/district selector is also available. The bundled 84-district list is not exhaustive; device location is the all-India path.
- The report says TFLite assets are not bundled; `app/src/main/assets` has only knowledge JSON files. Real image diagnosis and heatmap output therefore cannot be completed until model artifacts exist.
- Both variants now compile and assemble, and both unit-test tasks pass. Seven tests run in each variant for trust scoring, crop suitability, and weather disease-risk rules. Physical-device/emulator and accessibility review remain outstanding.
- Several labels on the added screens remain English literals despite localized data/resource tables; complete native-language UI localization is still needed.
- Agronomy JSON contains citations and review metadata, but those claims have not been independently validated in this implementation pass. Verify doses, pesticide labels/PHI, local recommendations, and translations with qualified sources/reviewers before presenting them as approved advice.
- Do not treat a missing item in this snapshot as evidence that it was never implemented; re-check files immediately before each implementation phase.

---

## 1. Code vs. Project Summary Audit

### 1.1 File Inventory (Phase 0 baseline)

| File | Type | Status | Notes |
|------|------|--------|-------|
| `AgroDocApp.kt` | App | Real | Hilt entry point only |
| `MainActivity.kt` | Activity | Real | DataStore locale init, single Compose setContent |
| `data/db/AppDatabase.kt` | Room | Real | v1, exportSchema=false, one entity |
| `data/db/ScanEntity.kt` | Room Entity | Real | All columns present |
| `data/db/ScanDao.kt` | DAO | Real | insert, observeAll, observeByCrop, getById, delete, deleteAll |
| `data/db/Converters.kt` | Room | Real | TrustLevel ↔ String |
| `data/prefs/SettingsRepository.kt` | DataStore | Real | 5 keys: languageChosen, localeTag, voiceEnabled, textSize, heatmapStyle |
| `di/AppModule.kt` | Hilt | Real | Provides DB, DAO, SettingsRepository |
| `domain/model/Scan.kt` | Domain | Real | Full domain model (Bitmap-bearing) |
| `domain/model/ScanEntity.kt` | Data | Real | Path-based (no Bitmap) |
| `domain/model/Prediction.kt` | Domain | Real | classId, label, prob |
| `domain/model/TrustLevel.kt` | Domain | Real | Enum: HIGH, MEDIUM, LOW |
| `domain/model/DiseaseInfo.kt` | Domain | Real | Defined but **never populated** — no disease_info.json exists |
| `ml/Analyzer.kt` | ML | Partial | Full algorithm written; **crashes at init** if assets missing |
| `ui/navigation/NavRoutes.kt` | Nav | Real | 8 routes defined |
| `ui/navigation/AgroDocNavGraph.kt` | Nav | Real | Routes wired, Result optional arg fixed |
| `ui/navigation/MainScaffold.kt` | Nav | Real | 4-tab bottom bar works |
| `ui/screens/home/HomeScreen.kt` | UI | **Stub** | No ViewModel, no history row, gallery click is no-op |
| `ui/screens/camera/CameraScreen.kt` | UI | **Stub** | "Coming in Phase 3" text only — no CameraX |
| `ui/screens/result/ResultScreen.kt` | UI | **Stub** | "Coming in Phase 3" text only — no heatmap, no TTS |
| `ui/screens/history/HistoryScreen.kt` | UI | **Stub** | "Coming in Phase 5" placeholder |
| `ui/screens/library/LibraryScreen.kt` | UI | **Stub** | "Coming in Phase 5" placeholder |
| `ui/screens/settings/SettingsScreen.kt` | UI | **Stub** | Shows "Phase 5" + About button only |
| `ui/screens/about/AboutScreen.kt` | UI | Partial | Static text; metrics section is placeholder |
| `ui/screens/onboarding/LanguageScreen.kt` | UI | Real | Language picker works |
| `ui/screens/onboarding/LanguageViewModel.kt` | ViewModel | Real | DataStore write + locale apply |
| `ui/theme/Theme.kt` | Theme | Real | Light colors only |
| `ui/theme/Type.kt` | Theme | Real | Noto Sans Kannada/Devanagari downloadable fonts |
| `ml/train_export.py` | ML | Real | Complete training + export pipeline; GPU-dependent |
| `app/src/main/assets/` | Assets | **EMPTY** | No TFLite model, no labels.json, no cam_weights.bin |
| `docs/BACKLOG.md` | Docs | Real | 10-item backlog tracked |
| `docs/TRANSLATION_REVIEW.md` | Docs | Real | 19 key strings tabled for native speaker review |

**ARCHITECTURE.md referenced in rules but does not exist.** The `.agent/rules/agrodoc.md` rule file references it.

---

### 1.2 Analyzer.kt — Deep Read

`Analyzer` is a class initialized with `Context`. On construction it:
1. Memory-maps `agrodoc_model.tflite` from assets via `FileInputStream + FileChannel.map` → `Interpreter`
2. Reads `labels.json` (JSONArray) and creates a cleaned `List<String>` (replaces `___` with ` - `, `_` with ` `)
3. Reads `cam_weights.bin` as raw bytes → `FloatArray` (float32, little-endian)

`analyze(Bitmap)` does:
1. Centre-crops bitmap to square, scales to 224×224
2. Packs raw uint8 RGB values as float32 input buffer (the model contains a `Rescaling(1/127.5, -1)` layer)
3. Runs multi-output inference (output 0: `[1, N]` logits; output 1: `[1, 7, 7, 1280]` feature map)
4. Softmax over logits; picks top-1 class `c` and margin `p[c] - p[second]`
5. Computes 7×7 CAM: `cam[i][j] = ReLU(Σ_k feats[i][j][k] * W[k*N + c])`
6. HSV leaf mask per pixel: hue 20–170°, saturation >0.25, value >0.15
7. Averages CAM mass over 32×32-pixel cells; computes `focus = Σ(cam_v * leafCell) / Σ(cam_v)`
8. `area = leafPx / (224*224)`
9. Trust rules: LOW if focus < 0.45 || p[c] < 0.50 || area < 0.15; HIGH if focus ≥ 0.70 && p[c] ≥ 0.85 && margin ≥ 0.30; else MEDIUM
10. Renders 7×7 HSV heatmap scaled to 512×512 (alpha = `v*190`, hue = `(1-v)*240`)
11. Returns `ScanResult` (legacy class, separate from domain `Scan`)

**Known issues in Analyzer.kt:**
- `ScanResult` is a legacy class that duplicates `domain/model/Scan` — needs migration
- `cam_bias.bin` is in BACKLOG (#8) but not used in code — `Analyzer` uses weights only (no bias), consistent with GAP+Dense
- `interp.outputTensorCount` is used to detect output indices dynamically — fragile if tensor order changes
- Cell leaf computation uses integer division `y/32` and `x/32` — correct for 224/7 ≈ 32
- No error handling for missing asset files; `FileNotFoundException` will crash the app

---

## 2. Build Health

### Gradle Build (Phase 0)
- `./gradlew assembleDebug`: **BUILD SUCCESSFUL** (verified)
- `./gradlew lintDebug`: 0 errors, 111 warnings (69 UnusedResources, 16 GradleDependency, 22 Typos, 1 UnusedAttribute, 1 LockedOrientation, 1 DiscouragedApi, 1 DataExtractionRules)
- `./gradlew testDebugUnitTest`: **NO-SOURCE** (no unit tests exist)
- `./gradlew installDebug` + launch on emulator: **PASS** — app runs, bottom nav works, no crashes

### No product flavors defined (confirmed)
The `app/build.gradle.kts` has no `flavorDimensions` or `productFlavors` block.

---

## 3. Conflict with Offline Promise

The **headline promise** is "100% offline, no INTERNET permission." This is architecturally enforced via the manifest.

**Weather (Feature 1) directly conflicts with this promise.** Fetching weather forecasts requires:
- `INTERNET` permission
- `ACCESS_NETWORK_STATE` permission
- Background WorkManager scheduling

**Resolution (per DECISIONS.md):** Two Gradle product flavors — `offline` (keeps the original promise) and `connected` (adds weather capabilities, declares INTERNET only for WeatherRepository). All networking is behind a `WeatherRepository` interface; the `offline` flavor binds a `NoOpWeatherRepository` that returns null/empty. Weather is optional everywhere.

---

## 4. Gap Analysis — New Features

### Feature 1: Weather Integration
| Item | Gap | Effort |
|------|-----|--------|
| Gradle product flavors | Not present | M (4h) |
| WeatherRepository interface + NoOp impl | Not present | S (2h) |
| Open-Meteo client (connected flavor) | Not present | M (4h) |
| districts.json (full India, state+district+lat+lon) | Not present | M (6h data prep) |
| Manual location picker (State → District) | Not present | M (4h UI) |
| WeatherCacheEntity + Room migration | Not present | S (2h) |
| WorkManager weather refresh | Not present | M (3h) |
| Weather card + 7-day strip UI | Not present | M (5h) |
| DiseaseRiskEngine (pure Kotlin, unit tested) | Not present | L (8h) |
| Disease risk conditions in disease_info.json | Not present | L (10h data authoring) |
| Disease Alerts card on Home | Not present | M (3h) |

### Feature 2: Karnataka Disease Expansion
| Item | Gap | Effort |
|------|-----|--------|
| disease_info.json schema + validator | Not present | M (3h) |
| Knowledge entries (35+ diseases × 4 languages) | Not present | XL (20h+ data authoring) |
| Train pipeline enhancements (augmentation, field images) | Partial (train_export.py exists) | L (8h) |
| Per-class metrics, confusion matrix, calibration | Not present | M (4h) |
| Colab notebook | Not present | M (3h) |
| "Information only" label for model-less crops | Not present | S (1h) |

### Feature 3: Crop Category
| Item | Gap | Effort |
|------|-----|--------|
| crops.json schema + validator | Not present | M (3h) |
| Crop data (25+ Karnataka crops × 4 languages) | Not present | XL (20h+ data authoring) |
| Crops screen (grid, filter chips, search) | Not present | L (8h UI) |
| Crop detail screen | Not present | L (6h UI) |
| "My Crops" DataStore / Room | Not present | S (2h) |
| Suitability check rule engine | Not present | M (4h) |

### Feature 4: Multilingual Knowledge Assistant
| Item | Gap | Effort |
|------|-----|--------|
| Explanation generator (template engine) | Not present | L (8h) |
| Offline Q&A retrieval (keyword + synonym match) | Not present | L (10h) |
| "Why this result?" button on Result screen | Not present | S (1h) |
| On-device TTS wiring (KN/HI/MR voice check) | Not present | M (4h) |

### Feature 5: Fertilizer Recommendation
| Item | Gap | Effort |
|------|-----|--------|
| fertilizer_rules.json schema + validator | Not present | M (3h) |
| Fertilizer rules data (25+ crops, sourced) | Not present | L (10h data authoring) |
| FertilizerAdvisor engine + unit tests | Not present | L (8h) |
| FertilizerPlanEntity + Room migration | Not present | S (2h) |
| Fertilizer screen UI | Not present | L (6h) |

### Roadmap items (Phases 2–5, carried from backlog)
| Item | Gap | Effort |
|------|-----|--------|
| Phase 2: ModelRunner / CamGenerator / TrustScorer split | Partial (Analyzer exists) | M (4h) |
| Phase 2: Trust calibration + unit tests | Not present | M (4h) |
| Phase 3: CameraX live viewfinder | Not present | L (8h) |
| Phase 3: Gallery picker | Not present | M (3h) |
| Phase 3: Blur/darkness quality feedback | Not present | M (3h) |
| Phase 4: Heatmap overlay slider (Result screen) | Not present | M (4h) |
| Phase 4: TTS on Result screen | Not present | M (3h) |
| Phase 5: Room-backed history with swipe-to-delete | Not present | M (4h) |
| Phase 5: Library from disease_info.json | Not present | M (4h) |

**Total estimated effort: ~180–220 engineering-hours**

---

## 5. Risk Register

| Risk | Severity | Likelihood | Mitigation |
|------|----------|------------|------------|
| **Model accuracy on real field photos** | HIGH | HIGH | PlantVillage images are lab-clean; field photos have background clutter, variable lighting, EXIF rotation. Augmentation partially compensates. Mandatory Trust score already flags low-confidence results. Document in limitations. |
| **Data licensing** | HIGH | MEDIUM | PlantVillage: non-commercial research (Penn State). Must verify before commercial use. PlantDoc: CC BY 4.0 (safe). New datasets must be checked per-source before inclusion. |
| **Agronomic correctness** | HIGH | MEDIUM | All disease thresholds, fertilizer doses, sowing windows must cite UAS Bengaluru/Dharwad, ICAR, or state extension docs. Machine-translated content must be flagged `review_status: draft`. No value without `source`. |
| **APK size** | MEDIUM | HIGH | TFLite model ~3–8 MB, Noto fonts via downloadable, knowledge JSONs ~2–5 MB, districts.json ~200 KB. Target <30 MB APK. Use R8 + resource shrinking. |
| **Low-end device performance** | MEDIUM | MEDIUM | Inference target: <1.5s. CAM computation is CPU-only on main thread in current Analyzer — must move to coroutine/dispatcher. Cold start target: <2s. |
| **Kannada/Marathi TTS availability** | MEDIUM | HIGH | System TTS may not have Kannada voice pre-installed. Must detect and show actionable fallback message. |
| **Weather API reliability** | LOW | MEDIUM | Open-Meteo is free and reliable. Cache last-good result in Room to handle offline. |
| **Missing ARCHITECTURE.md** | LOW | CERTAIN | File referenced in rules but missing. Create it as a summary of decisions. |
| **Downloadable fonts require GMS** | LOW | MEDIUM | Noto downloadable fonts need Google Play Services. For offline guarantee on non-GMS devices, bundle .ttf files (BACKLOG #1). |

---

## 6. String Keys Summary (Phase 0)

All 4 locales have exactly **84 string keys**, perfectly in sync. The 84 keys cover:
- App / language screen (5 keys)
- Navigation (4 keys)
- Home (4 keys)
- Camera (6 keys — currently **unused** in UI stubs)
- Result (10 keys — currently **unused** in stubs)
- Trust (8 keys)
- Explanation regions (9 keys + 1 template)
- History (7 keys)
- Library (2 keys)
- Settings (9 keys)
- About (6 keys)
- Disclaimer (1 key)
- Errors / status (5 keys)

**69 lint "UnusedResources" warnings** are expected — most camera/result/settings keys are pre-defined for upcoming phases and will be consumed when those screens are implemented.

---

## 7. Dependencies Audit

| Dependency | Version | Status |
|-----------|---------|--------|
| Compose BOM | 2024.10.01 | Outdated — 2026.09.00 available |
| `core-ktx` | 1.15.0 | Outdated — 1.19.1 available |
| `activity-compose` | 1.9.3 | Outdated — 1.13.0 available |
| `appcompat` | 1.7.0 | Outdated — 1.8.0 available |
| `lifecycle-*` | 2.8.7 | Outdated — 2.11.0 available |
| `navigation-compose` | 2.8.4 | Current for this stack |
| `hilt-android` | 2.53.1 | Current |
| `room-*` | 2.6.1 | Check — 2.7.x may be available |
| `datastore-preferences` | 1.1.1 | Current |
| `camera-*` | 1.4.1 | Current |
| `coil-compose` | 2.7.0 | Current |
| `litert` | 1.4.0 | Current |
| `kotlinx-coroutines-android` | 1.9.0 | Current |
| WorkManager | **NOT PRESENT** | Needed for Feature 1 weather refresh |
| Retrofit / OkHttp | **NOT PRESENT** | Needed for Feature 1 (connected flavor only) |
| kotlinx-serialization | **NOT PRESENT** | Needed for JSON parsing of weather / assets |

**No analytics, no crash reporting, no ad SDK present** — consistent with privacy promise.

---

_This analysis is the basis for `docs/DECISIONS.md` and the implementation plan._

