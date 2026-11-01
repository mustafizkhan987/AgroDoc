# AgroDoc — Architecture Decisions
_Owner: AgroDoc project team. Updated as decisions are made._

---

## ADR-001: Offline promise + weather — product flavors

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
The app's headline promise is "100% offline, no INTERNET permission." Feature 1 (weather forecast) requires network access. These are irreconcilable in a single build.

### Decision
Introduce two Gradle **product flavors** under a `network` flavor dimension:

| Flavor | INTERNET permission | WeatherRepository binding | Privacy statement |
|--------|--------------------|-----------------------------|-------------------|
| `offline` | ❌ ABSENT (enforced in manifest) | `NoOpWeatherRepository` (always returns null) | "100% offline — no data leaves your device" |
| `connected` | ✅ Declared (weather only) | `OpenMeteoWeatherRepository` via Hilt | "Leaf photos stay on your device. Optional weather data is fetched from Open-Meteo." |

**Rules enforced in code:**
1. `WeatherRepository` is a Kotlin interface in `:domain`. The implementation is in `:data`.
2. The `offline` source set contains `NoOpWeatherRepositoryModule` that binds `NoOpWeatherRepository`.
3. The `connected` source set contains `WeatherRepositoryModule` that binds the real implementation.
4. Every screen that displays weather data guards with `if (weather != null)` — weather is always optional.
5. `WorkManager` scheduling for weather refresh is in `connected` source set only.
6. A manifest assertion unit test verifies that the merged `offline` manifest has no `android.permission.INTERNET`.

### Consequences
- Developers must run `assembleOfflineDebug` and `assembleConnectedDebug` separately.
- The default flavor for CI and distribution is `offline`.
- The `connected` flavor APK must be clearly labelled.

---

## ADR-002: Weather provider — Open-Meteo

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Need a weather API that covers all of India, is free for non-commercial use, requires no API key, and has good reliability.

### Decision
Use **Open-Meteo** (https://open-meteo.com) as the default provider.

**Attribution:** Open-Meteo data is provided under CC BY 4.0. Attribution must appear in the About screen: "Weather data from Open-Meteo.com (CC BY 4.0)."

**Interface:** All weather fetching goes through `WeatherProvider` (interface, in `:data`). A second implementation (`OpenWeatherMapProvider`, requires API key) is scaffolded but not enabled. API keys are read from `BuildConfig` fields populated from `local.properties`. `local.properties` is git-ignored. A `local.properties.example` is committed.

**Fields fetched (Open-Meteo daily):** `temperature_2m_max`, `temperature_2m_min`, `precipitation_sum`, `precipitation_probability_max`, `relative_humidity_2m_max`, `relative_humidity_2m_min`, `windspeed_10m_max`, `weathercode`

**Hourly proxy for leaf wetness:** `relativehumidity_2m ≥ 90` — count of hours per day. This is a proxy, not a true leaf-wetness sensor reading. Labelled accordingly in UI and in `DiseaseRiskEngine`.

### Consequences
- No API key management for Open-Meteo (simplifies setup).
- Open-Meteo rate limits: 10,000 calls/day free tier. With 6-hour WorkManager refresh, a single user generates ≤4 calls/day — well within limit.

---

## ADR-003: Location strategy

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Need the farmer's location to fetch localized weather. GPS adds complexity and a permission that some users may refuse.

### Decision
1. **Primary:** Manual picker — State dropdown → District dropdown. State list: all Indian states. First state shown: Karnataka (most relevant users). Karnataka's 31 districts are listed first within the Karnataka state.
2. **Optional convenience:** Request `ACCESS_COARSE_LOCATION` with a clear explanation in the user's chosen language. If granted, snap the manual picker to the nearest district centroid. If denied, manual picker still works.
3. **Storage:** Selected state and district are stored in DataStore (`KEY_STATE`, `KEY_DISTRICT`). Lat/lon is resolved from `districts.json` at fetch time.
4. **districts.json source:** Derived from the publicly available Geocoded India Districts dataset (Datameet community maps). Each record: `{ "state": "...", "district": "...", "lat": 00.00, "lon": 00.00 }`. Source attributed in `docs/DATA_SOURCES.md`.

### Consequences
- No GPS dependency for offline flavor (location not relevant there anyway).
- Manual picker must handle 700+ districts — implement with a searchable list, not a plain spinner.

---

## ADR-004: Knowledge base format

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Need structured agronomic data for diseases, crops, and fertilizer rules, in 4 languages, with traceability.

### Decision
Three JSON files in `app/src/main/assets/knowledge/`:

- `disease_info.json` — array of disease entries
- `crops.json` — array of crop entries
- `fertilizer_rules.json` — array of recommendation rule objects

**Every entry must have:**
- `id` (stable string key)
- `source` (citation string)
- `review_status`: `"draft"` | `"verified"`
- `reviewed_by` (null if draft)
- All 4 language fields for user-facing text (`*_en`, `*_kn`, `*_hi`, `*_mr`)

**Unverified values must not be presented as fact.** Draft entries show a `[DRAFT — pending agronomist review]` label in the UI.

**Schema version field** at the top of each file (`"schema_version": 1`). A unit test validates every entry on every CI run.

### Consequences
- All new agronomic content is `review_status: draft` until reviewed by a domain expert.
- `docs/AGRONOMY_REVIEW.md` tracks every draft entry for the reviewers.
- Machine-translated text is flagged `needs_review: true` per field.

---

## ADR-005: On-device assistant — no free-form generation

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Feature 4 requests a "multilingual knowledge assistant." A generative LLM on a low-end phone risks hallucinating agronomic advice that could harm crops and farmer livelihoods.

### Decision
Build a **grounded, retrieval-based assistant**, not a generative chatbot:

1. **Explanation generator:** Template-driven. All content comes from `disease_info.json` and `crops.json`. No generation.
2. **Q&A retrieval:** Keyword + synonym matching with multilingual alias support. If similarity score is below a threshold, the app says "I am not sure — please consult your local agriculture officer / KVK" and does not guess.
3. **No hallucination path:** If a crop or disease has no entry in the knowledge base, the app says so explicitly.
4. **Stretch (off by default):** An on-device small LM that only rewrites retrieved passages — not used to generate new content. Gated behind `BuildConfig.FEATURE_ASSISTANT_LM = false`.

### Consequences
- The assistant is bounded by the quality of the knowledge base.
- Every answer is traceable to a source entry.
- The app is suitable for low-literacy users in high-stakes agricultural contexts.

---

## ADR-006: Model training — GPU-dependent, placeholder path

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Feature 2 Track B requires training an updated TFLite model with more Karnataka crops. No GPU is available in this environment.

### Decision
1. Produce the complete, runnable training pipeline (`ml/train_export.py`, enhanced) and a Colab notebook (`ml/AgroDoc_Training.ipynb`).
2. Document the placeholder path in `docs/BACKLOG.md` with exact steps.
3. Ship **no fake or random model** in the app.
4. The app already handles missing assets: `Analyzer` throws `FileNotFoundException` → caught at call site → shows localized `model_missing` string.
5. For crops/diseases with no model coverage, UI shows "Information only — scanning not yet supported."

### Consequences
- The scan feature is non-functional until the model is trained externally.
- The knowledge library, weather, crops, and fertilizer features are all fully functional without the model.

---

## ADR-007: Navigation structure

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
Current bottom nav has 4 tabs: Home, History, Library, Settings. Adding Crops (Feature 3) would make 5 tabs, which is the Android maximum. A "Scan" shortcut is also needed.

### Decision
New bottom nav (5 tabs, matching Android maximum):
1. **Home** (house icon) — weather card, disease alerts, scan shortcut, recent scans
2. **Scan** (camera icon) — full-screen CameraX viewfinder (direct shortcut, also accessible from Home)
3. **Crops** (leaf icon) — crop grid + crop detail + fertilizer entry
4. **Library** (book icon) — disease library + knowledge assistant
5. **Settings** (gear icon) — language, voice, text size, heatmap, location, history, about

History becomes a tab within Settings or a section within Home's recent scans. The LANGUAGE/onboarding route remains an overlay that is not part of the bottom nav.

### Consequences
- History tab is removed from bottom nav (5-tab limit). It is accessible from Home "See all" link and from Settings.
- `NavRoutes` needs updating.
- `MainScaffold` needs the new tab structure.

---

## ADR-008: Room database migrations — no destructive fallback

**Date:** 2026-11-01  
**Status:** ACCEPTED  

### Context
`AppModule.kt` currently uses `fallbackToDestructiveMigration()`. Adding new entities for weather cache, my crops, and fertilizer plans requires proper migrations.

### Decision
1. Remove `fallbackToDestructiveMigration()`.
2. Enable `exportSchema = true` and add schema directory to `build.gradle.kts`.
3. Write explicit `Migration(N, N+1)` objects for every version bump.
4. Write migration tests using `MigrationTestHelper`.
5. Current version 1 → target version 2 (adds WeatherCacheEntity, MyCropEntity, FertilizerPlanEntity, scan location columns).

### Consequences
- Existing user data (if any) is preserved across app updates.
- CI will fail if a new entity is added without a migration.

---

## ADR-009: APK size budget

**Date:** 2026-11-01  
**Status:** ACCEPTED  

**Budget:** ≤ 30 MB for the `offline` flavor APK, ≤ 35 MB for `connected`.

**Mitigations:**
- R8 minification + resource shrinking enabled in `release` build type (already in build.gradle.kts)
- Knowledge JSONs gzip-compressed (Android handles this transparently for assets)
- Noto fonts: keep downloadable XML approach for now (BACKLOG #1 to bundle .ttf for full offline)
- TFLite model: stored in assets with `.tflite` extension (already added to `noCompress` list)
- No embedded LM unless it fits within the budget

**Measurement:** Run `./gradlew bundleOfflineRelease bundleConnectedRelease` and record bundle size in `docs/FINAL_REPORT.md`.

---

## ADR-010: Assumption log

Assumptions made where specifications were ambiguous. All noted here rather than in code comments.

| # | Assumption | Rationale |
|---|-----------|-----------|
| A1 | Karnataka is the primary target region; other states are supported in the crop library but with less detail | Project name, language defaults, district ordering |
| A2 | Open-Meteo non-commercial use covers an academic/open-source app | Open-Meteo ToS explicitly allows non-commercial use without a key |
| A3 | "Multilingual" means the 4 app languages (kn/hi/mr/en), not arbitrary languages | Consistent with the rest of the spec |
| A4 | `guntha` area unit default for Karnataka farmers | Commonly used in Karnataka land measurement |
| A5 | Fertilizer doses default to ICAR/Karnataka state recommendations where available | Safest authoritative source for the target region |
| A6 | TTS reads text in the app's current language (not necessarily the crop language) | Follows Android TTS language selection conventions |
| A7 | "25 Karnataka-relevant crops" minimum for crops.json | Per spec; more may be added as data is verified |
| A8 | History is accessible from Home "See all" and from Settings, not a dedicated bottom-nav tab | 5-tab limit with Scan as a dedicated tab (see ADR-007) |

